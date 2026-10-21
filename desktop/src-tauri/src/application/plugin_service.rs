use std::sync::Arc;
use tokio::sync::RwLock;

use crate::domain::error::DomainError;
use crate::domain::model::{EqualizerPreset, PluginMeta, Track};
use crate::domain::ports::MusicStreamPluginPort;
use crate::infrastructure::plugins::{
    AppleCanvasPlugin, BandcampJamendoPlugin, DeezerPlugin, DiscordRpcPlugin,
    EqualizerDspPlugin, JioSaavnPlugin, LastFmPlugin, MonochromeLosslessPlugin,
    SpotifyImporterPlugin,
};

pub struct PluginService {
    pub monochrome: Arc<RwLock<MonochromeLosslessPlugin>>,
    pub deezer: Arc<RwLock<DeezerPlugin>>,
    pub jiosaavn: Arc<RwLock<JioSaavnPlugin>>,
    pub jamendo: Arc<RwLock<BandcampJamendoPlugin>>,
    pub spotify_importer: Arc<RwLock<SpotifyImporterPlugin>>,
    pub discord_rpc: Arc<RwLock<DiscordRpcPlugin>>,
    pub equalizer_dsp: Arc<RwLock<EqualizerDspPlugin>>,
    pub apple_canvas: Arc<RwLock<AppleCanvasPlugin>>,
    pub lastfm: Arc<RwLock<LastFmPlugin>>,
}

impl Default for PluginService {
    fn default() -> Self {
        Self::new()
    }
}

impl PluginService {
    pub fn new() -> Self {
        Self {
            monochrome: Arc::new(RwLock::new(MonochromeLosslessPlugin::default())),
            deezer: Arc::new(RwLock::new(DeezerPlugin::default())),
            jiosaavn: Arc::new(RwLock::new(JioSaavnPlugin::default())),
            jamendo: Arc::new(RwLock::new(BandcampJamendoPlugin::default())),
            spotify_importer: Arc::new(RwLock::new(SpotifyImporterPlugin::default())),
            discord_rpc: Arc::new(RwLock::new(DiscordRpcPlugin::default())),
            equalizer_dsp: Arc::new(RwLock::new(EqualizerDspPlugin::default())),
            apple_canvas: Arc::new(RwLock::new(AppleCanvasPlugin::default())),
            lastfm: Arc::new(RwLock::new(LastFmPlugin::default())),
        }
    }

    pub async fn get_all_plugins(&self) -> Vec<PluginMeta> {
        vec![
            self.monochrome.read().await.meta(),
            self.deezer.read().await.meta(),
            self.jiosaavn.read().await.meta(),
            self.jamendo.read().await.meta(),
            self.spotify_importer.read().await.meta(),
            self.discord_rpc.read().await.meta(),
            self.equalizer_dsp.read().await.meta(),
            self.apple_canvas.read().await.meta(),
            self.lastfm.read().await.meta(),
        ]
    }

    pub async fn toggle_plugin(&self, plugin_id: &str, is_enabled: bool) -> Result<bool, DomainError> {
        match plugin_id {
            "monochrome_lossless" => {
                self.monochrome.write().await.set_enabled(is_enabled);
                Ok(is_enabled)
            }
            "deezer" => {
                self.deezer.write().await.set_enabled(is_enabled);
                Ok(is_enabled)
            }
            "jiosaavn" => {
                self.jiosaavn.write().await.set_enabled(is_enabled);
                Ok(is_enabled)
            }
            "bandcamp_jamendo" => {
                self.jamendo.write().await.set_enabled(is_enabled);
                Ok(is_enabled)
            }
            "spotify_importer" => {
                self.spotify_importer.write().await.set_enabled(is_enabled);
                Ok(is_enabled)
            }
            "discord_rpc" => {
                self.discord_rpc.write().await.set_enabled(is_enabled);
                Ok(is_enabled)
            }
            "equalizer_dsp" => {
                self.equalizer_dsp.write().await.set_enabled(is_enabled);
                Ok(is_enabled)
            }
            "apple_canvas" => {
                self.apple_canvas.write().await.set_enabled(is_enabled);
                Ok(is_enabled)
            }
            "lastfm_scrobbler" => {
                self.lastfm.write().await.set_enabled(is_enabled);
                Ok(is_enabled)
            }
            _ => Err(DomainError::InvalidParameter(format!("Unknown plugin: {}", plugin_id))),
        }
    }

    /// Search across all enabled music provider plugins concurrently
    pub async fn search_all_plugins(&self, query: &str) -> Vec<Track> {
        let mono = self.monochrome.read().await.clone();
        let dz = self.deezer.read().await.clone();
        let js = self.jiosaavn.read().await.clone();
        let jm = self.jamendo.read().await.clone();

        let (mono_res, dz_res, js_res, jm_res) = tokio::join!(
            async move { mono.search(query).await },
            async move { dz.search(query).await },
            async move { js.search(query).await },
            async move { jm.search(query).await },
        );

        let mut combined = Vec::new();
        if let Ok(mut tracks) = mono_res {
            combined.append(&mut tracks);
        }
        if let Ok(mut tracks) = dz_res {
            combined.append(&mut tracks);
        }
        if let Ok(mut tracks) = js_res {
            combined.append(&mut tracks);
        }
        if let Ok(mut tracks) = jm_res {
            combined.append(&mut tracks);
        }

        combined
    }

    /// Cascade stream resolution across specialized plugins
    pub async fn resolve_stream_cascade(&self, track: &Track) -> Option<String> {
        if track.id.starts_with("mono_") {
            if let Ok(Some(url)) = self.monochrome.read().await.resolve_stream(track).await {
                return Some(url);
            }
        } else if track.id.starts_with("dz:") {
            if let Ok(Some(url)) = self.deezer.read().await.resolve_stream(track).await {
                return Some(url);
            }
        } else if track.id.starts_with("js:") {
            if let Ok(Some(url)) = self.jiosaavn.read().await.resolve_stream(track).await {
                return Some(url);
            }
        } else if track.id.starts_with("jm:") {
            if let Ok(Some(url)) = self.jamendo.read().await.resolve_stream(track).await {
                return Some(url);
            }
        }

        // Try Monochrome FLAC fallback
        if let Ok(Some(url)) = self.monochrome.read().await.resolve_stream(track).await {
            return Some(url);
        }

        None
    }

    pub async fn import_spotify_playlist(&self, url: &str) -> Result<Vec<Track>, DomainError> {
        self.spotify_importer.read().await.import_playlist_url(url).await
    }

    pub async fn set_equalizer_preset(&self, preset_name: &str) -> EqualizerPreset {
        self.equalizer_dsp.read().await.set_preset(preset_name).unwrap_or_else(EqualizerDspPlugin::preset_flat)
    }

    pub async fn get_equalizer_preset(&self) -> EqualizerPreset {
        self.equalizer_dsp.read().await.get_current_preset()
    }

    pub async fn get_available_equalizer_presets(&self) -> Vec<EqualizerPreset> {
        self.equalizer_dsp.read().await.get_available_presets()
    }

    pub async fn on_track_change(&self, track: &Track, is_playing: bool) {
        let _ = self.discord_rpc.read().await.on_track_change(track, is_playing).await;
        let _ = self.lastfm.read().await.on_track_change(track, is_playing).await;
    }
}
