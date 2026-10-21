use async_trait::async_trait;
use serde_json::json;
use std::sync::{Arc, Mutex};

use crate::domain::error::DomainError;
use crate::domain::model::{PluginMeta, Track};
use crate::domain::ports::MusicStreamPluginPort;

#[derive(Clone)]
pub struct DiscordRpcPlugin {
    pub is_enabled: bool,
    pub client_id: String,
    current_activity: Arc<Mutex<Option<String>>>,
}

impl Default for DiscordRpcPlugin {
    fn default() -> Self {
        Self::new(true)
    }
}

impl DiscordRpcPlugin {
    pub fn new(is_enabled: bool) -> Self {
        Self {
            is_enabled,
            client_id: "123456789012345678".to_string(),
            current_activity: Arc::new(Mutex::new(None)),
        }
    }

    pub fn set_activity(&self, track_title: &str, artist_name: &str, is_playing: bool) {
        if !self.is_enabled {
            return;
        }

        let activity = json!({
            "state": format!("by {}", artist_name),
            "details": track_title,
            "type": 2, // 2 = Listening to
            "assets": {
                "large_image": "sorafy_logo",
                "large_text": "SoraFy Music Player",
                "small_image": if is_playing { "play" } else { "pause" },
                "small_text": if is_playing { "Playing" } else { "Paused" }
            }
        });

        if let Ok(mut guard) = self.current_activity.lock() {
            *guard = Some(activity.to_string());
        }
    }

    pub fn get_current_payload(&self) -> Option<String> {
        self.current_activity.lock().ok().and_then(|g| g.clone())
    }
}

#[async_trait]
impl MusicStreamPluginPort for DiscordRpcPlugin {
    fn meta(&self) -> PluginMeta {
        PluginMeta {
            id: "discord_rpc".to_string(),
            name: "Discord Rich Presence (Kizzy)".to_string(),
            description: "Tampilkan lagu yang sedang kamu dengarkan di profil Discord secara realtime".to_string(),
            version: "1.0.0".to_string(),
            author: "Kizzy Open Spec & Musicfy".to_string(),
            category: "Integrasi & Sosial".to_string(),
            github_url: "https://github.com/realidkroo/musicfy".to_string(),
            is_builtin: true,
            is_enabled: self.is_enabled,
        }
    }

    fn is_enabled(&self) -> bool {
        self.is_enabled
    }

    fn set_enabled(&mut self, enabled: bool) {
        self.is_enabled = enabled;
    }

    async fn search(&self, _query: &str) -> Result<Vec<Track>, DomainError> {
        Ok(vec![])
    }

    async fn resolve_stream(&self, _track: &Track) -> Result<Option<String>, DomainError> {
        Ok(None)
    }

    async fn on_track_change(&self, track: &Track, is_playing: bool) -> Result<(), DomainError> {
        self.set_activity(&track.title, &track.artist, is_playing);
        Ok(())
    }
}
