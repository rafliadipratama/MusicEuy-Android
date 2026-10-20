use std::sync::Arc;

use crate::domain::error::DomainError;
use crate::domain::model::{Playlist, Track};
use crate::domain::ports::StoragePort;

pub struct LibraryService {
    storage_repo: Arc<dyn StoragePort>,
}

impl LibraryService {
    pub fn new(storage_repo: Arc<dyn StoragePort>) -> Self {
        Self { storage_repo }
    }

    pub async fn toggle_favorite(&self, track: &Track) -> Result<bool, DomainError> {
        self.storage_repo.toggle_favorite(track).await
    }

    pub async fn get_favorites(&self) -> Result<Vec<Track>, DomainError> {
        self.storage_repo.get_favorites().await
    }

    pub async fn record_history(&self, track: &Track) -> Result<(), DomainError> {
        self.storage_repo.record_history(track).await
    }

    pub async fn get_history(&self) -> Result<Vec<Track>, DomainError> {
        self.storage_repo.get_history().await
    }

    pub async fn create_playlist(&self, playlist: &Playlist) -> Result<(), DomainError> {
        self.storage_repo.save_playlist(playlist).await
    }

    pub async fn get_playlists(&self) -> Result<Vec<Playlist>, DomainError> {
        self.storage_repo.get_playlists().await
    }
}
