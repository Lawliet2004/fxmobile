use crate::biquad::{Biquad, FilterType};
use crate::smoother::Smoother;
use std::collections::VecDeque;

pub struct DcBlocker {
    r: f32,
    prev_in_l: f32,
    prev_out_l: f32,
    prev_in_r: f32,
    prev_out_r: f32,
}

impl DcBlocker {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            r: 0.995,
            prev_in_l: 0.0,
            prev_out_l: 0.0,
            prev_in_r: 0.0,
            prev_out_r: 0.0,
        };
        s.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        let sr = if sr > 0.0 { sr } else { 48_000.0 };
        self.r = (1.0 - (2.0 * std::f32::consts::PI * 20.0 / sr)).clamp(0.90, 0.9999);
        // Filter state is no longer valid at the new sample rate.
        self.reset();
    }

    pub fn reset(&mut self) {
        self.prev_in_l = 0.0;
        self.prev_out_l = 0.0;
        self.prev_in_r = 0.0;
        self.prev_out_r = 0.0;
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        let r = self.r;
        for i in 0..left.len().min(right.len()) {
            let in_l = left[i];
            let out_l = in_l - self.prev_in_l + r * self.prev_out_l;
            self.prev_in_l = in_l;
            self.prev_out_l = out_l;
            left[i] = out_l;

            let in_r = right[i];
            let out_r = in_r - self.prev_in_r + r * self.prev_out_r;
            self.prev_in_r = in_r;
            self.prev_out_r = out_r;
            right[i] = out_r;
        }
    }
}

pub struct InputGain {
    gain_db: f32,
    smoother: Smoother,
}

impl InputGain {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            gain_db: 0.0,
            smoother: Smoother::new(1.0, 10.0),
        };
        s.smoother.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        self.smoother.set_sample_rate(sr);
    }

    pub fn set_gain_db(&mut self, db: f32) {
        self.gain_db = db.clamp(-24.0, 24.0);
        self.smoother.set_target(10.0_f32.powf(self.gain_db / 20.0));
    }

    pub fn reset(&mut self) {
        self.smoother.snap(10.0_f32.powf(self.gain_db / 20.0));
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        if !self.smoother.is_smoothing() && (self.smoother.current() - 1.0).abs() < 1e-5 {
            return;
        }
        for i in 0..left.len().min(right.len()) {
            let g = self.smoother.next();
            left[i] *= g;
            right[i] *= g;
        }
    }
}

pub struct BassBoost {
    amount: f32,
    smoother: Smoother,
    low_shelf: Biquad,
    low_pass: Biquad,
    prev_in_l: f32,
    prev_out_l: f32,
    prev_in_r: f32,
    prev_out_r: f32,
}

impl BassBoost {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            amount: 0.0,
            smoother: Smoother::new(0.0, 15.0),
            low_shelf: Biquad::default(),
            low_pass: Biquad::default(),
            prev_in_l: 0.0,
            prev_out_l: 0.0,
            prev_in_r: 0.0,
            prev_out_r: 0.0,
        };
        s.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        self.smoother.set_sample_rate(sr);
        self.low_shelf.set_sample_rate(sr);
        self.low_pass.set_sample_rate(sr);
        self.low_shelf.configure(FilterType::LowShelf, 90.0, 0.0, 0.8);
        self.low_pass.configure(FilterType::LowPass, 140.0, 0.0, 0.707);
    }

    pub fn set_amount(&mut self, amount: f32) {
        self.amount = amount.clamp(0.0, 10.0);
        self.smoother.set_target(self.amount);
        self.low_shelf.set_gain_db((self.amount / 10.0) * 8.5);
    }

    pub fn amount(&self) -> f32 {
        self.amount
    }

    pub fn reset(&mut self) {
        self.low_shelf.reset();
        self.low_pass.reset();
        self.prev_in_l = 0.0;
        self.prev_out_l = 0.0;
        self.prev_in_r = 0.0;
        self.prev_out_r = 0.0;
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        if self.amount <= 0.01 && !self.smoother.is_smoothing() {
            return;
        }
        const R: f32 = 0.995;
        for i in 0..left.len().min(right.len()) {
            let amt = self.smoother.next();
            let mix = (amt / 10.0) * 0.45;
            self.low_shelf.process_sample_stereo(&mut left[i], &mut right[i]);
            let harm_l = self.low_pass.process_mono(left[i]);
            let harm_r = self.low_pass.process_mono(right[i]);
            left[i] += (harm_l * 2.2).tanh() * mix;
            right[i] += (harm_r * 2.2).tanh() * mix;

            let dc_l = left[i] - self.prev_in_l + R * self.prev_out_l;
            self.prev_in_l = left[i];
            self.prev_out_l = dc_l;
            left[i] = dc_l;
            let dc_r = right[i] - self.prev_in_r + R * self.prev_out_r;
            self.prev_in_r = right[i];
            self.prev_out_r = dc_r;
            right[i] = dc_r;
        }
    }
}

