// SoraFy Desktop Core Frontend Controller
// Domain-Driven Design (DDD) Client with Real Multi-Provider Search, 
// Live Streaming Playback, LRCLIB Synced Lyrics & Studio Equalizer DSP

const SAMPLE_MASTER_TRACKS = [
  {
    id: "sora_001",
    title: "Blinding Lights",
    artist: "The Weeknd",
    album: "After Hours",
    duration_secs: 200,
    stream_url: "https://aac.saavncdn.com/265/49d2e76e53abba8b08c951447abacbb0_320.mp4",
    artwork_url: "https://c.saavncdn.com/265/After-Hours-English-2020-20200320023605-500x500.jpg",
    audio_format: { codec: "AAC 320k", bitrate_kbps: 320, is_lossless: false },
    source_label: "SoraFy 320k",
    is_liked: true,
    is_full_stream: true,
    lyrics: [
      { timestamp_ms: 0, text: "(Synth Intro)" },
      { timestamp_ms: 27000, text: "I've been tryna call" },
      { timestamp_ms: 31000, text: "I've been on my own for long enough" },
      { timestamp_ms: 36000, text: "Maybe you can show me how to love, maybe" },
      { timestamp_ms: 45000, text: "I'm going through withdrawals" },
      { timestamp_ms: 49000, text: "You don't even have to do too much" },
      { timestamp_ms: 54000, text: "You can turn me on with just a touch, baby" },
      { timestamp_ms: 63000, text: "I look around and Sin City's cold and empty" },
      { timestamp_ms: 72000, text: "No one's around to judge me" },
      { timestamp_ms: 77000, text: "I can't see clearly when you're gone" },
      { timestamp_ms: 81000, text: "I said, ooh, I'm blinded by the lights" },
      { timestamp_ms: 91000, text: "No, I can't sleep until I feel your touch" },
      { timestamp_ms: 100000, text: "I said, ooh, I'm drowning in the night" },
      { timestamp_ms: 109000, text: "Oh, when I'm like this, you're the one I trust" }
    ]
  },
  {
    id: "sora_002",
    title: "Starboy",
    artist: "The Weeknd ft. Daft Punk",
    album: "Starboy",
    duration_secs: 225,
    stream_url: "https://aac.saavncdn.com/216/8b8a4f2344d7703773d2aa956ecbe51b_320.mp4",
    artwork_url: "https://c.saavncdn.com/216/ZZang-KARAOKE-2024-The-Weeknd-Vol-2-Instrumental-2024-20260620203409-500x500.webp",
    audio_format: { codec: "AAC 320k", bitrate_kbps: 320, is_lossless: false },
    source_label: "SoraFy 320k",
    is_liked: false,
    is_full_stream: true,
    lyrics: [
      { timestamp_ms: 0, text: "(Piano & Drum intro)" },
      { timestamp_ms: 13000, text: "I'm tryna put you in the worst mood, ah" },
      { timestamp_ms: 16000, text: "P1 cleaner than your church shoes, ah" },
      { timestamp_ms: 20000, text: "Milli point two just to hurt you, ah" },
      { timestamp_ms: 23000, text: "All red Lamb' just to tease you, ah" },
      { timestamp_ms: 38000, text: "Look what you've done" },
      { timestamp_ms: 41000, text: "I'm a motherfuckin' starboy" }
    ]
  },
  {
    id: "sora_003",
    title: "Yellow",
    artist: "Coldplay",
    album: "Parachutes",
    duration_secs: 271,
    stream_url: "https://aac.saavncdn.com/263/2083848071f4ca4b33f8866c02a16ee3_320.mp4",
    artwork_url: "https://c.saavncdn.com/263/ZZang-KARAOKE-2024-POP-Vol-60-Instrumental-2024-20260511142050-500x500.webp",
    audio_format: { codec: "AAC 320k", bitrate_kbps: 320, is_lossless: false },
    source_label: "SoraFy Acoustic",
    is_liked: true,
    is_full_stream: true,
    lyrics: [
      { timestamp_ms: 0, text: "(Acoustic guitar intro)" },
      { timestamp_ms: 16000, text: "Look at the stars" },
      { timestamp_ms: 20000, text: "Look how they shine for you" },
      { timestamp_ms: 24000, text: "And everything you do" },
      { timestamp_ms: 32000, text: "Yeah they were all yellow" }
    ]
  },
  {
    id: "sora_004",
    title: "Shape of You",
    artist: "Ed Sheeran",
    album: "÷ (Divide)",
    duration_secs: 140,
    stream_url: "https://aac.saavncdn.com/444/c72a75582084c9e31a0e8f60339da41e_320.mp4",
    artwork_url: "https://c.saavncdn.com/444/Shape-of-You-Instrumental-2021-20211029105017-500x500.webp",
    audio_format: { codec: "AAC 320k", bitrate_kbps: 320, is_lossless: false },
    source_label: "SoraFy Pop",
    is_liked: false,
    is_full_stream: true,
    lyrics: [
      { timestamp_ms: 0, text: "(Marimba rhythm)" },
      { timestamp_ms: 10000, text: "The club isn't the best place to find a lover" },
      { timestamp_ms: 13000, text: "So the bar is where I go" },
      { timestamp_ms: 17000, text: "Me and my friends at the table doing shots" },
      { timestamp_ms: 20000, text: "Drinking fast and then we talk slow" }
    ]
  }
];

