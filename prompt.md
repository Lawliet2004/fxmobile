# Build an Ultra-Lightweight Mobile Audio Enhancer Based on FxSound

## 1. Objective

Build a production-quality mobile audio enhancement application inspired by and functionally based on the open-source FxSound project:

https://github.com/fxsound2/fxsound-app

The desktop FxSound repository is the primary reference for understanding the application's conceptual feature set, DSP behavior, presets, equalizer architecture, controls, terminology, and visual language.

The goal is NOT to create a WebView wrapper, a remote-control application, or a superficial UI clone.

Build a real native mobile audio-processing application that performs audio enhancement locally on the device with extremely low CPU, memory, battery, and storage overhead.

The target application should feel like "FxSound for mobile":

* Very small application footprint.
* Extremely fast startup.
* Native UI.
* Real-time audio processing.
* Offline-first operation.
* No mandatory account.
* No cloud dependency for DSP.
* No unnecessary background services.
* Minimal battery consumption.
* Minimal RAM usage.
* Professional audio controls.
* Visual design closely inspired by the desktop application's visual identity and color coordination.
* Preserve the conceptual behavior of FxSound's major controls wherever the mobile platform allows it.

The initial target platform should be Android.

Design the architecture so that an iOS implementation can be added later without rewriting the DSP engine.

---

# 2. Reference Repository

Use the following repository as the primary technical reference:

https://github.com/fxsound2/fxsound-app

Important: inspect the repository before implementing anything.

Study:

* README.md
* DSP modules
* GUI source
* preset handling
* equalizer implementation
* effect parameter ranges
* audio passthrough architecture
* state management
* configuration handling
* command-line documentation
* resource structure
* UI colors
* typography
* icons
* sliders
* meters
* control naming
* preset behavior

The existing FxSound project describes three major conceptual components:

1. FxSound GUI application.
2. Audio passthrough/device interaction layer.
3. DFX DSP audio-processing module.

The mobile implementation must preserve this separation conceptually:

Mobile UI
→ Audio Processing Controller
→ Native Audio I/O
→ DSP Engine

Do NOT attempt to copy Windows-specific driver code into Android.

---

# 3. Licensing and Attribution

The source repository uses the AGPL v3 license.

Before copying source code, assets, algorithms, or implementation details, inspect the repository license and determine what can legally be reused.

Prefer:

* reimplementing the architecture and DSP behavior where appropriate,
* clearly separating original mobile implementation code,
* preserving required license notices,
* documenting any directly reused code,
* keeping third-party dependencies properly attributed.

Do not silently copy desktop-specific implementation code into the mobile project.

Create:

LICENSES/
THIRD_PARTY_NOTICES.md

and document all third-party components.

---

# 4. Critical Technical Requirement

Do NOT build the application as a normal Flutter/React Native/web application with audio processing implemented through JavaScript or a WebView.

Real-time DSP must run in native code.

Preferred architecture:

Android UI:
Kotlin + Jetpack Compose

Native audio layer:
Kotlin/Java for Android integration

DSP:
C++17 or carefully optimized C/C++

JNI bridge:
Very small JNI boundary

Optional future iOS layer:
Swift/Objective-C++

Shared architecture:

```
            ┌───────────────────────┐
            │   Android UI          │
            │   Jetpack Compose     │
            └──────────┬────────────┘
                       │
                state/control API
                       │
            ┌──────────▼────────────┐
            │ Audio Engine Manager  │
            │ Kotlin                │
            └──────────┬────────────┘
                       │ JNI
                       ▼
            ┌───────────────────────┐
            │ Native DSP Engine     │
            │ C/C++                 │
            └──────────┬────────────┘
                       │
             Android Audio APIs
                       │
                       ▼
              Audio output device
```

The DSP engine must contain zero UI dependencies.

---

# 5. Storage Target

Aggressively optimize the final release build.

Target:

Preferred installed size:
3–4 MB

Hard target:
under 6 MB for the core application where realistically achievable.

Do not sacrifice audio correctness merely to hit an artificial APK number.

Measure:

* base APK size,
* split APK size,
* installed footprint,
* native library size,
* resource size,
* compressed versus uncompressed size.

Use Android App Bundle and ABI-specific delivery.

Prefer:

