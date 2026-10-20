use async_trait::async_trait;
use regex::Regex;
use serde::Deserialize;
use std::sync::Arc;

use crate::domain::error::DomainError;
use crate::domain::model::{LyricLine, Lyrics};
use crate::domain::ports::LyricsRepositoryPort;

#[derive(Clone)]
pub struct LrcLibClient {
    client: reqwest::Client,
    base_url: String,
    timestamp_regex: Arc<Regex>,
}

#[derive(Deserialize, Debug)]
struct LrcLibResponse {
    #[serde(rename = "plainLyrics")]
    plain_lyrics: Option<String>,
    #[serde(rename = "syncedLyrics")]
    synced_lyrics: Option<String>,
}

impl Default for LrcLibClient {
    fn default() -> Self {
        Self::new("https://lrclib.net/api".to_string())
    }
}

impl LrcLibClient {
    pub fn new(base_url: String) -> Self {
        Self {
            client: reqwest::Client::builder()
                .user_agent("SoraFy-Desktop/1.0.0 (https://github.com/rafliadipratama/sorafy)")
                .build()
                .unwrap_or_default(),
            base_url,
            timestamp_regex: Arc::new(Regex::new(r"\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)").unwrap()),
        }
    }

    pub fn parse_synced_lyrics(&self, synced_text: &str) -> Vec<LyricLine> {
        let mut lines = Vec::new();

        for line in synced_text.lines() {
            let line = line.trim();
            if let Some(caps) = self.timestamp_regex.captures(line) {
                let minutes: u64 = caps[1].parse().unwrap_or(0);
                let seconds: u64 = caps[2].parse().unwrap_or(0);
                let fraction_str = &caps[3];
                let fraction_ms: u64 = if fraction_str.len() == 2 {
                    fraction_str.parse::<u64>().unwrap_or(0) * 10
                } else {
                    fraction_str.parse::<u64>().unwrap_or(0)
                };

                let total_ms = (minutes * 60 + seconds) * 1000 + fraction_ms;
                let text = caps[4].trim().to_string();

                lines.push(LyricLine {
                    timestamp_ms: total_ms,
                    text,
                });
            }
        }

        lines.sort_by_key(|l| l.timestamp_ms);
        lines
    }
}

#[async_trait]
impl LyricsRepositoryPort for LrcLibClient {
    async fn get_lyrics(
        &self,
        track_id: &str,
        title: &str,
        artist: &str,
        duration_secs: Option<u64>,
    ) -> Result<Option<Lyrics>, DomainError> {
        let mut url = format!(
            "{}/get?track_name={}&artist_name={}",
            self.base_url,
            urlencoding(title),
            urlencoding(artist)
        );

        if let Some(dur) = duration_secs {
            url.push_str(&format!("&duration={}", dur));
        }

        let resp = self.client.get(&url).send().await;
        match resp {
            Ok(response) if response.status().is_success() => {
                if let Ok(lrc) = response.json::<LrcLibResponse>().await {
                    let synced_lines = if let Some(ref sync_text) = lrc.synced_lyrics {
                        self.parse_synced_lyrics(sync_text)
                    } else {
                        Vec::new()
                    };

                    Ok(Some(Lyrics {
                        track_id: track_id.to_string(),
                        plain_lyrics: lrc.plain_lyrics,
                        synced_lines,
                    }))
                } else {
                    Ok(None)
                }
            }
            Ok(_) => Ok(None),
            Err(e) => Err(DomainError::NetworkError(e.to_string())),
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