pub struct Clarity {
    amount: f32,
    smoother: Smoother,
    high_shelf: Biquad,
    high_pass: Biquad,
}

impl Clarity {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            amount: 0.0,
            smoother: Smoother::new(0.0, 15.0),
            high_shelf: Biquad::default(),
            high_pass: Biquad::default(),
        };
        s.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        self.smoother.set_sample_rate(sr);
        self.high_shelf.set_sample_rate(sr);
        self.high_pass.set_sample_rate(sr);
        self.high_shelf.configure(FilterType::HighShelf, 6500.0, 0.0, 0.8);
        self.high_pass.configure(FilterType::HighPass, 3500.0, 0.0, 0.707);
    }

    pub fn set_amount(&mut self, amount: f32) {
        self.amount = amount.clamp(0.0, 10.0);
        self.smoother.set_target(self.amount);
        self.high_shelf.set_gain_db((self.amount / 10.0) * 7.5);
    }

    pub fn amount(&self) -> f32 {
        self.amount
    }

    pub fn reset(&mut self) {
        self.high_shelf.reset();
        self.high_pass.reset();
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        if self.amount <= 0.01 && !self.smoother.is_smoothing() {
            return;
        }
        for i in 0..left.len().min(right.len()) {
            let amt = self.smoother.next();
            let mix = (amt / 10.0) * 0.35;
            self.high_shelf.process_sample_stereo(&mut left[i], &mut right[i]);
            let h_l = self.high_pass.process_mono(left[i]);
            let h_r = self.high_pass.process_mono(right[i]);
            left[i] += (h_l / (1.0 + h_l.abs() * 1.5)) * mix;
            right[i] += (h_r / (1.0 + h_r.abs() * 1.5)) * mix;
        }
    }
}

pub struct DynamicBoost {
    amount: f32,
    smoother: Smoother,
    attack: f32,
    release: f32,
    env_l: f32,
    env_r: f32,
}

impl DynamicBoost {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            amount: 0.0,
            smoother: Smoother::new(0.0, 15.0),
            attack: 0.0,
            release: 0.0,
            env_l: 0.0,
            env_r: 0.0,
        };
        s.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        let sr = if sr > 0.0 { sr } else { 48_000.0 };
        self.smoother.set_sample_rate(sr);
        self.attack = (-1.0 / (0.004 * sr)).exp();
        self.release = (-1.0 / (0.050 * sr)).exp();
    }

    pub fn set_amount(&mut self, amount: f32) {
        self.amount = amount.clamp(0.0, 10.0);
        self.smoother.set_target(self.amount);
    }

    pub fn amount(&self) -> f32 {
        self.amount
    }

    pub fn reset(&mut self) {
        self.env_l = 0.0;
        self.env_r = 0.0;
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        if self.amount <= 0.01 && !self.smoother.is_smoothing() {
            return;
        }
        for i in 0..left.len().min(right.len()) {
            let boost = (self.smoother.next() / 10.0).max(0.0);
            let abs_l = left[i].abs();
            let abs_r = right[i].abs();
            self.env_l = if abs_l > self.env_l {
                abs_l + self.attack * (self.env_l - abs_l)
            } else {
                abs_l + self.release * (self.env_l - abs_l)
            };
            self.env_r = if abs_r > self.env_r {
                abs_r + self.attack * (self.env_r - abs_r)
            } else {
                abs_r + self.release * (self.env_r - abs_r)
            };
            let g_l = 1.0 + boost * (1.0 / (1.0 + 3.0 * self.env_l) - 0.25 * self.env_l);
            let g_r = 1.0 + boost * (1.0 / (1.0 + 3.0 * self.env_r) - 0.25 * self.env_r);
            left[i] *= g_l.max(0.0);
            right[i] *= g_r.max(0.0);
        }
    }
}

pub struct VolumeLeveler {
    amount: f32,
    smoother: Smoother,
    rms_coeff: f32,
    gain_coeff: f32,
    rms: f32,
    gain: f32,
}