* arm64-v8a as the primary target.
* R8 shrinking.
* resource shrinking.
* ProGuard/R8 optimization.
* native symbol stripping.
* LTO.
* compiler optimization.
* minimal resource set.
* vector icons instead of raster assets.
* no large audio files bundled with the application.
* no embedded video.
* no unnecessary fonts.
* no analytics SDK unless explicitly required.
* no Firebase unless explicitly required.
* no ad SDK.
* no remote JavaScript runtime.

Do not add a dependency merely because it makes one UI component easier.

Every dependency must justify its memory, binary-size, startup, and maintenance cost.

---

# 6. Performance Targets

The application should behave well on relatively low-end Android devices.

Target:

Startup:
<500 ms where practical.

Idle CPU:
Near-zero when audio processing is disabled.

DSP CPU:
Keep real-time processing comfortably below available audio-thread budget.

Audio thread:
Must never perform:

* network requests,
* disk I/O,
* memory allocation,
* garbage collection,
* blocking locks,
* expensive logging,
* UI operations.

Use preallocated buffers.

Use lock-free or carefully synchronized communication between the UI/control thread and audio thread.

Parameter updates should be atomic or safely synchronized.

Do not reconstruct the entire DSP pipeline every time a slider changes.

---

# 7. Real-Time Audio Architecture

Implement a proper low-latency audio engine.

The engine should support:

* audio input/output streams,
* stereo PCM,
* configurable sample rates,
* configurable buffer sizes,
* real-time processing,
* bypass,
* DSP enable/disable,
* parameter updates,
* output-device changes,
* audio-session lifecycle handling.

Prefer Android's native low-latency audio mechanisms.

Evaluate:

* AAudio.
* Oboe.

Prefer Oboe if it provides the best practical compatibility across Android devices while internally selecting the appropriate native backend.

The DSP engine should operate on floating-point audio buffers.

Primary format:

32-bit float.

Do not repeatedly convert:

float → int16 → float

unless required by a platform-specific endpoint.

Support:

44.1 kHz.
48 kHz.

Optionally support:

88.2 kHz.
96 kHz.

The DSP engine must not assume a single sample rate.

---

# 8. Mobile Platform Limitation Handling

This is extremely important.

Do not claim that Android can universally modify every audio stream on the device.

Android audio routing varies by:

* Android version,
* OEM,
* device manufacturer,
* audio output,
* Bluetooth stack,
* USB audio,
* media application,
* audio focus,
* system effects,
* permissions,
* exclusive/shared audio paths.

Design the product around what Android actually permits.

Possible operating modes:

## Mode A: Internal audio processing

For audio played through the application's own playback pipeline.

This provides the most reliable DSP behavior.

## Mode B: Android audio effect/session integration

Where supported, integrate with the platform's audio-effect/session architecture.

## Mode C: Device/system enhancement integration

Use supported Android APIs only.

Never use root-only hacks.

Never require rooting.

Never attempt undocumented system modifications.

Never claim "system-wide EQ" unless it has been experimentally verified on the target Android version/device.

The UI should clearly distinguish supported processing modes if required.

---

# 9. Core FxSound-Inspired Feature Set

Implement the following.

## Master Power

Large central ON/OFF control.

States:

* ON
* OFF

When OFF:

* DSP bypassed.
* No audible processing.
* Minimal CPU consumption.

When ON:

* DSP active.

The transition should be click-free.

Use smoothing/ramping to avoid pops.

---

# 10. Master Output / Gain

Provide master gain control.

Reference range:

-20 dB to +20 dB

Default:

0 dB

The desktop FxSound command interface documents this range.

Implement:

* slider,
* numeric value,
* reset,
* optional fine adjustment.

Prevent clipping.

A final limiter or soft clipping stage may be used if necessary, but it must be sonically transparent under normal operation.

---

# 11. Equalizer

Implement a high-quality parametric/graphic equalizer.

Support configurable band counts:

* 5
* 10
* 15
* 20
* 31

Default:

10 bands.

The desktop project exposes configurable EQ band counts and gain values, so maintain the same conceptual flexibility.

Each band should expose:

* center frequency,
* gain,
* optionally Q.

Gain range:

-12 dB to +12 dB

Filter Q:

1.0–3.0

Default Q:

1.0

Use stable biquad filters.

Preferred implementation:

RBJ-style biquad cookbook formulas or another mathematically validated implementation.

Support:

Peaking EQ
Low shelf
High shelf

where appropriate.