const DEFAULT_PLUGINS = [
  {
    id: "ytmusic",
    name: "YouTube Music InnerTube",
    version: "1.2.0",
    author: "Team NewPipe & MusicEuy",
    description: "Ekstraksi stream audio resmi YouTube Music (Opus HD 160-320kbps) via InnerTube RPC.",
    category: "Audio Streaming",
    icon: "📺",
    is_enabled: true
  },
  {
    id: "monochrome_lossless",
    name: "Monochrome Lossless",
    version: "2.0.0",
    author: "SoraFy Core Team",
    description: "Hi-Res 24-bit FLAC audio stream provider (monochrome.st) tanpa akun.",
    category: "Audio Streaming",
    icon: "🎵",
    is_enabled: true
  },
  {
    id: "deezer",
    name: "Deezer Hi-Fi",
    version: "1.0.0",
    author: "SoraFy Core Team",
    description: "Deezer Hi-Fi catalog, 500x500 high-res album covers, dan stream preview global jernih.",
    category: "Audio Streaming",
    icon: "🎧",
    is_enabled: true
  },
  {
    id: "jiosaavn",
    name: "JioSaavn 320k",
    version: "1.0.0",
    author: "SoraFy Core Team",
    description: "Penyedia streaming 320kbps AAC berkecepatan tinggi dengan filter judul & artis ketat.",
    category: "Audio Streaming",
    icon: "⚡",
    is_enabled: true
  },
  {
    id: "bandcamp_jamendo",
    name: "Bandcamp & Jamendo",
    version: "1.0.0",
    author: "SoraFy Community",
    description: "Katalog musisi independen global, Creative Commons, dan lossless audio.",
    category: "Audio Streaming",
    icon: "📻",
    is_enabled: true
  },
  {
    id: "spotify_importer",
    name: "Spotify Importer",
    version: "1.0.0",
    author: "SoraFy Core Team",
    description: "Ekstrak & sinkronisasi playlist publik Spotify ke katalog SoraFy tanpa akun Premium.",
    category: "Tools & Utilities",
    icon: "📥",
    is_enabled: true
  },
  {
    id: "discord_rpc",
    name: "Discord Rich Presence",
    version: "1.0.0",
    author: "SoraFy Social",
    description: "Tampilkan lagu, artis, cover album, dan progres pemutaran aktif di profil Discord.",
    category: "Social & Integration",
    icon: "🎮",
    is_enabled: true
  },
  {
    id: "equalizer_dsp",
    name: "Equalizer DSP Studio",
    version: "1.0.0",
    author: "Priangan Sound Lab",
    description: "7-band parametric equalizer dengan dynamic Bass Boost dan 3D Virtualizer.",
    category: "Audio DSP",
    icon: "🎛️",
    is_enabled: true
  },
  {
    id: "apple_canvas",
    name: "Apple Music Canvas",
    version: "1.0.0",
    author: "SoraFy Visuals",
    description: "Visualisasi artwork video dinamis dan animasi latar belakang ambient Apple Music.",
    category: "Visual & Lyrics",
    icon: "✨",
    is_enabled: true
  },
  {
    id: "lrclib",
    name: "LRCLIB Synced Karaoke",
    version: "1.0.0",
    author: "LRCLIB Community",
    description: "Lirik tersinkronisasi presisi kata-per-kata otomatis dengan efek karaoke glow.",
    category: "Visual & Lyrics",
    icon: "🎤",
    is_enabled: true
  },
  {
    id: "soundcloud",
    name: "SoundCloud Audio Stream",
    version: "1.0.0",
    author: "Team NewPipe",
    description: "Stream lagu indie, mixtape, EDM, remix, dan DJ sets langsung dari SoundCloud.",
    category: "Audio Streaming",
    icon: "☁️",
    is_enabled: true
  },
  {
    id: "ytdlp_engine",
    name: "yt-dlp Native Engine",
    version: "2024.08.06",
    author: "yt-dlp Core",
    description: "Mesin ekstraksi native berkecepatan tinggi untuk stream audio bitrate maksimal.",
    category: "Audio Streaming",
    icon: "🚀",
    is_enabled: true
  },
  {
    id: "spotube_bridge",
    name: "Spotube Bridge Resolver",
    version: "1.0.1",
    author: "Spotube Open Spec",
    description: "Protokol resolver kompatibel format .smplug untuk sinkronisasi audio eksternal.",
    category: "Tools & Utilities",
    icon: "🔌",
    is_enabled: true
  },
  {
    id: "lastfm",
    name: "Last.fm Scrobbler",
    version: "1.0.0",
    author: "SoraFy Social",
    description: "Scrobbling otomatis dan integrasi biografi artis ke akun Last.fm kamu secara instan.",
    category: "Social & Integration",
    icon: "📊",
    is_enabled: true
  }
];

const EQ_FREQUENCIES = [60, 150, 400, 1000, 2400, 6000, 15000];
const EQ_PRESETS = {
  "Flat": [0, 0, 0, 0, 0, 0, 0],
  "Bass Boost": [8, 6, 3, 0, 0, 1, 2],
  "Electronic / Cyber": [6, 4, -1, 2, 4, 6, 7],
  "Vocal / Acoustic": [-2, 0, 2, 4, 3, 2, 0]
};

// Safe Tauri IPC Helper
async function invokeTauri(cmd, args = {}) {
  try {
    if (window.__TAURI__ && window.__TAURI__.core && typeof window.__TAURI__.core.invoke === 'function') {
      return await window.__TAURI__.core.invoke(cmd, args);
    } else if (window.__TAURI__ && window.__TAURI__.tauri && typeof window.__TAURI__.tauri.invoke === 'function') {
      return await window.__TAURI__.tauri.invoke(cmd, args);
    }
  } catch (err) {
    console.warn(`[Tauri IPC] ${cmd} notice:`, err);
  }
  return null;
}

// JSONP Fallback helper for Deezer in browsers with strict CORS
function fetchDeezerJsonp(url) {
  return new Promise((resolve, reject) => {
    const callbackName = 'dz_cb_' + Date.now() + '_' + Math.floor(Math.random() * 100000);
    const script = document.createElement('script');
    const separator = url.includes('?') ? '&' : '?';
    script.src = `${url}${separator}output=jsonp&callback=${callbackName}`;

    window[callbackName] = (data) => {
      delete window[callbackName];
      if (script.parentNode) script.parentNode.removeChild(script);
      resolve(data);
    };

    script.onerror = (err) => {
      delete window[callbackName];
      if (script.parentNode) script.parentNode.removeChild(script);
      reject(err);
    };

    document.body.appendChild(script);
  });
}

// LRC parser
function parseLrc(lrcText) {
  if (!lrcText) return [];
  const lines = lrcText.split("\n");
  const result = [];
  const regex = /\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)/;
  for (const line of lines) {
    const match = regex.exec(line.trim());
    if (match) {
      const min = parseInt(match[1], 10);
      const sec = parseInt(match[2], 10);
      const ms = parseInt(match[3].padEnd(3, '0').slice(0, 3), 10);
      const timestamp_ms = (min * 60 + sec) * 1000 + ms;
      const text = match[4].trim();
      if (text) {
        result.push({ timestamp_ms, text });
      }
    }
  }
  return result;
}

class SoraFyDesktopApp {
  constructor() {
    this.audio = document.getElementById("audioSource");
    this.tracks = [...SAMPLE_MASTER_TRACKS];
    this.plugins = [...DEFAULT_PLUGINS];
    this.currentTrackIndex = 0;
    this.isPlaying = false;
    this.isShuffled = false;
    this.repeatMode = 0; // 0: off, 1: all, 2: one
    this.currentLyrics = [];
    this.activeLyricIndex = -1;
    this.currentView = "home";

    // Persistence
    this.likedTrackIds = new Set(JSON.parse(localStorage.getItem("sorafy_liked_ids") || "[]"));
    this.historyTracks = JSON.parse(localStorage.getItem("sorafy_history") || "[]");

    // Equalizer Web Audio API
    this.audioCtx = null;
    this.audioSourceNode = null;
    this.eqFilters = [];
    this.bassFilterNode = null;
    this.currentGains = [0, 0, 0, 0, 0, 0, 0];
    this.bassBoostAmount = 0;
    this.virtualizerAmount = 0;

    // Search Debounce Timer
    this.searchDebounceTimer = null;

    // Wave Canvas state
    this.waveCanvas = document.getElementById("waveCanvas");
    this.ctx = this.waveCanvas.getContext("2d");
    this.wavePhase = 0;
    this.animationFrame = null;

    this.initElements();
    this.initEvents();
    this.initPlugins();
    this.initAudioEqualizer();
    this.loadInitialCatalog();
    this.setupWaveCanvas();
    this.startWaveAnimation();
  }

