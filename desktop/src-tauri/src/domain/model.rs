use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq)]
pub enum RepeatMode {
    Off,
    All,
    One,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct AudioFormat {
    pub codec: String,         // e.g. "Opus", "AAC", "FLAC"
    pub bitrate_kbps: u32,     // e.g. 160, 256, 320, 1411
    pub sample_rate_hz: u32,   // e.g. 44100, 48000, 96000
    pub is_lossless: bool,
}

impl Default for AudioFormat {
    fn default() -> Self {
        Self {
            codec: "Opus".to_string(),
            bitrate_kbps: 160,
            sample_rate_hz: 48000,
            is_lossless: false,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Track {
    pub id: String,
    pub title: String,
    pub artist: String,
    pub artist_id: Option<String>,
    pub album: Option<String>,
    pub duration_secs: u64,
    pub stream_url: Option<String>,
    pub artwork_url: Option<String>,
    pub audio_format: AudioFormat,
    pub is_liked: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Artist {
    pub id: String,
    pub name: String,
    pub artwork_url: Option<String>,
    pub bio: Option<String>,
    pub monthly_listeners: Option<u64>,
    pub top_tracks: Vec<Track>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Album {
    pub id: String,
    pub title: String,
    pub artist: String,
    pub cover_url: Option<String>,
    pub year: Option<u32>,
    pub tracks: Vec<Track>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Playlist {
    pub id: String,
    pub title: String,
    pub description: Option<String>,
    pub cover_url: Option<String>,
    pub tracks: Vec<Track>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct LyricLine {
    pub timestamp_ms: u64,
    pub text: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct Lyrics {
    pub track_id: String,
    pub plain_lyrics: Option<String>,
    pub synced_lines: Vec<LyricLine>,
}

#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub struct PlaybackState {
    pub current_track: Option<Track>,
    pub is_playing: bool,
    pub position_ms: u64,
    pub duration_ms: u64,
    pub volume: f32, // 0.0 to 1.0
    pub repeat_mode: RepeatMode,
    pub is_shuffled: bool,
}

impl Default for PlaybackState {
    fn default() -> Self {
        Self {
            current_track: None,
            is_playing: false,
            position_ms: 0,
            duration_ms: 0,
            volume: 0.85,
            repeat_mode: RepeatMode::Off,
            is_shuffled: false,
        }
    }
}