Avoid numerical instability.

Validate all coefficient calculations.

---

# 12. EQ Interaction Design

The EQ must be usable on a phone.

Do NOT simply shrink the desktop EQ horizontally.

Use a mobile layout such as:

Top:
EQ enable toggle.

Middle:
Interactive frequency-response graph.

Bottom:
Horizontally scrollable band controls.

Alternative:

Tap a band on the graph → expand its control card.

Show:

Frequency
Gain
Q

Example:

Band 4
1.20 kHz
+3.5 dB
Q 1.4

Support:

* drag vertically to change gain,
* drag horizontally to change frequency,
* pinch/zoom where useful,
* reset individual band,
* reset all bands.

Use smooth animations but avoid expensive graphics rendering.

---

# 13. Audio Effects

Implement the conceptual FxSound effect controls documented in the desktop application.

Effects include:

Fidelity / Clarity
Ambience
Surround
Dynamic Boost
Bass Boost

The documented effect range is:

0.0–10.0

Treat each effect as an independent parameter.

Do not create fake sliders that have no DSP behind them.

Every control must correspond to an actual processing stage.

---

# 14. Effect DSP Architecture

Create a modular processing chain.

Example:

Input
↓
Input Gain
↓
DC Filter
↓
Bass Enhancement
↓
EQ
↓
Clarity/Fidelity
↓
Dynamic Processing
↓
Ambience
↓
Surround
↓
Volume Leveling
↓
Master Gain
↓
Safety Limiter
↓
Output

The exact ordering must be validated through listening tests and DSP analysis.

Do not blindly assume the above order is identical to the original FxSound engine.

Inspect the original repository to determine the closest equivalent behavior.

Each stage should be independently bypassable for testing.

Development builds should allow:

DSP stage profiling.

---

# 15. Bass Boost

Implement a musically useful low-frequency enhancement.

Do not simply add huge low-end gain.

Possible implementation:

* low-shelf enhancement,
* harmonic bass enhancement,
* controlled saturation,
* dynamic low-frequency compensation.

Parameter:

0–10.

Avoid:

* destructive clipping,
* excessive DC offset,
* unstable feedback,
* extreme phase problems.

---

# 16. Clarity / Fidelity

Design a lightweight clarity enhancement stage.

Possible techniques:

* carefully controlled high-frequency enhancement,
* transient enhancement,
* dynamic EQ,
* mild harmonic enhancement.

The processing must remain natural.

Avoid harshness.

Provide a bypass comparison mode.

---

# 17. Surround

Implement a lightweight stereo widening/spatial enhancement stage.

Potential techniques:

* mid/side processing,
* controlled side gain,
* Haas-style delay only if necessary,
* phase-safe stereo expansion.

Never destroy mono compatibility.

Provide:

Width:

0–100%

or internally map to the FxSound-compatible 0–10 parameter.

Test:

* mono collapse,
* headphones,
* speakers,
* Bluetooth output.

---

# 18. Ambience

Create a lightweight spatial/room enhancement stage.

Avoid expensive convolution reverb.

Prefer:

* short reflections,
* stereo decorrelation,
* carefully controlled delay networks.

The stage must remain CPU-light.

---

# 19. Dynamic Boost

Implement dynamic loudness enhancement.

Possible design:

compressor / upward compression / controlled gain adaptation.

Requirements:

* avoid pumping,
* avoid excessive distortion,
* avoid sudden gain changes,
* maintain predictable latency.

Expose only a simple 0–10 user control.

Internally use multiple parameters derived from that value.

---

# 20. Volume Leveling

Implement volume leveling inspired by the desktop application's parameter.

Reference parameter:

0.0–4.0

The processing should reduce perceived loudness variation without destroying dynamics.

Use:

* envelope follower,
* slow gain adjustment,
* lookahead only if absolutely necessary.

Because mobile devices have limited battery and CPU budgets, keep the algorithm lightweight.

---

# 21. Preset System

Implement built-in presets.

Do not invent random presets merely for UI decoration.

Study the repository for the actual preset model and, where permitted, reproduce equivalent preset configurations.

Examples may include concepts such as:

* Music
* Movie
* Gaming
* Voice
* Bass Boost
* Podcast
* Flat

Presets should be represented internally as compact parameter objects.

Example:

