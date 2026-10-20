use async_trait::async_trait;
use serde::Deserialize;

use crate::domain::error::DomainError;
use crate::domain::model::{Artist, AudioFormat, Track};
use crate::domain::ports::CatalogRepositoryPort;

#[derive(Clone)]
pub struct InnertubeCatalogClient {
    client: reqwest::Client,
    piped_instances: Vec<String>,
}

#[derive(Deserialize, Debug)]
struct PipedSearchResult {
    url: Option<String>,
    title: Option<String>,
    #[serde(rename = "uploaderName")]
    uploader_name: Option<String>,
    duration: Option<i64>,
    thumbnail: Option<String>,
}

#[derive(Deserialize, Debug)]
struct PipedStreamResponse {
    #[serde(rename = "audioStreams")]
    audio_streams: Option<Vec<PipedAudioStream>>,
}

#[allow(dead_code)]
#[derive(Deserialize, Debug)]
struct PipedAudioStream {
    url: String,
    format: Option<String>,
    bitrate: Option<u32>,
    quality: Option<String>,
}

impl Default for InnertubeCatalogClient {
    fn default() -> Self {
        Self::new(vec![
            "https://pipedapi.kavin.rocks".to_string(),
            "https://api.piped.privacydev.net".to_string(),
            "https://piped-api.lunar.icu".to_string(),
        ])
    }
}

impl InnertubeCatalogClient {
    pub fn new(piped_instances: Vec<String>) -> Self {
        Self {
            client: reqwest::Client::builder()
                .user_agent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .timeout(std::time::Duration::from_secs(8))
                .build()
                .unwrap_or_default(),
            piped_instances,
        }
    }

    fn sample_tracks(&self) -> Vec<Track> {
        vec![
            Track {
                id: "sora_001".to_string(),
                title: "Midnight Emerald (SoraFy Drift)".to_string(),
                artist: "Priangan Sound Lab".to_string(),
                artist_id: Some("art_priangan".to_string()),
                album: Some("Electric Obsidian EP".to_string()),
                duration_secs: 214,
                stream_url: Some("https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3".to_string()),
                artwork_url: Some("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=600&q=80".to_string()),
                audio_format: AudioFormat {
                    codec: "FLAC Lossless".to_string(),
                    bitrate_kbps: 1411,
                    sample_rate_hz: 96000,
                    is_lossless: true,
                },
                is_liked: true,
            },
            Track {
                id: "sora_002".to_string(),
                title: "Electric Lime Pulse".to_string(),
                artist: "Neon Bandung".to_string(),
                artist_id: Some("art_neon_bdg".to_string()),
                album: Some("West Java Cyber".to_string()),
                duration_secs: 185,
                stream_url: Some("https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=electronic-future-beats-117997.mp3".to_string()),
                artwork_url: Some("https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=80".to_string()),
                audio_format: AudioFormat {
                    codec: "Opus Hi-Res".to_string(),
                    bitrate_kbps: 320,
                    sample_rate_hz: 48000,
                    is_lossless: false,
                },
                is_liked: false,
            },
            Track {
                id: "sora_003".to_string(),
                title: "Origami Ribbons".to_string(),
                artist: "Sora Ambient".to_string(),
                artist_id: Some("art_sora_amb".to_string()),
                album: Some("Geometry of Waves".to_string()),
                duration_secs: 242,
                stream_url: Some("https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=chill-abstract-intention-12099.mp3".to_string()),
                artwork_url: Some("https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?auto=format&fit=crop&w=600&q=80".to_string()),
                audio_format: AudioFormat {
                    codec: "AAC High".to_string(),
                    bitrate_kbps: 256,
                    sample_rate_hz: 44100,
                    is_lossless: false,
                },
                is_liked: true,
            },
        ]
    }
}

