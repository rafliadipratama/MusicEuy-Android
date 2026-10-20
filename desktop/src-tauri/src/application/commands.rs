use std::sync::Arc;
use serde::{Deserialize, Serialize};

use crate::application::catalog_service::CatalogService;
use crate::application::library_service::LibraryService;
use crate::application::lyrics_service::LyricsService;
use crate::application::player_service::PlayerService;
use crate::domain::model::{Artist, Lyrics, PlaybackState, Track};

pub struct AppState {
    pub player: Arc<PlayerService>,
    pub catalog: Arc<CatalogService>,
    pub lyrics: Arc<LyricsService>,
    pub library: Arc<LibraryService>,
}

#[derive(Serialize, Deserialize)]
pub struct CommandResponse<T> {
    pub success: bool,
    pub data: Option<T>,
    pub error: Option<String>,
}

impl<T> CommandResponse<T> {
    pub fn ok(data: T) -> Self {
        Self {
            success: true,
            data: Some(data),
            error: None,
        }
    }

    pub fn err(err: impl ToString) -> Self {
        Self {
            success: false,
            data: None,
            error: Some(err.to_string()),
        }
    }
}

// Concrete Command Handlers
pub async fn search_tracks(state: &AppState, query: String) -> CommandResponse<Vec<Track>> {
    match state.catalog.search_tracks(&query).await {
        Ok(tracks) => CommandResponse::ok(tracks),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn get_trending(state: &AppState) -> CommandResponse<Vec<Track>> {
    match state.catalog.get_trending().await {
        Ok(tracks) => CommandResponse::ok(tracks),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn play_track(state: &AppState, track: Track) -> CommandResponse<PlaybackState> {
    // Record to history in background
    let _ = state.library.record_history(&track).await;

    // Resolve stream if needed
    let mut resolved_track = track;
    if resolved_track.stream_url.is_none() {
        if let Ok(url) = state.catalog.resolve_stream(&resolved_track.id).await {
            resolved_track.stream_url = Some(url);
        }
    }

    match state.player.play_track(resolved_track).await {
        Ok(_) => CommandResponse::ok(state.player.get_state().await),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn toggle_play_pause(state: &AppState) -> CommandResponse<PlaybackState> {
    match state.player.toggle_play_pause().await {
        Ok(new_state) => CommandResponse::ok(new_state),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn next_track(state: &AppState) -> CommandResponse<PlaybackState> {
    match state.player.next_track().await {
        Ok(Some(track)) => {
            let _ = state.library.record_history(&track).await;
            CommandResponse::ok(state.player.get_state().await)
        }
        Ok(None) => CommandResponse::ok(state.player.get_state().await),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn previous_track(state: &AppState) -> CommandResponse<PlaybackState> {
    match state.player.previous_track().await {
        Ok(Some(track)) => {
            let _ = state.library.record_history(&track).await;
            CommandResponse::ok(state.player.get_state().await)
        }
        Ok(None) => CommandResponse::ok(state.player.get_state().await),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn seek(state: &AppState, position_ms: u64) -> CommandResponse<PlaybackState> {
    match state.player.seek(position_ms).await {
        Ok(_) => CommandResponse::ok(state.player.get_state().await),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn set_volume(state: &AppState, volume: f32) -> CommandResponse<PlaybackState> {
    match state.player.set_volume(volume).await {
        Ok(_) => CommandResponse::ok(state.player.get_state().await),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn get_state(state: &AppState) -> CommandResponse<PlaybackState> {
    CommandResponse::ok(state.player.get_state().await)
}

pub async fn get_lyrics(
    state: &AppState,
    track_id: String,
    title: String,
    artist: String,
    duration_secs: Option<u64>,
) -> CommandResponse<Option<Lyrics>> {
    match state
        .lyrics
        .fetch_lyrics(&track_id, &title, &artist, duration_secs)
        .await
    {
        Ok(lrc) => CommandResponse::ok(lrc),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn toggle_favorite(state: &AppState, track: Track) -> CommandResponse<bool> {
    match state.library.toggle_favorite(&track).await {
        Ok(is_fav) => CommandResponse::ok(is_fav),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn get_favorites(state: &AppState) -> CommandResponse<Vec<Track>> {
    match state.library.get_favorites().await {
        Ok(favs) => CommandResponse::ok(favs),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn get_history(state: &AppState) -> CommandResponse<Vec<Track>> {
    match state.library.get_history().await {
        Ok(hist) => CommandResponse::ok(hist),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn get_artist(state: &AppState, artist_id: String) -> CommandResponse<Artist> {
    match state.catalog.get_artist(&artist_id).await {
        Ok(artist) => CommandResponse::ok(artist),
        Err(e) => CommandResponse::err(e),
    }
}
