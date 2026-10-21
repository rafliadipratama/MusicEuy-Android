use async_trait::async_trait;
use serde::Deserialize;

use crate::domain::error::DomainError;
use crate::domain::model::{AudioFormat, PluginMeta, Track};
use crate::domain::ports::MusicStreamPluginPort;

#[derive(Clone)]
pub struct BandcampJamendoPlugin {
    client: reqwest::Client,
    pub is_enabled: bool,
}

#[derive(Deserialize, Debug)]
struct JamendoSearchResponse {
    results: Option<Vec<JamendoTrackItem>>,
}

#[derive(Deserialize, Debug)]
struct JamendoTrackItem {
    id: Option<String>,
    name: Option<String>,
    duration: Option<u64>,
    artist_name: Option<String>,
    album_name: Option<String>,
    audio: Option<String>,
    audiodownload: Option<String>,
    image: Option<String>,
}

impl Default for BandcampJamendoPlugin {
    fn default() -> Self {
        Self::new(true)
    }
}

impl BandcampJamendoPlugin {
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
impl MusicStreamPluginPort for BandcampJamendoPlugin {
    fn meta(&self) -> PluginMeta {
        PluginMeta {
            id: "bandcamp_jamendo".to_string(),
            name: "Bandcamp & Jamendo Indie".to_string(),
            description: "Koleksi musisi independen global, Creative Commons, dan rilisan vinyl dari Bandcamp & Jamendo".to_string(),
            version: "1.0.0".to_string(),
            author: "Jamendo & Team NewPipe".to_string(),
            category: "Audio Stream".to_string(),
            github_url: "https://github.com/jamendo/api".to_string(),
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

        let url = format!(
            "https://api.jamendo.com/v3.0/tracks/?client_id=56d30c95&format=json&limit=15&namesearch={}",
            urlencoding(query)
        );

        let resp = self.client.get(&url).send().await;
        match resp {
            Ok(response) if response.status().is_success() => {
                if let Ok(search_res) = response.json::<JamendoSearchResponse>().await {
                    let mut tracks = Vec::new();
                    if let Some(items) = search_res.results {
                        for item in items {
                            let track_id = match item.id {
                                Some(ref id) if !id.is_empty() => id.clone(),
                                _ => continue,
                            };

                            let title = item.name.unwrap_or_else(|| "Indie Track".to_string());
                            let artist_name = item.artist_name.unwrap_or_else(|| "Indie Artist".to_string());
                            let duration_secs = item.duration.unwrap_or(180);
                            let stream_url = item.audio.or(item.audiodownload);
                            let cover_url = item.image;

                            tracks.push(Track {
                                id: format!("jm:{}", track_id),
                                title,
                                artist: artist_name,
                                artist_id: None,
                                album: item.album_name,
                                duration_secs,
                                stream_url,
                                artwork_url: cover_url,
                                audio_format: AudioFormat {
                                    codec: "MP3 VBR".to_string(),
                                    bitrate_kbps: 192,
                                    sample_rate_hz: 44100,
                                    is_lossless: false,
                                },
                                is_liked: false,
                                source_label: Some("Jamendo Indie".to_string()),
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
            if url.starts_with("http") {
                return Ok(Some(url.clone()));
            }
        }

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
