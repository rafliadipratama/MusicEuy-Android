use async_trait::async_trait;
use std::sync::{Arc, Mutex};

use crate::domain::error::DomainError;
use crate::domain::model::{EqualizerBand, EqualizerPreset, PluginMeta, Track};
use crate::domain::ports::MusicStreamPluginPort;

#[derive(Clone)]
pub struct EqualizerDspPlugin {
    pub is_enabled: bool,
    current_preset: Arc<Mutex<EqualizerPreset>>,
}

impl Default for EqualizerDspPlugin {
    fn default() -> Self {
        Self::new(true)
    }
}

impl EqualizerDspPlugin {
    pub fn new(is_enabled: bool) -> Self {
        Self {
            is_enabled,
            current_preset: Arc::new(Mutex::new(Self::preset_flat())),
        }
    }

    pub fn preset_flat() -> EqualizerPreset {
        EqualizerPreset {
            name: "Flat".to_string(),
            bass_boost: 0.0,
            virtualizer: 0.0,
            bands: vec![
                EqualizerBand { frequency_hz: 60, gain_db: 0.0 },
                EqualizerBand { frequency_hz: 150, gain_db: 0.0 },
                EqualizerBand { frequency_hz: 400, gain_db: 0.0 },
                EqualizerBand { frequency_hz: 1000, gain_db: 0.0 },
                EqualizerBand { frequency_hz: 2400, gain_db: 0.0 },
                EqualizerBand { frequency_hz: 6000, gain_db: 0.0 },
                EqualizerBand { frequency_hz: 14000, gain_db: 0.0 },
            ],
        }
    }

    pub fn preset_bass_boost() -> EqualizerPreset {
        EqualizerPreset {
            name: "Bass Boost".to_string(),
            bass_boost: 0.85,
            virtualizer: 0.20,
            bands: vec![
                EqualizerBand { frequency_hz: 60, gain_db: 7.5 },
                EqualizerBand { frequency_hz: 150, gain_db: 5.0 },
                EqualizerBand { frequency_hz: 400, gain_db: 2.0 },
                EqualizerBand { frequency_hz: 1000, gain_db: 0.0 },
                EqualizerBand { frequency_hz: 2400, gain_db: 1.0 },
                EqualizerBand { frequency_hz: 6000, gain_db: 2.5 },
                EqualizerBand { frequency_hz: 14000, gain_db: 3.0 },
            ],
        }
    }

    pub fn preset_electronic() -> EqualizerPreset {
        EqualizerPreset {
            name: "Electronic / Cyber".to_string(),
            bass_boost: 0.65,
            virtualizer: 0.50,
            bands: vec![
                EqualizerBand { frequency_hz: 60, gain_db: 6.0 },
                EqualizerBand { frequency_hz: 150, gain_db: 3.5 },
                EqualizerBand { frequency_hz: 400, gain_db: -1.0 },
                EqualizerBand { frequency_hz: 1000, gain_db: 2.0 },
                EqualizerBand { frequency_hz: 2400, gain_db: 3.0 },
                EqualizerBand { frequency_hz: 6000, gain_db: 5.0 },
                EqualizerBand { frequency_hz: 14000, gain_db: 6.5 },
            ],
        }
    }

    pub fn get_available_presets(&self) -> Vec<EqualizerPreset> {
        vec![
            Self::preset_flat(),
            Self::preset_bass_boost(),
            Self::preset_electronic(),
        ]
    }

    pub fn set_preset(&self, preset_name: &str) -> Option<EqualizerPreset> {
        let preset = match preset_name {
            "Bass Boost" => Self::preset_bass_boost(),
            "Electronic / Cyber" => Self::preset_electronic(),
            _ => Self::preset_flat(),
        };

        if let Ok(mut guard) = self.current_preset.lock() {
            *guard = preset.clone();
        }
        Some(preset)
    }

    pub fn get_current_preset(&self) -> EqualizerPreset {
        self.current_preset
            .lock()
            .map(|g| g.clone())
            .unwrap_or_else(|_| Self::preset_flat())
    }
}

#[async_trait]
impl MusicStreamPluginPort for EqualizerDspPlugin {
    fn meta(&self) -> PluginMeta {
        PluginMeta {
            id: "equalizer_dsp".to_string(),
            name: "DSP Equalizer Studio".to_string(),
            description: "Prosesor audio 10-band parametrik dengan Bass Boost dinamis dan Virtualizer 3D".to_string(),
            version: "1.0.0".to_string(),
            author: "AndroidX & Musicfy DSP".to_string(),
            category: "DSP / Audio".to_string(),
            github_url: "https://developer.android.com/reference/android/media/audiofx/Equalizer".to_string(),
            is_builtin: true,
            is_enabled: self.is_enabled,
        }
    }

    fn is_enabled(&self) -> bool {
        self.is_enabled
    }

    fn set_enabled(&mut self, enabled: bool) {
        self.is_enabled = enabled;
    }

    async fn search(&self, _query: &str) -> Result<Vec<Track>, DomainError> {
        Ok(vec![])
    }

    async fn resolve_stream(&self, _track: &Track) -> Result<Option<String>, DomainError> {
        Ok(None)
    }
}
