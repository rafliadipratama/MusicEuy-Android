use std::sync::Arc;
use serde::{Deserialize, Serialize};

use crate::application::catalog_service::CatalogService;
use crate::application::library_service::LibraryService;
use crate::application::lyrics_service::LyricsService;
use crate::application::player_service::PlayerService;
use crate::application::plugin_service::PluginService;
use crate::domain::model::{Artist, EqualizerPreset, Lyrics, PlaybackState, PluginMeta, Track};

pub struct AppState {
    pub player: Arc<PlayerService>,
    pub catalog: Arc<CatalogService>,
    pub lyrics: Arc<LyricsService>,
    pub library: Arc<LibraryService>,
    pub plugins: Arc<PluginService>,
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
    let mut catalog_results = match state.catalog.search_tracks(&query).await {
        Ok(tracks) => tracks,
        Err(_) => vec![],
    };

    // Also fetch from enabled plugins (Monochrome FLAC, Deezer, JioSaavn, Jamendo)
    let plugin_results = state.plugins.search_all_plugins(&query).await;

    // Combine with plugin results prioritized first
    let mut combined = plugin_results;
    combined.append(&mut catalog_results);

    // Deduplicate by title & artist
    let mut unique = Vec::new();
    for track in combined {
        if !unique.iter().any(|t: &Track| t.title.eq_ignore_ascii_case(&track.title) && t.artist.eq_ignore_ascii_case(&track.artist)) {
            unique.push(track);
        }
    }

    CommandResponse::ok(unique)
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

    // Resolve stream if needed (first check specialized plugin cascade, then catalog fallback)
    let mut resolved_track = track;
    if resolved_track.stream_url.is_none() {
        if let Some(url) = state.plugins.resolve_stream_cascade(&resolved_track).await {
            resolved_track.stream_url = Some(url);
        } else if let Ok(url) = state.catalog.resolve_stream(&resolved_track.id).await {
            resolved_track.stream_url = Some(url);
        }
    }

    // Broadcast track change to social/RPC plugins
    state.plugins.on_track_change(&resolved_track, true).await;

    match state.player.play_track(resolved_track).await {
        Ok(_) => CommandResponse::ok(state.player.get_state().await),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn toggle_play_pause(state: &AppState) -> CommandResponse<PlaybackState> {
    match state.player.toggle_play_pause().await {
        Ok(new_state) => {
            if let Some(ref track) = new_state.current_track {
                state.plugins.on_track_change(track, new_state.is_playing).await;
            }
            CommandResponse::ok(new_state)
        }
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn next_track(state: &AppState) -> CommandResponse<PlaybackState> {
    match state.player.next_track().await {
        Ok(Some(track)) => {
            let _ = state.library.record_history(&track).await;
            state.plugins.on_track_change(&track, true).await;
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
            state.plugins.on_track_change(&track, true).await;
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

// Plugin Management Commands
pub async fn get_plugins(state: &AppState) -> CommandResponse<Vec<PluginMeta>> {
    let plugins = state.plugins.get_all_plugins().await;
    CommandResponse::ok(plugins)
}

pub async fn toggle_plugin(state: &AppState, plugin_id: String, enabled: bool) -> CommandResponse<bool> {
    match state.plugins.toggle_plugin(&plugin_id, enabled).await {
        Ok(res) => CommandResponse::ok(res),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn import_spotify_playlist(state: &AppState, url: String) -> CommandResponse<Vec<Track>> {
    match state.plugins.import_spotify_playlist(&url).await {
        Ok(tracks) => CommandResponse::ok(tracks),
        Err(e) => CommandResponse::err(e),
    }
}

pub async fn get_equalizer_presets(state: &AppState) -> CommandResponse<Vec<EqualizerPreset>> {
    let presets = state.plugins.get_available_equalizer_presets().await;
    CommandResponse::ok(presets)
}

pub async fn set_equalizer_preset(state: &AppState, preset_name: String) -> CommandResponse<EqualizerPreset> {
    let preset = state.plugins.set_equalizer_preset(&preset_name).await;
    CommandResponse::ok(preset)
}

pub async fn get_current_equalizer(state: &AppState) -> CommandResponse<EqualizerPreset> {
    let preset = state.plugins.get_equalizer_preset().await;
    CommandResponse::ok(preset)
}