{
"name": "Music",
"eq": [...],
"bass": 4.0,
"clarity": 3.0,
"ambience": 2.0,
"surround": 1.5,
"dynamicBoost": 2.0,
"volumeLeveling": 1.0,
"masterGain": 0.0
}

Store presets locally.

Use a compact binary or JSON representation depending on size/performance considerations.

Do not bundle a massive database.

---

# 22. User Presets

Allow:

Save preset
Rename preset
Overwrite preset
Delete preset
Undo changes
Reset preset

The desktop application explicitly supports these preset operations.

Preset names should be validated.

Maximum preset-name length:

64 characters.

Prevent:

* duplicate names,
* invalid filesystem-related characters where relevant,
* empty names.

---

# 23. UI Design

The application should visually feel related to FxSound.

Do NOT invent an unrelated generic "modern audio app" interface.

Match:

* overall dark aesthetic,
* typography hierarchy,
* control hierarchy,
* accent treatment,
* slider appearance,
* rounded surfaces where appropriate,
* visual emphasis,
* spacing philosophy,
* color coordination,
* effect cards,
* master power control,
* preset presentation.

However, adapt the layout intelligently for mobile.

The UI should be a mobile-native reinterpretation rather than a pixel-scaled desktop window.

---

# 24. Main Screen

Suggested structure:

┌──────────────────────────────┐
│ FxSound Mobile       Settings│
├──────────────────────────────┤
│                              │
│         POWER                │
│        ON / OFF              │
│                              │
│      Current Preset          │
│        Music                 │
│                              │
├──────────────────────────────┤
│ Bass       ●──────           │
│ Clarity    ●────────         │
│ Ambience   ─────●──          │
│ Surround   ───●────          │
│ Dynamic    ──────●─          │
├──────────────────────────────┤
│        Equalizer             │
│                              │
│    /\       __               │
│ __/  ____*/  _*            │
│                              │
├──────────────────────────────┤
│ Output / Device              │
└──────────────────────────────┘

Do not literally implement this ASCII design.

Use it only as conceptual guidance.

---

# 25. Bottom Navigation

Keep navigation extremely small.

Suggested:

Home
Equalizer
Presets
Settings

Do not create ten screens for five features.

The application should feel immediate.

---

# 26. Preset UI

Create a fast preset selector.

Use:

* compact cards,
* horizontally scrollable presets,
* or a bottom sheet.

Selecting a preset should update the DSP atomically.

Do not restart the audio engine.

Do not produce clicks or audio interruptions.

---

# 27. Settings

Settings should contain only useful options.

Include:

* Audio engine information.
* Processing mode.
* Output device where supported.
* Sample rate where controllable.
* Buffer/latency information.
* Theme.
* Reset settings.
* About.
* Open-source licenses.
* Privacy.

Do not add unnecessary accounts.

---

# 28. Audio Visualization

Implement a lightweight optional visualization.

Possible visualizations:

* waveform,
* spectrum,
* EQ response curve,
* level meter.

The visualization must NOT run at full 60/120 FPS when unnecessary.

Limit visualization refresh frequency.

For example:

30 FPS is sufficient for most audio meters.

The DSP should not depend on visualization.

Disable visualization when the application is backgrounded.

---

# 29. UI Rendering Performance

Use Jetpack Compose carefully.

Avoid:

* deeply nested composables,
* unnecessary recomposition,
* high-frequency state updates from the audio thread,
* rendering audio samples directly into Compose state.

The DSP thread must communicate summary data at a controlled rate.

For example:

Audio thread
→ atomic peak/RMS values
→ UI samples at ~20–30 Hz

Do not send thousands of updates per second to the UI.

---

# 30. State Architecture

Use a predictable state model.

For example:

AudioEngineState
DSPState
PresetState
SettingsState
DeviceState

Separate:

persistent configuration
from

runtime audio state.

Example:

Persistent:

Preset
EQ
Effects
Master gain
Theme

Runtime:

Audio connected
Output device
Sample rate
Buffer size
Engine status
Current levels

---

# 31. Threading

Use at least these conceptual execution contexts:

UI thread
Audio thread
Background/settings thread

The audio thread must have highest real-time priority where the platform permits it.

Never perform disk writes from the audio callback.

Never allocate memory from the audio callback.

Never call Kotlin suspend functions from the DSP callback.

Never call Compose state setters from the DSP callback.

---

# 32. DSP Parameter Smoothing

