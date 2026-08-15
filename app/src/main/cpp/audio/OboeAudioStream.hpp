#pragma once

#include "../dsp/DspEngine.hpp"
#include "PcmRingBuffer.hpp"
#include <oboe/Oboe.h>
#include <memory>
#include <atomic>
#include <mutex>
#include <thread>
#include <vector>

namespace fxmobile {

enum class AudioSourceType {
    ToneSine = 0,
    ToneSweep = 1,
    TonePinkNoise = 2,
    ToneSilence = 3,
    CustomPcm = 4
};

class OboeAudioStream : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
public:
    OboeAudioStream(std::shared_ptr<DspEngine> dspEngine);
    virtual ~OboeAudioStream();

    bool start();
    bool stop();
    bool pause();
    void restart();

    bool isPlaying() const;

    void setSourceType(AudioSourceType type);
    void setToneFrequency(float freq);
    void setLowPowerIo(bool enabled);

    size_t writePcmFrames(const float* interleavedStereo, size_t numFrames);
    size_t pcmAvailableWrite() const;
    size_t pcmAvailableRead() const;
    void resetPcmRing();

    // Stream telemetry
    int32_t getSampleRate() const;
    int32_t getChannelCount() const;
    int32_t getBufferSize() const;
    int32_t getFramesPerBurst() const;
    double getCpuLoad() const;
    int64_t getFramesWritten() const;
    bool isAAudio() const;

    // oboe::AudioStreamDataCallback
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* audioStream, void* audioData, int32_t numFrames) override;

    // oboe::AudioStreamErrorCallback
    void onErrorAfterClose(oboe::AudioStream* audioStream, oboe::Result error) override;
    void onErrorBeforeClose(oboe::AudioStream* audioStream, oboe::Result error) override;

private:
    void setupToneGenerators(float sampleRate);
    void generateAudioSource(float* buffer, int32_t numFrames);

    std::shared_ptr<DspEngine> m_dspEngine;
    std::shared_ptr<oboe::AudioStream> m_stream;

    std::atomic<bool> m_isPlaying{false};
    std::atomic<AudioSourceType> m_sourceType{AudioSourceType::ToneSine};
    std::atomic<float> m_toneFrequency{440.0f};

    // Synthesizer state
    float m_sinePhase{0.0f};
    float m_sweepPhase{0.0f};
    float m_sweepCurrentFreq{20.0f};
    uint32_t m_noiseSeed{22222};
    float m_pinkB0{0.0f}, m_pinkB1{0.0f}, m_pinkB2{0.0f}, m_pinkB3{0.0f}, m_pinkB4{0.0f}, m_pinkB5{0.0f}, m_pinkB6{0.0f};

    std::atomic<double> m_cpuLoad{0.0};
    std::atomic<bool> m_lowPowerIo{false};
    uint32_t m_callbackCount{0};

    PcmRingBuffer m_pcmRing;

    std::mutex m_ioMutex;
    std::atomic<bool> m_destroyed{false};
    std::atomic<bool> m_restartPending{false};
    std::thread m_restartThread;

    void scheduleRestart();
    bool startUnlocked();
    bool stopUnlocked();
};

} // namespace fxmobile
