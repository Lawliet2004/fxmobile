#pragma once

#include "fxengine.h"
#include <cstddef>
#include <mutex>

namespace fxmobile {

// All public methods take mutex so the audio callback (process*) and UI/JNI
// setters cannot alias the Rust Engine as concurrent &mut references.
class DspEngine {
public:
    DspEngine(float sampleRate = 48000.0f, size_t maxBlockSize = 1024)
        : handle(fx_engine_create(sampleRate, static_cast<uint32_t>(maxBlockSize))) {}

    ~DspEngine() {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_destroy(handle);
        handle = nullptr;
    }

    DspEngine(const DspEngine&) = delete;
    DspEngine& operator=(const DspEngine&) = delete;

    void prepare(float sampleRate, size_t maxBlockSize) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_prepare(handle, sampleRate, static_cast<uint32_t>(maxBlockSize));
    }

    void reset() {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_reset(handle);
    }

    void setPowerEnabled(bool enabled) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_power(handle, enabled);
    }
    bool isPowerEnabled() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_power(handle);
    }

    void setBass(float amount) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_bass(handle, amount);
    }
    void setClarity(float amount) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_clarity(handle, amount);
    }
    void setAmbience(float amount) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_ambience(handle, amount);
    }
    void setSurround(float amount) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_surround(handle, amount);
    }
    void setDynamicBoost(float amount) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_dynamic(handle, amount);
    }

    float getBass() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_bass(handle);
    }
    float getClarity() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_clarity(handle);
    }
    float getAmbience() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_ambience(handle);
    }
    float getSurround() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_surround(handle);
    }
    float getDynamicBoost() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_dynamic(handle);
    }

    void setEqEnabled(bool enabled) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_eq_enabled(handle, enabled);
    }
    bool isEqEnabled() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_eq_enabled(handle);
    }
    void setEqBandCount(size_t bandCount) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_eq_band_count(handle, static_cast<uint32_t>(bandCount));
    }
    size_t getEqBandCount() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_eq_band_count(handle);
    }
    void setEqBandGain(size_t bandIndex, float gainDb) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_eq_gain(handle, static_cast<uint32_t>(bandIndex), gainDb);
    }
    float getEqBandGain(size_t bandIndex) const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_eq_gain(handle, static_cast<uint32_t>(bandIndex));
    }
    void setEqBandFrequency(size_t bandIndex, float freq) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_eq_freq(handle, static_cast<uint32_t>(bandIndex), freq);
    }
    float getEqBandFrequency(size_t bandIndex) const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_eq_freq(handle, static_cast<uint32_t>(bandIndex));
    }
    void setEqBandQ(size_t bandIndex, float q) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_eq_q(handle, static_cast<uint32_t>(bandIndex), q);
    }
    float getEqBandQ(size_t bandIndex) const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_eq_q(handle, static_cast<uint32_t>(bandIndex));
    }
    void resetAllEqBands() {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_reset_eq(handle);
    }

    void setVolumeLeveling(float amount) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_leveling(handle, amount);
    }
    float getVolumeLeveling() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_leveling(handle);
    }
    void setMasterGainDb(float gainDb) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_master_db(handle, gainDb);
    }
    float getMasterGainDb() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_master_db(handle);
    }
    void setBalance(float balance) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_balance(handle, balance);
    }
    float getBalance() const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_get_balance(handle);
    }
    void setInputGainDb(float gainDb) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_set_input_db(handle, gainDb);
    }

    void processInterleaved(float* audioData, size_t numFrames) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_process_interleaved(handle, audioData, static_cast<uint32_t>(numFrames));
    }

    void process(const float* input, float* output, size_t numFrames) {
        std::lock_guard<std::mutex> lock(mutex);
        fx_engine_process(handle, input, output, static_cast<uint32_t>(numFrames));
    }

    float getCompositeMagnitudeResponseDb(float freq) const {
        std::lock_guard<std::mutex> lock(mutex);
        return fx_engine_magnitude_db(handle, freq);
    }

    void getMeterData(float& peakL, float& peakR, float& rmsL, float& rmsR, bool& isClipping) {
        std::lock_guard<std::mutex> lock(mutex);
        float m[5] = {0, 0, 0, 0, 0};
        fx_engine_get_meter(handle, m);
        peakL = m[0];
        peakR = m[1];
        rmsL = m[2];
        rmsR = m[3];
        isClipping = m[4] > 0.5f;
    }

    static const char* backendName() { return fx_engine_backend_name(); }

private:
    FxEngine* handle;
    mutable std::mutex mutex;
};

} // namespace fxmobile
