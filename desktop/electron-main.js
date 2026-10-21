const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const https = require('https');

// Disable incompatible Vulkan on Linux Wayland to guarantee silky smooth rendering
app.commandLine.appendSwitch('disable-features', 'Vulkan');

// In-memory stream cache for instant 0ms playback on repeated tracks
const streamCache = new Map();

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
              mimeType: formats[0].mimeType || 'audio/webm; codecs="opus"'
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
      webSecurity: false // allow cross-origin streams & covers
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

  let vid = videoId;
  if (!vid && title && artist) {
    vid = await searchYouTubeId(`${artist} ${title}`);
  }

  if (vid) {
    const stream = await resolveInnerTubeStream(vid);
    if (stream && stream.url) {
      const result = {
        url: stream.url,
        bitrate: stream.bitrate,
        mimeType: stream.mimeType,
        isFull: true
      };
      // Cache for 45 minutes
      streamCache.set(cacheKey, { data: result, expireAt: Date.now() + 45 * 60 * 1000 });
      return result;
    }
  }

  return null;
});

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
