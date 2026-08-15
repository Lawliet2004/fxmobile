#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum FilterType {
    Peaking,
    LowShelf,
    HighShelf,
    LowPass,
    HighPass,
}

#[derive(Clone, Debug)]
pub struct Biquad {
    kind: FilterType,
    freq: f32,
    gain_db: f32,
    q: f32,
    sample_rate: f32,
    b0: f32,
    b1: f32,
    b2: f32,
    a1: f32,
    a2: f32,
    z1_l: f32,
    z2_l: f32,
    z1_r: f32,
    z2_r: f32,
}

impl Default for Biquad {
    fn default() -> Self {
        let mut b = Self {
            kind: FilterType::Peaking,
            freq: 1000.0,
            gain_db: 0.0,
            q: 1.0,
            sample_rate: 48_000.0,
            b0: 1.0,
            b1: 0.0,
            b2: 0.0,
            a1: 0.0,
            a2: 0.0,
            z1_l: 0.0,
            z2_l: 0.0,
            z1_r: 0.0,
            z2_r: 0.0,
        };
        b.update();
        b
    }
}

impl Biquad {
    pub fn set_sample_rate(&mut self, sr: f32) {
        self.sample_rate = if sr > 0.0 { sr } else { 48_000.0 };
        self.update();
    }

    pub fn configure(&mut self, kind: FilterType, freq: f32, gain_db: f32, q: f32) {
        self.kind = kind;
        self.freq = freq.clamp(10.0, self.sample_rate * 0.49);
        self.gain_db = gain_db.clamp(-30.0, 30.0);
        self.q = q.clamp(0.1, 10.0);
        self.update();
    }

    pub fn set_gain_db(&mut self, g: f32) {
        self.gain_db = g.clamp(-30.0, 30.0);
        self.update();
    }

    pub fn set_frequency(&mut self, f: f32) {
        self.freq = f.clamp(10.0, self.sample_rate * 0.49);
        self.update();
    }

    pub fn set_q(&mut self, q: f32) {
        self.q = q.clamp(0.1, 10.0);
        self.update();
    }

    pub fn reset(&mut self) {
        self.z1_l = 0.0;
        self.z2_l = 0.0;
        self.z1_r = 0.0;
        self.z2_r = 0.0;
    }

    #[inline]
    pub fn process_mono(&mut self, x: f32) -> f32 {
        let y = self.b0 * x + self.z1_l;
        self.z1_l = self.b1 * x - self.a1 * y + self.z2_l;
        self.z2_l = self.b2 * x - self.a2 * y;
        y
    }

    #[inline]
    pub fn process_sample_stereo(&mut self, left: &mut f32, right: &mut f32) {
        let y_l = self.b0 * *left + self.z1_l;
        self.z1_l = self.b1 * *left - self.a1 * y_l + self.z2_l;
        self.z2_l = self.b2 * *left - self.a2 * y_l;
        *left = y_l;

        let y_r = self.b0 * *right + self.z1_r;
        self.z1_r = self.b1 * *right - self.a1 * y_r + self.z2_r;
        self.z2_r = self.b2 * *right - self.a2 * y_r;
        *right = y_r;
    }

    pub fn process_stereo(&mut self, left: &mut [f32], right: &mut [f32]) {
        let n = left.len().min(right.len());
        for i in 0..n {
            self.process_sample_stereo(&mut left[i], &mut right[i]);
        }
    }

    pub fn magnitude_db(&self, freq: f32) -> f32 {
        if freq <= 0.0 {
            return 0.0;
        }
        let w = 2.0 * std::f32::consts::PI * freq / self.sample_rate;
        let (sin_w, cos_w) = w.sin_cos();
        let (sin_2w, cos_2w) = (2.0 * w).sin_cos();
        let num_re = self.b0 + self.b1 * cos_w + self.b2 * cos_2w;
        let num_im = -self.b1 * sin_w - self.b2 * sin_2w;
        let den_re = 1.0 + self.a1 * cos_w + self.a2 * cos_2w;
        let den_im = -self.a1 * sin_w - self.a2 * sin_2w;
        let den = den_re * den_re + den_im * den_im;
        if den < 1e-12 {
            return 0.0;
        }
        let mag = ((num_re * num_re + num_im * num_im) / den).sqrt();
        20.0 * mag.max(1e-6).log10()
    }

    fn update(&mut self) {
        let w0 = 2.0 * std::f32::consts::PI * self.freq / self.sample_rate;
        let (sin_w0, cos_w0) = w0.sin_cos();
        let a = 10.0_f32.powf(self.gain_db / 40.0);
        let alpha = sin_w0 / (2.0 * self.q);

        let (b0, b1, b2, a0, a1, a2);

        match self.kind {
            FilterType::Peaking => {
                b0 = 1.0 + alpha * a;
                b1 = -2.0 * cos_w0;
                b2 = 1.0 - alpha * a;
                a0 = 1.0 + alpha / a;
                a1 = -2.0 * cos_w0;
                a2 = 1.0 - alpha / a;
            }
            FilterType::LowShelf => {
                let sa = a.sqrt();
                b0 = a * ((a + 1.0) - (a - 1.0) * cos_w0 + 2.0 * sa * alpha);
                b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cos_w0);
                b2 = a * ((a + 1.0) - (a - 1.0) * cos_w0 - 2.0 * sa * alpha);
                a0 = (a + 1.0) + (a - 1.0) * cos_w0 + 2.0 * sa * alpha;
                a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cos_w0);
                a2 = (a + 1.0) + (a - 1.0) * cos_w0 - 2.0 * sa * alpha;
            }
            FilterType::HighShelf => {
                let sa = a.sqrt();
                b0 = a * ((a + 1.0) + (a - 1.0) * cos_w0 + 2.0 * sa * alpha);
                b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cos_w0);
                b2 = a * ((a + 1.0) + (a - 1.0) * cos_w0 - 2.0 * sa * alpha);
                a0 = (a + 1.0) - (a - 1.0) * cos_w0 + 2.0 * sa * alpha;
                a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cos_w0);
                a2 = (a + 1.0) - (a - 1.0) * cos_w0 - 2.0 * sa * alpha;
            }
            FilterType::LowPass => {
                b0 = (1.0 - cos_w0) / 2.0;
                b1 = 1.0 - cos_w0;
                b2 = (1.0 - cos_w0) / 2.0;
                a0 = 1.0 + alpha;
                a1 = -2.0 * cos_w0;
                a2 = 1.0 - alpha;
            }
            FilterType::HighPass => {
                b0 = (1.0 + cos_w0) / 2.0;
                b1 = -(1.0 + cos_w0);
                b2 = (1.0 + cos_w0) / 2.0;
                a0 = 1.0 + alpha;
                a1 = -2.0 * cos_w0;
                a2 = 1.0 - alpha;
            }
        }

        let inv = if a0.abs() > 1e-12 { 1.0 / a0 } else { 1.0 };
        self.b0 = b0 * inv;
        self.b1 = b1 * inv;
        self.b2 = b2 * inv;
        self.a1 = a1 * inv;
        self.a2 = a2 * inv;
    }
}
