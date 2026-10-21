use async_trait::async_trait;
use serde::Deserialize;

use crate::domain::error::DomainError;
use crate::domain::model::{AudioFormat, PluginMeta, Track};
use crate::domain::ports::MusicStreamPluginPort;

#[derive(Clone)]
pub struct MonochromeLosslessPlugin {
    client: reqwest::Client,
    base_url: String,
    pub is_enabled: bool,
}

#[derive(Deserialize, Debug)]
struct MonochromeSearchResponse {
    tracks: Option<Vec<MonochromeTrackItem>>,
}

#[derive(Deserialize, Debug)]
struct MonochromeTrackItem {
    id: Option<String>,
    #[serde(rename = "trackId")]
    track_id: Option<String>,
    title: Option<String>,
    #[serde(rename = "artistNames")]
    artist_names: Option<Vec<String>>,
    artist: Option<String>,
    album: Option<String>,
    #[serde(rename = "releaseTitle")]
    release_title: Option<String>,
    duration: Option<u64>,
    #[serde(rename = "durationMs")]
    duration_ms: Option<u64>,
    artwork: Option<String>,
    #[serde(rename = "coverUrl")]
    cover_url: Option<String>,
}

impl Default for MonochromeLosslessPlugin {
    fn default() -> Self {
        Self::new(true)
    }
}

impl MonochromeLosslessPlugin {
    pub fn new(is_enabled: bool) -> Self {
        Self {
            client: reqwest::Client::builder()
                .user_agent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36")
                .timeout(std::time::Duration::from_secs(8))
                .build()
                .unwrap_or_default(),
            base_url: "https://tracks.monochrome.st".to_string(),
            is_enabled,
        }
    }
}

#[async_trait]
impl MusicStreamPluginPort for MonochromeLosslessPlugin {
    fn meta(&self) -> PluginMeta {
        PluginMeta {
            id: "monochrome_lossless".to_string(),
            name: "Monochrome Hi-Res FLAC".to_string(),
            description: "Stream Lossless FLAC murni 16-bit / 24-bit tanpa akun langsung dari Monochrome Engine.".to_string(),
            version: "2.0.0".to_string(),
            author: "Monochrome & Musicfy".to_string(),
            category: "Audio Stream".to_string(),
            github_url: "https://github.com/monochrome-music/monochrome".to_string(),
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

        let url = format!("{}/search/tracks?q={}&limit=12", self.base_url, urlencoding(query));
        let resp = self.client.get(&url).send().await;

        match resp {
            Ok(response) if response.status().is_success() => {
                if let Ok(search_res) = response.json::<MonochromeSearchResponse>().await {
                    let mut tracks = Vec::new();
                    if let Some(items) = search_res.tracks {
                        for item in items {
                            let track_id = item.id.or(item.track_id).unwrap_or_default();
                            let title = item.title.unwrap_or_default();
                            if track_id.is_empty() || title.is_empty() {
                                continue;
                            }

                            let artist_name = if let Some(names) = item.artist_names {
                                names.join(", ")
                            } else {
                                item.artist.unwrap_or_else(|| "Unknown Artist".to_string())
                            };

                            let album_title = item.album.or(item.release_title);
                            let dur_ms = item.duration.or(item.duration_ms).unwrap_or(180);
                            let duration_secs = if dur_ms > 10_000 { dur_ms / 1000 } else { dur_ms };
                            let artwork = item.artwork.or(item.cover_url);

                            tracks.push(Track {
                                id: format!("mono_{}", track_id),
                                title,
                                artist: artist_name,
                                artist_id: None,
                                album: album_title,
                                duration_secs,
                                stream_url: Some(format!("{}/track/{}", self.base_url, track_id)),
                                artwork_url: artwork,
                                audio_format: AudioFormat {
                                    codec: "FLAC 24-bit".to_string(),
                                    bitrate_kbps: 1411,
                                    sample_rate_hz: 96000,
                                    is_lossless: true,
                                },
                                is_liked: false,
                                source_label: Some("Monochrome FLAC".to_string()),
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

        if track.id.starts_with("mono_") {
            let real_id = track.id.trim_start_matches("mono_");
            return Ok(Some(format!("{}/track/{}", self.base_url, real_id)));
        }

        // Search by artist and title
        let query = format!("{} {}", track.artist, track.title);
        let found = self.search(&query).await?;
        if let Some(first) = found.into_iter().next() {
            Ok(first.stream_url)
        } else {
            Ok(None)
        }
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
