const { app, BrowserWindow, ipcMain, session } = require('electron');
const path = require('path');
const http = require('http');
const https = require('https');
const des = require('des.js');

// Disable incompatible Vulkan on Linux Wayland to guarantee silky smooth rendering
app.commandLine.appendSwitch('disable-features', 'Vulkan');
app.commandLine.appendSwitch('disable-dev-shm-usage');
app.commandLine.appendSwitch('log-level', '3'); // Suppress harmless Chromium Ozone/Vulkan warnings

// Apple / Safari User-Agent (exact match with Android MusicStreamSource.kt & MusicCacheManager.kt)
const APPLE_UA = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.0 Safari/605.1.15';
const IOS_UA = 'com.google.ios.youtube/21.03.3 (iPad7,6; U; CPU iPadOS 17_7_10 like Mac OS X; en-US)';

let cachedVisitorData = null;
function getVisitorData() {
  if (cachedVisitorData) return Promise.resolve(cachedVisitorData);
  return new Promise((resolve) => {
    https.get('https://music.youtube.com/sw.js_data', {
      headers: { 'User-Agent': APPLE_UA }
    }, res => {
      let b = '';
      res.on('data', c => b += c);
      res.on('end', () => {
        try {
          if (b.length > 5) {
            const arr = JSON.parse(b.substring(5));
            const candidates = arr[0] && arr[0][2];
            if (candidates) {
              for (const c of candidates) {
                if (typeof c === 'string' && c.startsWith('Cgt')) {
                  cachedVisitorData = c;
                  return resolve(c);
                }
              }
            }
          }
          resolve(null);
        } catch (e) {
          resolve(null);
        }
      });
    }).on('error', () => resolve(null));
  });
}

// Lightweight local audio proxy for YouTube InnerTube streams (handles Range & iOS headers)
let localProxyPort = 0;
const localAudioProxy = http.createServer((req, res) => {
  const reqUrl = new URL(req.url, `http://127.0.0.1:${localProxyPort}`);
  const target = reqUrl.searchParams.get('url');
  if (!target) {
    res.writeHead(400);
    return res.end('Missing url');
  }

  try {
    const u = new URL(target);
    const headers = {
      'User-Agent': IOS_UA
    };
    if (req.headers.range) {
      headers['Range'] = req.headers.range;
    }

    const upstream = https.request({
      hostname: u.hostname,
      path: u.pathname + u.search,
      headers: headers
    }, (upRes) => {
      const respHeaders = {
        'Access-Control-Allow-Origin': '*',
        'Content-Type': upRes.headers['content-type'] || 'audio/mp4'
      };
      if (upRes.headers['content-length']) respHeaders['Content-Length'] = upRes.headers['content-length'];
      if (upRes.headers['content-range']) respHeaders['Content-Range'] = upRes.headers['content-range'];
      if (upRes.headers['accept-ranges']) respHeaders['Accept-Ranges'] = upRes.headers['accept-ranges'];

      res.writeHead(upRes.statusCode, respHeaders);
      upRes.pipe(res);
    });

    upstream.on('error', (err) => {
      if (!res.headersSent) res.writeHead(502);
      res.end();
    });

    req.on('close', () => {
      upstream.destroy();
    });

    upstream.end();
  } catch (err) {
    if (!res.headersSent) res.writeHead(500);
    res.end();
  }
});

localAudioProxy.listen(0, '127.0.0.1', () => {
  localProxyPort = localAudioProxy.address().port;
});

// In-memory stream cache for instant 0ms playback on repeated tracks
const streamCache = new Map();

// Strict filters & matching algorithms (ported directly from Android MusicStreamSource.kt)
const BANNED_LANGUAGES = new Set([
  'hindi', 'punjabi', 'bhojpuri', 'tamil', 'telugu', 'marathi',
  'bengali', 'kannada', 'gujarati', 'malayalam', 'urdu', 'haryanvi',
  'rajasthani', 'odia', 'assamese', 'sanskrit'
]);

