use async_trait::async_trait;
use crate::domain::error::DomainError;
use crate::domain::model::{Artist, Lyrics, PlaybackState, Playlist, PluginMeta, Track};

#[async_trait]
pub trait AudioPlayerPort: Send + Sync {
    async fn play(&mut self, track: &Track) -> Result<(), DomainError>;
    async fn pause(&mut self) -> Result<(), DomainError>;
    async fn resume(&mut self) -> Result<(), DomainError>;
    async fn stop(&mut self) -> Result<(), DomainError>;
    async fn seek(&mut self, position_ms: u64) -> Result<(), DomainError>;
    async fn set_volume(&mut self, volume: f32) -> Result<(), DomainError>;
    async fn get_state(&self) -> PlaybackState;
}

#[async_trait]
pub trait CatalogRepositoryPort: Send + Sync {
    async fn search(&self, query: &str) -> Result<Vec<Track>, DomainError>;
    async fn get_trending(&self) -> Result<Vec<Track>, DomainError>;
    async fn get_artist(&self, artist_id: &str) -> Result<Artist, DomainError>;
    async fn resolve_stream_url(&self, track_id: &str) -> Result<String, DomainError>;
}

#[async_trait]
pub trait LyricsRepositoryPort: Send + Sync {
    async fn get_lyrics(
        &self,
        track_id: &str,
        title: &str,
        artist: &str,
        duration_secs: Option<u64>,
    ) -> Result<Option<Lyrics>, DomainError>;
}

#[async_trait]
pub trait StoragePort: Send + Sync {
    async fn save_playlist(&self, playlist: &Playlist) -> Result<(), DomainError>;
    async fn get_playlists(&self) -> Result<Vec<Playlist>, DomainError>;
    async fn toggle_favorite(&self, track: &Track) -> Result<bool, DomainError>;
    async fn get_favorites(&self) -> Result<Vec<Track>, DomainError>;
    async fn record_history(&self, track: &Track) -> Result<(), DomainError>;
    async fn get_history(&self) -> Result<Vec<Track>, DomainError>;
}

#[async_trait]
pub trait MusicStreamPluginPort: Send + Sync {
    fn meta(&self) -> PluginMeta;
    fn is_enabled(&self) -> bool;
    fn set_enabled(&mut self, enabled: bool);
    async fn search(&self, query: &str) -> Result<Vec<Track>, DomainError>;
    async fn resolve_stream(&self, track: &Track) -> Result<Option<String>, DomainError>;
    async fn on_track_change(&self, _track: &Track, _is_playing: bool) -> Result<(), DomainError> {
        Ok(())
    }
}