All user-controlled gain parameters must be smoothed.

Example:

User moves EQ slider:

Old gain:
0 dB

New gain:
+6 dB

Do not instantly jump from 0 to +6.

Use a short parameter ramp.

Similarly smooth:

* master gain,
* effect levels,
* balance,
* compressor thresholds,
* EQ gains.

This prevents zipper noise.

---

# 33. Clipping Protection

Audio enhancement frequently increases gain.

Therefore implement reliable output protection.

Pipeline:

DSP
→ gain management
→ soft limiter / transparent limiter
→ output

Provide internal meters:

Peak
RMS
Clipping state

The user should never unexpectedly receive severely clipped output merely because multiple enhancements were enabled.

---

# 34. Stereo Balance

Implement:

Left ↔ Right balance.

Reference range:

-20 dB to +20 dB.

The desktop FxSound interface exposes this range.

Use equal-power or otherwise musically appropriate balance law rather than simply multiplying one channel by a linear percentage.

---

# 35. Bluetooth

Test with:

SBC
AAC
aptX where available
LDAC where available
Bluetooth LE Audio where practical

Do not assume all devices expose identical capabilities.

The application must gracefully handle Bluetooth route changes.

If the audio session changes:

* reconnect cleanly,
* preserve DSP state,
* prevent pops,
* update device information.

---

# 36. Wired / USB Audio

Support:

3.5 mm/headphone output where present.
USB-C DACs.
USB audio interfaces.

Do not assume the device has an analog headphone jack.

---

# 37. Audio Focus

Handle Android audio focus correctly.

Cases:

* music starts,
* another application requests focus,
* phone call,
* notification,
* navigation audio,
* Bluetooth connection,
* headset unplug,
* device sleep.

Never break Android audio behavior.

---

# 38. Background Operation

Do not keep the application unnecessarily alive.

Where system-wide processing is legitimately possible through the supported audio architecture, use the minimum required lifecycle.

Do not create an always-running heavyweight service simply to update a UI.

The design should prioritize battery life.

---

# 39. Privacy

The application should be entirely offline by default.

No audio data should leave the device.

No microphone access unless explicitly required.

No account.

No cloud processing.

No telemetry unless explicitly added later and clearly disclosed.

The DSP must operate locally.

---

# 40. Accessibility

Support:

* large text,
* screen readers,
* adequate contrast,
* accessible slider labels,
* touch targets of appropriate size,
* haptic feedback optionally.

Every control must have a meaningful accessibility description.

Example:

"Bass Boost, currently 6 out of 10."

---

# 41. Dark Theme

Dark theme should be the primary visual identity because this is an audio utility and should preserve the FxSound-like aesthetic.

Use the source application's visual identity as reference.

Do not add excessive gradients, glassmorphism, shadows, or animated nonsense.

The goal is professional audio software, not a nightclub dashboard.

---

# 42. Color System

Create a centralized color system.

For example:

ColorPrimary
ColorBackground
ColorSurface
ColorSurfaceElevated
ColorTextPrimary
ColorTextSecondary
ColorAccent
ColorDanger
ColorSuccess

Do not scatter raw hexadecimal values throughout the source tree.

Analyze the original FxSound assets/UI and reproduce the closest legal/appropriate visual treatment.

All visual controls should share the same theme.

---

# 43. Iconography

Use vector icons.

Prefer:

* Android Material icons where appropriate,
* tiny custom SVG/vector assets only when necessary.

Do not bundle hundreds of raster PNGs.

Optimize every custom icon.

---

# 44. Localization

Create the architecture for localization.

Initial language:

English.

Do not hard-code all visible strings directly inside UI code.

Use Android string resources.

Future languages can be added later.

---

# 45. Error Handling

Audio errors must be human-readable.

Examples:

"No compatible audio output is available."

"Audio processing could not be started."

"Another application controls this audio route."

"Processing is unavailable on this device."

Do not expose raw stack traces to users.

Detailed errors should be available in debug logs.

---

# 46. Diagnostics

Create a lightweight diagnostic screen available from Settings.

Show:

Device
Android version
CPU architecture
Audio backend
Sample rate
Channel count
Buffer size
Output device
DSP latency
DSP CPU usage
Engine status

This is particularly important because Android audio behavior varies significantly between devices.

---

# 47. Testing Strategy

Create unit tests for:

