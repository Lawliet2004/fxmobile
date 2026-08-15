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
        let freqs = band_freqs(count);
        let n = freqs.len().min(MAX_BANDS);
        self.active = n;
        let q = if count >= 20 {
            2.8
        } else if count == 5 {
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
            self.freqs[i] = freqs[i];
            self.gains[i] = 0.0;
            self.qs[i] = q;
            self.bands[i].set_sample_rate(self.sample_rate);
            self.bands[i].configure(kind, freqs[i], 0.0, q);
            self.bands[i].reset();
        }
    }

    pub fn set_gain(&mut self, i: usize, db: f32) {
        if i < self.active {
            let g = db.clamp(-12.0, 12.0);
            self.gains[i] = g;
            self.bands[i].set_gain_db(g);
        }
    }

    pub fn set_freq(&mut self, i: usize, f: f32) {
        if i < self.active {
            self.freqs[i] = f;
            self.bands[i].set_frequency(f);
        }
    }

    pub fn set_q(&mut self, i: usize, q: f32) {
        if i < self.active {
            self.qs[i] = q;
            self.bands[i].set_q(q);
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
        for i in 0..self.active {
            self.bands[i].process_stereo(left, right);
        }
    }

    pub fn magnitude_db(&self, freq: f32) -> f32 {
        if !self.enabled {
            return 0.0;
        }
        let mut total = 0.0;
        for i in 0..self.active {
            total += self.bands[i].magnitude_db(freq);
        }
        total
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
