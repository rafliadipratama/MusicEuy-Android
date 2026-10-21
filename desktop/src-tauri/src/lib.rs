pub mod application;
pub mod domain;
pub mod infrastructure;

use std::sync::Arc;
use tokio::sync::Mutex;

use application::{
    AppState, CatalogService, LibraryService, LyricsService, PlayerService, PluginService,
};
use infrastructure::{
    InnertubeCatalogClient, LocalStorageAdapter, LrcLibClient, MockAudioEngine,
};

pub fn build_app_state() -> AppState {
    let audio_player = Arc::new(Mutex::new(MockAudioEngine::new()));
    let catalog_repo = Arc::new(InnertubeCatalogClient::default());
    let lyrics_repo = Arc::new(LrcLibClient::default());
    let storage_repo = Arc::new(LocalStorageAdapter::new());

    let player_service = Arc::new(PlayerService::new(audio_player));
    let catalog_service = Arc::new(CatalogService::new(catalog_repo));
    let lyrics_service = Arc::new(LyricsService::new(lyrics_repo));
    let library_service = Arc::new(LibraryService::new(storage_repo));
    let plugin_service = Arc::new(PluginService::new());

    AppState {
        player: player_service,
        catalog: catalog_service,
        lyrics: lyrics_service,
        library: library_service,
        plugins: plugin_service,
    }
}