const BANNED_KEYWORDS = [
  't-series', 'zee music', 'bhangra', 'dholak', 'qawwali', 'naat',
  'sonu nigam', 'arijit', 'badshah', 'sidhu', 'shreya ghoshal', 'kumar sanu',
  'lata mangeshkar', 'dj remix', 'viral rimek', 'pashmina', 'punjabi', 'bhojpuri',
  'hawa hawa', 'dilbar', 'diljit', 'neha kakkar', 'yo yo honey', 'tribute', 'cover by'
];

function isIndianOrCoverContent(title, artist, language) {
  if (language && BANNED_LANGUAGES.has(language.toLowerCase())) {
    return true;
  }
  const lowerT = (title || '').toLowerCase();
  const lowerA = (artist || '').toLowerCase();
  return BANNED_KEYWORDS.some(k => lowerT.includes(k) || lowerA.includes(k));
}

function normalize(s) {
  return (s || '').toLowerCase()
    .replace(/\(.*\)|\[.*\]/g, '')
    .replace(/[^a-z0-9]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

function isStrictMatch(expectedTitle, expectedArtist, candTitle, candArtist) {
  const normExpT = normalize(expectedTitle);
  const normCandT = normalize(candTitle);
  const normExpA = normalize(expectedArtist);
  const normCandA = normalize(candArtist);

  if (!normExpT || !normCandT) return false;

  const titleMatch = normExpT === normCandT ||
    normExpT.includes(normCandT) || normCandT.includes(normExpT) ||
    (() => {
      const expWords = normExpT.split(' ').filter(w => w.length > 2);
      const candWords = normCandT.split(' ').filter(w => w.length > 2);
      return (expWords.length > 0 && expWords.every(w => candWords.includes(w))) ||
             (candWords.length > 0 && candWords.every(w => expWords.includes(w)));
    })();

  if (!titleMatch) return false;

  const artistMatch = !normExpA || !normCandA ||
    normExpA === normCandA ||
    normExpA.includes(normCandA) || normCandA.includes(normExpA) ||
    normExpA.split(' ').filter(w => w.length > 2).some(w => normCandA.includes(w));

  return artistMatch;
}

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
            const candTitle = item.title || item.song || '';
            const candArtist = item.more_info?.primary_artists || item.singers || '';
            const candLang = item.language;

            // Reject Indian regional & banned keywords
            if (isIndianOrCoverContent(candTitle, candArtist, candLang)) continue;

            // Reject if title and artist don't strictly match the requested song
            if (!isStrictMatch(title, artist, candTitle, candArtist)) continue;

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

function searchYouTubeMusicInnerTube(query, filterType = 'song') {
  return new Promise((resolve) => {
    let params = 'EgWKAQIIAWoKEAkQBRAKEAMQBw%3D%3D'; // Songs
    if (filterType === 'video') params = 'EgWKAQIQAWoKEAkQBRAKEAMQBw%3D%3D'; // Videos
    if (filterType === 'artist') params = ''; // Unfiltered: yields musicCardShelfRenderer with top 3 songs & real monthly listeners
    if (filterType === 'playlist') params = 'EgWKAQI4AWoKEAkQBRAKEAMQBw%3D%3D'; // Playlists
    if (filterType === 'album') params = 'EgWKAQIYAWoSEAUQAxAEEAoQDhAJEBAQFRAR'; // Albums

    const payloadObj = {
      query: query.trim(),
      context: {
        client: {
          clientName: 'WEB_REMIX',
          clientVersion: '1.20240318.01.00',
          hl: 'id',
          gl: 'ID'
        }
      }
    };
    if (params) {
      payloadObj.params = params;
    }
    const payload = JSON.stringify(payloadObj);

    const req = https.request({
      hostname: 'music.youtube.com',
      path: '/youtubei/v1/search',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Content-Length': Buffer.byteLength(payload)
      }
    }, res => {
      let body = '';
      res.on('data', c => body += c);
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          const contents = json.contents?.tabbedSearchResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents;
          const itemsList = [];

          if (contents) {
            for (const sec of contents) {
              // Top Verified Artist / Hero Card
              if (sec.musicCardShelfRenderer) {
                const card = sec.musicCardShelfRenderer;
                const title = card.title?.runs?.[0]?.text || '';
                const sub = card.subtitle?.runs?.map(r => r.text).join('') || '';
                const thumbs = card.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
                  || card.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails || [];
                let artwork = thumbs.length ? thumbs[thumbs.length - 1].url : '';
                if (artwork) {
                  artwork = artwork.replace(/=(?:w\d+-h\d+|s\d+).*$/, '=w544-h544-p-l90-rj');
                }
                const browseId = card.onTap?.browseEndpoint?.browseId
                  || card.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId || '';
                const pageType = card.onTap?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                  || card.title?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType || '';

                const isArtistCard = pageType === 'MUSIC_PAGE_TYPE_ARTIST' || sub.toLowerCase().includes('artis') || (browseId && browseId.startsWith('UC'));
                const isPlaylistCard = pageType === 'MUSIC_PAGE_TYPE_PLAYLIST' || (browseId && (browseId.startsWith('VL') || browseId.startsWith('PL')));

                // Extract 3 top tracks from the artist card shelf (exact matches to YouTube Music)
                const cardSongs = [];
                for (const item of (card.contents || [])) {
                  const r = item.musicResponsiveListItemRenderer;
                  if (!r) continue;
                  const t = r.flexColumns?.[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text || '';
                  const sRuns = r.flexColumns?.[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];
                  const subText = sRuns.map(x => x.text).join('');
                  const durStr = sRuns.length > 0 ? sRuns[sRuns.length - 1]?.text : '3:30';
                  let dSec = 210;
                  if (durStr && durStr.includes(':')) {
                    const parts = durStr.split(':').map(p => parseInt(p, 10));
                    if (parts.length === 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
                      dSec = parts[0] * 60 + parts[1];
                    }
                  }
                  const sThumbs = r.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
                    || r.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails || [];
                  let sArt = sThumbs.length ? sThumbs[sThumbs.length - 1].url : '';
                  if (sArt) sArt = sArt.replace(/=(?:w\d+-h\d+|s\d+).*$/, '=w544-h544-l90-rj');
                  const sVid = r.overlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                    || r.playlistItemData?.videoId || '';

                  if (t && sVid) {
                    cardSongs.push({
                      id: 'yt_' + sVid,
                      video_id: sVid,
                      title: t,
                      artist: title,
                      subtitle: subText,
                      duration_secs: dSec,
                      artwork_url: sArt || `https://i.ytimg.com/vi/${sVid}/hqdefault.jpg`,
                      stream_url: '',
                      source_label: 'SoraFy Stream',
                      audio_format: { codec: 'Opus HD', bitrate_kbps: 160, is_lossless: false }
                    });
                  }
                }

                if (isArtistCard && (filterType === 'artist' || filterType === 'all')) {
                  if (title) {
                    itemsList.unshift({
                      id: browseId || ('art_' + title.toLowerCase().replace(/[^a-z0-9]/g, '_')),
                      name: title,
                      title: title,
                      avatar_url: artwork || '',
                      artwork_url: artwork || '',
                      browse_id: browseId,
                      sub_label: sub || 'Artis Terverifikasi',
                      is_top_hero: true,
                      top_songs: cardSongs
                    });
                  }
                } else if (isPlaylistCard && filterType === 'playlist') {
                  itemsList.unshift({
                    id: browseId || ('pl_' + title.toLowerCase().replace(/[^a-z0-9]/g, '_')),
                    title: title,
                    author: 'SoraFy Koleksi',
                    artwork_url: artwork,
                    browse_id: browseId,
                    track_count: 'Daftar Putar',
                    source_label: 'Koleksi Spesial'
                  });
                }
              }

              const shelf = sec.musicShelfRenderer || sec.musicPlaylistShelfRenderer;
              if (!shelf || !shelf.contents) continue;

              for (const item of shelf.contents) {
                const renderer = item.musicResponsiveListItemRenderer;
                if (!renderer || !renderer.flexColumns) continue;

                // Title
                const title = renderer.flexColumns[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text || '';
                if (!title) continue;

                // Subtitle (Artist, Album, Duration)
                const subRuns = renderer.flexColumns[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];
                const artist = subRuns[0]?.text || 'Artist';
                const artistBrowseId = subRuns[0]?.navigationEndpoint?.browseEndpoint?.browseId || '';
                const album = subRuns.length > 2 ? subRuns[2]?.text : '';
                const durationStr = subRuns.length > 0 ? subRuns[subRuns.length - 1]?.text : '3:30';

                let durationSec = 210;
                if (durationStr && durationStr.includes(':')) {
                  const parts = durationStr.split(':').map(p => parseInt(p, 10));
                  if (parts.length === 2 && !isNaN(parts[0]) && !isNaN(parts[1])) {
                    durationSec = parts[0] * 60 + parts[1];
                  }
                }

                // Video ID & Browse ID
                const overlay = renderer.overlay?.musicItemThumbnailOverlayRenderer;
                const videoId = overlay?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                  || renderer.playlistItemData?.videoId || '';
                const navEndpoint = item.navigationEndpoint || renderer.navigationEndpoint;
                const browseId = navEndpoint?.browseEndpoint?.browseId
                  || renderer.flexColumns[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseId || '';

                // Thumbnail (upgrade resolution from Google/YouTube CDN)
                const thumbs = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
                  || renderer.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails || [];
                let artwork = thumbs.length ? thumbs[thumbs.length - 1].url : '';
                if (artwork) {
                  artwork = artwork.replace(/=(?:w\d+-h\d+|s\d+).*$/, '=w544-h544-p-l90-rj');
                }

                if (filterType === 'artist') {
                  const pType = navEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType
                    || renderer.flexColumns[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.navigationEndpoint?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType || '';
                  const subStr = subRuns.map(r => r.text).join('').toLowerCase();
                  const isArt = (pType === 'MUSIC_PAGE_TYPE_ARTIST' || subStr.includes('artis') || artist.toLowerCase() === 'artist' || (browseId && browseId.startsWith('UC'))) && !browseId.startsWith('VL') && !browseId.startsWith('PL');

                  if (isArt) {
                    itemsList.push({
                      id: browseId || ('art_' + title.toLowerCase().replace(/[^a-z0-9]/g, '_')),
                      name: title,
                      title: title,
                      avatar_url: artwork || '',
                      artwork_url: artwork || '',
                      browse_id: browseId,
                      sub_label: subRuns.map(r => r.text).join('') || 'Artis'
                    });
                  }
                } else if (filterType === 'playlist') {
                  let authorClean = artist;
                  if (!authorClean || authorClean.toLowerCase().includes('youtube') || authorClean === 'Artist') {
                    authorClean = 'SoraFy Koleksi';
                  }
                  itemsList.push({
                    id: browseId || ('pl_' + title.toLowerCase().replace(/[^a-z0-9]/g, '_')),
                    title: title,
                    author: authorClean,
                    artwork_url: artwork,
                    browse_id: browseId,
                    track_count: album || 'Daftar Putar',
                    source_label: 'Koleksi Spesial'
                  });
                } else if (filterType === 'video') {
                  const isOfficial = /\b(official\s*(?:music\s*)?video|video\s*musik|music\s*video|mv|m\/v)\b/i.test(title);
                  const isLiveOrCover = /\b(live|acoustic|akustik|cover|session|remix|konser|karaoke)\b/i.test(title);
                  const videoBadge = isOfficial ? 'Official' : (isLiveOrCover ? 'Live / Session' : 'Video');

                  if (videoId) {
                    itemsList.push({
                      id: 'yt_' + videoId,
                      video_id: videoId,
                      title: title,
                      artist: artist,
                      artist_browse_id: artistBrowseId,
                      album: album || title,
                      duration_secs: durationSec,
                      artwork_url: artwork || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
                      stream_url: '',
                      is_video: true,
                      video_badge: videoBadge,
                      source_label: 'Video Musik',
                      audio_format: { codec: 'Opus HD', bitrate_kbps: 160, is_lossless: false }
                    });
                  }
                } else {
                  // Standard Song format
                  if (videoId) {
                    const isOfficial = /\b(official\s*(?:music\s*)?video|official\s*audio|video\s*musik)\b/i.test(title);
                    itemsList.push({
                      id: 'yt_' + videoId,
                      video_id: videoId,
                      title: title,
                      artist: artist,
                      artist_browse_id: artistBrowseId,
                      album: album || title,
                      duration_secs: durationSec,
                      artwork_url: artwork,
                      stream_url: '',
                      is_video: false,
                      is_official: isOfficial,
                      source_label: 'SoraFy Stream',
                      audio_format: { codec: 'Opus HD', bitrate_kbps: 160, is_lossless: false }
                    });
                  }
                }
              }
            }
          }
          resolve(itemsList);
        } catch (e) {
          resolve([]);
        }
      });
    });
    req.on('error', () => resolve([]));
    req.write(payload);
    req.end();
  });
}

function browseYouTubeInnerTube(browseId) {
  if (!browseId) return Promise.resolve(null);
  return new Promise((resolve) => {
    const payload = JSON.stringify({
      browseId,
      context: {
        client: {
          clientName: 'WEB_REMIX',
          clientVersion: '1.20240318.01.00',
          hl: 'id',
          gl: 'ID'
        }
      }
    });

    const req = https.request({
      hostname: 'music.youtube.com',
      path: '/youtubei/v1/browse',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Content-Length': Buffer.byteLength(payload)
      }
    }, res => {
      let body = '';
      res.on('data', c => body += c);
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          const header = json.header?.musicImmersiveHeaderRenderer || json.header?.musicVisualHeaderRenderer || json.header?.musicDetailHeaderRenderer;
          const name = header?.title?.runs?.[0]?.text || '';
          const desc = header?.description?.runs?.[0]?.text || '';
          const subText = header?.subscriptionButton?.subscribeButtonRenderer?.subscriberCountText?.runs?.[0]?.text
            || header?.straplineTextOne?.runs?.map(x => x.text).join('')
            || header?.subtitle?.runs?.map(x => x.text).join('')
            || '';
          const thumbs = header?.thumbnails
            || header?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
            || header?.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails
            || [];
          let avatarUrl = thumbs.length ? thumbs[thumbs.length - 1].url : '';
          if (avatarUrl) avatarUrl = avatarUrl.replace(/=(?:w\d+-h\d+|s\d+).*$/, '=w544-h544-p-l90-rj');

          // Extract tracks, albums, videos, and peers
          const parsedTracks = [];
          const parsedAlbums = [];
          const parsedVideos = [];
          const parsedPeers = [];

          const contents = json.contents?.singleColumnBrowseResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents
            || json.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents || [];

          for (const sec of contents) {
            // Top Songs
            const shelf = sec.musicShelfRenderer || sec.musicPlaylistShelfRenderer;
            if (shelf && shelf.contents) {
              for (const item of shelf.contents) {
                const r = item.musicResponsiveListItemRenderer;
                if (!r || !r.flexColumns) continue;
                const tTitle = r.flexColumns[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text || '';
                const subRuns = r.flexColumns[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs || [];
                const tArtist = subRuns[0]?.text || name || 'Artist';
                const tAlbum = subRuns.length > 2 ? subRuns[2]?.text : '';
                const overlay = r.overlay?.musicItemThumbnailOverlayRenderer;
                const videoId = overlay?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                  || r.playlistItemData?.videoId || '';
                const tThumbs = r.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails
                  || r.thumbnail?.croppedSquareThumbnailRenderer?.thumbnail?.thumbnails || [];
                let artwork = tThumbs.length ? tThumbs[tThumbs.length - 1].url : '';
                if (artwork) artwork = artwork.replace(/=(?:w\d+-h\d+|s\d+).*$/, '=w544-h544-p-l90-rj');

                if (tTitle && videoId) {
                  parsedTracks.push({
                    id: 'yt_' + videoId,
                    video_id: videoId,
                    title: tTitle,
                    artist: tArtist,
                    album: tAlbum || tTitle,
                    duration_secs: 210,
                    artwork_url: artwork || `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
                    stream_url: '',
                    source_label: 'SoraFy Stream',
                    audio_format: { codec: 'Opus HD', bitrate_kbps: 160, is_lossless: false }
                  });
                }
              }
            }

            // Carousels (Albums, Singles, Videos, Live Sessions, Peers)
            if (sec.musicCarouselShelfRenderer) {
              const carousel = sec.musicCarouselShelfRenderer;
              const cTitle = (carousel.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.[0]?.text || '').toLowerCase();
              const items = carousel.contents || [];

              for (const it of items) {
                const r = it.musicTwoRowItemRenderer;
                if (!r) continue;
                const itTitle = r.title?.runs?.[0]?.text || '';
                const itSub = r.subtitle?.runs?.map(x => x.text).join('') || '';
                const itThumbs = r.thumbnailRenderer?.musicThumbnailRenderer?.thumbnail?.thumbnails || [];
                let itArt = itThumbs.length ? itThumbs[itThumbs.length - 1].url : '';
                if (itArt) itArt = itArt.replace(/=(?:w\d+-h\d+|s\d+).*$/, '=w544-h544-p-l90-rj');

                const nav = r.navigationEndpoint;
                const itBrowseId = nav?.browseEndpoint?.browseId || '';
                const itVideoId = nav?.watchEndpoint?.videoId || '';
                const itPageType = nav?.browseEndpoint?.browseEndpointContextSupportedConfigs?.browseEndpointContextMusicConfig?.pageType || '';

                if (cTitle.includes('album') || cTitle.includes('single') || itPageType === 'MUSIC_PAGE_TYPE_ALBUM') {
                  if (itTitle) {
                    parsedAlbums.push({
                      id: itBrowseId || ('alb_' + itTitle.toLowerCase().replace(/[^a-z0-9]/g, '_')),
                      title: itTitle,
                      artist: name,
                      year: itSub,
                      artwork_url: itArt,
                      browse_id: itBrowseId
                    });
                  }
                } else if (cTitle.includes('video') || cTitle.includes('langsung') || itVideoId) {
                  if (itTitle && itVideoId) {
                    const isOfficial = cTitle.includes('video') || /\b(official|mv|video musik)\b/i.test(itTitle);
                    parsedVideos.push({
                      id: 'yt_' + itVideoId,
                      video_id: itVideoId,
                      title: itTitle,
                      artist: name,
                      artwork_url: itArt || `https://i.ytimg.com/vi/${itVideoId}/hqdefault.jpg`,
                      duration_secs: 210,
                      is_official: isOfficial,
                      video_badge: isOfficial ? 'Official' : 'Live / Session'
                    });
                  }
                } else if (cTitle.includes('suka') || itPageType === 'MUSIC_PAGE_TYPE_ARTIST') {
                  if (itTitle) {
                    parsedPeers.push({
                      id: itBrowseId || ('art_' + itTitle.toLowerCase().replace(/[^a-z0-9]/g, '_')),
                      name: itTitle,
                      avatar_url: itArt,
                      browse_id: itBrowseId,
                      sub_label: itSub
                    });
                  }
                }
              }
            }
          }

          resolve({
            name,
            description: desc,
            avatar_url: avatarUrl,
            sub_label: subText,
            tracks: parsedTracks,
            albums: parsedAlbums,
            videos: parsedVideos,
            peers: parsedPeers
          });
        } catch (e) {
          resolve(null);
        }
      });
    });
    req.on('error', () => resolve(null));
    req.write(payload);
    req.end();
  });
}

