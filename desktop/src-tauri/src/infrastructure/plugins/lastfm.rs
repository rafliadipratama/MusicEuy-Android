use async_trait::async_trait;

use crate::domain::error::DomainError;
use crate::domain::model::{PluginMeta, Track};
use crate::domain::ports::MusicStreamPluginPort;

#[derive(Clone)]
pub struct LastFmPlugin {
    pub is_enabled: bool,
    pub session_key: Option<String>,
}

impl Default for LastFmPlugin {
    fn default() -> Self {
        Self::new(false)
    }
}

impl LastFmPlugin {
    pub fn new(is_enabled: bool) -> Self {
        Self {
            is_enabled,
            session_key: None,
        }
    }
}

#[async_trait]
impl MusicStreamPluginPort for LastFmPlugin {
    fn meta(&self) -> PluginMeta {
        PluginMeta {
            id: "lastfm_scrobbler".to_string(),
            name: "Last.fm Scrobbler & Stats".to_string(),
            description: "Scrobble otomatis setiap lagu yang kamu putar ke profil akun Last.fm kamu secara instan".to_string(),
            version: "1.0.0".to_string(),
            author: "Last.fm Open API".to_string(),
            category: "Integrasi & Sosial".to_string(),
            github_url: "https://www.last.fm/api".to_string(),
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
