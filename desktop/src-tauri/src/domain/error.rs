use serde::{Deserialize, Serialize};
use thiserror::Error;

#[derive(Error, Debug, Clone, Serialize, Deserialize)]
pub enum DomainError {
    #[error("Track not found: {0}")]
    TrackNotFound(String),

    #[error("Audio stream is unavailable: {0}")]
    StreamUnavailable(String),

    #[error("Lyrics not found: {0}")]
    LyricsNotFound(String),

    #[error("Storage failure: {0}")]
    StorageError(String),

    #[error("Audio engine error: {0}")]
    AudioError(String),

    #[error("Network connection error: {0}")]
    NetworkError(String),

    #[error("Invalid parameter: {0}")]
    InvalidParameter(String),
}