function searchYouTubeMusicStrict(title, artist) {
  return new Promise((resolve) => {
    const payload = JSON.stringify({
      query: `${artist} ${title}`.trim(),
      params: 'EgWKAQIIAWoKEAkQBRAKEAMQBw%3D%3D', // Filter Songs only
      context: {
        client: {
          clientName: 'WEB_REMIX',
          clientVersion: '1.20240318.01.00',
          hl: 'id',
          gl: 'ID'
        }
      }
    });

    const req = https.request({
      hostname: 'music.youtube.com',
      path: '/youtubei/v1/search',
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Content-Length': Buffer.byteLength(payload)
      }
    }, res => {
      let body = '';
      res.on('data', c => body += c);
      res.on('end', () => {
        try {
          const json = JSON.parse(body);
          const contents = json.contents?.tabbedSearchResultsRenderer?.tabs?.[0]?.tabRenderer?.content?.sectionListRenderer?.contents;
          const shelf = contents?.[0]?.musicShelfRenderer?.contents || [];

          // 1. First pass: strict match title and artist (guarantees authentic artist recording)
          for (const item of shelf) {
            const r = item.musicResponsiveListItemRenderer;
            const cTitle = r?.flexColumns[0]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text;
            const cArtist = r?.flexColumns[1]?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.[0]?.text;
            const overlay = r?.overlay?.musicItemThumbnailOverlayRenderer;
            const videoId = overlay?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
              || r?.playlistItemData?.videoId;
            if (videoId && isStrictMatch(title, artist, cTitle, cArtist)) {
              return resolve(videoId);
            }
          }

          // 2. Second pass: fallback to first playable videoId from song results
          for (const item of shelf) {
            const r = item.musicResponsiveListItemRenderer;
            const overlay = r?.overlay?.musicItemThumbnailOverlayRenderer;
            const videoId = overlay?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
              || r?.playlistItemData?.videoId;
            if (videoId) return resolve(videoId);
          }
          resolve(null);
        } catch (e) {
          resolve(null);
        }
      });
    });
    req.on('error', () => resolve(null));
    req.write(payload);
    req.end();
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

async function resolveInnerTubeStream(videoId) {
  return new Promise((resolve) => {
    const clientContext = {
      clientName: "IOS",
      clientVersion: "21.03.3",
      osName: "iPadOS",
      osVersion: "17.7.10.21H450",
      deviceMake: "Apple",
      deviceModel: "iPad7,6",
      hl: "id",
      gl: "ID"
    };

    const payload = JSON.stringify({
      context: { client: clientContext },
      videoId: videoId
    });

    const req = https.request({
      hostname: "www.youtube.com",
      path: "/youtubei/v1/player",
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "User-Agent": IOS_UA,
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
            const rawUrl = formats[0].url;
            const streamUrl = (localProxyPort > 0 && rawUrl)
              ? `http://127.0.0.1:${localProxyPort}/stream?url=${encodeURIComponent(rawUrl)}`
              : rawUrl;
            resolve({
              url: streamUrl,
              rawUrl: rawUrl,
              videoId: videoId,
              bitrate: formats[0].bitrate || 160000,
              mimeType: formats[0].mimeType || 'audio/mp4',
              provider: 'YouTube Music InnerTube',
              duration: parseInt(json.videoDetails?.lengthSeconds, 10) || 0,
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

async function resolveArtistProfile(name) {
  if (!name || !name.trim()) return null;
  const trimmed = name.trim();
  try {
    const artists = await searchYouTubeMusicInnerTube(trimmed, 'artist');
    if (!artists || !artists.length) return null;
    const match = artists.find(a => a.name.toLowerCase() === trimmed.toLowerCase()) || artists[0];
    let bio = null;
    if (match.browse_id) {
      bio = await browseYouTubeInnerTube(match.browse_id);
    }
    const artName = bio?.name || match.name || trimmed;
    const artAvatar = bio?.avatar_url || match.avatar_url || '';
    return {
      name: artName,
      title: artName,
      browse_id: match.browse_id || '',
      avatar_url: artAvatar,
      artwork_url: artAvatar,
      banner_url: artAvatar,
      sub_label: match.sub_label || (bio?.sub_label || ''),
      description: bio?.description || '',
      tracks: (match.top_songs && match.top_songs.length) ? match.top_songs : (bio?.tracks || []),
      albums: bio?.albums || [],
      videos: bio?.videos || [],
      peers: bio?.peers || []
    };
  } catch (err) {
    console.warn('resolveArtistProfile error:', err);
    return null;
  }
}

// IPC Handlers for YouTube Music InnerTube Search & Audio Streaming
ipcMain.handle('search-yt-music', async (event, query, filterType = 'song') => {
  if (!query || !query.trim()) return [];
  return await searchYouTubeMusicInnerTube(query, filterType);
});

ipcMain.handle('browse-yt', async (event, browseId) => {
  if (!browseId) return null;
  return await browseYouTubeInnerTube(browseId);
});

ipcMain.handle('resolve-artist-profile', async (event, name) => {
  if (!name || !name.trim()) return null;
  return await resolveArtistProfile(name);
});

ipcMain.handle('resolve-full-stream', async (event, { title, artist, videoId }) => {
  const cacheKey = videoId || `${artist} - ${title}`.toLowerCase().trim();
  const cached = streamCache.get(cacheKey);
  if (cached && cached.expireAt > Date.now()) {
    return cached.data;
  }

  // 1. If videoId is provided directly (e.g. from YouTube Music search item),
  // ALWAYS resolve InnerTube stream directly! This guarantees 100% authentic recording by the original artist.
  let vid = videoId;
  if (vid) {
    const stream = await resolveInnerTubeStream(vid);
    if (stream && stream.url) {
      streamCache.set(cacheKey, { data: stream, expireAt: Date.now() + 45 * 60 * 1000 });
      return stream;
    }
  }

  // 2. If no videoId provided, search YouTube Music strictly for original recording by artist
  if (title && artist) {
    const matchedVid = await searchYouTubeMusicStrict(title, artist);
    if (matchedVid) {
      const stream = await resolveInnerTubeStream(matchedVid);
      if (stream && stream.url) {
        streamCache.set(cacheKey, { data: stream, expireAt: Date.now() + 45 * 60 * 1000 });
        return stream;
      }
    }
  }

  // 3. Fallback search on YouTube
  if (title && artist) {
    const fallbackVid = await searchYouTubeId(`${artist} ${title}`);
    if (fallbackVid) {
      const stream = await resolveInnerTubeStream(fallbackVid);
      if (stream && stream.url) {
        streamCache.set(cacheKey, { data: stream, expireAt: Date.now() + 45 * 60 * 1000 });
        return stream;
      }
    }
  }

  return null;
});

app.whenReady().then(() => {
  // Set Safari/Apple User-Agent on googlevideo requests to prevent 403 Forbidden
  if (session && session.defaultSession) {
    session.defaultSession.webRequest.onBeforeSendHeaders(
      { urls: ['*://*.googlevideo.com/*', '*://*.youtube.com/*'] },
      (details, callback) => {
        delete details.requestHeaders['Origin'];
        delete details.requestHeaders['sec-ch-ua'];
        delete details.requestHeaders['sec-ch-ua-mobile'];
        delete details.requestHeaders['sec-ch-ua-platform'];
        details.requestHeaders['User-Agent'] = APPLE_UA;
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