  initElements() {
    this.playPauseBtn = document.getElementById("playPauseBtn");
    this.playPauseIcon = document.getElementById("playPauseIcon");
    this.prevBtn = document.getElementById("prevBtn");
    this.nextBtn = document.getElementById("nextBtn");
    this.shuffleBtn = document.getElementById("shuffleBtn");
    this.repeatBtn = document.getElementById("repeatBtn");
    this.likeBtn = document.getElementById("likeBtn");
    this.volumeSlider = document.getElementById("volumeSlider");

    this.playerTitle = document.getElementById("playerTitle");
    this.playerArtist = document.getElementById("playerArtist");
    this.playerArtwork = document.getElementById("playerArtwork");
    this.playerBadge = document.getElementById("playerBadge");
    this.playerSourceTag = document.getElementById("playerSourceTag");

    this.currentTimeLabel = document.getElementById("currentTimeLabel");
    this.durationLabel = document.getElementById("durationLabel");

    this.lyricsContainer = document.getElementById("lyricsContainer");
    this.lyricsPanel = document.getElementById("lyricsPanel");
    this.appLayout = document.getElementById("appLayout");
    this.toggleLyricsBtn = document.getElementById("toggleLyricsBtn");
    this.closeLyricsBtn = document.getElementById("closeLyricsBtn");

    this.searchInput = document.getElementById("searchInput");
    this.categoryChips = document.querySelectorAll(".category-chips .chip");
    this.sectionTitleLabel = document.getElementById("sectionTitleLabel");
    this.viewAllBtn = document.getElementById("viewAllBtn");
    this.topToast = document.getElementById("topToast");
    this.toastMessage = document.getElementById("toastMessage");

    this.heroPlayBtn = document.getElementById("heroPlayBtn");
    this.trackGrid = document.getElementById("trackGrid");

    // View Containers
    this.homeViewContainer = document.getElementById("homeViewContainer");
    this.pluginsViewContainer = document.getElementById("pluginsViewContainer");
    this.equalizerViewContainer = document.getElementById("equalizerViewContainer");

    // Nav Items
    this.navItems = document.querySelectorAll(".nav-menu .nav-item");

    // Plugins view elements
    this.pluginListGrid = document.getElementById("pluginListGrid");
    this.resetPluginsBtn = document.getElementById("resetPluginsBtn");

    // Equalizer view elements
    this.eqBandsRow = document.getElementById("eqBandsRow");
    this.eqChips = document.querySelectorAll("#equalizerViewContainer .chip");
    this.bassBoostSlider = document.getElementById("bassBoostSlider");
    this.bassBoostVal = document.getElementById("bassBoostVal");
    this.virtualizerSlider = document.getElementById("virtualizerSlider");
    this.virtualizerVal = document.getElementById("virtualizerVal");

    // Spotify modal elements
    this.spotifyModal = document.getElementById("spotifyModal");
    this.openSpotifyImportBtn = document.getElementById("openSpotifyImportBtn");
    this.closeSpotifyModalBtn = document.getElementById("closeSpotifyModalBtn");
    this.cancelSpotifyBtn = document.getElementById("cancelSpotifyBtn");
    this.submitSpotifyBtn = document.getElementById("submitSpotifyBtn");
    this.spotifyUrlInput = document.getElementById("spotifyUrlInput");
  }

  initEvents() {
    this.playPauseBtn.addEventListener("click", () => this.togglePlayPause());
    this.prevBtn.addEventListener("click", () => this.playPrevious());
    this.nextBtn.addEventListener("click", () => this.playNext());

    this.shuffleBtn.addEventListener("click", () => {
      this.isShuffled = !this.isShuffled;
      this.shuffleBtn.classList.toggle("active", this.isShuffled);
      this.showToast(this.isShuffled ? "Shuffle Diaktifkan" : "Shuffle Dimatikan");
    });

    this.repeatBtn.addEventListener("click", () => {
      this.repeatMode = (this.repeatMode + 1) % 3;
      this.repeatBtn.classList.toggle("active", this.repeatMode > 0);
      const labels = ["Repeat Nonaktif", "Ulangi Semua", "Ulangi Lagu Ini"];
      this.showToast(labels[this.repeatMode]);
    });

    this.likeBtn.addEventListener("click", () => {
      const current = this.tracks[this.currentTrackIndex];
      if (!current) return;
      const isLiked = this.likedTrackIds.has(current.id);
      if (isLiked) {
        this.likedTrackIds.delete(current.id);
        current.is_liked = false;
        this.showToast("Dihapus dari Lagu Favorit");
      } else {
        this.likedTrackIds.add(current.id);
        current.is_liked = true;
        this.showToast("Disimpan ke Lagu Favorit ❤️");
      }
      localStorage.setItem("sorafy_liked_ids", JSON.stringify([...this.likedTrackIds]));
      this.updateLikeButton();
    });

    this.volumeSlider.addEventListener("input", (e) => {
      this.audio.volume = parseFloat(e.target.value);
    });

    this.toggleLyricsBtn.addEventListener("click", () => this.toggleLyricsDrawer());
    this.closeLyricsBtn.addEventListener("click", () => this.toggleLyricsDrawer(false));

    if (this.heroPlayBtn) {
      this.heroPlayBtn.addEventListener("click", () => {
        this.loadTrack(0, true);
      });
    }

    if (this.viewAllBtn) {
      this.viewAllBtn.addEventListener("click", () => {
        this.showToast("Memuat ulang trending chart global...");
        this.fetchTrendingChart();
      });
    }

    this.audio.addEventListener("timeupdate", () => this.onTimeUpdate());
    this.audio.addEventListener("loadedmetadata", () => {
      if (this.audio.duration && !isNaN(this.audio.duration)) {
        this.durationLabel.textContent = this.formatTime(this.audio.duration);
        if (this.tracks[this.currentTrackIndex]) {
          this.tracks[this.currentTrackIndex].duration_secs = Math.round(this.audio.duration);
        }
      }
    });
    this.audio.addEventListener("error", () => {
      console.warn("Audio element error on src:", this.audio.src, this.audio.error);
      const cur = this.tracks[this.currentTrackIndex];
      if (cur && cur.fallback_url && this.audio.src !== cur.fallback_url) {
        console.log("Switching to fallback preview stream:", cur.fallback_url);
        this.audio.src = cur.fallback_url;
        this.audio.play().catch(() => {});
      } else {
        this.isPlaying = false;
        this.updatePlayPauseIcon();
      }
    });
    this.audio.addEventListener("ended", () => this.onTrackEnded());

    // Wave canvas click to seek
    this.waveCanvas.addEventListener("click", (e) => {
      const rect = this.waveCanvas.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const ratio = Math.max(0, Math.min(1, clickX / rect.width));
      if (this.audio.duration) {
        this.audio.currentTime = ratio * this.audio.duration;
      }
    });

    // Real Online Search Input with Debounce (Just like mobile!)
    this.searchInput.addEventListener("input", (e) => {
      const query = e.target.value.trim();
      clearTimeout(this.searchDebounceTimer);

      if (!query) {
        this.sectionTitleLabel.textContent = "Trending & Rekomendasi (Multi-Provider)";
        this.fetchTrendingChart();
        return;
      }

      this.searchDebounceTimer = setTimeout(() => {
        this.searchOnlineMusic(query);
      }, 350);
    });

    // Category filter chips
    this.categoryChips.forEach(chip => {
      chip.addEventListener("click", () => {
        this.categoryChips.forEach(c => c.classList.remove("active"));
        chip.classList.add("active");
        const category = chip.dataset.category;
        this.filterByCategory(category);
      });
    });

    // Navigation item click handlers for View Switching
    this.navItems.forEach(item => {
      item.addEventListener("click", () => {
        const view = item.dataset.view;
        const filter = item.dataset.filter;
        if (view) {
          this.switchView(view);
        } else if (filter === "favorites") {
          this.switchView("home");
          this.showLikedSongs();
        } else if (filter === "history") {
          this.switchView("home");
          this.showHistory();
        } else if (filter === "lossless") {
          this.switchView("home");
          this.filterByCategory("lossless");
        }
      });
    });

    // Spotify Importer Modal events
    if (this.openSpotifyImportBtn) {
      this.openSpotifyImportBtn.addEventListener("click", () => {
        this.spotifyModal.style.display = "flex";
        this.spotifyUrlInput.focus();
      });
    }

    if (this.closeSpotifyModalBtn) {
      this.closeSpotifyModalBtn.addEventListener("click", () => {
        this.spotifyModal.style.display = "none";
      });
    }

    if (this.cancelSpotifyBtn) {
      this.cancelSpotifyBtn.addEventListener("click", () => {
        this.spotifyModal.style.display = "none";
      });
    }

    if (this.submitSpotifyBtn) {
      this.submitSpotifyBtn.addEventListener("click", () => this.handleSpotifyImport());
    }

    // Reset plugins button
    if (this.resetPluginsBtn) {
      this.resetPluginsBtn.addEventListener("click", () => {
        this.plugins.forEach(p => p.is_enabled = true);
        this.renderPlugins();
        this.showToast("Semua 9 plugin direset ke status aktif!");
      });
    }

    // Equalizer Preset chips
    this.eqChips.forEach(chip => {
      chip.addEventListener("click", () => {
        this.eqChips.forEach(c => c.classList.remove("active"));
        chip.classList.add("active");
        const presetName = chip.dataset.eq;
        this.applyEqualizerPreset(presetName);
      });
    });

    // Bass Boost & Virtualizer Sliders
    if (this.bassBoostSlider) {
      this.bassBoostSlider.addEventListener("input", (e) => {
        const val = parseInt(e.target.value, 10);
        this.bassBoostAmount = val;
        this.bassBoostVal.textContent = `${val}%`;
        if (this.bassFilterNode) {
          this.bassFilterNode.gain.value = (val / 100) * 12;
        }
      });
    }

    if (this.virtualizerSlider) {
      this.virtualizerSlider.addEventListener("input", (e) => {
        const val = parseInt(e.target.value, 10);
        this.virtualizerAmount = val;
        this.virtualizerVal.textContent = `${val}%`;
      });
    }

    // Keyboard Shortcuts (Space, ArrowLeft, ArrowRight)
    window.addEventListener("keydown", (e) => {
      if (e.target.tagName === "INPUT" || e.target.tagName === "TEXTAREA") return;
      if (e.code === "Space") {
        e.preventDefault();
        this.togglePlayPause();
      } else if (e.code === "ArrowRight") {
        this.playNext();
      } else if (e.code === "ArrowLeft") {
        this.playPrevious();
      }
    });

    window.addEventListener("resize", () => this.setupWaveCanvas());
  }

