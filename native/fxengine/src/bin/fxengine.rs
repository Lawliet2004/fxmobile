use fxengine::Engine;
use std::env;
use std::f32::consts::PI;
use std::process;

const SMOKE_FRAMES: usize = 128;
const SMOKE_SR: f32 = 48_000.0;
const SMOKE_HZ: f32 = 440.0;

fn version_line() -> String {
    format!("fxengine {}", env!("CARGO_PKG_VERSION"))
}

fn print_usage() {
    println!(
        "\
{}
FxMobile host enhancer — shared Rust DSP engine CLI

The Android GUI (Jetpack Compose + Oboe) is Android-only and cannot run on
desktop. This binary is the host-runnable CLI around native/fxengine, the
same DSP used in the Android APK.

USAGE:
    fxengine [FLAG]

FLAGS:
    -h, --help       Print this help (including version, platforms, smoke)
    -V, --version    Print version and exit
        --smoke      Run Engine::new(48000, 512) with bass=5, clarity=3 on a
                     128-frame interleaved stereo 440 Hz sine and print peak/rms

PLATFORMS:
    Windows   host CLI artifact: fxengine-windows-x64.exe
    macOS     host CLI artifact: fxengine-macos-x64
    Linux     host CLI artifact: fxengine-linux-x64
    Android   GUI APK (not this binary); download from GitHub Releases

EXAMPLES:
    fxengine --help
    fxengine --smoke",
        version_line()
    );
}

fn smoke() {
    let mut engine = Engine::new(SMOKE_SR, 512);
    engine.set_bass(5.0);
    engine.set_clarity(3.0);

    let mut audio = vec![0.0f32; SMOKE_FRAMES * 2];
    for i in 0..SMOKE_FRAMES {
        let s = (2.0 * PI * SMOKE_HZ * i as f32 / SMOKE_SR).sin();
        audio[i * 2] = s;
        audio[i * 2 + 1] = s;
    }

    engine.process_interleaved(&mut audio, SMOKE_FRAMES);

    let mut peak = 0.0f32;
    let mut sumsq = 0.0f32;
    for &x in &audio {
        peak = peak.max(x.abs());
        sumsq += x * x;
    }
    let rms = (sumsq / audio.len() as f32).sqrt();

    println!(
        "smoke peak={peak:.6} rms={rms:.6} frames={SMOKE_FRAMES} sr={}",
        SMOKE_SR as u32
    );
}

fn main() {
    let args: Vec<String> = env::args().skip(1).collect();

    if args.is_empty() || args.iter().any(|a| a == "--help" || a == "-h") {
        print_usage();
        process::exit(0);
    }

    if args.iter().any(|a| a == "--version" || a == "-V") {
        println!("{}", version_line());
        process::exit(0);
    }

    if args.iter().any(|a| a == "--smoke") {
        smoke();
        process::exit(0);
    }

    eprintln!("error: unknown argument: {}", args[0]);
    eprintln!();
    print_usage();
    process::exit(1);
}
