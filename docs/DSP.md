# Digital Signal Processing (DSP) Engine & Mathematics

The live engine is **Rust** (`native/fxengine`). Algorithms below match that crate. C++ only wraps the C ABI and runs Oboe.

## 1. Parameter Smoothing
To prevent zipper noise during user slider adjustments, all gain and level parameters are filtered using an exponential one-pole smoothing filter:
$$y[n] = y[n-1] + \alpha \cdot (x[n] - y[n-1])$$
where the smoothing coefficient $\alpha$ is derived from target time constant $\tau = 15\text{ ms}$:
$$\alpha = 1 - e^{-\frac{1}{\tau \cdot f_s}}$$

---

## 2. Biquad Equalizer Filters
Each equalizer band implements the Robert Bristow-Johnson (RBJ) audio filter equations in **Direct Form II Transposed**:
$$y[n] = b_0 x[n] + d_1[n-1]$$
$$d_1[n] = b_1 x[n] - a_1 y[n] + d_2[n-1]$$
$$d_2[n] = b_2 x[n] - a_2 y[n]$$

For a peaking filter with center frequency $f_0$, quality factor $Q$, and gain $A = 10^{\text{gainDb}/40}$:
$$\omega_0 = 2\pi \frac{f_0}{f_s}, \quad \alpha = \frac{\sin(\omega_0)}{2Q}$$
$$b_0 = 1 + \alpha A, \quad b_1 = -2\cos(\omega_0), \quad b_2 = 1 - \alpha A$$
$$a_0 = 1 + \frac{\alpha}{A}, \quad a_1 = -2\cos(\omega_0), \quad a_2 = 1 - \frac{\alpha}{A}$$

---

## 3. Five Core Enhancement Effects

### 3.1 Bass Boost
Combines a low-shelf boost filter ($60\text{ Hz}–120\text{ Hz}$) with non-linear harmonic excitation.
To make sub-bass audible on miniature mobile phone transducers and earbuds, low frequencies ($<140\text{ Hz}$) are extracted and passed through soft polynomial saturation:
$$f(x) = \tanh(2.2 x)$$
This synthesizes $2^{\text{nd}}$ and $3^{\text{rd}}$ harmonics that psychoacoustically recreate the fundamental bass note in the listener's brain without causing cone over-excursion. A $10\text{ Hz}$ high-pass DC blocker ensures zero baseline shift.

### 3.2 Clarity / Fidelity
Targets presence frequencies ($4\text{ kHz}–12\text{ kHz}$) using a high-shelf boost combined with asymmetric harmonic generation:
$$f(x) = \frac{x}{1 + 1.5 |x|}$$
This adds high-end sparkle, open airy sheen, and vocal presence without harsh sibilance.

### 3.3 Ambience
Implements a multi-tap delay network using prime millisecond spacing ($17.3\text{ ms}, 29.1\text{ ms}, 37.7\text{ ms}, 47.9\text{ ms}$) with $5\text{ kHz}$ low-pass damping and cross-channel feedback. This creates a realistic acoustic room reflections envelope with ultra-low CPU consumption.

### 3.4 Surround Spatializer & Mono Compatibility Proof
Performs Mid/Side spatialization:
$$M[n] = \frac{L[n] + R[n]}{2}, \quad S[n] = \frac{L[n] - R[n]}{2}$$
The side channel is processed through high-pass filtering (keeping sub-bass centered in mono) and psychoacoustic delay:
$$S_{\text{enhanced}}[n] = S[n] \cdot (1 + \text{widthBoost}) + S_{\text{delayed}}[n] \cdot \text{decorr}$$
Reconstructing stereo:
$$L_{\text{out}}[n] = M[n] + S_{\text{enhanced}}[n], \quad R_{\text{out}}[n] = M[n] - S_{\text{enhanced}}[n]$$

**Mono Downmix Proof**:
$$\text{Mono}[n] = \frac{L_{\text{out}}[n] + R_{\text{out}}[n]}{2} = \frac{(M + S_{\text{enh}}) + (M - S_{\text{enh}})}{2} = M[n] = \frac{L[n] + R[n]}{2}$$
All side alterations mathematically cancel out upon mono summing, guaranteeing **100% mono compatibility**!

### 3.5 Dynamic Boost
Uses upward compression to boost quiet musical details:
$$\text{Gain}(E) = 1 + \text{boostRatio} \cdot \left(\frac{1}{1 + 3 E} - 0.25 E\right)$$
where $E$ is tracked using dual-ballistics peak/RMS detection with $4\text{ ms}$ attack and $50\text{ ms}$ release.

---

## 4. Safety Peak Limiter
Guarantees audio never clips above $0\text{ dBFS}$ ($1.0\text{ float}$):
- Threshold: $-0.3\text{ dBFS}$ ($0.965$)
- Attack time: $0.2\text{ ms}$
- Release time: $45\text{ ms}$
- Transfer curve: envelope gain reduction plus `tanh` safety clip
- **No lookahead delay line** (latency stays at the Oboe callback size)

The first pipeline stages are a dedicated DC blocker and unity input gain, then bass → EQ → clarity → dynamics → ambience → surround → leveler → master → limiter.
