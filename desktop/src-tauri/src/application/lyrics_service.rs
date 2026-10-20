use std::sync::Arc;

use crate::domain::error::DomainError;
use crate::domain::model::Lyrics;
use crate::domain::ports::LyricsRepositoryPort;

pub struct LyricsService {
    lyrics_repo: Arc<dyn LyricsRepositoryPort>,
}

impl LyricsService {
    pub fn new(lyrics_repo: Arc<dyn LyricsRepositoryPort>) -> Self {
        Self { lyrics_repo }
    }

    pub async fn fetch_lyrics(
        &self,
        track_id: &str,
        title: &str,
        artist: &str,
        duration_secs: Option<u64>,
    ) -> Result<Option<Lyrics>, DomainError> {
        self.lyrics_repo
            .get_lyrics(track_id, title, artist, duration_secs)
            .await
    }
}
