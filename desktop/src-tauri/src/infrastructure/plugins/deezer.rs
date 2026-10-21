use async_trait::async_trait;
use serde::Deserialize;

use crate::domain::error::DomainError;
use crate::domain::model::{AudioFormat, PluginMeta, Track};
use crate::domain::ports::MusicStreamPluginPort;

#[derive(Clone)]
pub struct DeezerPlugin {
    client: reqwest::Client,
    pub is_enabled: bool,
}

#[derive(Deserialize, Debug)]
struct DeezerSearchResponse {
    data: Option<Vec<DeezerTrackItem>>,
}

#[derive(Deserialize, Debug)]
struct DeezerTrackItem {
    id: Option<u64>,
    title: Option<String>,
    duration: Option<u64>,
    preview: Option<String>,
    artist: Option<DeezerArtistItem>,
    album: Option<DeezerAlbumItem>,
}

#[allow(dead_code)]
#[derive(Deserialize, Debug)]
struct DeezerArtistItem {
    id: Option<u64>,
    name: Option<String>,
    picture_medium: Option<String>,
    picture_big: Option<String>,
}

#[allow(dead_code)]
#[derive(Deserialize, Debug)]
struct DeezerAlbumItem {
    id: Option<u64>,
    title: Option<String>,
    cover_medium: Option<String>,
    cover_big: Option<String>,
}

impl Default for DeezerPlugin {
    fn default() -> Self {
        Self::new(true)
    }
}

impl DeezerPlugin {
    pub fn new(is_enabled: bool) -> Self {
        Self {
            client: reqwest::Client::builder()
                .user_agent("Mozilla/5.0")
                .timeout(std::time::Duration::from_secs(8))
                .build()
                .unwrap_or_default(),
            is_enabled,
        }
    }
}

#[async_trait]
impl MusicStreamPluginPort for DeezerPlugin {
    fn meta(&self) -> PluginMeta {
        PluginMeta {
            id: "deezer".to_string(),
            name: "Deezer Hi-Fi Resolver".to_string(),
            description: "Katalog musik global Deezer resmi dengan metadata jernih dan cover beresolusi tinggi 500x500".to_string(),
            version: "1.0.0".to_string(),
            author: "Deezer Open Community".to_string(),
            category: "Audio Stream".to_string(),
            github_url: "https://github.com/RemixDev/deezload".to_string(),
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

    async fn search(&self, query: &str) -> Result<Vec<Track>, DomainError> {
        if !self.is_enabled || query.trim().is_empty() {
            return Ok(vec![]);
        }

        let url = format!("https://api.deezer.com/search?q={}&limit=15", urlencoding(query));
        let resp = self.client.get(&url).send().await;

        match resp {
            Ok(response) if response.status().is_success() => {
                if let Ok(search_res) = response.json::<DeezerSearchResponse>().await {
                    let mut tracks = Vec::new();
                    if let Some(items) = search_res.data {
                        for item in items {
                            let track_id = match item.id {
                                Some(id) => id.to_string(),
                                None => continue,
                            };

                            let title = item.title.unwrap_or_else(|| "Track".to_string());
                            let duration_secs = item.duration.unwrap_or(180);
                            let preview_url = item.preview;
                            let artist_name = item
                                .artist
                                .as_ref()
                                .and_then(|a| a.name.clone())
                                .unwrap_or_else(|| "Unknown Artist".to_string());
                            let cover_url = item
                                .album
                                .as_ref()
                                .and_then(|a| a.cover_big.clone().or_else(|| a.cover_medium.clone()))
                                .or_else(|| {
                                    item.artist
                                        .as_ref()
                                        .and_then(|a| a.picture_big.clone().or_else(|| a.picture_medium.clone()))
                                });

                            tracks.push(Track {
                                id: format!("dz:{}", track_id),
                                title,
                                artist: artist_name,
                                artist_id: None,
                                album: item.album.and_then(|a| a.title),
                                duration_secs,
                                stream_url: preview_url,
                                artwork_url: cover_url,
                                audio_format: AudioFormat {
                                    codec: "MP3 320kbps".to_string(),
                                    bitrate_kbps: 320,
                                    sample_rate_hz: 44100,
                                    is_lossless: false,
                                },
                                is_liked: false,
                                source_label: Some("Deezer".to_string()),
                            });
                        }
                    }
                    Ok(tracks)
                } else {
                    Ok(vec![])
                }
            }
            _ => Ok(vec![]),
        }
    }

    async fn resolve_stream(&self, track: &Track) -> Result<Option<String>, DomainError> {
        if !self.is_enabled {
            return Ok(None);
        }

        if let Some(ref url) = track.stream_url {
            if url.contains("dzcdn.net") {
                return Ok(Some(url.clone()));
            }
        }

        let dz_id = if track.id.starts_with("dz:") {
            track.id.trim_start_matches("dz:").to_string()
        } else {
            let found = self.search(&format!("{} {}", track.artist, track.title)).await?;
            if let Some(first) = found.into_iter().next() {
                first.id.trim_start_matches("dz:").to_string()
            } else {
                return Ok(None);
            }
        };

        let url = format!("https://api.deezer.com/track/{}", dz_id);
        if let Ok(res) = self.client.get(&url).send().await {
            if res.status().is_success() {
                if let Ok(item) = res.json::<DeezerTrackItem>().await {
                    return Ok(item.preview);
                }
            }
        }

        Ok(None)
    }
}

fn urlencoding(input: &str) -> String {
    let mut encoded = String::new();
    for b in input.bytes() {
        match b {
            b'a'..=b'z' | b'A'..=b'Z' | b'0'..=b'9' | b'-' | b'_' | b'.' | b'~' => {
                encoded.push(b as char);
            }
            b' ' => encoded.push('+'),
            _ => {
                encoded.push_str(&format!("%{:02X}", b));
            }
        }
    }
    encoded
}