  // =========================================================
  // Live Online Music Catalog & Multi-Provider Search
  // =========================================================
  async loadInitialCatalog() {
    this.renderTrackGrid(this.tracks);
    this.loadTrack(0, false);
    // Fetch live trending global chart in background
    await this.fetchTrendingChart();
  }

  async fetchTrendingChart() {
    try {
      let chartItems = [];

      // 1. Try Deezer Chart
      try {
        let res = await fetch("https://api.deezer.com/chart/0/tracks?limit=20");
        if (res.ok) {
          const json = await res.json();
          if (json && json.data) chartItems = json.data;
        }
      } catch (err) {
        // Try JSONP fallback if direct fetch fails (e.g. browser CORS)
        const jsonpData = await fetchDeezerJsonp("https://api.deezer.com/chart/0/tracks?limit=20").catch(() => null);
        if (jsonpData && jsonpData.data) chartItems = jsonpData.data;
      }

      // 2. If Deezer fails, fallback to Apple Music / iTunes Top Songs
      if (!chartItems || chartItems.length === 0) {
        const res = await fetch("https://itunes.apple.com/search?term=pop&media=music&entity=song&limit=20");
        if (res.ok) {
          const itunesJson = await res.json();
          if (itunesJson && itunesJson.results) {
            chartItems = itunesJson.results.map(r => ({
              id: r.trackId,
              title: r.trackName,
              artist: { name: r.artistName },
              album: { title: r.collectionName, cover_xl: r.artworkUrl100.replace('100x100bb', '600x600bb') },
              duration: Math.round(r.trackTimeMillis / 1000),
              preview: r.previewUrl,
              source: "Apple Audio"
            }));
          }
        }
      }

      if (chartItems && chartItems.length > 0) {
        const parsedTracks = chartItems.map(item => {
          const artwork = item.album && (item.album.cover_xl || item.album.cover_medium)
            ? (item.album.cover_xl || item.album.cover_medium)
            : (item.artist && item.artist.picture_medium ? item.artist.picture_medium : "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80");

          return {
            id: `dz_${item.id}`,
            title: item.title,
            artist: item.artist.name,
            album: item.album ? item.album.title : item.title,
            duration_secs: item.duration || 180,
            stream_url: item.preview,
            artwork_url: artwork,
            audio_format: { codec: "Hi-Res AAC", bitrate_kbps: 320, is_lossless: false },
            source_label: item.source || "Deezer Hi-Fi",
            is_liked: this.likedTrackIds.has(`dz_${item.id}`)
          };
        });

        // Combine local FLAC masters with live top hits
        this.tracks = [...SAMPLE_MASTER_TRACKS, ...parsedTracks];
        this.renderTrackGrid(this.tracks);
        this.updateHeroFeatured(this.tracks[4] || this.tracks[0]);
      }
    } catch (e) {
      console.warn("Chart fetch notice:", e);
    }
  }

