use crate::biquad::{Biquad, FilterType};

const MAX_BANDS: usize = 31;

pub struct Equalizer {
    sample_rate: f32,
    enabled: bool,
    active: usize,
    bands: [Biquad; MAX_BANDS],
    freqs: [f32; MAX_BANDS],
    gains: [f32; MAX_BANDS],
    qs: [f32; MAX_BANDS],
}

impl Equalizer {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            sample_rate: if sr > 0.0 { sr } else { 48_000.0 },
            enabled: true,
            active: 0,
            bands: std::array::from_fn(|_| Biquad::default()),
            freqs: [1000.0; MAX_BANDS],
            gains: [0.0; MAX_BANDS],
            qs: [1.414; MAX_BANDS],
        };
        for b in s.bands.iter_mut() {
            b.set_sample_rate(s.sample_rate);
        }
        s.setup_bands(10);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        self.sample_rate = if sr > 0.0 { sr } else { 48_000.0 };
        for b in self.bands.iter_mut() {
            b.set_sample_rate(self.sample_rate);
        }
        // Reapply current parameter snapshot so the biquad coefficients
        // reflect the new sample rate, then reset states (filter history is
        // stale at the new rate).
        self.recompute_all_coefficients();
        self.reset_states();
    }

    pub fn set_enabled(&mut self, on: bool) {
        self.enabled = on;
    }

    pub fn enabled(&self) -> bool {
        self.enabled
    }

    pub fn band_count(&self) -> usize {
        self.active
    }

    pub fn setup_bands(&mut self, count: usize) {
        // Clamp to a known standard and emit a warning for unknown counts.
        // - 1..=4 -> 5-band centers
        // - 6..=8 -> 9-band
        // - everything else -> use the requested count if standard, else 10-band default
        let effective = if (1..=4).contains(&count) {
            eprintln!("fxengine: eq band count {count} not standard, clamping to 5");
            5
        } else if (6..=8).contains(&count) {
            eprintln!("fxengine: eq band count {count} not standard, clamping to 9");
            9
        } else if matches!(count, 5 | 9 | 10 | 15 | 20 | 31) {
            count
        } else {
            eprintln!("fxengine: eq band count {count} not standard, using 10");
            10
        };

        let freqs = band_freqs(effective);
        let n = freqs.len().min(MAX_BANDS);
        let prev_active = self.active;
        let prev_gains = self.gains; // copy out before any re-zeroing

        self.active = n;
        let q = if effective >= 20 {
            2.8
        } else if effective == 5 {
            1.0
        } else {
            1.414
        };
        for i in 0..n {
            let kind = if i == 0 {
                FilterType::LowShelf
            } else if i + 1 == n {
                FilterType::HighShelf
            } else {
                FilterType::Peaking
            };
            // Preserve prior gains for indices that exist in both old and new
            // layouts (when the user re-arranges from e.g. 5 -> 10 bands).
            self.gains[i] = if i < prev_active { prev_gains[i] } else { 0.0 };
            self.freqs[i] = freqs[i];
            self.qs[i] = q;
            self.bands[i].set_sample_rate(self.sample_rate);
            self.bands[i].configure(kind, freqs[i], self.gains[i], q);
            self.bands[i].reset();
        }
    }

    pub fn set_gain(&mut self, i: usize, db: f32) {
        if i < self.active {
            let g = db.clamp(-12.0, 12.0);
            // Update only the snapshot fields. The audio callback will read
            // them on its next block and recompute coefficients, so there is
            // no race with the audio thread even when this is called from
            // the UI thread.
            self.gains[i] = g;
        }
    }

    pub fn set_freq(&mut self, i: usize, f: f32) {
        if i < self.active {
            self.freqs[i] = f;
        }
    }

    pub fn set_q(&mut self, i: usize, q: f32) {
        if i < self.active {
            self.qs[i] = q;
        }
    }

    pub fn gain(&self, i: usize) -> f32 {
        if i < self.active {
            self.gains[i]
        } else {
            0.0
        }
    }

    pub fn freq(&self, i: usize) -> f32 {
        if i < self.active {
            self.freqs[i]
        } else {
            1000.0
        }
    }

    pub fn q(&self, i: usize) -> f32 {
        if i < self.active {
            self.qs[i]
        } else {
            1.414
        }
    }

    pub fn reset_gains(&mut self) {
        for i in 0..self.active {
            self.set_gain(i, 0.0);
        }
    }

    pub fn reset_states(&mut self) {
        for i in 0..self.active {
            self.bands[i].reset();
        }
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        if !self.enabled {
            return;
        }
        // Recompute-once-per-block: read the latest gain/freq/q snapshot
        // from the (UI-thread-updated) arrays and update the biquad
        // coefficients. The biquad state itself (z1/z2) is owned by the
        // audio thread and only touched here.
        for i in 0..self.active {
            let kind = if i == 0 {
                FilterType::LowShelf
            } else if i + 1 == self.active {
                FilterType::HighShelf
            } else {
                FilterType::Peaking
            };
            self.bands[i].configure(kind, self.freqs[i], self.gains[i], self.qs[i]);
            self.bands[i].process_stereo(left, right);
        }
    }

    pub fn magnitude_db(&self, freq: f32) -> f32 {
        if !self.enabled {
            return 0.0;
        }
        let mut total = 0.0;
        for i in 0..self.active {
            let kind = if i == 0 {
                FilterType::LowShelf
            } else if i + 1 == self.active {
                FilterType::HighShelf
            } else {
                FilterType::Peaking
            };
            total += self
                .bands[i]
                .magnitude_db_for(kind, self.freqs[i], self.gains[i], self.qs[i], freq);
        }
        total
    }

    /// Re-apply all current gain/freq/q snapshots to the underlying biquads
    /// without resetting state. Used after a sample-rate change.
    fn recompute_all_coefficients(&mut self) {
        for i in 0..self.active {
            let kind = if i == 0 {
                FilterType::LowShelf
            } else if i + 1 == self.active {
                FilterType::HighShelf
            } else {
                FilterType::Peaking
            };
            self.bands[i].configure(kind, self.freqs[i], self.gains[i], self.qs[i]);
        }
    }
}

fn band_freqs(count: usize) -> &'static [f32] {
    match count {
        5 => &[60.0, 250.0, 1000.0, 4000.0, 12000.0],
        9 => &[62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0],
        15 => &[
            25.0, 40.0, 63.0, 100.0, 160.0, 250.0, 400.0, 630.0, 1000.0, 1600.0, 2500.0, 4000.0,
            6300.0, 10000.0, 16000.0,
        ],
        20 => &[
            31.0, 44.0, 63.0, 88.0, 125.0, 177.0, 250.0, 355.0, 500.0, 710.0, 1000.0, 1400.0,
            2000.0, 2800.0, 4000.0, 5600.0, 8000.0, 11300.0, 16000.0, 20000.0,
        ],
        31 => &[
            20.0, 25.0, 31.5, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0, 160.0, 200.0, 250.0, 315.0,
            400.0, 500.0, 630.0, 800.0, 1000.0, 1250.0, 1600.0, 2000.0, 2500.0, 3150.0, 4000.0,
            5000.0, 6300.0, 8000.0, 10000.0, 12500.0, 16000.0, 20000.0,
        ],
        _ => &[
            31.25, 62.5, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
        ],
    }
}
