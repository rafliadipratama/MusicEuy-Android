use std::sync::Arc;
use tokio::sync::Mutex;

use crate::domain::error::DomainError;
use crate::domain::model::{PlaybackState, Track};
use crate::domain::ports::AudioPlayerPort;

pub struct PlayerService {
    audio_player: Arc<Mutex<dyn AudioPlayerPort>>,
    queue: Arc<Mutex<Vec<Track>>>,
    current_index: Arc<Mutex<Option<usize>>>,
}

impl PlayerService {
    pub fn new(audio_player: Arc<Mutex<dyn AudioPlayerPort>>) -> Self {
        Self {
            audio_player,
            queue: Arc::new(Mutex::new(Vec::new())),
            current_index: Arc::new(Mutex::new(None)),
        }
    }

    pub async fn play_track(&self, track: Track) -> Result<(), DomainError> {
        {
            let mut q = self.queue.lock().await;
            if let Some(pos) = q.iter().position(|t| t.id == track.id) {
                let mut idx = self.current_index.lock().await;
                *idx = Some(pos);
            } else {
                q.push(track.clone());
                let mut idx = self.current_index.lock().await;
                *idx = Some(q.len() - 1);
            }
        }

        let mut player = self.audio_player.lock().await;
        player.play(&track).await
    }

    pub async fn play_queue(&self, tracks: Vec<Track>, start_index: usize) -> Result<(), DomainError> {
        if tracks.is_empty() {
            return Err(DomainError::InvalidParameter("Queue cannot be empty".to_string()));
        }

        let index = start_index.min(tracks.len() - 1);
        let track = tracks[index].clone();

        {
            let mut q = self.queue.lock().await;
            *q = tracks;
            let mut idx = self.current_index.lock().await;
            *idx = Some(index);
        }

        let mut player = self.audio_player.lock().await;
        player.play(&track).await
    }

    pub async fn toggle_play_pause(&self) -> Result<PlaybackState, DomainError> {
        let mut player = self.audio_player.lock().await;
        let state = player.get_state().await;
        if state.is_playing {
            player.pause().await?;
        } else {
            player.resume().await?;
        }
        Ok(player.get_state().await)
    }

    pub async fn next_track(&self) -> Result<Option<Track>, DomainError> {
        let q = self.queue.lock().await;
        let mut idx_guard = self.current_index.lock().await;

        if q.is_empty() {
            return Ok(None);
        }

        let next_idx = match *idx_guard {
            Some(curr) => (curr + 1) % q.len(),
            None => 0,
        };

        *idx_guard = Some(next_idx);
        let track = q[next_idx].clone();
        drop(q);
        drop(idx_guard);

        let mut player = self.audio_player.lock().await;
        player.play(&track).await?;
        Ok(Some(track))
    }

    pub async fn previous_track(&self) -> Result<Option<Track>, DomainError> {
        let q = self.queue.lock().await;
        let mut idx_guard = self.current_index.lock().await;

        if q.is_empty() {
            return Ok(None);
        }

        let prev_idx = match *idx_guard {
            Some(curr) => {
                if curr == 0 {
                    q.len() - 1
                } else {
                    curr - 1
                }
            }
            None => 0,
        };

        *idx_guard = Some(prev_idx);
        let track = q[prev_idx].clone();
        drop(q);
        drop(idx_guard);

        let mut player = self.audio_player.lock().await;
        player.play(&track).await?;
        Ok(Some(track))
    }

    pub async fn seek(&self, position_ms: u64) -> Result<(), DomainError> {
        let mut player = self.audio_player.lock().await;
        player.seek(position_ms).await
    }

    pub async fn set_volume(&self, volume: f32) -> Result<(), DomainError> {
        let mut player = self.audio_player.lock().await;
        player.set_volume(volume).await
    }

    pub async fn get_state(&self) -> PlaybackState {
        let player = self.audio_player.lock().await;
        player.get_state().await
    }

    pub async fn get_queue(&self) -> Vec<Track> {
        let q = self.queue.lock().await;
        q.clone()
    }
}
