use crate::eq::Equalizer;
use crate::processors::{
    BassBoost, Clarity, DcBlocker, DynamicBoost, InputGain, Limiter, Master, Meter, VolumeLeveler,
};
use crate::smoother::Smoother;
use crate::spatial::{Ambience, Surround};
use std::sync::atomic::{AtomicBool, Ordering};

pub struct Engine {
    sample_rate: f32,
    max_block: usize,
    power: AtomicBool,
    power_xfade: Smoother,
    scratch_l: Vec<f32>,
    scratch_r: Vec<f32>,
    dry_l: Vec<f32>,
    dry_r: Vec<f32>,
    input_copy: Vec<f32>,
    dc: DcBlocker,
    input: InputGain,
    bass: BassBoost,
    eq: Equalizer,
    clarity: Clarity,
    dynamic: DynamicBoost,
    ambience: Ambience,
    surround: Surround,
    leveler: VolumeLeveler,
    master: Master,
    limiter: Limiter,
    meter: Meter,
}

impl Engine {
    pub fn new(sample_rate: f32, max_block: usize) -> Self {
        let max_block = max_block.max(2048);
        let sr = if sample_rate > 0.0 { sample_rate } else { 48_000.0 };
        let mut e = Self {
            sample_rate: sr,
            max_block,
            power: AtomicBool::new(true),
            power_xfade: Smoother::new(1.0, 15.0),
            scratch_l: vec![0.0; max_block],
            scratch_r: vec![0.0; max_block],
            dry_l: vec![0.0; max_block],
            dry_r: vec![0.0; max_block],
            input_copy: vec![0.0; max_block * 2],
            dc: DcBlocker::new(sr),
            input: InputGain::new(sr),
            bass: BassBoost::new(sr),
            eq: Equalizer::new(sr),
            clarity: Clarity::new(sr),
            dynamic: DynamicBoost::new(sr),
            ambience: Ambience::new(sr),
            surround: Surround::new(sr),
            leveler: VolumeLeveler::new(sr),
            master: Master::new(sr),
            limiter: Limiter::new(sr),
            meter: Meter::new(),
        };
        e.prepare(sr, max_block);
        e
    }

    pub fn prepare(&mut self, sample_rate: f32, max_block: usize) {
        self.sample_rate = if sample_rate > 0.0 { sample_rate } else { 48_000.0 };
        let requested = max_block.max(1);
        if requested > self.max_block {
            self.max_block = requested;
            self.scratch_l.resize(requested, 0.0);
            self.scratch_r.resize(requested, 0.0);
            self.dry_l.resize(requested, 0.0);
            self.dry_r.resize(requested, 0.0);
            self.input_copy.resize(requested * 2, 0.0);
        }
        self.power_xfade.set_sample_rate(self.sample_rate);
        self.power_xfade.set_time_ms(15.0);
        self.dc.set_sample_rate(self.sample_rate);
        self.input.set_sample_rate(self.sample_rate);
        self.bass.set_sample_rate(self.sample_rate);
        self.eq.set_sample_rate(self.sample_rate);
        self.clarity.set_sample_rate(self.sample_rate);
        self.dynamic.set_sample_rate(self.sample_rate);
        self.ambience.set_sample_rate(self.sample_rate);
        self.surround.set_sample_rate(self.sample_rate);
        self.leveler.set_sample_rate(self.sample_rate);
        self.master.set_sample_rate(self.sample_rate);
        self.limiter.set_sample_rate(self.sample_rate);
        self.meter.reset();
    }

    /// Maximum block size, in frames, the engine was prepared for. JNI callers
    /// can use this to bound the frame count they pass into process() without
    /// risking `frames * 2` overflow.
    pub fn max_block(&self) -> usize {
        self.max_block
    }

    pub fn reset(&mut self) {
        self.dc.reset();
        self.input.reset();
        self.bass.reset();
        self.eq.reset_states();
        self.clarity.reset();
        self.dynamic.reset();
        self.ambience.reset();
        self.surround.reset();
        self.leveler.reset();
        self.master.reset();
        self.limiter.reset();
        self.meter.reset();
    }

    pub fn set_power(&mut self, on: bool) {
        self.power.store(on, Ordering::Relaxed);
        self.power_xfade.set_target(if on { 1.0 } else { 0.0 });
    }

    pub fn power(&self) -> bool {
        self.power.load(Ordering::Relaxed)
    }

    pub fn set_bass(&mut self, v: f32) {
        self.bass.set_amount(v);
    }
    pub fn set_clarity(&mut self, v: f32) {
        self.clarity.set_amount(v);
    }
    pub fn set_ambience(&mut self, v: f32) {
        self.ambience.set_amount(v);
    }
    pub fn set_surround(&mut self, v: f32) {
        self.surround.set_amount(v);
    }
    pub fn set_dynamic(&mut self, v: f32) {
        self.dynamic.set_amount(v);
    }
    pub fn set_leveling(&mut self, v: f32) {
        self.leveler.set_amount(v);
    }
    pub fn set_master_db(&mut self, v: f32) {
        self.master.set_gain_db(v);
    }
    pub fn set_balance(&mut self, v: f32) {
        self.master.set_balance(v);
    }
    pub fn set_input_db(&mut self, v: f32) {
        self.input.set_gain_db(v);
    }

