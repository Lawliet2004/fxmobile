//! Factory preset DSP stress tests.
//! Tables must stay in lockstep with `BuiltInPresets.kt`.

use fxengine::Engine;

struct Factory {
    name: &'static str,
    clarity: f32,
    surround: f32,
    ambience: f32,
    dynamic: f32,
    bass: f32,
    eq: [f32; 10],
}

// Must match app/src/main/java/com/fxsound/mobile/core/presets/BuiltInPresets.kt
const FACTORY: &[Factory] = &[
    Factory {
        name: "General",
        clarity: 3.9,
        surround: 1.6,
        ambience: 0.0,
        dynamic: 4.7,
        bass: 4.7,
        eq: [0.0, 0.0, 1.0, 2.0, 0.0, -1.0, 0.0, -1.0, -2.0, 0.0],
    },
    Factory {
        name: "Music",
        clarity: 3.9,
        surround: 2.8,
        ambience: 2.8,
        dynamic: 1.6,
        bass: 4.7,
        eq: [0.0, 2.0, 2.0, 1.0, 0.0, 0.0, 0.0, -1.0, 0.0, 2.0],
    },
    Factory {
        name: "Voice",
        clarity: 5.7,
        surround: 0.0,
        ambience: 0.0,
        dynamic: 7.5,
        bass: 0.0,
        eq: [0.0, -4.0, -2.0, 2.0, 4.0, 5.0, 3.0, 3.0, 5.0, -11.0],
    },
    Factory {
        name: "Volume Boost",
        clarity: 2.5,
        surround: 1.6,
        ambience: 0.0,
        dynamic: 8.1,
        bass: 2.8,
        eq: [0.0, 3.0, 2.0, 2.0, 0.0, 0.0, 1.0, 1.0, 2.0, 2.0],
    },
    Factory {
        name: "Gaming",
        clarity: 2.8,
        surround: 0.0,
        ambience: 0.0,
        dynamic: 6.7,
        bass: 2.8,
        eq: [0.0, 0.0, 2.0, 2.0, 2.0, 0.0, -1.0, -1.0, 2.0, 2.0],
    },
    Factory {
        name: "Classic Processing",
        clarity: 4.7,
        surround: 2.8,
        ambience: 4.7,
        dynamic: 4.7,
        bass: 5.5,
        eq: [0.0; 10],
    },
    Factory {
        name: "Light Processing",
        clarity: 2.0,
        surround: 0.0,
        ambience: 2.8,
        dynamic: 0.4,
        bass: 1.6,
        eq: [0.0, -1.0, 1.0, 1.0, -1.0, -1.0, -2.0, -1.0, -1.0, 1.0],
    },
    Factory {
        name: "Bass Boost",
        clarity: 2.4,
        surround: 2.8,
        ambience: 2.8,
        dynamic: 1.6,
        bass: 5.9,
        eq: [0.0, 3.0, 3.0, 2.0, 1.0, -1.0, -1.0, -1.0, -1.0, 0.0],
    },
    Factory {
        name: "Streaming Video",
        clarity: 2.8,
        surround: 2.8,
        ambience: 0.0,
        dynamic: 4.3,
        bass: 2.8,
        eq: [0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 1.0, -1.0, 0.0, 2.0],
    },
    Factory {
        name: "Movies",
        clarity: 4.7,
        surround: 3.9,
        ambience: 0.0,
        dynamic: 6.7,
        bass: 2.8,
        eq: [0.0, 0.0, 2.0, 0.0, 2.0, 2.0, 1.0, -1.0, 0.0, 2.0],
    },
    Factory {
        name: "TV",
        clarity: 3.9,
        surround: 3.9,
        ambience: 1.6,
        dynamic: 4.7,
        bass: 3.5,
        eq: [0.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, -1.0, -1.0, 2.0],
    },
    Factory {
        name: "Transcription",
        clarity: 7.9,
        surround: 0.0,
        ambience: 0.0,
        dynamic: 9.1,
        bass: 5.9,
        eq: [0.0, -12.0, 7.0, 2.0, -1.0, 7.0, 0.0, 10.0, 3.0, -12.0],
    },
    Factory {
        name: "Default",
        clarity: 0.0,
        surround: 0.0,
        ambience: 0.0,
        dynamic: 0.0,
        bass: 0.0,
        eq: [0.0; 10],
    },
];

