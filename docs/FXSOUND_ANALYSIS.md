# FxSound Architecture & DSP Analysis

## 1. Executive Summary
This document analyzes the open-source **FxSound** desktop application ([fxsound2/fxsound-app](https://github.com/fxsound2/fxsound-app)) to establish the reference conceptual model, DSP signal flow, effect parameters, equalizer architecture, presets, and visual language for **FxMobile** (the lightweight Android native re-implementation).

---

## 2. Desktop FxSound Architecture Decomposition

The desktop FxSound application consists of three primary subsystems:
1. **FxSound GUI**: Visual interface built on JUCE / Win32 handling user interaction, preset selection, EQ curves, spectrum rendering, and IPC commands.
2. **Audio Passthrough Layer (`audiopassthru`)**: Windows-specific virtual audio device endpoint driver and capture loop that redirects system audio through the enhancement pipeline.
3. **DfxDsp Module**: Real-time C++ audio DSP engine performing multi-stage enhancement (spectral excitation, low-end enhancement, dynamic loudness boost, spatial room simulation, 9-band EQ, and output limiting).

### Key Architectural Learnings for Mobile:
- **Zero Windows Driver Code**: Mobile operating systems (Android/iOS) do not use virtual Windows audio driver models (WASAPI / APO / WDM). Android requires either an internal low-latency playback stream (AAudio/Oboe) or session-based `AudioEffect` integration.
- **Pure Decoupled DSP**: The DSP core must be strictly decoupled from UI and platform APIs, operating directly on standard interleaved or de-interleaved 32-bit float buffers (`float*`).
- **Real-Time Thread Guarantees**: The DSP loop must guarantee $O(1)$ execution time, zero memory allocations (`malloc`/`new`), zero locks, zero file I/O, and zero garbage collection.

---

## 3. DSP Effects & Parameter Mapping

FxSound desktop exposes 5 core enhancement sliders, a multi-band equalizer, master gain, balance, and volume leveling. Below is the mapping and behavioral specification:

| Control | User Range | DSP Core Algorithm | Audio Function |
|---|---|---|---|
| **Master Power** | ON / OFF | Click-free exponential cross-fade (15 ms ramp) | Master DSP active/bypass toggle |
| **Clarity (Fidelity)** | 0.0 – 10.0 | High-frequency harmonic exciter + transient sharpening (4 kHz–12 kHz) | Restores crispness, vocal presence, and high-end air |
| **Bass Boost** | 0.0 – 10.0 | Second-harmonic low exciter + dynamic low-shelf filter (50 Hz–120 Hz) with soft saturation | Deep, punchy bass without muddy sub-bass rumble or speaker distortion |
| **Ambience** | 0.0 – 10.0 | Multi-tap early reflection delay network + stereo decorrelation | Creates natural room dimensions and depth without heavy CPU convolution |
| **Surround** | 0.0 – 10.0 | Mid/Side psychoacoustic spatializer with phase-correlation limiter | Expands stereo soundstage while preserving 100% mono compatibility |
| **Dynamic Boost** | 0.0 – 10.0 | Upward compressor & crest factor reducer | Elevates quiet passages and enhances perceived loudness without harsh clipping |
| **Equalizer** | -12 dB to +12 dB | Cascaded RBJ Biquad Peaking / Shelf Filters | 5, 9, 10, 15, 20, or 31 configurable frequency bands |
| **Volume Leveling** | 0.0 – 4.0 | Slow-acting RMS envelope follower ($350\text{ ms}$) | Minimizes track-to-track volume jumps |
| **Master Gain** | -20 dB to +20 dB | Linear gain multiplier $10^{\text{gain}/20}$ with parameter smoothing | Master output level adjustment |
| **Stereo Balance** | -20 dB to +20 dB (L100 - R100) | Equal-power panning law ($\sin/\cos$) | Left/Right channel rebalancing |
| **Safety Limiter** | Fixed threshold ($0\text{ dBFS}$) | Fast-attack envelope limiter + tanh clip (no lookahead) | Eliminates digital clipping during high-gain boost |

---

## 4. Signal Processing Chain Order

To ensure maximum fidelity and prevent inter-modulation distortion, the processing order in FxMobile is structured as follows:

```
Audio Input (PCM 32-bit float)
   │
   ▼
[ 1. Input Gain & DC Blocker ]          <-- Removes DC bias (<10 Hz)
   │
   ▼
[ 2. Bass Boost Enhancer ]              <-- Harmonic generation & low-shelf
   │
   ▼
[ 3. Parametric Equalizer ]             <-- Multi-band Biquad IIR filters
   │
   ▼
[ 4. Clarity / Fidelity Exciter ]       <-- HF transient enhancement & harmonics
   │
   ▼
[ 5. Dynamic Boost Compressor ]         <-- Upward dynamic loudness enhancement
   │
   ▼
[ 6. Ambience Processor ]               <-- Acoustic room reflections & decorrelation
   │
   ▼
[ 7. Surround Spatializer ]             <-- Mid/Side phase-safe width expansion
   │
   ▼
[ 8. Volume Leveler ]                   <-- RMS dynamic leveling
   │
   ▼
[ 9. Master Gain & Balance ]            <-- Master attenuation/gain & equal-power pan
   │
   ▼
[ 10. Safety Peak Limiter ]             <-- Lookahead transparent soft limiter
   │
   ▼
[ 11. Atomic Peak/RMS Metering ]        <-- UI telemetry sampling (25–30 Hz)
   │
   ▼
Audio Output (PCM 32-bit float to DAC / Bluetooth / USB)
```

---

## 5. Preset Architecture

FxSound desktop presets configure all effect sliders and EQ bands simultaneously. In FxMobile, presets are represented in a compact, versioned JSON format:

```json
{
  "version": 1,
  "id": "preset_music",
  "name": "Music",
  "isBuiltIn": true,
  "masterGainDb": 0.0,
  "balanceDb": 0.0,
  "volumeLeveling": 1.0,
  "effects": {
    "bass": 3.5,
    "clarity": 3.0,
    "ambience": 2.0,
    "surround": 1.5,
    "dynamicBoost": 2.0
  },
  "eq": {
    "bandCount": 10,
    "gains": [2.5, 2.0, 1.0, 0.0, 0.0, 0.5, 1.5, 2.5, 3.0, 3.5]
  }
}
```

Built-in reference presets to include:
- **Music**: Rich warmth, subtle stereo enhancement, natural clarity boost.
- **Movie**: Enhanced speech clarity, expanded surround soundstage, deep cinematic bass.
- **Gaming**: Heightened high-frequency awareness (footsteps), wide surround spatialization, punchy explosions.
- **Voice / Speech**: Low-end rumble attenuation, strong midrange speech clarity, dynamic leveling for podcasts.
- **Bass Boost**: High low-end shelf, second-harmonic sub-bass excitation.
- **Flat (Neutral)**: All effects at 0.0, all EQ bands at 0.0 dB.

---

## 6. Visual Language & UX Principles

The visual design is inspired by FxSound's iconic dark aesthetic:
- **Color Scheme**:
  - Main Background: `#12151A` (Deep Slate Charcoal)
  - Card & Elevated Containers: `#1C2029` and `#252B37`
  - Border Accents: `#2D3342`
  - Primary Active Accent (Power & Highlights): `#E23E57` / `#FF3B30` (Electric Coral Red)
  - Secondary Accent (EQ Curves & Metrics): `#00ADB5` / `#00D2D3` (Vibrant Cyan)
  - High Contrast Text: `#FFFFFF` (Primary), `#9EABB8` (Secondary / Muted)
- **Controls**:
  - Large central circular Power Button with glowing halo animation when enabled.
  - Smooth vertical/horizontal slider tracks with filled progress and instantaneous dB/numeric badge.
  - Interactive EQ Bezier curve showing continuous frequency magnitude response.
  - Throttled real-time Peak & RMS stereo meter bar.
