use std::sync::Arc;

use crate::domain::error::DomainError;
use crate::domain::model::{Artist, Track};
use crate::domain::ports::CatalogRepositoryPort;

pub struct CatalogService {
    catalog_repo: Arc<dyn CatalogRepositoryPort>,
}

impl CatalogService {
    pub fn new(catalog_repo: Arc<dyn CatalogRepositoryPort>) -> Self {
        Self { catalog_repo }
    }

    pub async fn search_tracks(&self, query: &str) -> Result<Vec<Track>, DomainError> {
        self.catalog_repo.search(query).await
    }

    pub async fn get_trending(&self) -> Result<Vec<Track>, DomainError> {
        self.catalog_repo.get_trending().await
    }

    pub async fn get_artist(&self, artist_id: &str) -> Result<Artist, DomainError> {
        self.catalog_repo.get_artist(artist_id).await
    }

    pub async fn resolve_stream(&self, track_id: &str) -> Result<String, DomainError> {
        self.catalog_repo.resolve_stream_url(track_id).await
    }
}