fn find(name: &str) -> &'static Factory {
    FACTORY
        .iter()
        .find(|p| p.name == name)
        .unwrap_or_else(|| panic!("missing factory {name}"))
}

fn apply(e: &mut Engine, p: &Factory) {
    e.set_power(true);
    e.set_bass(p.bass);
    e.set_clarity(p.clarity);
    e.set_ambience(p.ambience);
    e.set_surround(p.surround);
    e.set_dynamic(p.dynamic);
    e.set_leveling(0.0);
    e.set_master_db(0.0);
    e.set_balance(0.0);
    e.set_eq_enabled(true);
    e.set_eq_band_count(10);
    e.reset_eq();
    for (i, g) in p.eq.iter().enumerate() {
        e.set_eq_gain(i as u32, *g);
    }
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

fn noise(frames: usize, seed: u32) -> Vec<f32> {
    let mut state = seed.max(1);
    let mut buf = vec![0.0f32; frames * 2];
    for i in 0..frames {
        state = state.wrapping_mul(1664525).wrapping_add(1013904223);
        let n = (state as f32 / u32::MAX as f32) * 2.0 - 1.0;
        buf[i * 2] = n * 0.25;
        buf[i * 2 + 1] = n * 0.18;
    }
    buf
}

fn assert_safe(buf: &[f32], name: &str) {
    let mut peak = 0.0f32;
    let mut energy = 0.0f32;
    for &x in buf {
        assert!(x.is_finite(), "{name} non-finite {x}");
        peak = peak.max(x.abs());
        energy += x * x;
    }
    assert!(peak <= 1.0 + 1e-4, "{name} peak {peak}");
    assert!(energy > 0.0, "{name} silent");
}

fn process_preset(p: &Factory, buf: &mut [f32], frames: usize) {
    let mut e = Engine::new(48_000.0, 512);
    apply(&mut e, p);
    settle(&mut e);
    e.process_interleaved(buf, frames);
}

fn rms_skip(buf: &[f32], skip_frames: usize) -> f32 {
    let start = skip_frames * 2;
    if start >= buf.len() {
        return 0.0;
    }
    let slice = &buf[start..];
    let e: f32 = slice.iter().map(|x| x * x).sum();
    (e / slice.len() as f32).sqrt()
}

#[test]
fn every_factory_preset_is_safe_on_sine_and_noise() {
    assert_eq!(FACTORY.len(), 13);
    for p in FACTORY {
        assert!(p.bass >= 0.0 && p.bass <= 10.0, "{}", p.name);
        assert!(p.clarity >= 0.0 && p.clarity <= 10.0, "{}", p.name);
        assert!(p.ambience >= 0.0 && p.ambience <= 10.0, "{}", p.name);
        assert!(p.surround >= 0.0 && p.surround <= 10.0, "{}", p.name);
        assert!(p.dynamic >= 0.0 && p.dynamic <= 10.0, "{}", p.name);
        for g in p.eq {
            assert!((-12.0..=12.0).contains(&g), "{} eq {g}", p.name);
        }

        let mut sine_buf = sine(2048, 0.35, 220.0);
        process_preset(p, &mut sine_buf, 2048);
        assert_safe(&sine_buf, &format!("{} sine", p.name));

        let mut noise_buf = noise(2048, 0xC0FFEE);
        process_preset(p, &mut noise_buf, 2048);
        assert_safe(&noise_buf, &format!("{} noise", p.name));
    }
}

#[test]
fn rapid_preset_switching_stays_safe() {
    let mut e = Engine::new(48_000.0, 256);
    let mut buf = sine(256, 0.4, 440.0);
    for _ in 0..50 {
        for p in FACTORY {
            apply(&mut e, p);
            e.process_interleaved(&mut buf, 256);
            assert_safe(&buf, p.name);
            buf = sine(256, 0.4, 440.0);
        }
    }
}

#[test]
fn eq_signatures_match_factory_tables() {
    let mut e = Engine::new(48_000.0, 512);
    apply(&mut e, find("Voice"));
    let voice_low = e.magnitude_db(62.5);
    let voice_speech = e.magnitude_db(1000.0);
    let voice_air = e.magnitude_db(16_000.0);
    assert!(
        voice_low < -2.0,
        "Voice should cut 62.5 Hz, got {voice_low}"
    );
    assert!(
        voice_speech > 3.0,
        "Voice should lift speech, got {voice_speech}"
    );
    assert!(
        voice_air < -4.0,
        "Voice should cut 16 kHz, got {voice_air}"
    );

    apply(&mut e, find("Bass Boost"));
    let bass_low = e.magnitude_db(62.5);
    assert!(
        bass_low > 2.0,
        "Bass Boost should lift 62.5 Hz, got {bass_low}"
    );
    assert!(
        bass_low > voice_low + 4.0,
        "Bass Boost {bass_low} should exceed Voice {voice_low}"
    );

    apply(&mut e, find("Transcription"));
    let tx_low = e.magnitude_db(62.5);
    let tx_presence = e.magnitude_db(4000.0);
    let tx_air = e.magnitude_db(16_000.0);
    assert!(tx_low < -8.0, "Transcription 62.5 Hz {tx_low}");
    assert!(tx_presence > 7.0, "Transcription 4 kHz {tx_presence}");
    assert!(tx_air < -4.0, "Transcription 16 kHz {tx_air}");

    apply(&mut e, find("Default"));
    for f in [62.5, 1000.0, 4000.0, 16_000.0] {
        let mag = e.magnitude_db(f);
        assert!(mag.abs() < 0.35, "Default EQ at {f} Hz was {mag}");
    }
}

#[test]
fn volume_boost_is_louder_than_default() {
    let default = find("Default");
    let boost = find("Volume Boost");
    let mut dry = sine(4096, 0.2, 440.0);
    let mut wet = sine(4096, 0.2, 440.0);
    process_preset(default, &mut dry, 4096);
    process_preset(boost, &mut wet, 4096);
    let dry_rms = rms_skip(&dry, 1024);
    let wet_rms = rms_skip(&wet, 1024);
    assert!(
        wet_rms > dry_rms * 1.05,
        "Volume Boost rms {wet_rms} vs Default {dry_rms}"
    );
}

#[test]
fn bass_boost_lifts_low_sine_vs_voice() {
    let mut bass = sine(4096, 0.2, 80.0);
    let mut voice = sine(4096, 0.2, 80.0);
    process_preset(find("Bass Boost"), &mut bass, 4096);
    process_preset(find("Voice"), &mut voice, 4096);
    let bass_rms = rms_skip(&bass, 1024);
    let voice_rms = rms_skip(&voice, 1024);
    assert!(
        bass_rms > voice_rms * 1.15,
        "80 Hz Bass Boost {bass_rms} vs Voice {voice_rms}"
    );
}

#[test]
fn default_is_near_unity_after_settle() {
    let mut buf = sine(4096, 0.2, 440.0);
    process_preset(find("Default"), &mut buf, 4096);
    let out = rms_skip(&buf, 1024);
    let expect = 0.2 / std::f32::consts::SQRT_2;
    assert!(
        (out / expect - 1.0).abs() < 0.12,
        "Default rms {out} vs {expect}"
    );
}

#[test]
fn isolated_bass_and_clarity_move_the_right_bands() {
    let mut bass = Engine::new(48_000.0, 512);
    bass.set_bass(0.0);
    bass.set_clarity(0.0);
    bass.set_ambience(0.0);
    bass.set_surround(0.0);
    bass.set_dynamic(0.0);
    bass.set_leveling(0.0);
    bass.set_eq_enabled(false);
    bass.set_master_db(0.0);
    settle(&mut bass);
    let mut dry = sine(4096, 0.2, 80.0);
    bass.process_interleaved(&mut dry, 4096);
    let dry_low = rms_skip(&dry, 1024);

    bass.set_bass(8.0);
    settle(&mut bass);
    let mut wet = sine(4096, 0.2, 80.0);
    bass.process_interleaved(&mut wet, 4096);
    let wet_low = rms_skip(&wet, 1024);
    assert!(
        wet_low > dry_low * 1.2,
        "bass 80 Hz {wet_low} vs dry {dry_low}"
    );

    let mut clarity = Engine::new(48_000.0, 512);
    clarity.set_bass(0.0);
    clarity.set_clarity(0.0);
    clarity.set_ambience(0.0);
    clarity.set_surround(0.0);
    clarity.set_dynamic(0.0);
    clarity.set_eq_enabled(false);
    settle(&mut clarity);
    let mut dry_hi = sine(4096, 0.2, 8000.0);
    clarity.process_interleaved(&mut dry_hi, 4096);
    let dry_h = rms_skip(&dry_hi, 1024);

    clarity.set_clarity(8.0);
    settle(&mut clarity);
    let mut wet_hi = sine(4096, 0.2, 8000.0);
    clarity.process_interleaved(&mut wet_hi, 4096);
    let wet_h = rms_skip(&wet_hi, 1024);
    assert!(
        wet_h > dry_h * 1.15,
        "clarity 8 kHz {wet_h} vs dry {dry_h}"
    );
}

#[test]
fn getters_round_trip_every_factory_table() {
    let mut e = Engine::new(48_000.0, 512);
    for p in FACTORY {
        apply(&mut e, p);
        assert!((e.bass() - p.bass).abs() < 1e-5, "{}", p.name);
        assert!((e.clarity() - p.clarity).abs() < 1e-5, "{}", p.name);
        assert!((e.ambience() - p.ambience).abs() < 1e-5, "{}", p.name);
        assert!((e.surround() - p.surround).abs() < 1e-5, "{}", p.name);
        assert!((e.dynamic() - p.dynamic).abs() < 1e-5, "{}", p.name);
        for (i, g) in p.eq.iter().enumerate() {
            assert!(
                (e.eq_gain(i as u32) - g).abs() < 1e-5,
                "{} band {i}",
                p.name
            );
        }
    }
}

/// Property test: 100 random preset selections in random order must not
/// produce any non-finite samples and must keep the output peak bounded.
/// Exercises the recompute-once-per-block EQ path under churn.
#[test]
fn random_preset_churn_stays_bounded() {
    use std::collections::hash_map::DefaultHasher;
    use std::hash::{Hash, Hasher};

    // Linear congruential generator — no `rand` crate required, but with
    // enough bits of state to look random for 100 iterations.
    fn next(state: &mut u64) -> u32 {
        *state = state
            .wrapping_mul(6364136223846793005)
            .wrapping_add(1442695040888963407);
        (*state >> 33) as u32
    }
    let mut hasher = DefaultHasher::new();
    0x00C0_FFEE_BEEFu64.hash(&mut hasher);
    let mut rng_state = hasher.finish();

    let mut e = Engine::new(48_000.0, 256);
    let mut sine_buf = sine(256, 0.4, 440.0);
    let mut noise_buf = noise(256, 0xDEAD_BEEF);

    for step in 0..100 {
        // Pick a random preset.
        let idx = (next(&mut rng_state) as usize) % FACTORY.len();
        let p = &FACTORY[idx];
        apply(&mut e, p);
        // Mutate one random EQ band a little, to exercise the recompute path.
        let band = (next(&mut rng_state) as usize) % p.eq.len();
        let bump = ((next(&mut rng_state) as f32) / u32::MAX as f32) * 4.0 - 2.0;
        e.set_eq_gain(band as u32, p.eq[band] + bump);

        // Feed a sine and a noise block.
        e.process_interleaved(&mut sine_buf, 256);
        e.process_interleaved(&mut noise_buf, 256);

        for (label, buf) in [("sine", &sine_buf), ("noise", &noise_buf)] {
            let mut peak = 0.0f32;
            for &x in buf {
                assert!(x.is_finite(), "step {step} {label} non-finite {x}");
                peak = peak.max(x.abs());
            }
            assert!(
                peak <= 1.0 + 1e-4,
                "step {step} {label} peak {peak} (preset {})",
                p.name
            );
        }
    }
}