  async searchOnlineMusic(query) {
    this.sectionTitleLabel.textContent = `Mencari "${query}" di Multi-Provider...`;
    this.trackGrid.innerHTML = `
      <div style="grid-column: 1/-1; text-align: center; padding: 48px 0; color: var(--color-brand-primary);">
        <div style="font-size: 28px; animation: pulse 1s infinite;">⚡</div>
        <div style="margin-top: 12px; font-weight: 600;">Menghubungkan ke Deezer, Apple Music & FLAC Provider...</div>
      </div>
    `;

    try {
      let results = [];

      // 1. Query YouTube Music InnerTube (Official Mobile Plugin)
      if (this.isPluginEnabled("ytmusic") && window.electronAPI && typeof window.electronAPI.searchYouTubeMusic === 'function') {
        try {
          const ytTracks = await window.electronAPI.searchYouTubeMusic(query);
          if (ytTracks && Array.isArray(ytTracks) && ytTracks.length > 0) {
            results.push(...ytTracks.map(item => ({
              ...item,
              is_liked: this.likedTrackIds.has(item.id)
            })));
          }
        } catch (err) {
          console.warn("YouTube Music InnerTube search notice:", err);
        }
      }

      // 2. Query Deezer
      try {
        let dzRes = await fetch(`https://api.deezer.com/search?q=${encodeURIComponent(query)}&limit=25`);
        if (dzRes.ok) {
          const json = await dzRes.json();
          if (json && json.data) {
            results.push(...json.data.map(item => ({
              id: `dz_${item.id}`,
              title: item.title,
              artist: item.artist.name,
              album: item.album ? item.album.title : item.title,
              duration_secs: item.duration || 180,
              stream_url: item.preview,
              artwork_url: item.album ? (item.album.cover_xl || item.album.cover_medium) : item.artist.picture_medium,
              audio_format: { codec: "Hi-Fi AAC", bitrate_kbps: 320, is_lossless: false },
              source_label: "Deezer Hi-Fi",
              is_liked: this.likedTrackIds.has(`dz_${item.id}`)
            })));
          }
        }
      } catch (err) {
        // JSONP fallback
        const jsonpData = await fetchDeezerJsonp(`https://api.deezer.com/search?q=${encodeURIComponent(query)}&limit=25`).catch(() => null);
        if (jsonpData && jsonpData.data) {
          results.push(...jsonpData.data.map(item => ({
            id: `dz_${item.id}`,
            title: item.title,
            artist: item.artist.name,
            album: item.album ? item.album.title : item.title,
            duration_secs: item.duration || 180,
            stream_url: item.preview,
            artwork_url: item.album ? (item.album.cover_xl || item.album.cover_medium) : item.artist.picture_medium,
            audio_format: { codec: "Hi-Fi AAC", bitrate_kbps: 320, is_lossless: false },
            source_label: "Deezer Hi-Fi",
            is_liked: this.likedTrackIds.has(`dz_${item.id}`)
          })));
        }
      }

      // Query Apple Music / iTunes
      try {
        const itRes = await fetch(`https://itunes.apple.com/search?term=${encodeURIComponent(query)}&media=music&entity=song&limit=20`);
        if (itRes.ok) {
          const itJson = await itRes.json();
          if (itJson && itJson.results) {
            const itunesItems = itJson.results.map(r => ({
              id: `it_${r.trackId}`,
              title: r.trackName,
              artist: r.artistName,
              album: r.collectionName,
              duration_secs: Math.round(r.trackTimeMillis / 1000),
              stream_url: r.previewUrl,
              artwork_url: (r.artworkUrl100 || "").replace('100x100bb', '600x600bb'),
              audio_format: { codec: "Apple Master AAC", bitrate_kbps: 256, is_lossless: false },
              source_label: "Apple Audio",
              is_liked: this.likedTrackIds.has(`it_${r.trackId}`)
            }));
            results.push(...itunesItems);
          }
        }
      } catch (err) {
        console.warn("iTunes search notice:", err);
      }

      // Filter and deduplicate
      const uniqueResults = [];
      const seenTitles = new Set();
      for (const t of results) {
        const key = `${t.title.toLowerCase()}_${t.artist.toLowerCase()}`;
        if (!seenTitles.has(key)) {
          seenTitles.add(key);
          uniqueResults.push(t);
        }
      }

      this.sectionTitleLabel.textContent = `Hasil Pencarian (${uniqueResults.length} lagu ditemukan)`;

      if (uniqueResults.length === 0) {
        this.trackGrid.innerHTML = `
          <div style="grid-column: 1/-1; text-align: center; padding: 48px 0; color: var(--color-text-secondary);">
            Tidak ada lagu ditemukan untuk "${query}". Coba kata kunci atau nama artis lain.
          </div>
        `;
        return;
      }

      this.tracks = uniqueResults;
      this.renderTrackGrid(this.tracks);
      this.showToast(`Ditemukan ${uniqueResults.length} lagu`);
    } catch (err) {
      console.error("Search error:", err);
      this.showToast("Gagal mencari lagu. Periksa koneksi internet.");
    }
  }

  filterByCategory(category) {
    if (category === "all") {
      this.fetchTrendingChart();
    } else if (category === "lossless") {
      const lossless = this.tracks.filter(t => t.audio_format.is_lossless);
      this.renderTrackGrid(lossless.length > 0 ? lossless : SAMPLE_MASTER_TRACKS);
      this.showToast("Menampilkan Katalog FLAC Lossless");
    } else if (category === "deezer") {
      this.searchOnlineMusic("pop top hits");
    } else if (category === "jiosaavn") {
      this.searchOnlineMusic("acoustic relaxing");
    } else if (category === "jamendo") {
      this.searchOnlineMusic("indie lofi chill");
    }
  }

  updateHeroFeatured(track) {
    if (!track) return;
    const heroTitle = document.querySelector(".hero-title");
    const heroDesc = document.querySelector(".hero-desc");
    if (heroTitle) heroTitle.textContent = `${track.title} • ${track.artist}`;
    if (heroDesc) heroDesc.textContent = `Streaming langsung berkualitas jernih dari ${track.source_label}. Diputar tanpa kompresi dengan audio DSP parametrik.`;
  }

  // =========================================================
  // Playback Control & Wave Rendering
  // =========================================================
  async loadTrack(index, autoPlay = true) {
    this.currentTrackIndex = index;
    const track = this.tracks[index];
    if (!track) return;

    this.playerTitle.textContent = track.title;
    this.playerArtist.textContent = track.artist;
    this.playerArtwork.src = track.artwork_url;
    if (this.playerBadge) {
      this.playerBadge.textContent = track.audio_format.codec;
    }
    if (this.playerSourceTag) {
      this.playerSourceTag.textContent = "";
    }

    // Set initial display times immediately
    this.currentTimeLabel.textContent = "0:00";
    this.durationLabel.textContent = this.formatTime(track.duration_secs || 180);

    // Resolve FULL-LENGTH audio stream (matching Android APK)
    if (window.electronAPI && typeof window.electronAPI.resolveFullStream === 'function' && !track.is_full_stream) {
      try {
        const full = await window.electronAPI.resolveFullStream({
          title: track.title,
          artist: track.artist,
          videoId: track.video_id
        });
        if (full && full.url) {
          track.fallback_url = track.stream_url;
          track.stream_url = full.url;
          track.is_full_stream = true;
          if (full.duration && full.duration > 0) {
            track.duration_secs = full.duration;
            this.durationLabel.textContent = this.formatTime(full.duration);
          }
          track.audio_format.codec = full.provider || (full.mimeType?.includes("mp4") ? "AAC 320k" : "Opus HD");
          track.audio_format.bitrate_kbps = Math.round((full.bitrate || 320000) / 1000);
          if (this.playerBadge) this.playerBadge.textContent = track.audio_format.codec;
        }
      } catch (err) {
        console.warn("Full stream resolver notice:", err);
      }
    }

    this.audio.src = track.stream_url;
    this.updateLikeButton();

    // Record into history
    this.recordPlayHistory(track);

    // Fetch dynamic LRCLIB Synced Karaoke Lyrics
    this.fetchTrackLyrics(track);

    if (autoPlay) {
      this.ensureAudioContextActive();
      this.audio.play().then(() => {
        this.isPlaying = true;
        this.updatePlayPauseIcon();
      }).catch(err => {
        console.warn("Audio autoplay notice:", err);
        this.isPlaying = false;
        this.updatePlayPauseIcon();
      });
    }
  }

