#[derive(Clone, Debug)]
pub struct Smoother {
    target: f32,
    current: f32,
    alpha: f32,
    time_ms: f32,
    sample_rate: f32,
}

impl Smoother {
    pub fn new(initial: f32, time_ms: f32) -> Self {
        let mut s = Self {
            target: initial,
            current: initial,
            alpha: 0.05,
            time_ms,
            sample_rate: 48_000.0,
        };
        s.recalc();
        s
    }

    pub fn set_sample_rate(&mut self, sr: f32) {
        self.sample_rate = if sr > 0.0 { sr } else { 48_000.0 };
        self.recalc();
    }

    pub fn set_time_ms(&mut self, ms: f32) {
        self.time_ms = if ms > 0.1 { ms } else { 15.0 };
        self.recalc();
    }

    pub fn set_target(&mut self, v: f32) {
        self.target = v;
    }

    pub fn snap(&mut self, v: f32) {
        self.target = v;
        self.current = v;
    }

    pub fn current(&self) -> f32 {
        self.current
    }

    pub fn is_smoothing(&self) -> bool {
        (self.target - self.current).abs() > 1e-5
    }

    #[inline]
    pub fn next(&mut self) -> f32 {
        if self.is_smoothing() {
            self.current += self.alpha * (self.target - self.current);
            if (self.target - self.current).abs() < 1e-6 {
                self.current = self.target;
            }
        }
        self.current
    }

    fn recalc(&mut self) {
        let t = self.time_ms / 1000.0;
        self.alpha = (1.0 - (-1.0 / (t * self.sample_rate)).exp()).clamp(0.0001, 1.0);
    }
}