impl VolumeLeveler {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            amount: 0.0,
            smoother: Smoother::new(0.0, 15.0),
            rms_coeff: 0.0,
            gain_coeff: 0.0,
            rms: 0.01,
            gain: 1.0,
        };
        s.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        let sr = if sr > 0.0 { sr } else { 48_000.0 };
        self.smoother.set_sample_rate(sr);
        self.rms_coeff = (-1.0 / (0.400 * sr)).exp();
        self.gain_coeff = (-1.0 / (0.250 * sr)).exp();
    }

    pub fn set_amount(&mut self, amount: f32) {
        self.amount = amount.clamp(0.0, 4.0);
        self.smoother.set_target(self.amount);
    }

    pub fn amount(&self) -> f32 {
        self.amount
    }

    pub fn reset(&mut self) {
        self.rms = 0.01;
        self.gain = 1.0;
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        if self.amount <= 0.01 && !self.smoother.is_smoothing() {
            return;
        }
        const TARGET: f32 = 0.125;
        for i in 0..left.len().min(right.len()) {
            let strength = self.smoother.next() / 4.0;
            let energy = (left[i] * left[i] + right[i] * right[i]) * 0.5;
            self.rms = energy + self.rms_coeff * (self.rms - energy);
            let current = self.rms.max(1e-6).sqrt();
            let ideal = (TARGET / current.max(0.02)).clamp(0.6, 2.5);
            let desired = 1.0 + (ideal - 1.0) * strength;
            self.gain = desired + self.gain_coeff * (self.gain - desired);
            left[i] *= self.gain;
            right[i] *= self.gain;
        }
    }
}

pub struct Master {
    gain_db: f32,
    balance: f32,
    sm_l: Smoother,
    sm_r: Smoother,
}

impl Master {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            gain_db: 0.0,
            balance: 0.0,
            sm_l: Smoother::new(1.0, 15.0),
            sm_r: Smoother::new(1.0, 15.0),
        };
        s.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        self.sm_l.set_sample_rate(sr);
        self.sm_r.set_sample_rate(sr);
        self.update();
    }

    pub fn set_gain_db(&mut self, db: f32) {
        self.gain_db = db.clamp(-20.0, 20.0);
        self.update();
    }

    pub fn set_balance(&mut self, bal: f32) {
        self.balance = bal.clamp(-20.0, 20.0);
        self.update();
    }

    pub fn gain_db(&self) -> f32 {
        self.gain_db
    }

    pub fn balance(&self) -> f32 {
        self.balance
    }

    pub fn reset(&mut self) {
        self.gain_db = 0.0;
        self.balance = 0.0;
        self.sm_l.snap(1.0);
        self.sm_r.snap(1.0);
        self.update();
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        for i in 0..left.len().min(right.len()) {
            left[i] *= self.sm_l.next();
            right[i] *= self.sm_r.next();
        }
    }

    fn update(&mut self) {
        let linear = 10.0_f32.powf(self.gain_db / 20.0);
        let norm = (self.balance + 20.0) / 40.0;
        let angle = norm * (std::f32::consts::PI / 2.0);
        let pan_l = angle.cos() * std::f32::consts::SQRT_2;
        let pan_r = angle.sin() * std::f32::consts::SQRT_2;
        self.sm_l.set_target(linear * pan_l);
        self.sm_r.set_target(linear * pan_r);
    }
}

pub struct Limiter {
    threshold: f32,
    attack: f32,
    release: f32,
    envelope: f32,
    clipped: bool,
    /// Per-channel lookahead delay lines, in sample order. The head is the
    /// oldest sample and the tail is the most recent. Length is fixed at
    /// `lookahead_samples`.
    lookahead_l: VecDeque<f32>,
    lookahead_r: VecDeque<f32>,
    /// Number of samples of lookahead in use (0 means bypassed).
    lookahead_samples: usize,
    /// 2 ms of lookahead by default. Will be clamped to a sane upper bound.
    lookahead_ms: f32,
}