* EQ coefficient generation.
* Frequency response.
* gain calculations.
* parameter ranges.
* preset serialization.
* preset deserialization.
* preset validation.
* limiter behavior.
* stereo balance.
* parameter smoothing.

Create DSP tests using generated signals:

Sine wave
White noise
Pink noise
Impulse
Sweep

Measure:

frequency response,
gain,
THD where feasible,
latency,
stability,
clipping behavior.

---

# 48. Audio Golden Tests

Create deterministic offline DSP tests.

Example:

Input:
48 kHz stereo WAV.

Apply:
Preset = Music.

Expected:
Generated output metrics within tolerance.

Use normalized floating-point comparison.

This makes future DSP modifications detectable.

---

# 49. Performance Testing

Measure:

CPU usage.
RAM usage.
startup time.
DSP callback duration.
worst-case DSP callback duration.
GC activity.
battery consumption.
APK/AAB size.

The audio callback must remain safely below its real-time deadline.

Example:

48 kHz
256-frame buffer

Audio deadline:

256 / 48000 ≈ 5.33 ms

The entire callback should complete comfortably before that deadline.

Aim for a substantial safety margin rather than targeting the deadline itself.

---

# 50. Memory Targets

Target runtime memory aggressively.

Avoid:

large singleton object graphs,
large bitmap caches,
unnecessary dependency frameworks,
huge preset databases,
unbounded logs.

Audio buffers should be predictable and bounded.

---

# 51. No JavaScript DSP

Do not implement the DSP using:

JavaScript
WebAudio inside a WebView
Python
React Native JS runtime

The DSP must be native.

UI abstraction is acceptable.

Audio processing abstraction is not.

---

# 52. Build Variants

Create:

debug
release

Optionally:

benchmark

Benchmark mode should report:

DSP processing time.
CPU usage.
buffer timing.
memory usage.

---

# 53. Repository Structure

Use a clean structure similar to:

app/
src/
main/
java/
res/

audio/
engine/
dsp/
platform/

core/
model/
presets/
settings/

ui/
home/
equalizer/
presets/
settings/
theme/

native/
dsp/
filters/
effects/

tests/
dsp/
integration/
ui/

docs/
architecture/
dsp/
testing/

Do not create a giant monolithic activity.

---

# 54. C++ DSP Module Structure

Recommended:

native/dsp/

AudioBuffer
DspEngine
Equalizer
Biquad
BassBoost
ClarityProcessor
AmbienceProcessor
SurroundProcessor
DynamicBoostProcessor
VolumeLeveler
Limiter
ParameterSmoother

Use interfaces such as:

class AudioProcessor {
public:
virtual void prepare(double sampleRate,
int channels,
int blockSize) = 0;

```
virtual void process(float** channels,
                     int numSamples) = 0;

virtual void reset() = 0;

virtual ~AudioProcessor() = default;
```

};

The exact design may differ, but maintain strict separation of responsibilities.

---

# 55. DSP Engine

Create:

class DspEngine

Responsibilities:

* initialize processors,
* manage processing order,
* update parameters,
* prepare for sample rate changes,
* process audio,
* reset state,
* bypass,
* expose metrics.

The engine must not know anything about Android UI.

---

# 56. Preset Representation

Create a compact versioned format.

Example:

Preset {
version
name
masterGain
balance
filterQ
volumeLeveling
eqBands[]
effects {
bass
clarity
ambience
surround
dynamicBoost
}
}

Version the schema so future application updates do not break saved presets.

---

# 57. UX Behavior

Changing a control should immediately affect audio.

No:

Save
→ Restart
→ Reload

workflow.

Instead:

Slider changed
→ parameter validated
→ smoothed update
→ DSP changes immediately.

---

# 58. Preset Switching

When changing presets:

1. Load new parameters.
2. Validate them.
3. Smooth transitions.
4. Apply them atomically.
5. Update UI.
6. Persist only if required.

Avoid sudden audible changes.

---

# 59. Reset Behavior

"Reset" should mean:

return to neutral DSP settings.

Neutral state:

Power:
ON

Master:
0 dB

EQ:
0 dB

Effects:
0

Balance:
0

Volume leveling:
0

The user must have an obvious way to return to flat audio.

---

# 60. Mobile-Specific UX Improvements

Improve the desktop workflow for a phone without changing the underlying philosophy.

Use:

