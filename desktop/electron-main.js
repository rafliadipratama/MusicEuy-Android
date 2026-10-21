const { app, BrowserWindow, ipcMain, session } = require('electron');
const path = require('path');
const https = require('https');
const des = require('des.js');

// Disable incompatible Vulkan on Linux Wayland to guarantee silky smooth rendering
app.commandLine.appendSwitch('disable-features', 'Vulkan');
app.commandLine.appendSwitch('disable-dev-shm-usage');

// In-memory stream cache for instant 0ms playback on repeated tracks
const streamCache = new Map();

// JioSaavn DES 320kbps Stream Decryptor (Key: 38346591, matches Android MusicStreamSource.kt)
function decryptSaavnMediaUrl(encryptedUrl) {
  try {
    const key = Buffer.from('38346591', 'ascii');
    const d = des.DES.create({ type: 'decrypt', key: key });
    const input = Buffer.from(encryptedUrl, 'base64');
    const out = Buffer.concat([Buffer.from(d.update(input)), Buffer.from(d.final())]);
    const pad = out[out.length - 1];
    const cleanBuf = (pad > 0 && pad <= 8) ? out.slice(0, out.length - pad) : out;
    const rawUrl = cleanBuf.toString('utf8');
    return rawUrl.replace(/_(96|160|320)\.mp4/, '_320.mp4').replace('http://', 'https://');
  } catch (e) {
    return null;
  }
}

function resolveJioSaavnStream(title, artist) {
  return new Promise((resolve) => {
    const query = `${artist} ${title}`.trim();
    const encoded = encodeURIComponent(query);
    const url = `https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&n=5&p=1&q=${encoded}&_marker=0&ctx=android`;

    const req = https.get(url, {
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
      }
    }, res => {
      let body = '';
      res.on('data', c => body += c);
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          const results = json.results || [];
          if (!results.length) return resolve(null);

          for (const item of results) {
            if (!item.encrypted_media_url) continue;
            const fullUrl = decryptSaavnMediaUrl(item.encrypted_media_url);
            if (fullUrl && fullUrl.startsWith('https://')) {
              return resolve({
                url: fullUrl,
                bitrate: 320000,
                mimeType: 'audio/mp4',
                duration: parseInt(item.duration, 10) || 0,
                provider: 'JioSaavn 320k',
                isFull: true
              });
            }
          }
          resolve(null);
        } catch (e) {
          resolve(null);
        }
      });
    });
    req.on('error', () => resolve(null));
  });
}

function searchYouTubeId(query) {
  return new Promise((resolve) => {
    const payload = JSON.stringify({
      query: query.trim(),
      params: "EgWKAQIIAWoKEAkQBRAKEAMQBw%3D%3D", // Filter Songs only
      context: {
        client: {
          clientName: "WEB_REMIX",
          clientVersion: "1.20240318.01.00",
          hl: "id",
          gl: "ID"
        }
      }
    });

    const req = https.request({
      hostname: "music.youtube.com",
      path: "/youtubei/v1/search",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Content-Length": Buffer.byteLength(payload)
      }
    }, res => {
      let body = "";
      res.on("data", c => body += c);
      res.on("end", () => {
        try {
          const json = JSON.parse(body);
          const str = JSON.stringify(json);
          const vids = [...str.matchAll(/"videoId":"([a-zA-Z0-9_-]{11})"/g)].map(m => m[1]);
          const unique = [...new Set(vids)];
          resolve(unique[0] || null);
        } catch (e) {
          resolve(null);
        }
      });
    });
    req.on("error", () => resolve(null));
    req.write(payload);
    req.end();
  });
}

