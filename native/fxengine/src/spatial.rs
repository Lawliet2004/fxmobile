use crate::biquad::{Biquad, FilterType};
use crate::smoother::Smoother;

const MAX_AMB_DELAY: usize = (96_000.0 * 0.080) as usize + 16;
const MAX_SIDE_DELAY: usize = (96_000.0 * 0.0006) as usize + 32;

pub struct Ambience {
    amount: f32,
    smoother: Smoother,
    delay_l: Vec<f32>,
    delay_r: Vec<f32>,
    buf_size: usize,
    write: usize,
    tap1: usize,
    tap2: usize,
    tap3: usize,
    tap4: usize,
    damp_l: Biquad,
    damp_r: Biquad,
}

impl Ambience {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            amount: 0.0,
            smoother: Smoother::new(0.0, 15.0),
            delay_l: vec![0.0; MAX_AMB_DELAY],
            delay_r: vec![0.0; MAX_AMB_DELAY],
            buf_size: MAX_AMB_DELAY,
            write: 0,
            tap1: 1,
            tap2: 1,
            tap3: 1,
            tap4: 1,
            damp_l: Biquad::default(),
            damp_r: Biquad::default(),
        };
        s.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        let sr = if sr > 0.0 { sr } else { 48_000.0 };
        self.smoother.set_sample_rate(sr);
        self.damp_l.set_sample_rate(sr);
        self.damp_r.set_sample_rate(sr);
        self.damp_l.configure(FilterType::LowPass, 5000.0, 0.0, 0.707);
        self.damp_r.configure(FilterType::LowPass, 5000.0, 0.0, 0.707);
        self.delay_l.fill(0.0);
        self.delay_r.fill(0.0);
        self.buf_size = ((sr * 0.080) as usize + 16).min(self.delay_l.len());
        self.tap1 = (sr * 0.0173) as usize;
        self.tap2 = (sr * 0.0291) as usize;
        self.tap3 = (sr * 0.0377) as usize;
        self.tap4 = (sr * 0.0479) as usize;
        self.write = 0;
        // Buffer was just resized/cleared — make sure the state is consistent.
        self.reset();
    }

    pub fn set_amount(&mut self, amount: f32) {
        self.amount = amount.clamp(0.0, 10.0);
        self.smoother.set_target(self.amount);
    }

    pub fn amount(&self) -> f32 {
        self.amount
    }

    pub fn reset(&mut self) {
        self.delay_l.fill(0.0);
        self.delay_r.fill(0.0);
        self.damp_l.reset();
        self.damp_r.reset();
        self.write = 0;
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        if self.amount <= 0.01 && !self.smoother.is_smoothing() {
            return;
        }
        let size = self.buf_size.max(1);
        for i in 0..left.len().min(right.len()) {
            let amt = self.smoother.next();
            let wet = (amt / 10.0) * 0.38;
            let fb = 0.25 + (amt / 10.0) * 0.15;
            let in_l = left[i];
            let in_r = right[i];
            let r1 = (self.write + size - self.tap1) % size;
            let r2 = (self.write + size - self.tap2) % size;
            let r3 = (self.write + size - self.tap3) % size;
            let r4 = (self.write + size - self.tap4) % size;
            let mut refl_l = 0.5 * self.delay_l[r1] + 0.35 * self.delay_r[r3] - 0.25 * self.delay_l[r4];
            let mut refl_r = 0.5 * self.delay_r[r2] + 0.35 * self.delay_l[r4] - 0.25 * self.delay_r[r3];
            refl_l = self.damp_l.process_mono(refl_l);
            refl_r = self.damp_r.process_mono(refl_r);
            self.delay_l[self.write] = in_l + refl_r * fb;
            self.delay_r[self.write] = in_r + refl_l * fb;
            self.write = (self.write + 1) % size;
            left[i] += refl_l * wet;
            right[i] += refl_r * wet;
        }
    }
}

pub struct Surround {
    amount: f32,
    smoother: Smoother,
    side_hp: Biquad,
    delay: Vec<f32>,
    delay_samples: usize,
    delay_index: usize,
}

impl Surround {
    pub fn new(sr: f32) -> Self {
        let mut s = Self {
            amount: 0.0,
            smoother: Smoother::new(0.0, 15.0),
            side_hp: Biquad::default(),
            delay: vec![0.0; MAX_SIDE_DELAY],
            delay_samples: 1,
            delay_index: 0,
        };
        s.set_sample_rate(sr);
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        let sr = if sr > 0.0 { sr } else { 48_000.0 };
        self.smoother.set_sample_rate(sr);
        self.side_hp.set_sample_rate(sr);
        self.side_hp.configure(FilterType::HighPass, 160.0, 0.0, 0.707);
        self.delay.fill(0.0);
        self.delay_samples = ((sr * 0.0006) as usize + 1).min(self.delay.len().saturating_sub(1));
        self.delay_index = 0;
    }

    pub fn set_amount(&mut self, amount: f32) {
        self.amount = amount.clamp(0.0, 10.0);
        self.smoother.set_target(self.amount);
    }

    pub fn amount(&self) -> f32 {
        self.amount
    }

    pub fn reset(&mut self) {
        self.side_hp.reset();
        self.delay.fill(0.0);
        self.delay_index = 0;
    }

    pub fn process(&mut self, left: &mut [f32], right: &mut [f32]) {
        if self.amount <= 0.01 && !self.smoother.is_smoothing() {
            return;
        }
        let dlen = self.delay.len();
        for i in 0..left.len().min(right.len()) {
            let amt = self.smoother.next();
            let side_boost = 1.0 + (amt / 10.0) * 1.2;
            let decorr = (amt / 10.0) * 0.35;
            let in_l = left[i];
            let in_r = right[i];
            let mid = (in_l + in_r) * 0.5;
            let side = (in_l - in_r) * 0.5;
            let filtered = self.side_hp.process_mono(side);
            let low = side - filtered;
            let read = (self.delay_index + dlen - self.delay_samples) % dlen;
            let delayed = self.delay[read];
            self.delay[self.delay_index] = filtered;
            self.delay_index = (self.delay_index + 1) % dlen;
            let enhanced = filtered * side_boost + delayed * decorr + low;
            left[i] = mid + enhanced;
            right[i] = mid - enhanced;
        }
    }
}
