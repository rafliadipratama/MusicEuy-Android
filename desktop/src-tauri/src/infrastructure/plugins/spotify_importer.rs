use async_trait::async_trait;
use regex::Regex;
use serde::Deserialize;

use crate::domain::error::DomainError;
use crate::domain::model::{AudioFormat, PluginMeta, Track};
use crate::domain::ports::MusicStreamPluginPort;

#[derive(Clone)]
pub struct SpotifyImporterPlugin {
    client: reqwest::Client,
    pub is_enabled: bool,
}

#[allow(dead_code)]
#[derive(Deserialize, Debug)]
struct SpotifyEmbedData {
    name: Option<String>,
    track_list: Option<Vec<SpotifyEmbedTrack>>,
}

#[allow(dead_code)]
#[derive(Deserialize, Debug)]
struct SpotifyEmbedTrack {
    title: Option<String>,
    subtitle: Option<String>, // artist
    duration: Option<u64>,
}

impl Default for SpotifyImporterPlugin {
    fn default() -> Self {
        Self::new(true)
    }
}

impl SpotifyImporterPlugin {
    pub fn new(is_enabled: bool) -> Self {
        Self {
            client: reqwest::Client::builder()
                .user_agent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(std::time::Duration::from_secs(10))
                .build()
                .unwrap_or_default(),
            is_enabled,
        }
    }

    pub async fn import_playlist_url(&self, url: &str) -> Result<Vec<Track>, DomainError> {
        if !self.is_enabled {
            return Err(DomainError::InvalidParameter("Spotify Importer plugin is disabled".to_string()));
        }

        // Extract playlist ID from URL: open.spotify.com/playlist/{id}
        let re = Regex::new(r"playlist[/:]([a-zA-Z0-9]+)").unwrap();
        let playlist_id = re
            .captures(url)
            .and_then(|c| c.get(1))
            .map(|m| m.as_str())
            .ok_or_else(|| DomainError::InvalidParameter("Invalid Spotify playlist URL".to_string()))?;

        // Query public embed URL (No API key needed!)
        let embed_url = format!("https://open.spotify.com/embed/playlist/{}", playlist_id);
        let resp = self
            .client
            .get(&embed_url)
            .send()
            .await
            .map_err(|e| DomainError::NetworkError(e.to_string()))?;

        if !resp.status().is_success() {
            return Err(DomainError::NetworkError("Failed to fetch Spotify playlist".to_string()));
        }

        let html = resp.text().await.unwrap_or_default();

        // Extract JSON script inside <script id="__NEXT_DATA__">
        let script_re = Regex::new(r#"<script id="__NEXT_DATA__" type="application/json">([^<]+)</script>"#).unwrap();
        if let Some(caps) = script_re.captures(&html) {
            if let Some(json_str) = caps.get(1) {
                if let Ok(val) = serde_json::from_str::<serde_json::Value>(json_str.as_str()) {
                    let mut tracks = Vec::new();
                    if let Some(items) = val["props"]["pageProps"]["state"]["data"]["entity"]["trackList"].as_array() {
                        for (idx, item) in items.iter().enumerate() {
                            let title = item["title"].as_str().unwrap_or("Untitled").to_string();
                            let artist = item["subtitle"].as_str().unwrap_or("Unknown Artist").to_string();
                            let duration_ms = item["duration"].as_u64().unwrap_or(180_000);

                            tracks.push(Track {
                                id: format!("sp_{}_{}", playlist_id, idx),
                                title,
                                artist,
                                artist_id: None,
                                album: None,
                                duration_secs: duration_ms / 1000,
                                stream_url: None, // Will resolve through YouTube / Monochrome
                                artwork_url: None,
                                audio_format: AudioFormat::default(),
                                is_liked: false,
                                source_label: Some("Spotify Import".to_string()),
                            });
                        }
                    }
                    if !tracks.is_empty() {
                        return Ok(tracks);
                    }
                }
            }
        }

        Ok(vec![])
    }
}

#[async_trait]
impl MusicStreamPluginPort for SpotifyImporterPlugin {
    fn meta(&self) -> PluginMeta {
        PluginMeta {
            id: "spotify_importer".to_string(),
            name: "Spotify Playlist Importer".to_string(),
            description: "Ekstrak & sinkronisasi playlist publik Spotify ke koleksi SoraFy tanpa akun Premium".to_string(),
            version: "1.0.0".to_string(),
            author: "Spotube Open Spec".to_string(),
            category: "Alat & Impor".to_string(),
            github_url: "https://github.com/KRTirtho/Spotube".to_string(),
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