#[async_trait]
impl CatalogRepositoryPort for InnertubeCatalogClient {
    async fn search(&self, query: &str) -> Result<Vec<Track>, DomainError> {
        let trimmed = query.trim();
        if trimmed.is_empty() {
            return self.get_trending().await;
        }

        // Try piped instances
        for base in &self.piped_instances {
            let url = format!("{}/search?q={}&filter=music_songs", base, urlencoding(trimmed));
            if let Ok(res) = self.client.get(&url).send().await {
                if res.status().is_success() {
                    if let Ok(items) = res.json::<Vec<PipedSearchResult>>().await {
                        let tracks: Vec<Track> = items
                            .into_iter()
                            .filter_map(|item| {
                                let url_path = item.url?;
                                let video_id = url_path.replace("/watch?v=", "");
                                Some(Track {
                                    id: video_id,
                                    title: item.title.unwrap_or_else(|| "Unknown Title".to_string()),
                                    artist: item.uploader_name.unwrap_or_else(|| "Unknown Artist".to_string()),
                                    artist_id: None,
                                    album: None,
                                    duration_secs: (item.duration.unwrap_or(180)).max(0) as u64,
                                    stream_url: None,
                                    artwork_url: item.thumbnail,
                                    audio_format: AudioFormat {
                                        codec: "Opus".to_string(),
                                        bitrate_kbps: 160,
                                        sample_rate_hz: 48000,
                                        is_lossless: false,
                                    },
                                    is_liked: false,
                                })
                            })
                            .collect();

                        if !tracks.is_empty() {
                            return Ok(tracks);
                        }
                    }
                }
            }
        }

        // Fallback filter over sample catalog
        let samples = self.sample_tracks();
        let query_lower = trimmed.to_lowercase();
        let filtered: Vec<Track> = samples
            .into_iter()
            .filter(|t| {
                t.title.to_lowercase().contains(&query_lower)
                    || t.artist.to_lowercase().contains(&query_lower)
            })
            .collect();

        if !filtered.is_empty() {
            Ok(filtered)
        } else {
            Ok(self.sample_tracks())
        }
    }

    async fn get_trending(&self) -> Result<Vec<Track>, DomainError> {
        for base in &self.piped_instances {
            let url = format!("{}/trending?region=ID", base);
            if let Ok(res) = self.client.get(&url).send().await {
                if res.status().is_success() {
                    if let Ok(items) = res.json::<Vec<PipedSearchResult>>().await {
                        let tracks: Vec<Track> = items
                            .into_iter()
                            .take(15)
                            .filter_map(|item| {
                                let url_path = item.url?;
                                let video_id = url_path.replace("/watch?v=", "");
                                Some(Track {
                                    id: video_id,
                                    title: item.title.unwrap_or_else(|| "Trending Track".to_string()),
                                    artist: item.uploader_name.unwrap_or_else(|| "SoraFy Popular".to_string()),
                                    artist_id: None,
                                    album: None,
                                    duration_secs: (item.duration.unwrap_or(200)).max(0) as u64,
                                    stream_url: None,
                                    artwork_url: item.thumbnail,
                                    audio_format: AudioFormat::default(),
                                    is_liked: false,
                                })
                            })
                            .collect();

                        if !tracks.is_empty() {
                            return Ok(tracks);
                        }
                    }
                }
            }
        }

        Ok(self.sample_tracks())
    }

    async fn get_artist(&self, artist_id: &str) -> Result<Artist, DomainError> {
        Ok(Artist {
            id: artist_id.to_string(),
            name: "Priangan Sound Lab".to_string(),
            artwork_url: Some("https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?auto=format&fit=crop&w=800&q=80".to_string()),
            bio: Some("Electronic and ambient soundscapes born from the misty hills of Priangan, blending traditional microtones with neon cybernetic pulses.".to_string()),
            monthly_listeners: Some(1_420_000),
            top_tracks: self.sample_tracks(),
        })
    }

    async fn resolve_stream_url(&self, track_id: &str) -> Result<String, DomainError> {
        // If track_id is one of sample tracks
        for sample in self.sample_tracks() {
            if sample.id == track_id {
                if let Some(stream) = sample.stream_url {
                    return Ok(stream);
                }
            }
        }

        // Try resolving through Piped streams
        for base in &self.piped_instances {
            let url = format!("{}/streams/{}", base, track_id);
            if let Ok(res) = self.client.get(&url).send().await {
                if res.status().is_success() {
                    if let Ok(stream_res) = res.json::<PipedStreamResponse>().await {
                        if let Some(audio_streams) = stream_res.audio_streams {
                            if let Some(best) = audio_streams.iter().max_by_key(|s| s.bitrate.unwrap_or(0)) {
                                return Ok(best.url.clone());
                            }
                        }
                    }
                }
            }
        }

        // If not found, return sample stream fallback
        Ok("https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3".to_string())
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