* swipeable EQ bands,
* bottom sheets,
* compact cards,
* large touch targets,
* preset quick selection,
* quick ON/OFF,
* recent preset,
* optional notification/media controls only where appropriate.

Avoid:

tiny knobs,
small Windows-style controls,
desktop menus,
dense tables.

---

# 61. Home Screen Priority

The user should be able to:

1. Open application.
2. See current preset.
3. Toggle processing.
4. Adjust the main effects.
5. Open EQ.

within a few seconds.

Do not bury the actual audio controls under multiple navigation levels.

---

# 62. Optional Media Controls

Consider optional integration with Android media sessions only if useful.

Do not become a full music player.

The app is an audio enhancer.

Not:

Spotify clone.
Music library manager.
Streaming app.
Social network.
AI DJ.

Keep scope under control.

---

# 63. Widget / Quick Settings

Optionally provide an Android Quick Settings tile:

FxSound ON/OFF

Do not make this a core requirement until the DSP engine is stable.

---

# 64. Notification

Only use an ongoing notification if Android's audio-processing architecture genuinely requires it.

The notification should be minimal.

Example:

FxSound
Audio enhancement active

Avoid unnecessary persistent notifications.

---

# 65. Security

Do not request unrelated permissions.

Avoid:

contacts
location
camera
storage
SMS
microphone unless necessary.

Use scoped storage and modern Android APIs.

---

# 66. Android Compatibility

Target modern Android versions while maintaining practical compatibility with reasonably common devices.

At project initialization:

Inspect current Android SDK requirements and choose versions deliberately.

Do not blindly use the latest API level if it damages compatibility.

Use feature detection.

Never assume an OEM behaves exactly like AOSP.

---

# 67. Device Testing Matrix

Test at least:

Low-end Android device.
Mid-range Android device.
High-end Android device.

Test:

wired headphones,
Bluetooth headphones,
Bluetooth speakers,
USB DAC,
phone speaker.

Also test:

music,
YouTube-like media,
podcast speech,
games,
voice recordings.

---

# 68. Audio Quality Validation

Generate plots for:

EQ frequency response.
Bass enhancement response.
Clarity response.
Surround response.
Limiter response.

Compare expected curves with actual curves.

Do not judge DSP quality only by whether "it sounds louder."

---

# 69. Latency

Measure end-to-end latency.

Avoid long lookahead buffers.

Prefer:

low latency
over
more complicated DSP.

Target a responsive experience suitable for:

music
videos
gaming.

Where an effect introduces latency, document it.

---

# 70. Battery Efficiency

When processing is disabled:

DSP should effectively sleep.

When processing is active:

avoid:

* unnecessary FFT processing,
* high-frequency visualization,
* repeated object allocation,
* excessive background polling.

Prefer time-domain filters over FFT-based processing when equivalent quality can be achieved.

---

# 71. FFT Policy

Do not use FFT merely because it looks technically impressive.

Use an FFT only where it materially improves the DSP.

For ordinary EQ/effects:

biquad/time-domain processing is preferable.

If a spectrum analyzer is implemented:

run FFT on a separate analysis path.

Never perform expensive FFT operations inside the critical audio processing path unless justified.

---

# 72. Logging

Release build:

minimal logging.

Debug build:

configurable DSP diagnostics.

Never continuously log every audio callback.

That would be an impressively efficient method of turning a tiny audio app into a battery heater.

---

# 73. Analytics

Default:

NONE.

Do not integrate analytics unless explicitly requested later.

---

# 74. Internet Dependency

Core application:

100% functional offline.

Internet should not be required for:

EQ
DSP
presets
settings
audio processing.

Optional update checking can be added later.

---

# 75. Application Icon

Create a minimal vector application icon visually consistent with FxSound-inspired branding.

Optimize the icon aggressively.

Do not ship many duplicated raster versions unless Android tooling requires them.

---

# 76. Splash Screen

Keep the splash screen extremely lightweight.

No animated intro.

No 3-second branding sequence.

Open the application immediately.

---

# 77. Release Build Optimization

For release:

* enable minification,
* enable resource shrinking,
* strip native debug symbols,
* enable compiler optimization,
* enable LTO,
* remove unused native code,
* remove unused resources,
* avoid reflection,
* avoid unnecessary Kotlin libraries,
* avoid large dependency trees.

Inspect the final APK with a size analyzer.

