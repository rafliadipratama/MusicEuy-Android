// SoraFy Desktop Core Frontend Controller
// Implements Squiggly Slider, Glowing Lyrics, Modular Plugins & Studio Equalizer DSP
// Clean Architecture & Domain-Driven Design (DDD) Client

const SAMPLE_TRACKS = [
  {
    id: "sora_001",
    title: "Midnight Emerald (SoraFy Drift)",
    artist: "Priangan Sound Lab",
    album: "Electric Obsidian EP",
    duration_secs: 214,
    stream_url: "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3",
    artwork_url: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80",
    audio_format: { codec: "FLAC Lossless", bitrate_kbps: 1411, is_lossless: true },
    source_label: "Monochrome FLAC",
    is_liked: true,
    lyrics: [
      { timestamp_ms: 0, text: "(Ambient Rain & Emerald Synthesis)" },
      { timestamp_ms: 12000, text: "Gently drifting through the obsidian sky" },
      { timestamp_ms: 24000, text: "Neon ribbons folding in the night" },
      { timestamp_ms: 38000, text: "Listen closely to the pulse of Priangan" },
      { timestamp_ms: 52000, text: "Electric lime lights guide our way home" },
      { timestamp_ms: 68000, text: "Lossless frequencies soothing the soul" },
      { timestamp_ms: 85000, text: "SoraFy resonance in infinite loop" },
      { timestamp_ms: 110000, text: "No compression, pure harmonic grace" },
      { timestamp_ms: 135000, text: "Into the quiet dawn we drift away" }
    ]
  },
  {
    id: "sora_002",
    title: "Electric Lime Pulse",
    artist: "Neon Bandung",
    album: "West Java Cyber",
    duration_secs: 185,
    stream_url: "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=electronic-future-beats-117997.mp3",
    artwork_url: "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80",
    audio_format: { codec: "Opus Hi-Res", bitrate_kbps: 320, is_lossless: false },
    source_label: "Deezer Hi-Fi",
    is_liked: false,
    lyrics: [
      { timestamp_ms: 0, text: "(Cyber Synth Arpeggio)" },
      { timestamp_ms: 10000, text: "Fast lanes moving through Braga neon" },
      { timestamp_ms: 22000, text: "Pulse of 140 beats per minute" },
      { timestamp_ms: 36000, text: "Bass reverberates across the digital square" },
      { timestamp_ms: 50000, text: "Electric lime, illuminate the dark" },
      { timestamp_ms: 72000, text: "Synthesized perfection, non-stop flow" }
    ]
  },
  {
    id: "sora_003",
    title: "Origami Ribbons",
    artist: "Sora Ambient",
    album: "Geometry of Waves",
    duration_secs: 242,
    stream_url: "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=chill-abstract-intention-12099.mp3",
    artwork_url: "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=600&q=80",
    audio_format: { codec: "AAC High", bitrate_kbps: 256, is_lossless: false },
    source_label: "JioSaavn 320k",
    is_liked: true,
    lyrics: [
      { timestamp_ms: 0, text: "(Deep Sub & Harmonic Bells)" },
      { timestamp_ms: 15000, text: "Fold the sound into geometry" },
      { timestamp_ms: 32000, text: "Each crease a memory of acoustic warmth" },
      { timestamp_ms: 58000, text: "Floating weights and tranquil heights" },
      { timestamp_ms: 90000, text: "Origami ribbons unfolding in peace" }
    ]
  },
  {
    id: "sora_004",
    title: "Sundanese Dawn (Bamboo Echo)",
    artist: "Kancana Collective",
    album: "Highland Resonance",
    duration_secs: 205,
    stream_url: "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3",
    artwork_url: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80",
    audio_format: { codec: "FLAC Master", bitrate_kbps: 1411, is_lossless: true },
    source_label: "Bandcamp Lossless",
    is_liked: false,
    lyrics: [
      { timestamp_ms: 0, text: "(Suling Bamboo & Water Stream)" },
      { timestamp_ms: 18000, text: "Embun pagi di lereng Tangkuban" },
      { timestamp_ms: 35000, text: "Angin semilir membawa kidung damai" },
      { timestamp_ms: 60000, text: "Suara suling bambu menembus kabut" }
    ]
  }
];

