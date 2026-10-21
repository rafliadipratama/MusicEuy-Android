use async_trait::async_trait;

use crate::domain::error::DomainError;
use crate::domain::model::{PluginMeta, Track};
use crate::domain::ports::MusicStreamPluginPort;

#[derive(Clone)]
pub struct AppleCanvasPlugin {
    pub is_enabled: bool,
}

impl Default for AppleCanvasPlugin {
    fn default() -> Self {
        Self::new(true)
    }
}

impl AppleCanvasPlugin {
    pub fn new(is_enabled: bool) -> Self {
        Self { is_enabled }
    }

    pub fn get_canvas_for_track(&self, _track: &Track) -> Option<String> {
        if !self.is_enabled {
            return None;
        }
        // Animated gradient canvas or ambient video URL
        Some("linear-gradient(135deg, rgba(0, 229, 153, 0.25) 0%, rgba(200, 255, 41, 0.15) 50%, rgba(11, 13, 16, 0.95) 100%)".to_string())
    }
}

#[async_trait]
impl MusicStreamPluginPort for AppleCanvasPlugin {
    fn meta(&self) -> PluginMeta {
        PluginMeta {
            id: "apple_canvas".to_string(),
            name: "Apple Music Dynamic Canvas".to_string(),
            description: "Visualisasi artwork video dinamis dan animasi latar belakang bergaya Apple Music Canvas".to_string(),
            version: "1.0.0".to_string(),
            author: "Apple Music Open Spec".to_string(),
            category: "Visual & Antarmuka".to_string(),
            github_url: "https://music.apple.com".to_string(),
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
}