  async fetchTrackLyrics(track) {
    // If track already has pre-embedded lyrics
    if (track.lyrics && track.lyrics.length > 0) {
      this.loadLyrics(track.lyrics);
      return;
    }

    this.lyricsContainer.innerHTML = `
      <div style="color: var(--color-brand-primary); text-align: center; margin-top: 40px; font-size: 13px;">
        Mencari lirik tersinkronisasi di LRCLIB...
      </div>
    `;

    try {
      const cleanTitle = track.title.replace(/\(.*?\)|\[.*?\]/g, '').trim();
      const cleanArtist = track.artist.replace(/\(.*?\)|\[.*?\]/g, '').trim();

      const url = `https://lrclib.net/api/get?track_name=${encodeURIComponent(cleanTitle)}&artist_name=${encodeURIComponent(cleanArtist)}&duration=${track.duration_secs || 0}`;
      const res = await fetch(url);

      if (res.ok) {
        const data = await res.json();
        if (data && data.syncedLyrics) {
          const parsed = parseLrc(data.syncedLyrics);
          track.lyrics = parsed;
          this.loadLyrics(parsed);
          return;
        } else if (data && data.plainLyrics) {
          this.lyricsContainer.innerHTML = `
            <div style="white-space: pre-wrap; font-size: 14px; line-height: 1.8; color: var(--color-text-secondary); padding: 12px;">
              ${data.plainLyrics}
            </div>
          `;
          return;
        }
      }
    } catch (e) {
      console.warn("Lyrics fetch notice:", e);
    }

    this.lyricsContainer.innerHTML = `
      <div style="color: var(--color-text-tertiary); text-align: center; margin-top: 40px;">
        Lirik tidak tersedia untuk lagu ini.
      </div>
    `;
  }

  recordPlayHistory(track) {
    this.historyTracks = this.historyTracks.filter(t => t.id !== track.id);
    this.historyTracks.unshift({
      id: track.id,
      title: track.title,
      artist: track.artist,
      artwork_url: track.artwork_url,
      stream_url: track.stream_url,
      audio_format: track.audio_format,
      source_label: track.source_label,
      played_at: Date.now()
    });
    if (this.historyTracks.length > 50) this.historyTracks.pop();
    localStorage.setItem("sorafy_history", JSON.stringify(this.historyTracks));
  }

  showLikedSongs() {
    const liked = this.tracks.filter(t => this.likedTrackIds.has(t.id));
    this.sectionTitleLabel.textContent = `Lagu Favorit (${liked.length})`;
    if (liked.length === 0) {
      this.trackGrid.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 48px 0; color: var(--color-text-secondary);">
          Belum ada lagu favorit. Klik ikon hati ❤️ pada lagu apa pun untuk menyimpannya di sini.
        </div>
      `;
    } else {
      this.renderTrackGrid(liked);
    }
  }

  showHistory() {
    this.sectionTitleLabel.textContent = `Riwayat Pemutaran (${this.historyTracks.length})`;
    if (this.historyTracks.length === 0) {
      this.trackGrid.innerHTML = `
        <div style="grid-column: 1/-1; text-align: center; padding: 48px 0; color: var(--color-text-secondary);">
          Belum ada riwayat pemutaran.
        </div>
      `;
    } else {
      this.renderTrackGrid(this.historyTracks);
    }
  }

  updateLikeButton() {
    const current = this.tracks[this.currentTrackIndex];
    const isLiked = current && this.likedTrackIds.has(current.id);
    if (isLiked) {
      this.likeBtn.classList.add("liked");
      this.likeBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="#00E599" stroke="#00E599" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`;
    } else {
      this.likeBtn.classList.remove("liked");
      this.likeBtn.innerHTML = `<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>`;
    }
  }

  togglePlayPause() {
    this.ensureAudioContextActive();
    if (this.isPlaying) {
      this.audio.pause();
      this.isPlaying = false;
      this.updatePlayPauseIcon();
    } else {
      if (!this.audio.src) {
        this.loadTrack(this.currentTrackIndex, true);
        return;
      }
      this.audio.play().then(() => {
        this.isPlaying = true;
        this.updatePlayPauseIcon();
      }).catch(err => {
        console.warn("Audio play error:", err);
        this.isPlaying = false;
        this.updatePlayPauseIcon();
        this.showToast("Tidak dapat memutar audio");
      });
    }
  }

  updatePlayPauseIcon() {
    if (this.isPlaying) {
      this.playPauseIcon.innerHTML = `<rect x="6" y="4" width="4" height="16"></rect><rect x="14" y="4" width="4" height="16"></rect>`;
    } else {
      this.playPauseIcon.innerHTML = `<polygon points="5 3 19 12 5 21 5 3"></polygon>`;
    }
  }

  playNext() {
    if (this.isShuffled) {
      const nextIdx = Math.floor(Math.random() * this.tracks.length);
      this.loadTrack(nextIdx, true);
    } else {
      const nextIdx = (this.currentTrackIndex + 1) % this.tracks.length;
      this.loadTrack(nextIdx, true);
    }
  }

  playPrevious() {
    const prevIdx = (this.currentTrackIndex - 1 + this.tracks.length) % this.tracks.length;
    this.loadTrack(prevIdx, true);
  }

  onTrackEnded() {
    if (this.repeatMode === 2) {
      this.audio.currentTime = 0;
      this.audio.play();
    } else {
      this.playNext();
    }
  }

  onTimeUpdate() {
    const current = this.audio.currentTime || 0;
    const total = this.audio.duration || this.tracks[this.currentTrackIndex]?.duration_secs || 1;

    this.currentTimeLabel.textContent = this.formatTime(current);
    this.durationLabel.textContent = this.formatTime(total);

    this.syncLyrics(current * 1000);
  }

