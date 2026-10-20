use async_trait::async_trait;
use std::sync::{Arc, Mutex};
use std::time::Instant;

use crate::domain::error::DomainError;
use crate::domain::model::{PlaybackState, RepeatMode, Track};
use crate::domain::ports::AudioPlayerPort;

struct InternalAudioState {
    current_track: Option<Track>,
    is_playing: bool,
    position_ms: u64,
    last_tick: Option<Instant>,
    volume: f32,
    repeat_mode: RepeatMode,
    is_shuffled: bool,
}

#[derive(Clone)]
pub struct MockAudioEngine {
    state: Arc<Mutex<InternalAudioState>>,
}

impl MockAudioEngine {
    pub fn new() -> Self {
        let engine = Self {
            state: Arc::new(Mutex::new(InternalAudioState {
                current_track: None,
                is_playing: false,
                position_ms: 0,
                last_tick: None,
                volume: 0.85,
                repeat_mode: RepeatMode::Off,
                is_shuffled: false,
            })),
        };

        // Background ticker to advance playback position smoothly
        let ticker_state = Arc::clone(&engine.state);
        tokio::spawn(async move {
            let mut interval = tokio::time::interval(std::time::Duration::from_millis(250));
            loop {
                interval.tick().await;
                if let Ok(mut guard) = ticker_state.lock() {
                    if guard.is_playing {
                        if let Some(ref track) = guard.current_track {
                            let duration_ms = track.duration_secs * 1000;
                            let now = Instant::now();
                            if let Some(last) = guard.last_tick {
                                let elapsed = now.duration_since(last).as_millis() as u64;
                                guard.position_ms += elapsed;
                                if guard.position_ms >= duration_ms {
                                    guard.position_ms = duration_ms;
                                    guard.is_playing = false;
                                }
                            }
                            guard.last_tick = Some(now);
                        }
                    } else {
                        guard.last_tick = None;
                    }
                }
            }
        });

        engine
    }
}

impl Default for MockAudioEngine {
    fn default() -> Self {
        Self::new()
    }
}

#[async_trait]
impl AudioPlayerPort for MockAudioEngine {
    async fn play(&mut self, track: &Track) -> Result<(), DomainError> {
        let mut guard = self
            .state
            .lock()
            .map_err(|e| DomainError::AudioError(e.to_string()))?;
        guard.current_track = Some(track.clone());
        guard.position_ms = 0;
        guard.is_playing = true;
        guard.last_tick = Some(Instant::now());
        Ok(())
    }

    async fn pause(&mut self) -> Result<(), DomainError> {
        let mut guard = self
            .state
            .lock()
            .map_err(|e| DomainError::AudioError(e.to_string()))?;
        guard.is_playing = false;
        guard.last_tick = None;
        Ok(())
    }

    async fn resume(&mut self) -> Result<(), DomainError> {
        let mut guard = self
            .state
            .lock()
            .map_err(|e| DomainError::AudioError(e.to_string()))?;
        if guard.current_track.is_some() {
            guard.is_playing = true;
            guard.last_tick = Some(Instant::now());
        }
        Ok(())
    }

    async fn stop(&mut self) -> Result<(), DomainError> {
        let mut guard = self
            .state
            .lock()
            .map_err(|e| DomainError::AudioError(e.to_string()))?;
        guard.is_playing = false;
        guard.position_ms = 0;
        guard.last_tick = None;
        guard.current_track = None;
        Ok(())
    }

    async fn seek(&mut self, position_ms: u64) -> Result<(), DomainError> {
        let mut guard = self
            .state
            .lock()
            .map_err(|e| DomainError::AudioError(e.to_string()))?;
        guard.position_ms = position_ms;
        guard.last_tick = Some(Instant::now());
        Ok(())
    }

    async fn set_volume(&mut self, volume: f32) -> Result<(), DomainError> {
        let mut guard = self
            .state
            .lock()
            .map_err(|e| DomainError::AudioError(e.to_string()))?;
        guard.volume = volume.clamp(0.0, 1.0);
        Ok(())
    }

    async fn get_state(&self) -> PlaybackState {
        if let Ok(guard) = self.state.lock() {
            let duration_ms = guard
                .current_track
                .as_ref()
                .map(|t| t.duration_secs * 1000)
                .unwrap_or(0);
            PlaybackState {
                current_track: guard.current_track.clone(),
                is_playing: guard.is_playing,
                position_ms: guard.position_ms,
                duration_ms,
                volume: guard.volume,
                repeat_mode: guard.repeat_mode.clone(),
                is_shuffled: guard.is_shuffled,
            }
        } else {
            PlaybackState::default()
        }
    }
}
