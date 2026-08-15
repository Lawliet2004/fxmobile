# Testing & Validation Guide

## 1. Automated Rust DSP Test Suite
The live DSP engine is Rust (`native/fxengine`). Host tests:

```bash
cargo test --manifest-path native/fxengine/Cargo.toml
```

### Verified Test Cases:
1. **Biquad Filter Frequency Response**: Confirms RBJ peaking and shelving filters generate mathematically exact magnitude response at target frequency within $\pm 0.05\text{ dB}$.
2. **Mono Compatibility of Surround Widener**: Validates that summing the stereo enhanced output to mono ($(L+R)/2$) yields $\Delta < 10^{-6}$, proving zero destructive phase cancellation on mono phone speakers.
3. **Safety Limiter Anti-Clipping**: Feeds an overloaded $+12\text{ dB}$ signal into the master pipeline and verifies peak output never exceeds $1.0\text{ float}$ ($0\text{ dBFS}$).
4. **Parameter Smoothing (Zipper Noise)**: Verifies step transitions in sliders are smoothly continuous ($\Delta \le 0.0014$ per sample).
5. **Real-Time DSP Callback Duration**: Measures average callback execution duration over 1000 iterations to verify it finishes well below the $5.33\text{ ms}$ deadline.
6. **DC blocker**: Constant offset is removed by the dedicated first stage.
7. **Master gain / balance**: +6 dB sine peak and right-biased balance.
8. **Music preset golden metrics**: Peak stays ≤ 1.0; output energy is non-zero.

Kotlin: `PresetSerializationTest`, `DspParameterValidationTest`, `PresetNameTest`, `PresetRepositoryTest`, `SessionEffectMappingTest`, `EqBandTablesTest`.

CI also runs `./gradlew lint`.

---

## 2. Android Kotlin Unit Tests
Located in `app/src/test/java/com/fxsound/mobile/`:
- `PresetSerializationTest`: Verifies all built-in and user presets serialize to JSON and deserialize back with exact parameter fidelity.
- `DspParameterValidationTest`: Verifies parameter bounds and flat reset state.

To run Android tests:
```bash
./gradlew test
```