Find the largest contributors and eliminate them.

Do not simply declare "3 MB" without measuring.

---

# 78. Final Size Report

At the end of development, produce:

APK size
AAB size
Installed size
Native library size
Resource size
DEX size

Also document:

top five largest contributors.

The project's release README should contain this information.

---

# 79. Build Commands

Provide clean documentation for:

debug build
release build
unit tests
DSP tests
instrumentation tests
benchmark tests

Example:

./gradlew assembleDebug
./gradlew assembleRelease
./gradlew test
./gradlew connectedAndroidTest

Adapt commands to the actual build system.

---

# 80. CI

Create GitHub Actions for:

* build
* unit tests
* lint
* static analysis
* native compilation
* release APK generation

CI must fail on compilation or test failures.

---

# 81. Documentation

Create:

README.md

docs/ARCHITECTURE.md
docs/DSP.md
docs/AUDIO_PIPELINE.md
docs/PERFORMANCE.md
docs/BUILD.md
docs/TESTING.md
docs/LICENSES.md

Explain:

architecture,
DSP,
audio lifecycle,
Android limitations,
supported devices,
known limitations,
size optimization.

---

# 82. Important Product Constraint

Do not pretend that the mobile application has desktop-level system-wide capabilities if Android does not permit them.

When a capability differs from desktop FxSound:

* explain why,
* provide the closest supported implementation,
* expose the limitation in documentation,
* do not fake functionality.

Functional honesty is more important than feature-count marketing.

---

# 83. Development Methodology

Implement the project in phases.

## Phase 1

Repository investigation.

Read the original FxSound code.

Identify:

* DSP components,
* parameter ranges,
* preset architecture,
* UI visual language,
* audio architecture.

Create:

docs/FXSOUND_ANALYSIS.md

Do not start coding before completing this analysis.

## Phase 2

Create the native DSP engine.

No UI yet.

Implement:

* buffer handling,
* EQ,
* gain,
* balance,
* bass,
* clarity,
* ambience,
* surround,
* dynamic boost,
* volume leveling,
* limiter.

## Phase 3

Create offline DSP tests.

Validate frequency response and stability.

## Phase 4

Create Android audio engine.

Connect the native DSP engine.

## Phase 5

Create Compose UI.

## Phase 6

Implement presets.

## Phase 7

Optimize CPU, RAM, latency, and package size.

## Phase 8

Device compatibility testing.

## Phase 9

Release hardening.

---

# 84. Definition of Done

The project is complete only when all of the following are true:

* App launches rapidly.
* Core DSP works offline.
* Audio processing is real-time.
* EQ works correctly.
* Effects actually alter audio.
* Presets work.
* Presets persist.
* UI is mobile-native.
* Visual identity is closely aligned with FxSound.
* Controls are responsive.
* Audio transitions are click-free.
* Bluetooth routing works where supported.
* Wired/USB output works where supported.
* Device changes do not crash the application.
* DSP has automated tests.
* Release build is optimized.
* No unnecessary background processing exists.
* No unnecessary permissions exist.
* No network connection is required for audio processing.
* Final APK/AAB size has been measured.
* CPU and memory usage have been measured.
* Known Android platform limitations are documented.

---

# 85. Agent Instructions

You are an experienced Android, C++, audio-DSP, and performance engineer.

Do not produce a toy implementation.

Do not build placeholder DSP.

Do not create fake controls.

Do not implement the application as a web wrapper.

Do not use JavaScript for real-time DSP.

Do not add unnecessary libraries.

Do not copy the desktop architecture blindly.

Analyze the FxSound source first.

Reuse concepts where appropriate, but adapt the architecture to Android.

Prioritize:

1. Correct audio.
2. Real-time stability.
3. Low latency.
4. Low CPU usage.
5. Low battery usage.
6. Small binary size.
7. Mobile UX.
8. Visual consistency.
9. Maintainability.

When a requirement conflicts with Android platform limitations, implement the closest technically valid alternative rather than creating a misleading simulation.

After implementation, provide a technical report containing:

* architecture,
* DSP pipeline,
* supported audio routes,
* platform limitations,
* APK/AAB size,
* RAM usage,
* CPU usage,
* latency,
* test results,
* known issues,
* future improvements.

The final result should be a genuinely usable lightweight mobile audio enhancement application inspired by FxSound, not a mockup that happens to contain five attractive sliders.