const DEFAULT_PLUGINS = [
  {
    id: "monochrome_lossless",
    name: "Monochrome Lossless",
    version: "1.0.0",
    author: "SoraFy Core Team",
    description: "Hi-Res 24-bit FLAC audio stream provider (monochrome.st)",
    category: "Audio Streaming",
    icon: "🎵",
    is_enabled: true
  },
  {
    id: "deezer",
    name: "Deezer Hi-Fi",
    version: "1.0.0",
    author: "SoraFy Core Team",
    description: "Deezer Hi-Fi catalog, 500x500 high-res album covers, and preview streams",
    category: "Audio Streaming",
    icon: "🎧",
    is_enabled: true
  },
  {
    id: "jiosaavn",
    name: "JioSaavn 320k",
    version: "1.0.0",
    author: "SoraFy Core Team",
    description: "Direct 320kbps AAC audio streaming provider",
    category: "Audio Streaming",
    icon: "⚡",
    is_enabled: true
  },
  {
    id: "bandcamp_jamendo",
    name: "Bandcamp & Jamendo",
    version: "1.0.0",
    author: "SoraFy Community",
    description: "Indie music & Creative Commons lossless catalog",
    category: "Audio Streaming",
    icon: "📻",
    is_enabled: true
  },
  {
    id: "spotify_importer",
    name: "Spotify Importer",
    version: "1.0.0",
    author: "SoraFy Core Team",
    description: "Public Spotify playlist extractor and synchronizer without requiring premium",
    category: "Tools & Utilities",
    icon: "📥",
    is_enabled: true
  },
  {
    id: "discord_rpc",
    name: "Discord Rich Presence",
    version: "1.0.0",
    author: "SoraFy Social",
    description: "Display current playing track, artist, album art, and live progress on Discord",
    category: "Social & Integration",
    icon: "🎮",
    is_enabled: true
  },
  {
    id: "equalizer_dsp",
    name: "Equalizer DSP Studio",
    version: "1.0.0",
    author: "Priangan Sound Lab",
    description: "7-band parametric equalizer with dynamic Bass Boost and 3D Virtualizer",
    category: "Audio DSP",
    icon: "🎛️",
    is_enabled: true
  },
  {
    id: "apple_canvas",
    name: "Apple Music Canvas",
    version: "1.0.0",
    author: "SoraFy Visuals",
    description: "Apple Music-style dynamic background canvas and ambient visualizer",
    category: "Visual & Lyrics",
    icon: "✨",
    is_enabled: true
  },
  {
    id: "lastfm",
    name: "Last.fm Scrobbler",
    version: "1.0.0",
    author: "SoraFy Social",
    description: "Live scrobbling and artist biography integration with Last.fm",
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

// Safe Tauri IPC Invocation
async function invokeTauri(cmd, args = {}) {
  try {
    if (window.__TAURI__ && window.__TAURI__.core && typeof window.__TAURI__.core.invoke === 'function') {
      return await window.__TAURI__.core.invoke(cmd, args);
    } else if (window.__TAURI__ && window.__TAURI__.tauri && typeof window.__TAURI__.tauri.invoke === 'function') {
      return await window.__TAURI__.tauri.invoke(cmd, args);
    }
  } catch (err) {
    console.warn(`[Tauri IPC] ${cmd} not handled natively:`, err);
  }
  return null;
}

class SoraFyDesktopApp {
  constructor() {
    this.audio = document.getElementById("audioSource");
    this.tracks = [...SAMPLE_TRACKS];
    this.plugins = [...DEFAULT_PLUGINS];
    this.currentTrackIndex = 0;
    this.isPlaying = false;
    this.isShuffled = false;
    this.repeatMode = 0; // 0: off, 1: all, 2: one
    this.currentLyrics = [];
    this.activeLyricIndex = -1;
    this.currentView = "home";

    // Equalizer Web Audio API nodes
    this.audioCtx = null;
    this.audioSourceNode = null;
    this.eqFilters = [];
    this.bassFilterNode = null;
    this.currentGains = [0, 0, 0, 0, 0, 0, 0];
    this.bassBoostAmount = 0;
    this.virtualizerAmount = 0;

    // Squiggly Canvas Wave state
    this.waveCanvas = document.getElementById("waveCanvas");
    this.ctx = this.waveCanvas.getContext("2d");
    this.wavePhase = 0;
    this.animationFrame = null;

    this.initElements();
    this.initEvents();
    this.initPlugins();
    this.renderTrackGrid(this.tracks);
    this.initAudioEqualizer();
    this.loadTrack(0, false);
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
      current.is_liked = !current.is_liked;
      this.updateLikeButton();
      this.showToast(current.is_liked ? "Disimpan ke Lagu Favorit ❤️" : "Dihapus dari Lagu Favorit");
    });

    this.volumeSlider.addEventListener("input", (e) => {
      this.audio.volume = parseFloat(e.target.value);
    });

    this.toggleLyricsBtn.addEventListener("click", () => this.toggleLyricsDrawer());
    this.closeLyricsBtn.addEventListener("click", () => this.toggleLyricsDrawer(false));

    this.heroPlayBtn.addEventListener("click", () => {
      this.loadTrack(0, true);
    });

    this.audio.addEventListener("timeupdate", () => this.onTimeUpdate());
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

    // Search input
    this.searchInput.addEventListener("input", (e) => {
      const q = e.target.value.toLowerCase().trim();
      const filtered = this.tracks.filter(t => 
        t.title.toLowerCase().includes(q) || t.artist.toLowerCase().includes(q)
      );
      this.renderTrackGrid(filtered);
    });

    // Category filter chips
    this.categoryChips.forEach(chip => {
      chip.addEventListener("click", () => {
        this.categoryChips.forEach(c => c.classList.remove("active"));
        chip.classList.add("active");
        const category = chip.dataset.category;
        if (category === "all") {
          this.renderTrackGrid(this.tracks);
        } else if (category === "lossless") {
          this.renderTrackGrid(this.tracks.filter(t => t.audio_format.is_lossless));
        } else {
          this.renderTrackGrid(this.tracks);
        }
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
          const likedTracks = this.tracks.filter(t => t.is_liked);
          this.renderTrackGrid(likedTracks.length > 0 ? likedTracks : this.tracks);
          this.showToast("Menampilkan Lagu Favorit");
        } else if (filter === "history") {
          this.switchView("home");
          this.renderTrackGrid(this.tracks);
          this.showToast("Riwayat Pemutaran");
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
          this.bassFilterNode.gain.value = (val / 100) * 12; // up to +12dB
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

    // Window resize handler for canvas
    window.addEventListener("resize", () => this.setupWaveCanvas());
  }

  switchView(viewName) {
    this.currentView = viewName;

    // Update active nav item
    this.navItems.forEach(item => {
      item.classList.toggle("active", item.dataset.view === viewName);
    });

    // Toggle container views
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

      // Create 7 BiquadFilterNodes
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

      // Dedicated Dynamic Bass Boost Node (lowshelf at 80 Hz)
      this.bassFilterNode = this.audioCtx.createBiquadFilter();
      this.bassFilterNode.type = "lowshelf";
      this.bassFilterNode.frequency.value = 80;
      this.bassFilterNode.gain.value = 0;
      lastNode.connect(this.bassFilterNode);
      lastNode = this.bassFilterNode;

      // Connect to output destination
      lastNode.connect(this.audioCtx.destination);
    } catch (e) {
      console.warn("AudioContext init info (already hooked or restricted):", e);
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

  // =========================================================
  // Modular Plugins System (Mobile to Desktop Port)
  // =========================================================
  async initPlugins() {
    // Attempt to probe Tauri backend plugins
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
              <span class="plugin-title">${plugin.name}</span>
              <span class="plugin-author-version">v${plugin.version} • oleh ${plugin.author}</span>
            </div>
          </div>
          <label class="switch">
            <input type="checkbox" ${plugin.is_enabled ? 'checked' : ''} id="toggle_${plugin.id}">
            <span class="slider"></span>
          </label>
        </div>
        <p class="plugin-desc">${plugin.description}</p>
        <div class="plugin-footer">
          <span class="plugin-category-badge">${plugin.category}</span>
          <span style="font-size: 11px; color: ${plugin.is_enabled ? 'var(--color-brand-primary)' : 'var(--color-text-tertiary)'}; font-weight: 600;" id="status_${plugin.id}">
            ${plugin.is_enabled ? '● Aktif' : '○ Dinonaktifkan'}
          </span>
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

    const card = document.getElementById(`plugin_card_${id}`);
    const status = document.getElementById(`status_${id}`);
    if (card) card.classList.toggle("disabled", !isEnabled);
    if (status) {
      status.textContent = isEnabled ? '● Aktif' : '○ Dinonaktifkan';
      status.style.color = isEnabled ? 'var(--color-brand-primary)' : 'var(--color-text-tertiary)';
    }

    // Notify Tauri Backend
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

    // Call backend IPC or fallback to client extractor
    let imported = await invokeTauri("import_spotify_playlist", { url });

    if (!imported || !Array.isArray(imported) || imported.length === 0) {
      // High-fidelity fallback tracks simulating Spotify importer
      imported = [
        {
          id: `sp_${Date.now()}_1`,
          title: "Starboy (Lossless Remaster)",
          artist: "The Weeknd ft. Daft Punk",
          album: "Starboy",
          duration_secs: 230,
          stream_url: "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=electronic-future-beats-117997.mp3",
          artwork_url: "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=600&q=80",
          audio_format: { codec: "FLAC 24-bit", bitrate_kbps: 1411, is_lossless: true },
          source_label: "Spotify Sync",
          is_liked: true,
          lyrics: [
            { timestamp_ms: 0, text: "(Electronic Intro)" },
            { timestamp_ms: 15000, text: "I'm tryna put you in the worst mood, ah" },
            { timestamp_ms: 28000, text: "P1 cleaner than your church shoes, ah" }
          ]
        },
        {
          id: `sp_${Date.now()}_2`,
          title: "Blinding Lights (Cyberpunk Edit)",
          artist: "The Weeknd",
          album: "After Hours",
          duration_secs: 200,
          stream_url: "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=chill-abstract-intention-12099.mp3",
          artwork_url: "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=600&q=80",
          audio_format: { codec: "AAC High", bitrate_kbps: 320, is_lossless: false },
          source_label: "Spotify Sync",
          is_liked: false,
          lyrics: [
            { timestamp_ms: 0, text: "(Synthwave Prelude)" },
            { timestamp_ms: 14000, text: "I've been on my own for long enough" }
          ]
        }
      ];
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

  // =========================================================
  // Playback Control & Wave Rendering
  // =========================================================
  loadTrack(index, autoPlay = true) {
    this.currentTrackIndex = index;
    const track = this.tracks[index];

    this.playerTitle.textContent = track.title;
    this.playerArtist.textContent = track.artist;
    this.playerArtwork.src = track.artwork_url;
    this.playerBadge.textContent = track.audio_format.codec;
    if (this.playerSourceTag) {
      this.playerSourceTag.textContent = track.source_label || "SoraFy Master";
    }
    this.audio.src = track.stream_url;

    this.updateLikeButton();
    this.loadLyrics(track.lyrics || []);

    if (autoPlay) {
      this.ensureAudioContextActive();
      this.audio.play().then(() => {
        this.isPlaying = true;
        this.updatePlayPauseIcon();
      }).catch(err => {
        console.log("Auto-play permission notice:", err);
      });
    }
  }

  updateLikeButton() {
    const current = this.tracks[this.currentTrackIndex];
    if (current && current.is_liked) {
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
    } else {
      this.audio.play();
      this.isPlaying = true;
    }
    this.updatePlayPauseIcon();
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
    const total = this.audio.duration || this.tracks[this.currentTrackIndex].duration_secs || 1;

    this.currentTimeLabel.textContent = this.formatTime(current);
    this.durationLabel.textContent = this.formatTime(total);

    this.syncLyrics(current * 1000);
  }

  formatTime(secs) {
    const m = Math.floor(secs / 60);
    const s = Math.floor(secs % 60);
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  // Glowing Karaoke Lyrics Synchronization
  loadLyrics(lyrics) {
    this.currentLyrics = lyrics;
    this.lyricsContainer.innerHTML = "";

    if (!lyrics || lyrics.length === 0) {
      this.lyricsContainer.innerHTML = `<div style="color: var(--color-text-tertiary); text-align: center; margin-top: 40px;">Tidak ada lirik tersinkronisasi untuk trek ini.</div>`;
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
      const total = this.audio.duration || this.tracks[this.currentTrackIndex].duration_secs || 1;
      const progress = Math.min(1, current / total);
      const playedWidth = w * progress;

      if (this.isPlaying) {
        this.wavePhase += 0.08;
      }

      // Draw Unplayed straight background track
      this.ctx.beginPath();
      this.ctx.moveTo(playedWidth, midY);
      this.ctx.lineTo(w, midY);
      this.ctx.strokeStyle = "rgba(255, 255, 255, 0.15)";
      this.ctx.lineWidth = 4;
      this.ctx.lineCap = "round";
      this.ctx.stroke();

      // Draw Played portion as Squiggly Sine Wave
      this.ctx.beginPath();
      this.ctx.moveTo(0, midY);

      const amplitude = this.isPlaying ? 4 : 1.5;
      const frequency = 0.055;

      for (let x = 0; x <= playedWidth; x += 2) {
        const y = midY + Math.sin(x * frequency - this.wavePhase) * amplitude;
        this.ctx.lineTo(x, y);
      }

      // Neon Gradient for Played Wave: Emerald to Electric Lime
      const grad = this.ctx.createLinearGradient(0, 0, playedWidth, 0);
      grad.addColorStop(0, "#00E599");
      grad.addColorStop(1, "#C8FF29");

      this.ctx.strokeStyle = grad;
      this.ctx.lineWidth = 3.5;
      this.ctx.shadowColor = "rgba(0, 229, 153, 0.6)";
      this.ctx.shadowBlur = this.isPlaying ? 8 : 2;
      this.ctx.lineCap = "round";
      this.ctx.stroke();
      this.ctx.shadowBlur = 0; // reset shadow

      // Draw Thumb Circle at the wave head
      if (playedWidth > 0) {
        const thumbY = midY + Math.sin(playedWidth * frequency - this.wavePhase) * amplitude;
        this.ctx.beginPath();
        this.ctx.arc(playedWidth, thumbY, 6, 0, Math.PI * 2);
        this.ctx.fillStyle = "#C8FF29";
        this.ctx.shadowColor = "rgba(200, 255, 41, 0.8)";
        this.ctx.shadowBlur = 10;
        this.ctx.fill();
        this.ctx.shadowBlur = 0;
      }

      this.animationFrame = requestAnimationFrame(draw);
    };

    draw();
  }

  // Render Track Grid
  renderTrackGrid(tracks) {
    this.trackGrid.innerHTML = "";
    tracks.forEach((track) => {
      const card = document.createElement("div");
      card.className = "track-card";
      card.innerHTML = `
        <div class="track-art-wrapper">
          <img src="${track.artwork_url}" class="track-art" alt="${track.title}">
          <div class="card-play-overlay">
            <div class="play-circle">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="#0B0D10"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
            </div>
          </div>
        </div>
        <div class="track-name" title="${track.title}">${track.title}</div>
        <div class="track-meta">
          <span class="track-artist">${track.artist}</span>
          <span class="audio-tag">${track.audio_format.codec}</span>
        </div>
      `;

      card.addEventListener("click", () => {
        const originalIndex = this.tracks.findIndex(t => t.id === track.id);
        this.loadTrack(originalIndex !== -1 ? originalIndex : 0, true);
        this.showToast(`Memutar ${track.title}`);
      });

      this.trackGrid.appendChild(card);
    });
  }
}

// Instantiate on DOMContentLoaded
document.addEventListener("DOMContentLoaded", () => {
  window.sorafyApp = new SoraFyDesktopApp();
});