impl Limiter {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            threshold: 0.965,
            attack: 0.0,
            release: 0.0,
            envelope: 0.0,
            clipped: false,
            lookahead_l: VecDeque::new(),
            lookahead_r: VecDeque::new(),
            lookahead_samples: 0,
            lookahead_ms: 2.0,
        };
        s.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        let sr = if sr > 0.0 { sr } else { 48_000.0 };
        self.attack = (-1.0 / (0.0002 * sr)).exp();
        self.release = (-1.0 / (0.045 * sr)).exp();
        // 2 ms of lookahead, clamped to [1, 192] samples.
        let desired = (self.lookahead_ms * sr / 1000.0).round() as usize;
        let max_samples = 192usize;
        self.lookahead_samples = desired.clamp(1, max_samples);
        self.lookahead_l = VecDeque::from(vec![0.0f32; self.lookahead_samples]);
        self.lookahead_r = VecDeque::from(vec![0.0f32; self.lookahead_samples]);
    }

    pub fn reset(&mut self) {
        self.envelope = 0.0;
        self.clipped = false;
        for s in self.lookahead_l.iter_mut() {
            *s = 0.0;
        }
        for s in self.lookahead_r.iter_mut() {
            *s = 0.0;
        }
    }

    pub fn take_clip(&mut self) -> bool {
        let c = self.clipped;
        self.clipped = false;
        c
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        let n = left.len().min(right.len());
        let la = self.lookahead_samples;
        for i in 0..n {
            // Push the new sample into the lookahead buffer (tail = newest).
            self.lookahead_l.push_back(left[i]);
            self.lookahead_r.push_back(right[i]);
            if self.lookahead_l.len() > la {
                self.lookahead_l.pop_front();
            }
            if self.lookahead_r.len() > la {
                self.lookahead_r.pop_front();
            }

            // Find the peak over the entire lookahead window. This is what
            // determines the gain we apply to the sample at the head of the
            // buffer — i.e. the sample that emerged `lookahead_samples` ago.
            let mut window_peak = 0.0f32;
            for &s in self.lookahead_l.iter() {
                let a = s.abs();
                if a > window_peak {
                    window_peak = a;
                }
            }
            for &s in self.lookahead_r.iter() {
                let a = s.abs();
                if a > window_peak {
                    window_peak = a;
                }
            }
            // Update the envelope toward the window peak.
            self.envelope = if window_peak > self.envelope {
                window_peak + self.attack * (self.envelope - window_peak)
            } else {
                window_peak + self.release * (self.envelope - window_peak)
            };

            let mut gain = 1.0;
            if self.envelope > self.threshold {
                gain = self.threshold / self.envelope;
                self.clipped = true;
            }

            // Apply gain to the sample at the head of the delay line.
            let mut out_l = self.lookahead_l[0] * gain;
            let mut out_r = self.lookahead_r[0] * gain;
            if out_l.abs() > self.threshold {
                out_l = soft_clip(out_l);
            }
            if out_r.abs() > self.threshold {
                out_r = soft_clip(out_r);
            }
            left[i] = out_l;
            right[i] = out_r;
        }
    }
}

fn soft_clip(x: f32) -> f32 {
    if x > 1.0 {
        0.999
    } else if x < -1.0 {
        -0.999
    } else {
        (x * 1.05).tanh() * 0.995
    }
}

pub struct Meter {
    peak_l: f32,
    peak_r: f32,
    rms_l: f32,
    rms_r: f32,
    clipping: bool,
}

impl Meter {
    pub fn new() -> Self {
        Self {
            peak_l: 0.0,
            peak_r: 0.0,
            rms_l: 0.0,
            rms_r: 0.0,
            clipping: false,
        }
    }

    pub fn reset(&mut self) {
        *self = Self::new();
    }

    pub fn update(&mut self, left: &[f32], right: &[f32], clipped: bool) {
        if left.is_empty() {
            return;
        }
        let mut max_l = 0.0f32;
        let mut max_r = 0.0f32;
        let mut sq_l = 0.0f32;
        let mut sq_r = 0.0f32;
        let n = left.len().min(right.len());
        for i in 0..n {
            max_l = max_l.max(left[i].abs());
            max_r = max_r.max(right[i].abs());
            sq_l += left[i] * left[i];
            sq_r += right[i] * right[i];
        }
        self.peak_l = max_l;
        self.peak_r = max_r;
        self.rms_l = (sq_l / n as f32).sqrt();
        self.rms_r = (sq_r / n as f32).sqrt();
        if clipped {
            self.clipping = true;
        }
    }

    pub fn read_and_clear_clip(&mut self) -> (f32, f32, f32, f32, bool) {
        let clip = self.clipping;
        self.clipping = false;
        (self.peak_l, self.peak_r, self.rms_l, self.rms_r, clip)
    }
}