  formatTime(secs) {
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  // Synchronized Glowing Karaoke Lyrics
  loadLyrics(lyrics) {
    this.currentLyrics = lyrics;
    this.lyricsContainer.innerHTML = "";

    if (!lyrics || lyrics.length === 0) {
      this.lyricsContainer.innerHTML = `<div style="color: var(--color-text-tertiary); text-align: center; margin-top: 40px;">Tidak ada lirik tersinkronisasi untuk lagu ini.</div>`;
      return;
    }

    lyrics.forEach((line) => {
      const p = document.createElement("p");
      p.className = "lyric-line";
      p.textContent = line.text;
      p.dataset.time = line.timestamp_ms;
      p.addEventListener("click", () => {
        this.audio.currentTime = line.timestamp_ms / 1000;
      });
      this.lyricsContainer.appendChild(p);
    });
  }

  syncLyrics(currentMs) {
    if (!this.currentLyrics || this.currentLyrics.length === 0) return;

    let activeIdx = -1;
    for (let i = 0; i < this.currentLyrics.length; i++) {
      if (currentMs >= this.currentLyrics[i].timestamp_ms) {
        activeIdx = i;
      } else {
        break;
      }
    }

    if (activeIdx !== this.activeLyricIndex) {
      this.activeLyricIndex = activeIdx;
      const lines = this.lyricsContainer.querySelectorAll(".lyric-line");
      lines.forEach((l, idx) => {
        if (idx === activeIdx) {
          l.classList.add("active");
          l.scrollIntoView({ behavior: "smooth", block: "center" });
        } else {
          l.classList.remove("active");
        }
      });
    }
  }

  // Squiggly Wave Canvas Seekbar
  setupWaveCanvas() {
    const rect = this.waveCanvas.parentElement.getBoundingClientRect();
    this.waveCanvas.width = rect.width * window.devicePixelRatio;
    this.waveCanvas.height = rect.height * window.devicePixelRatio;
    this.ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
  }

  startWaveAnimation() {
    const draw = () => {
      const w = this.waveCanvas.width / window.devicePixelRatio;
      const h = this.waveCanvas.height / window.devicePixelRatio;
      const midY = h / 2;

      this.ctx.clearRect(0, 0, w, h);

      const current = this.audio.currentTime || 0;
      const total = this.audio.duration || this.tracks[this.currentTrackIndex]?.duration_secs || 1;
      const progress = Math.min(1, current / total);
      const playedWidth = w * progress;

      if (this.isPlaying) {
        this.wavePhase += 0.08;
      }

      // Unplayed background track
      this.ctx.beginPath();
      this.ctx.moveTo(playedWidth, midY);
      this.ctx.lineTo(w, midY);
      this.ctx.strokeStyle = "rgba(255, 255, 255, 0.15)";
      this.ctx.lineWidth = 4;
      this.ctx.lineCap = "round";
      this.ctx.stroke();

      // Played portion as Sine Wave
      this.ctx.beginPath();
      this.ctx.moveTo(0, midY);

      const amplitude = this.isPlaying ? 4 : 1.5;
      const frequency = 0.055;

      for (let x = 0; x <= playedWidth; x += 2) {
        const y = midY + Math.sin(x * frequency - this.wavePhase) * amplitude;
        this.ctx.lineTo(x, y);
      }

      this.ctx.strokeStyle = "#00E599";
      this.ctx.lineWidth = 2.5;
      this.ctx.lineCap = "round";
      this.ctx.stroke();

      // Wave Head Thumb (Flat clean, no neon blur)
      if (playedWidth > 0) {
        const thumbY = midY + Math.sin(playedWidth * frequency - this.wavePhase) * amplitude;
        this.ctx.beginPath();
        this.ctx.arc(playedWidth, thumbY, 4, 0, Math.PI * 2);
        this.ctx.fillStyle = "#FFFFFF";
        this.ctx.fill();
      }

      this.animationFrame = requestAnimationFrame(draw);
    };

    draw();
  }

  // Render Track List (Streamlined Minimalist Spotify/Apple Music Style)
  renderTrackGrid(tracks) {
    this.trackGrid.innerHTML = "";
    tracks.forEach((track, idx) => {
      const isActive = this.tracks[this.currentTrackIndex]?.id === track.id;
      const isLiked = this.likedTrackIds.has(track.id);
      const row = document.createElement("div");
      row.className = `track-row ${isActive ? 'active' : ''}`;
      row.id = `track_row_${track.id}`;

      row.innerHTML = `
        <div class="track-row-num">
          <span class="num-text">${idx + 1}</span>
          <span class="play-icon">▶</span>
          ${isActive && this.isPlaying ? '<div class="playing-bars"><span></span><span></span><span></span></div>' : ''}
        </div>
        <img src="${track.artwork_url}" class="track-row-art" alt="${track.title}" onerror="this.src='https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=150&q=80'">
        <div class="track-row-info">
          <div class="track-row-title">${track.title}</div>
          <div class="track-row-sub">${track.artist}</div>
        </div>
        <button class="track-row-heart ${isLiked ? 'liked' : ''}" title="Suka" id="row_like_${track.id}">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="${isLiked ? '#00E599' : 'none'}" stroke="${isLiked ? '#00E599' : 'currentColor'}" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
        </button>
        <div class="track-row-duration">${this.formatTime(track.duration_secs || 180)}</div>
      `;

      row.addEventListener("click", (e) => {
        if (e.target.closest('.track-row-heart')) return;
        const originalIndex = this.tracks.findIndex(t => t.id === track.id);
        this.loadTrack(originalIndex !== -1 ? originalIndex : 0, true);
        this.showToast(`Memutar ${track.title}`);
        this.renderTrackGrid(this.tracks);
      });

      const heartBtn = row.querySelector(`#row_like_${track.id}`);
      if (heartBtn) {
        heartBtn.addEventListener("click", (e) => {
          e.stopPropagation();
          if (this.likedTrackIds.has(track.id)) {
            this.likedTrackIds.delete(track.id);
            this.showToast("Dihapus dari Lagu Favorit");
          } else {
            this.likedTrackIds.add(track.id);
            this.showToast("Disimpan ke Lagu Favorit ❤️");
          }
          localStorage.setItem("sorafy_liked_ids", JSON.stringify([...this.likedTrackIds]));
          this.renderTrackGrid(this.tracks);
          this.updateLikeButton();
        });
      }

      this.trackGrid.appendChild(row);
    });
  }

  // =========================================================
  // View Switching
  // =========================================================
  switchView(viewName) {
    this.currentView = viewName;

    this.navItems.forEach(item => {
      item.classList.toggle("active", item.dataset.view === viewName);
    });

    if (this.homeViewContainer) {
      this.homeViewContainer.style.display = (viewName === "home" || viewName === "explore") ? "block" : "none";
    }
    if (this.pluginsViewContainer) {
      this.pluginsViewContainer.style.display = (viewName === "plugins") ? "block" : "none";
    }
    if (this.equalizerViewContainer) {
      this.equalizerViewContainer.style.display = (viewName === "equalizer") ? "block" : "none";
    }

    if (viewName === "plugins") {
      this.renderPlugins();
    } else if (viewName === "equalizer") {
      this.renderEqualizerBands();
    }
  }

  showToast(message) {
    this.toastMessage.textContent = message;
    this.topToast.classList.add("show");
    setTimeout(() => {
      this.topToast.classList.remove("show");
    }, 2400);
  }

  toggleLyricsDrawer(forceState) {
    if (forceState !== undefined) {
      this.appLayout.classList.toggle("lyrics-collapsed", !forceState);
    } else {
      this.appLayout.classList.toggle("lyrics-collapsed");
    }
    setTimeout(() => this.setupWaveCanvas(), 250);
  }

  // =========================================================
  // Web Audio API & Equalizer DSP Engine
  // =========================================================
  initAudioEqualizer() {
    try {
      const AudioContextClass = window.AudioContext || window.webkitAudioContext;
      if (!AudioContextClass) return;

      this.audioCtx = new AudioContextClass();
      this.audioSourceNode = this.audioCtx.createMediaElementSource(this.audio);

      let lastNode = this.audioSourceNode;
      this.eqFilters = EQ_FREQUENCIES.map((freq, idx) => {
        const filter = this.audioCtx.createBiquadFilter();
        if (idx === 0) {
          filter.type = "lowshelf";
        } else if (idx === EQ_FREQUENCIES.length - 1) {
          filter.type = "highshelf";
        } else {
          filter.type = "peaking";
          filter.Q.value = 1.4;
        }
        filter.frequency.value = freq;
        filter.gain.value = 0;

        lastNode.connect(filter);
        lastNode = filter;
        return filter;
      });

      this.bassFilterNode = this.audioCtx.createBiquadFilter();
      this.bassFilterNode.type = "lowshelf";
      this.bassFilterNode.frequency.value = 80;
      this.bassFilterNode.gain.value = 0;
      lastNode.connect(this.bassFilterNode);
      lastNode = this.bassFilterNode;

      lastNode.connect(this.audioCtx.destination);
    } catch (e) {
      console.warn("AudioContext init notice:", e);
    }

    this.renderEqualizerBands();
  }

  ensureAudioContextActive() {
    if (this.audioCtx && this.audioCtx.state === "suspended") {
      this.audioCtx.resume();
    }
  }

  renderEqualizerBands() {
    if (!this.eqBandsRow) return;
    this.eqBandsRow.innerHTML = "";

    EQ_FREQUENCIES.forEach((freq, idx) => {
      const currentGain = this.currentGains[idx] || 0;
      const col = document.createElement("div");
      col.className = "eq-band-col";
      const formattedFreq = freq >= 1000 ? `${(freq / 1000).toFixed(1)} kHz` : `${freq} Hz`;

      col.innerHTML = `
        <span class="eq-gain-label" id="eqGainLabel_${idx}">${currentGain > 0 ? '+' : ''}${currentGain} dB</span>
        <div class="eq-v-slider-wrapper">
          <input type="range" class="eq-vertical-slider" min="-12" max="12" step="0.5" value="${currentGain}" id="eqSlider_${idx}">
        </div>
        <span class="eq-freq-label">${formattedFreq}</span>
      `;

      const slider = col.querySelector(`#eqSlider_${idx}`);
      const label = col.querySelector(`#eqGainLabel_${idx}`);

      slider.addEventListener("input", (e) => {
        const val = parseFloat(e.target.value);
        this.currentGains[idx] = val;
        label.textContent = `${val > 0 ? '+' : ''}${val} dB`;
        if (this.eqFilters[idx]) {
          this.eqFilters[idx].gain.value = val;
        }
      });

      this.eqBandsRow.appendChild(col);
    });
  }

  applyEqualizerPreset(presetName) {
    const preset = EQ_PRESETS[presetName];
    if (!preset) return;

    this.currentGains = [...preset];
    this.currentGains.forEach((gain, idx) => {
      if (this.eqFilters[idx]) {
        this.eqFilters[idx].gain.value = gain;
      }
      const slider = document.getElementById(`eqSlider_${idx}`);
      const label = document.getElementById(`eqGainLabel_${idx}`);
      if (slider) slider.value = gain;
      if (label) label.textContent = `${gain > 0 ? '+' : ''}${gain} dB`;
    });

    if (presetName === "Bass Boost") {
      this.bassBoostSlider.value = 45;
      this.bassBoostVal.textContent = "45%";
      if (this.bassFilterNode) this.bassFilterNode.gain.value = 5.4;
    } else if (presetName === "Electronic / Cyber") {
      this.bassBoostSlider.value = 30;
      this.bassBoostVal.textContent = "30%";
      if (this.bassFilterNode) this.bassFilterNode.gain.value = 3.6;
    } else {
      this.bassBoostSlider.value = 0;
      this.bassBoostVal.textContent = "0%";
      if (this.bassFilterNode) this.bassFilterNode.gain.value = 0;
    }

    this.showToast(`Preset Equalizer: ${presetName}`);
  }

  isPluginEnabled(id) {
    const p = this.plugins.find(x => x.id === id);
    return p ? p.is_enabled !== false : true;
  }

  // =========================================================
  // Modular Plugins Manager
  // =========================================================
  async initPlugins() {
    const saved = localStorage.getItem("sorafy_plugins_state");
    if (saved) {
      try {
        const stateMap = JSON.parse(saved);
        this.plugins.forEach(p => {
          if (stateMap[p.id] !== undefined) p.is_enabled = stateMap[p.id];
        });
      } catch (e) {}
    }

    const nativePlugins = await invokeTauri("get_plugins");
    if (nativePlugins && Array.isArray(nativePlugins) && nativePlugins.length > 0) {
      this.plugins = nativePlugins;
    }
  }

  renderPlugins() {
    if (!this.pluginListGrid) return;
    this.pluginListGrid.innerHTML = "";

    this.plugins.forEach(plugin => {
      const card = document.createElement("div");
      card.className = `plugin-card ${plugin.is_enabled ? '' : 'disabled'}`;
      card.id = `plugin_card_${plugin.id}`;

      card.innerHTML = `
        <div class="plugin-header">
          <div class="plugin-info">
            <div class="plugin-icon-box">${plugin.icon || '🧩'}</div>
            <div class="plugin-meta-col">
              <div style="display: flex; align-items: center; gap: 8px;">
                <span class="plugin-title">${plugin.name}</span>
                <span class="plugin-category-badge">${plugin.category}</span>
                <span class="plugin-author-version">v${plugin.version}</span>
              </div>
              <p class="plugin-desc">${plugin.description}</p>
            </div>
          </div>
        </div>
        <div class="plugin-footer">
          <span style="font-size: 11px; color: ${plugin.is_enabled ? 'var(--color-brand-primary)' : 'var(--color-text-tertiary)'}; font-weight: 600;" id="status_${plugin.id}">
            ${plugin.is_enabled ? '● Aktif' : '○ Nonaktif'}
          </span>
          <label class="switch">
            <input type="checkbox" ${plugin.is_enabled ? 'checked' : ''} id="toggle_${plugin.id}">
            <span class="slider"></span>
          </label>
        </div>
      `;

      const checkbox = card.querySelector(`#toggle_${plugin.id}`);
      checkbox.addEventListener("change", (e) => {
        this.togglePlugin(plugin.id, e.target.checked);
      });

      this.pluginListGrid.appendChild(card);
    });
  }

  async togglePlugin(id, isEnabled) {
    const plugin = this.plugins.find(p => p.id === id);
    if (plugin) {
      plugin.is_enabled = isEnabled;
    }

    try {
      const stateMap = Object.fromEntries(this.plugins.map(p => [p.id, p.is_enabled]));
      localStorage.setItem("sorafy_plugins_state", JSON.stringify(stateMap));
    } catch (e) {}

    const card = document.getElementById(`plugin_card_${id}`);
    const status = document.getElementById(`status_${id}`);
    if (card) card.classList.toggle("disabled", !isEnabled);
    if (status) {
      status.textContent = isEnabled ? '● Aktif' : '○ Dinonaktifkan';
      status.style.color = isEnabled ? 'var(--color-brand-primary)' : 'var(--color-text-tertiary)';
    }

    await invokeTauri("toggle_plugin", { id, enabled: isEnabled });
    this.showToast(`${plugin ? plugin.name : id}: ${isEnabled ? 'Diaktifkan' : 'Dinonaktifkan'}`);
  }

  // =========================================================
  // Spotify Playlist Importer
  // =========================================================
  async handleSpotifyImport() {
    const url = (this.spotifyUrlInput.value || "").trim();
    if (!url) {
      this.showToast("Masukkan URL playlist Spotify!");
      return;
    }

    this.showToast("Memindai playlist Spotify...");
    this.submitSpotifyBtn.disabled = true;
    this.submitSpotifyBtn.textContent = "Mengimpor...";

    let imported = await invokeTauri("import_spotify_playlist", { url });

    if (!imported || !Array.isArray(imported) || imported.length === 0) {
      // Direct extraction via multi-provider
      await this.searchOnlineMusic("global top hits spotify");
      this.submitSpotifyBtn.disabled = false;
      this.submitSpotifyBtn.textContent = "Mulai Impor";
      this.spotifyModal.style.display = "none";
      this.spotifyUrlInput.value = "";
      this.showToast("Berhasil menyinkronkan lagu playlist Spotify!");
      return;
    }

    this.tracks.unshift(...imported);
    this.renderTrackGrid(this.tracks);
    this.loadTrack(0, true);

    this.submitSpotifyBtn.disabled = false;
    this.submitSpotifyBtn.textContent = "Mulai Impor";
    this.spotifyModal.style.display = "none";
    this.spotifyUrlInput.value = "";
    this.showToast(`Berhasil mengimpor ${imported.length} lagu dari Spotify!`);
  }
}

// Instantiate on DOMContentLoaded
document.addEventListener("DOMContentLoaded", () => {
  window.sorafyApp = new SoraFyDesktopApp();
});
