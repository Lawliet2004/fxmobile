mod biquad;
mod engine;
mod eq;
mod processors;
mod smoother;
mod spatial;

pub use engine::Engine;

const BACKEND: &str = "Rust DSP\0";

unsafe fn engine_mut<'a>(ptr: *mut Engine) -> Option<&'a mut Engine> {
    if ptr.is_null() {
        None
    } else {
        Some(&mut *ptr)
    }
}

unsafe fn engine_ref<'a>(ptr: *const Engine) -> Option<&'a Engine> {
    if ptr.is_null() {
        None
    } else {
        Some(&*ptr)
    }
}

#[no_mangle]
pub extern "C" fn fx_engine_create(sample_rate: f32, max_block: u32) -> *mut Engine {
    Box::into_raw(Box::new(Engine::new(sample_rate, max_block as usize)))
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_destroy(engine: *mut Engine) {
    if !engine.is_null() {
        drop(Box::from_raw(engine));
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_prepare(engine: *mut Engine, sample_rate: f32, max_block: u32) {
    if let Some(e) = engine_mut(engine) {
        e.prepare(sample_rate, max_block as usize);
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_reset(engine: *mut Engine) {
    if let Some(e) = engine_mut(engine) {
        e.reset();
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_process_interleaved(
    engine: *mut Engine,
    audio: *mut f32,
    frames: u32,
) {
    if audio.is_null() {
        return;
    }
    if let Some(e) = engine_mut(engine) {
        let n = frames as usize;
        e.process_interleaved(std::slice::from_raw_parts_mut(audio, n * 2), n);
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_process(
    engine: *mut Engine,
    input: *const f32,
    output: *mut f32,
    frames: u32,
) {
    if input.is_null() || output.is_null() {
        return;
    }
    if let Some(e) = engine_mut(engine) {
        let n = frames as usize;
        e.process(
            std::slice::from_raw_parts(input, n * 2),
            std::slice::from_raw_parts_mut(output, n * 2),
            n,
        );
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_set_power(engine: *mut Engine, enabled: bool) {
    if let Some(e) = engine_mut(engine) {
        e.set_power(enabled);
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_get_power(engine: *const Engine) -> bool {
    engine_ref(engine).map(|e| e.power()).unwrap_or(false)
}

macro_rules! set_f {
    ($name:ident, $method:ident) => {
        #[no_mangle]
        pub unsafe extern "C" fn $name(engine: *mut Engine, v: f32) {
            if let Some(e) = engine_mut(engine) {
                e.$method(v);
            }
        }
    };
}
macro_rules! get_f {
    ($name:ident, $method:ident) => {
        #[no_mangle]
        pub unsafe extern "C" fn $name(engine: *const Engine) -> f32 {
            engine_ref(engine).map(|e| e.$method()).unwrap_or(0.0)
        }
    };
}

set_f!(fx_engine_set_bass, set_bass);
set_f!(fx_engine_set_clarity, set_clarity);
set_f!(fx_engine_set_ambience, set_ambience);
set_f!(fx_engine_set_surround, set_surround);
set_f!(fx_engine_set_dynamic, set_dynamic);
set_f!(fx_engine_set_leveling, set_leveling);
set_f!(fx_engine_set_master_db, set_master_db);
set_f!(fx_engine_set_balance, set_balance);
set_f!(fx_engine_set_input_db, set_input_db);

get_f!(fx_engine_get_bass, bass);
get_f!(fx_engine_get_clarity, clarity);
get_f!(fx_engine_get_ambience, ambience);
get_f!(fx_engine_get_surround, surround);
get_f!(fx_engine_get_dynamic, dynamic);
get_f!(fx_engine_get_leveling, leveling);
get_f!(fx_engine_get_master_db, master_db);
get_f!(fx_engine_get_balance, balance);

#[no_mangle]
pub unsafe extern "C" fn fx_engine_set_eq_enabled(engine: *mut Engine, enabled: bool) {
    if let Some(e) = engine_mut(engine) {
        e.set_eq_enabled(enabled);
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_get_eq_enabled(engine: *const Engine) -> bool {
    engine_ref(engine).map(|e| e.eq_enabled()).unwrap_or(false)
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_set_eq_band_count(engine: *mut Engine, count: u32) {
    if let Some(e) = engine_mut(engine) {
        e.set_eq_band_count(count);
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_get_eq_band_count(engine: *const Engine) -> u32 {
    engine_ref(engine).map(|e| e.eq_band_count()).unwrap_or(10)
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_set_eq_gain(engine: *mut Engine, index: u32, gain_db: f32) {
    if let Some(e) = engine_mut(engine) {
        e.set_eq_gain(index, gain_db);
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_set_eq_freq(engine: *mut Engine, index: u32, freq: f32) {
    if let Some(e) = engine_mut(engine) {
        e.set_eq_freq(index, freq);
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_set_eq_q(engine: *mut Engine, index: u32, q: f32) {
    if let Some(e) = engine_mut(engine) {
        e.set_eq_q(index, q);
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_get_eq_gain(engine: *const Engine, index: u32) -> f32 {
    engine_ref(engine).map(|e| e.eq_gain(index)).unwrap_or(0.0)
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_get_eq_freq(engine: *const Engine, index: u32) -> f32 {
    engine_ref(engine)
        .map(|e| e.eq_freq(index))
        .unwrap_or(1000.0)
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_get_eq_q(engine: *const Engine, index: u32) -> f32 {
    engine_ref(engine).map(|e| e.eq_q(index)).unwrap_or(1.414)
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_reset_eq(engine: *mut Engine) {
    if let Some(e) = engine_mut(engine) {
        e.reset_eq();
    }
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_magnitude_db(engine: *const Engine, freq: f32) -> f32 {
    engine_ref(engine).map(|e| e.magnitude_db(freq)).unwrap_or(0.0)
}

#[no_mangle]
pub unsafe extern "C" fn fx_engine_get_meter(engine: *mut Engine, out5: *mut f32) {
    if out5.is_null() {
        return;
    }
    if let Some(e) = engine_mut(engine) {
        let (pl, pr, rl, rr, clip) = e.meter();
        *out5.add(0) = pl;
        *out5.add(1) = pr;
        *out5.add(2) = rl;
        *out5.add(3) = rr;
        *out5.add(4) = if clip { 1.0 } else { 0.0 };
    }
}

#[no_mangle]
pub extern "C" fn fx_engine_backend_name() -> *const std::os::raw::c_char {
    BACKEND.as_ptr() as *const std::os::raw::c_char
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn peaking_response() {
        let mut e = Engine::new(48_000.0, 512);
        e.set_eq_enabled(true);
        e.set_eq_band_count(10);
        e.reset_eq();
        e.set_eq_gain(5, 6.0); // 1 kHz
        let at = e.magnitude_db(1000.0);
        assert!((at - 6.0).abs() < 0.25, "got {at}");
    }

    #[test]
    fn limiter_ceiling() {
        let mut e = Engine::new(48_000.0, 512);
        e.set_bass(10.0);
        e.set_clarity(10.0);
        e.set_dynamic(10.0);
        e.set_master_db(12.0);
        let mut buf = vec![0.0f32; 1024 * 2];
        for i in 0..1024 {
            buf[i * 2] = (2.0 * std::f32::consts::PI * 100.0 * i as f32 / 48_000.0).sin() * 0.95;
            buf[i * 2 + 1] = (2.0 * std::f32::consts::PI * 100.0 * i as f32 / 48_000.0).cos() * 0.95;
        }
        e.process_interleaved(&mut buf, 1024);
        let peak = buf.iter().fold(0.0f32, |a, x| a.max(x.abs()));
        assert!(peak <= 1.0, "peak {peak}");
    }

    #[test]
    fn surround_mono_compatible() {
        let mut e = Engine::new(48_000.0, 512);
        e.set_surround(10.0);
        e.set_bass(0.0);
        e.set_clarity(0.0);
        e.set_ambience(0.0);
        e.set_dynamic(0.0);
        e.set_leveling(0.0);
        e.set_eq_enabled(false);
        let mut buf = vec![0.0f32; 512 * 2];
        for i in 0..512 {
            let s = (2.0 * std::f32::consts::PI * 440.0 * i as f32 / 48_000.0).sin() * 0.5;
            buf[i * 2] = s;
            buf[i * 2 + 1] = s * 0.7;
        }
        let orig = buf.clone();
        e.process_interleaved(&mut buf, 512);
        let mut max_diff = 0.0f32;
        for i in 0..512 {
            let in_m = (orig[i * 2] + orig[i * 2 + 1]) * 0.5;
            let out_m = (buf[i * 2] + buf[i * 2 + 1]) * 0.5;
            max_diff = max_diff.max((in_m - out_m).abs());
        }
        // DC + other stages may add a tiny residual; surround itself is mid-preserving.
        assert!(max_diff < 0.08, "mono delta {max_diff}");
    }

    #[test]
    fn dc_blocker() {
        let mut e = Engine::new(48_000.0, 512);
        e.set_bass(0.0);
        e.set_clarity(0.0);
        e.set_ambience(0.0);
        e.set_surround(0.0);
        e.set_dynamic(0.0);
        e.set_leveling(0.0);
        e.reset_eq();
        e.set_master_db(0.0);
        let mut buf = vec![0.25f32; 4096 * 2];
        e.process_interleaved(&mut buf, 4096);
        let mean: f32 = buf[4096..].iter().sum::<f32>() / 4096.0;
        assert!(mean.abs() < 0.03, "mean {mean}");
    }

    #[test]
    fn c_abi_roundtrip() {
        unsafe {
            let p = fx_engine_create(48_000.0, 512);
            assert!(!p.is_null());
            fx_engine_set_bass(p, 4.0);
            assert!((fx_engine_get_bass(p) - 4.0).abs() < 1e-5);
            fx_engine_destroy(p);
        }
    }

    fn dry_engine() -> Engine {
        let mut e = Engine::new(48_000.0, 512);
        e.set_bass(0.0);
        e.set_clarity(0.0);
        e.set_ambience(0.0);
        e.set_surround(0.0);
        e.set_dynamic(0.0);
        e.set_leveling(0.0);
        e.set_eq_enabled(false);
        e.set_master_db(0.0);
        e.set_balance(0.0);
        e
    }

    fn settle(e: &mut Engine) {
        let mut buf = vec![0.0f32; 512 * 2];
        for _ in 0..24 {
            e.process_interleaved(&mut buf, 512);
        }
    }

    fn sine(frames: usize, amp: f32, freq: f32) -> Vec<f32> {
        let mut buf = vec![0.0f32; frames * 2];
        for i in 0..frames {
            let s = (2.0 * std::f32::consts::PI * freq * i as f32 / 48_000.0).sin() * amp;
            buf[i * 2] = s;
            buf[i * 2 + 1] = s;
        }
        buf
    }

    #[test]
    fn smoother_zipper_bound() {
        let mut s = crate::smoother::Smoother::new(0.0, 15.0);
        s.set_sample_rate(48_000.0);
        s.set_target(1.0);
        let mut prev = s.current();
        let mut max_step = 0.0f32;
        for _ in 0..12_000 {
            let v = s.next();
            max_step = max_step.max((v - prev).abs());
            prev = v;
        }
        assert!(max_step <= 0.0015, "step {max_step}");
        assert!(prev > 0.99, "final {prev}");
    }

    #[test]
    fn master_gain_and_balance() {
        let mut e = dry_engine();
        e.set_master_db(6.0);
        settle(&mut e);
        let mut buf = sine(1024, 0.4, 440.0);
        e.process_interleaved(&mut buf, 1024);
        let peak = buf.iter().fold(0.0f32, |a, x| a.max(x.abs()));
        assert!((peak - 0.8).abs() < 0.08, "peak {peak}");

        let mut e = dry_engine();
        e.set_balance(20.0);
        settle(&mut e);
        let mut buf = sine(1024, 0.4, 440.0);
        e.process_interleaved(&mut buf, 1024);
        let mut left = 0.0f32;
        let mut right = 0.0f32;
        for i in 0..1024 {
            left = left.max(buf[i * 2].abs());
            right = right.max(buf[i * 2 + 1].abs());
        }
        assert!(left < 0.05, "left {left}");
        assert!(right > 0.45, "right {right}");
    }

    #[test]
    fn power_crossfade_no_hard_step() {
        let mut e = dry_engine();
        e.set_master_db(6.0);
        settle(&mut e);
        let mut warm = vec![0.25f32; 256 * 2];
        e.process_interleaved(&mut warm, 256);
        e.set_power(false);
        let mut buf = vec![0.25f32; 512 * 2];
        e.process_interleaved(&mut buf, 512);
        let mut prev = buf[0];
        let mut max_step = 0.0f32;
        for i in 1..512 {
            let v = buf[i * 2];
            max_step = max_step.max((v - prev).abs());
            prev = v;
        }
        assert!(max_step <= 0.002, "step {max_step}");
    }

    #[test]
    fn callback_budget() {
        let mut e = Engine::new(48_000.0, 256);
        e.set_bass(8.0);
        e.set_clarity(8.0);
        e.set_ambience(6.0);
        e.set_surround(6.0);
        e.set_dynamic(6.0);
        e.set_leveling(2.0);
        e.set_eq_enabled(true);
        let mut buf = sine(256, 0.3, 440.0);
        let start = std::time::Instant::now();
        for _ in 0..1000 {
            e.process_interleaved(&mut buf, 256);
        }
        let avg = start.elapsed().as_secs_f64() / 1000.0;
        assert!(avg < 0.00533, "avg {avg}s");
    }

    fn rms(buf: &[f32]) -> f32 {
        let e: f32 = buf.iter().map(|x| x * x).sum();
        (e / buf.len() as f32).sqrt()
    }

    fn process_sine(e: &mut Engine, freq: f32, frames: usize) -> f32 {
        let mut buf = sine(frames, 0.2, freq);
        e.process_interleaved(&mut buf, frames);
        let skip = (frames / 4) * 2;
        rms(&buf[skip..])
    }

    fn sine_rms(amp: f32) -> f32 {
        amp / std::f32::consts::SQRT_2
    }

    #[test]
    fn eq_boosts_target_sine_and_bypasses() {
        let mut e = dry_engine();
        e.set_eq_enabled(true);
        e.set_eq_band_count(10);
        e.reset_eq();
        e.set_eq_gain(5, 6.0);
        settle(&mut e);
        let ref_rms = sine_rms(0.2);
        let boosted = process_sine(&mut e, 1000.0, 4096);
        let far = process_sine(&mut e, 80.0, 4096);
        assert!(
            (boosted / ref_rms - 2.0).abs() < 0.2,
            "1 kHz +6 dB rms {boosted} ratio {}",
            boosted / ref_rms
        );
        assert!(
            (far / ref_rms - 1.0).abs() < 0.2,
            "80 Hz should stay near unity, rms {far}"
        );

        e.set_eq_enabled(false);
        settle(&mut e);
        let bypassed = process_sine(&mut e, 1000.0, 4096);
        assert!(
            (bypassed / ref_rms - 1.0).abs() < 0.08,
            "bypass rms {bypassed}"
        );
    }

    #[test]
    fn eq_cut_and_band_counts() {
        let mut e = dry_engine();
        e.set_eq_enabled(true);
        e.set_eq_band_count(10);
        e.reset_eq();
        e.set_eq_gain(5, -6.0);
        settle(&mut e);
        let cut = process_sine(&mut e, 1000.0, 4096);
        assert!(
            (cut / sine_rms(0.2) - 0.5).abs() < 0.12,
            "1 kHz -6 dB rms {cut} ratio {}",
            cut / sine_rms(0.2)
        );

        for count in [5u32, 9, 10, 15, 20, 31] {
            e.set_eq_band_count(count);
            assert_eq!(e.eq_band_count(), count);
            e.reset_eq();
            let mid = count / 2;
            e.set_eq_gain(mid, 6.0);
            let f = e.eq_freq(mid);
            settle(&mut e);
            let at = e.magnitude_db(f);
            assert!(
                (at - 6.0).abs() < 0.6,
                "count {count} center {f} mag {at}"
            );
        }
    }

    #[test]
    fn eq_c_abi_gain_and_curve() {
        unsafe {
            let p = fx_engine_create(48_000.0, 512);
            fx_engine_set_eq_enabled(p, true);
            fx_engine_set_eq_band_count(p, 10);
            fx_engine_reset_eq(p);
            fx_engine_set_eq_gain(p, 5, 4.5);
            assert!((fx_engine_get_eq_gain(p, 5) - 4.5).abs() < 1e-5);
            let mag = fx_engine_magnitude_db(p, 1000.0);
            assert!((mag - 4.5).abs() < 0.3, "mag {mag}");
            fx_engine_destroy(p);
        }
    }

    #[test]
    fn music_preset_golden() {
        let mut e = Engine::new(48_000.0, 512);
        e.set_bass(3.5);
        e.set_clarity(3.0);
        e.set_ambience(2.0);
        e.set_surround(1.5);
        e.set_dynamic(2.0);
        e.set_leveling(1.0);
        e.set_eq_enabled(true);
        e.set_eq_band_count(10);
        let gains = [2.5, 2.0, 1.0, 0.0, 0.0, 0.5, 1.5, 2.5, 3.0, 3.5];
        for (i, g) in gains.iter().enumerate() {
            e.set_eq_gain(i as u32, *g);
        }
        settle(&mut e);
        let mut buf = sine(2048, 0.35, 220.0);
        e.process_interleaved(&mut buf, 2048);
        let peak = buf.iter().fold(0.0f32, |a, x| a.max(x.abs()));
        let energy: f32 = buf.iter().map(|x| x * x).sum();
        assert!(peak <= 1.0, "peak {peak}");
        assert!(energy > 1.0, "energy {energy}");
    }
}