function resolveInnerTubeStream(videoId) {
  return new Promise((resolve) => {
    const payload = JSON.stringify({
      context: {
        client: {
          clientName: "IOS",
          clientVersion: "21.03.3",
          osName: "iPadOS",
          osVersion: "17.7.10.21H450",
          deviceMake: "Apple",
          deviceModel: "iPad7,6",
          hl: "id",
          gl: "ID"
        }
      },
      videoId: videoId
    });

    const req = https.request({
      hostname: "music.youtube.com",
      path: "/youtubei/v1/player",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "User-Agent": "com.google.ios.youtube/21.03.3 (iPad7,6; U; CPU iPadOS 17_7_10 like Mac OS X; en-US)",
        "Content-Length": Buffer.byteLength(payload)
      }
    }, res => {
      let body = "";
      res.on("data", c => body += c);
      res.on("end", () => {
        try {
          const json = JSON.parse(body);
          const formats = json.streamingData?.adaptiveFormats?.filter(f => f.mimeType?.includes("audio"));
          if (formats && formats.length > 0) {
            formats.sort((a, b) => (b.bitrate || 0) - (a.bitrate || 0));
            resolve({
              url: formats[0].url,
              bitrate: formats[0].bitrate || 160000,
              mimeType: formats[0].mimeType || 'audio/webm; codecs="opus"',
              provider: 'InnerTube Opus',
              isFull: true
            });
          } else {
            resolve(null);
          }
        } catch (e) {
          resolve(null);
        }
      });
    });
    req.on("error", () => resolve(null));
    req.write(payload);
    req.end();
  });
}

function createWindow() {
  const win = new BrowserWindow({
    width: 1280,
    height: 840,
    minWidth: 960,
    minHeight: 640,
    backgroundColor: '#0B0D10',
    title: 'SoraFy',
    icon: path.join(__dirname, 'src/assets/logo.png'),
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: false // allow cross-origin audio streams & covers
    }
  });

  win.loadFile(path.join(__dirname, 'src/index.html'));
  win.setMenuBarVisibility(false);
}

// IPC Handlers for Full-Length Audio Streaming
ipcMain.handle('resolve-full-stream', async (event, { title, artist, videoId }) => {
  const cacheKey = videoId || `${artist} - ${title}`.toLowerCase().trim();
  const cached = streamCache.get(cacheKey);
  if (cached && cached.expireAt > Date.now()) {
    return cached.data;
  }

  // 1. Try JioSaavn 320k direct stream first (matches Android APK!)
  if (title && artist) {
    const saavnStream = await resolveJioSaavnStream(title, artist);
    if (saavnStream && saavnStream.url) {
      streamCache.set(cacheKey, { data: saavnStream, expireAt: Date.now() + 45 * 60 * 1000 });
      return saavnStream;
    }
  }

  // 2. Fallback to InnerTube iOS stream
  let vid = videoId;
  if (!vid && title && artist) {
    vid = await searchYouTubeId(`${artist} ${title}`);
  }

  if (vid) {
    const stream = await resolveInnerTubeStream(vid);
    if (stream && stream.url) {
      streamCache.set(cacheKey, { data: stream, expireAt: Date.now() + 45 * 60 * 1000 });
      return stream;
    }
  }

  return null;
});

app.whenReady().then(() => {
  // Spoof iOS User-Agent on googlevideo requests to prevent 403 Forbidden
  if (session && session.defaultSession) {
    session.defaultSession.webRequest.onBeforeSendHeaders(
      { urls: ['*://*.googlevideo.com/*', '*://*.youtube.com/*'] },
      (details, callback) => {
        delete details.requestHeaders['Origin'];
        delete details.requestHeaders['sec-ch-ua'];
        delete details.requestHeaders['sec-ch-ua-mobile'];
        delete details.requestHeaders['sec-ch-ua-platform'];
        details.requestHeaders['User-Agent'] = 'com.google.ios.youtube/21.03.3 (iPad7,6; U; CPU iPadOS 17_7_10 like Mac OS X; en-US)';
        callback({ cancel: false, requestHeaders: details.requestHeaders });
      }
    );
  }

  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