    pub fn bass(&self) -> f32 {
        self.bass.amount()
    }
    pub fn clarity(&self) -> f32 {
        self.clarity.amount()
    }
    pub fn ambience(&self) -> f32 {
        self.ambience.amount()
    }
    pub fn surround(&self) -> f32 {
        self.surround.amount()
    }
    pub fn dynamic(&self) -> f32 {
        self.dynamic.amount()
    }
    pub fn leveling(&self) -> f32 {
        self.leveler.amount()
    }
    pub fn master_db(&self) -> f32 {
        self.master.gain_db()
    }
    pub fn balance(&self) -> f32 {
        self.master.balance()
    }

    pub fn set_eq_enabled(&mut self, on: bool) {
        self.eq.set_enabled(on);
    }
    pub fn eq_enabled(&self) -> bool {
        self.eq.enabled()
    }
    pub fn set_eq_band_count(&mut self, n: u32) {
        self.eq.setup_bands(n as usize);
    }
    pub fn eq_band_count(&self) -> u32 {
        self.eq.band_count() as u32
    }
    pub fn set_eq_gain(&mut self, i: u32, g: f32) {
        self.eq.set_gain(i as usize, g);
    }
    pub fn set_eq_freq(&mut self, i: u32, f: f32) {
        self.eq.set_freq(i as usize, f);
    }
    pub fn set_eq_q(&mut self, i: u32, q: f32) {
        self.eq.set_q(i as usize, q);
    }
    pub fn eq_gain(&self, i: u32) -> f32 {
        self.eq.gain(i as usize)
    }
    pub fn eq_freq(&self, i: u32) -> f32 {
        self.eq.freq(i as usize)
    }
    pub fn eq_q(&self, i: u32) -> f32 {
        self.eq.q(i as usize)
    }
    pub fn reset_eq(&mut self) {
        self.eq.reset_gains();
    }
    pub fn magnitude_db(&self, freq: f32) -> f32 {
        self.eq.magnitude_db(freq)
    }

    pub fn meter(&mut self) -> (f32, f32, f32, f32, bool) {
        self.meter.read_and_clear_clip()
    }

    pub fn process_interleaved(&mut self, audio: &mut [f32], frames: usize) {
        let needed = frames * 2;
        if audio.len() < needed {
            return;
        }
        if frames > self.max_block {
            let mut off = 0;
            while off < frames {
                let chunk = (frames - off).min(self.max_block);
                self.process_interleaved(&mut audio[off * 2..(off + chunk) * 2], chunk);
                off += chunk;
            }
            return;
        }
        self.input_copy[..needed].copy_from_slice(&audio[..needed]);
        self.process_from_copy(audio, frames);
    }

    pub fn process(&mut self, input: &[f32], output: &mut [f32], frames: usize) {
        if frames == 0 {
            return;
        }
        let needed = frames * 2;
        if input.len() < needed || output.len() < needed {
            return;
        }
        if frames > self.max_block {
            let mut off = 0;
            while off < frames {
                let chunk = (frames - off).min(self.max_block);
                self.process(
                    &input[off * 2..(off + chunk) * 2],
                    &mut output[off * 2..(off + chunk) * 2],
                    chunk,
                );
                off += chunk;
            }
            return;
        }
        self.input_copy[..needed].copy_from_slice(&input[..needed]);
        self.process_from_copy(output, frames);
    }

    fn process_from_copy(&mut self, output: &mut [f32], frames: usize) {
        let n = frames.min(self.max_block);
        let input = &self.input_copy;
        for i in 0..n {
            self.scratch_l[i] = input[i * 2];
            self.scratch_r[i] = input[i * 2 + 1];
        }

        let fading = self.power_xfade.is_smoothing();
        let mix = self.power_xfade.current();
        let on = self.power.load(Ordering::Relaxed);
        if fading || !on {
            for i in 0..n {
                self.dry_l[i] = input[i * 2];
                self.dry_r[i] = input[i * 2 + 1];
            }
        }

        if !on && !fading && mix <= 0.001 {
            if !std::ptr::eq(input.as_ptr(), output.as_ptr()) {
                output[..n * 2].copy_from_slice(&input[..n * 2]);
            }
            self.meter
                .update(&self.scratch_l[..n], &self.scratch_r[..n], false);
            return;
        }

        let left = &mut self.scratch_l[..n];
        let right = &mut self.scratch_r[..n];

        self.dc.process(left, right);
        self.input.process(left, right);
        self.bass.process(left, right);
        self.eq.process(left, right);
        self.clarity.process(left, right);
        self.dynamic.process(left, right);
        self.ambience.process(left, right);
        self.surround.process(left, right);
        self.leveler.process(left, right);
        self.master.process(left, right);
        self.limiter.process(left, right);
        let clipped = self.limiter.take_clip();

        if fading || mix < 0.999 {
            for i in 0..n {
                let wet = self.power_xfade.next();
                let dry = 1.0 - wet;
                self.scratch_l[i] = self.scratch_l[i] * wet + self.dry_l[i] * dry;
                self.scratch_r[i] = self.scratch_r[i] * wet + self.dry_r[i] * dry;
            }
        }

        for i in 0..n {
            output[i * 2] = self.scratch_l[i];
            output[i * 2 + 1] = self.scratch_r[i];
        }
        self.meter
            .update(&self.scratch_l[..n], &self.scratch_r[..n], clipped);
    }
}
