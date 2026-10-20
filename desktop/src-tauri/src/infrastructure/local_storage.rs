use async_trait::async_trait;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;
use std::sync::RwLock;

use crate::domain::error::DomainError;
use crate::domain::model::{Playlist, Track};
use crate::domain::ports::StoragePort;

#[derive(Serialize, Deserialize, Default, Clone)]
struct StorageData {
    favorites: Vec<Track>,
    history: Vec<Track>,
    playlists: Vec<Playlist>,
}

pub struct LocalStorageAdapter {
    storage_path: PathBuf,
    cache: RwLock<StorageData>,
}

impl LocalStorageAdapter {
    pub fn new() -> Self {
        let base_dir = dirs::config_dir()
            .unwrap_or_else(|| PathBuf::from("."))
            .join("sorafy");

        let _ = fs::create_dir_all(&base_dir);
        let storage_path = base_dir.join("library.json");

        let initial_data = if storage_path.exists() {
            fs::read_to_string(&storage_path)
                .ok()
                .and_then(|content| serde_json::from_str::<StorageData>(&content).ok())
                .unwrap_or_default()
        } else {
            StorageData::default()
        };

        Self {
            storage_path,
            cache: RwLock::new(initial_data),
        }
    }

    fn persist(&self) -> Result<(), DomainError> {
        let guard = self
            .cache
            .read()
            .map_err(|e| DomainError::StorageError(e.to_string()))?;
        let json = serde_json::to_string_pretty(&*guard)
            .map_err(|e| DomainError::StorageError(e.to_string()))?;
        fs::write(&self.storage_path, json).map_err(|e| DomainError::StorageError(e.to_string()))?;
        Ok(())
    }
}

impl Default for LocalStorageAdapter {
    fn default() -> Self {
        Self::new()
    }
}

#[async_trait]
impl StoragePort for LocalStorageAdapter {
    async fn save_playlist(&self, playlist: &Playlist) -> Result<(), DomainError> {
        {
            let mut guard = self
                .cache
                .write()
                .map_err(|e| DomainError::StorageError(e.to_string()))?;
            if let Some(existing) = guard.playlists.iter_mut().find(|p| p.id == playlist.id) {
                *existing = playlist.clone();
            } else {
                guard.playlists.push(playlist.clone());
            }
        }
        self.persist()
    }

    async fn get_playlists(&self) -> Result<Vec<Playlist>, DomainError> {
        let guard = self
            .cache
            .read()
            .map_err(|e| DomainError::StorageError(e.to_string()))?;
        Ok(guard.playlists.clone())
    }

    async fn toggle_favorite(&self, track: &Track) -> Result<bool, DomainError> {
        let is_now_fav = {
            let mut guard = self
                .cache
                .write()
                .map_err(|e| DomainError::StorageError(e.to_string()))?;
            if let Some(pos) = guard.favorites.iter().position(|t| t.id == track.id) {
                guard.favorites.remove(pos);
                false
            } else {
                let mut fav = track.clone();
                fav.is_liked = true;
                guard.favorites.insert(0, fav);
                true
            }
        };
        self.persist()?;
        Ok(is_now_fav)
    }

    async fn get_favorites(&self) -> Result<Vec<Track>, DomainError> {
        let guard = self
            .cache
            .read()
            .map_err(|e| DomainError::StorageError(e.to_string()))?;
        Ok(guard.favorites.clone())
    }

    async fn record_history(&self, track: &Track) -> Result<(), DomainError> {
        {
            let mut guard = self
                .cache
                .write()
                .map_err(|e| DomainError::StorageError(e.to_string()))?;
            guard.history.retain(|t| t.id != track.id);
            guard.history.insert(0, track.clone());
            if guard.history.len() > 100 {
                guard.history.truncate(100);
            }
        }
        self.persist()
    }

    async fn get_history(&self) -> Result<Vec<Track>, DomainError> {
        let guard = self
            .cache
            .read()
            .map_err(|e| DomainError::StorageError(e.to_string()))?;
        Ok(guard.history.clone())
    }
}
