#include "OboeAudioStream.hpp"
#include <chrono>
#include <cmath>
#include <algorithm>
#include <thread>

namespace fxmobile {

OboeAudioStream::OboeAudioStream(std::shared_ptr<DspEngine> dspEngine)
    : m_dspEngine(dspEngine) {
}

OboeAudioStream::~OboeAudioStream() {
    m_destroyed.store(true);
    if (m_restartThread.joinable()) {
        m_restartThread.join();
    }
    stop();
}

bool OboeAudioStream::start() {
    std::lock_guard<std::mutex> lock(m_ioMutex);
    return startUnlocked();
}

bool OboeAudioStream::startUnlocked() {
    if (m_isPlaying.load()) return true;

    const auto performanceMode = m_lowPowerIo.load()
        ? oboe::PerformanceMode::None
        : oboe::PerformanceMode::LowLatency;

    oboe::AudioStreamBuilder builder;
    builder.setDirection(oboe::Direction::Output)
           ->setPerformanceMode(performanceMode)
           ->setSharingMode(oboe::SharingMode::Shared)
           ->setFormat(oboe::AudioFormat::Float)
           ->setChannelCount(oboe::ChannelCount::Stereo)
           ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
           ->setDataCallback(this)
           ->setErrorCallback(this);

    oboe::Result result = builder.openStream(m_stream);
    if (result != oboe::Result::OK) {
        return false;
    }

    int32_t actualSampleRate = m_stream->getSampleRate();
    if (m_dspEngine) {
        m_dspEngine->prepare(static_cast<float>(actualSampleRate), 1024);
    }
    setupToneGenerators(static_cast<float>(actualSampleRate));

    result = m_stream->requestStart();
    if (result != oboe::Result::OK) {
        m_stream->close();
        m_stream.reset();
        return false;
    }

    m_isPlaying.store(true);
    return true;
}

bool OboeAudioStream::stop() {
    std::lock_guard<std::mutex> lock(m_ioMutex);
    return stopUnlocked();
}

bool OboeAudioStream::stopUnlocked() {
    m_isPlaying.store(false);
    if (m_stream) {
        m_stream->stop();
        m_stream->close();
        m_stream.reset();
    }
    return true;
}

bool OboeAudioStream::pause() {
    std::lock_guard<std::mutex> lock(m_ioMutex);
    if (m_stream && m_isPlaying.load()) {
        m_stream->pause();
        m_isPlaying.store(false);
        return true;
    }
    return false;
}

void OboeAudioStream::restart() {
    std::lock_guard<std::mutex> lock(m_ioMutex);
    stopUnlocked();
    startUnlocked();
}

bool OboeAudioStream::isPlaying() const {
    return m_isPlaying.load();
}

void OboeAudioStream::setSourceType(AudioSourceType type) {
    m_sourceType.store(type);
}

void OboeAudioStream::setToneFrequency(float freq) {
    m_toneFrequency.store(std::clamp(freq, 20.0f, 20000.0f));
}

void OboeAudioStream::setLowPowerIo(bool enabled) {
    m_lowPowerIo.store(enabled);
}

size_t OboeAudioStream::writePcmFrames(const float* interleavedStereo, size_t numFrames) {
    return m_pcmRing.write(interleavedStereo, numFrames);
}

size_t OboeAudioStream::pcmAvailableWrite() const {
    return m_pcmRing.availableWrite();
}

size_t OboeAudioStream::pcmAvailableRead() const {
    return m_pcmRing.availableRead();
}

void OboeAudioStream::resetPcmRing() {
    m_pcmRing.reset();
}

int32_t OboeAudioStream::getSampleRate() const {
    return m_stream ? m_stream->getSampleRate() : 48000;
}

int32_t OboeAudioStream::getChannelCount() const {
    return m_stream ? m_stream->getChannelCount() : 2;
}

int32_t OboeAudioStream::getBufferSize() const {
    return m_stream ? m_stream->getBufferSizeInFrames() : 256;
}

int32_t OboeAudioStream::getFramesPerBurst() const {
    return m_stream ? m_stream->getFramesPerBurst() : 128;
}

double OboeAudioStream::getCpuLoad() const {
    return m_cpuLoad.load();
}

int64_t OboeAudioStream::getFramesWritten() const {
    return m_stream ? m_stream->getFramesWritten() : 0;
}

bool OboeAudioStream::isAAudio() const {
    return m_stream ? (m_stream->getAudioApi() == oboe::AudioApi::AAudio) : true;
}

oboe::DataCallbackResult OboeAudioStream::onAudioReady(oboe::AudioStream* audioStream, void* audioData, int32_t numFrames) {
    const bool sampleLoad = (++m_callbackCount & 31u) == 0u;
    const auto startTime = sampleLoad
        ? std::chrono::high_resolution_clock::now()
        : std::chrono::high_resolution_clock::time_point{};

    float* floatData = static_cast<float*>(audioData);

    generateAudioSource(floatData, numFrames);

    if (m_dspEngine) {
        m_dspEngine->processInterleaved(floatData, static_cast<size_t>(numFrames));
    }

    if (sampleLoad) {
        auto endTime = std::chrono::high_resolution_clock::now();
        double durationSec = std::chrono::duration<double>(endTime - startTime).count();
        double budgetSec = static_cast<double>(numFrames) / static_cast<double>(audioStream->getSampleRate());
        if (budgetSec > 0.0) {
            double load = (durationSec / budgetSec) * 100.0;
            m_cpuLoad.store(load, std::memory_order_relaxed);
        }
    }

    return oboe::DataCallbackResult::Continue;
}

void OboeAudioStream::onErrorAfterClose(oboe::AudioStream* /*audioStream*/, oboe::Result error) {
    if (error == oboe::Result::ErrorDisconnected) {
        scheduleRestart();
    }
}

void OboeAudioStream::onErrorBeforeClose(oboe::AudioStream* /*audioStream*/, oboe::Result /*error*/) {
}

void OboeAudioStream::scheduleRestart() {
    if (m_destroyed.load() || m_restartPending.exchange(true)) {
        return;
    }
    if (m_restartThread.joinable()) {
        m_restartThread.join();
    }
    m_restartThread = std::thread([this]() {
        std::this_thread::sleep_for(std::chrono::milliseconds(50));
        if (!m_destroyed.load()) {
            restart();
        }
        m_restartPending.store(false);
    });
}

void OboeAudioStream::setupToneGenerators(float sampleRate) {
    m_sinePhase = 0.0f;
    m_sweepPhase = 0.0f;
    m_sweepCurrentFreq = 20.0f;
    m_pinkB0 = m_pinkB1 = m_pinkB2 = m_pinkB3 = m_pinkB4 = m_pinkB5 = m_pinkB6 = 0.0f;
}

void OboeAudioStream::generateAudioSource(float* buffer, int32_t numFrames) {
    AudioSourceType type = m_sourceType.load(std::memory_order_relaxed);
    float sampleRate = m_stream ? static_cast<float>(m_stream->getSampleRate()) : 48000.0f;
    constexpr float PI = 3.14159265358979323846f;

    switch (type) {
        case AudioSourceType::ToneSine: {
            float freq = m_toneFrequency.load(std::memory_order_relaxed);
            float phaseIncrement = (2.0f * PI * freq) / sampleRate;
            for (int32_t i = 0; i < numFrames; ++i) {
                float s = std::sin(m_sinePhase) * 0.45f;
                m_sinePhase += phaseIncrement;
                if (m_sinePhase >= 2.0f * PI) m_sinePhase -= 2.0f * PI;
                buffer[i * 2] = s;
                buffer[i * 2 + 1] = s;
            }
            break;
        }
        case AudioSourceType::CustomPcm: {
            m_pcmRing.read(buffer, static_cast<size_t>(numFrames));
            break;
        }
        case AudioSourceType::ToneSweep: {
            // Logarithmic sweep 20 Hz to 20 kHz over 5 seconds
            float sweepSpeed = std::pow(20000.0f / 20.0f, 1.0f / (5.0f * sampleRate));
            for (int32_t i = 0; i < numFrames; ++i) {
                float phaseIncrement = (2.0f * PI * m_sweepCurrentFreq) / sampleRate;
                float s = std::sin(m_sweepPhase) * 0.40f;
                m_sweepPhase += phaseIncrement;
                if (m_sweepPhase >= 2.0f * PI) m_sweepPhase -= 2.0f * PI;
                m_sweepCurrentFreq *= sweepSpeed;
                if (m_sweepCurrentFreq > 20000.0f) {
                    m_sweepCurrentFreq = 20.0f;
                }
                buffer[i * 2] = s;
                buffer[i * 2 + 1] = s;
            }
            break;
        }
        case AudioSourceType::TonePinkNoise: {
            // Paul Kellet's filtered pink noise generator
            for (int32_t i = 0; i < numFrames; ++i) {
                // White noise (-1.0 to 1.0)
                m_noiseSeed = 214013u * m_noiseSeed + 2531011u;
                float white = static_cast<float>(static_cast<int32_t>(m_noiseSeed >> 16)) / 32768.0f;

                m_pinkB0 = 0.99886f * m_pinkB0 + white * 0.0555179f;
                m_pinkB1 = 0.99332f * m_pinkB1 + white * 0.0750759f;
                m_pinkB2 = 0.96900f * m_pinkB2 + white * 0.1538520f;
                m_pinkB3 = 0.86650f * m_pinkB3 + white * 0.3104856f;
                m_pinkB4 = 0.55000f * m_pinkB4 + white * 0.5329522f;
                m_pinkB5 = -0.7616f * m_pinkB5 - white * 0.0168980f;
                float pink = (m_pinkB0 + m_pinkB1 + m_pinkB2 + m_pinkB3 + m_pinkB4 + m_pinkB5 + m_pinkB6 + white * 0.5362f) * 0.06f;
                m_pinkB6 = white * 0.115926f;

                buffer[i * 2] = pink;
                buffer[i * 2 + 1] = pink;
            }
            break;
        }
        case AudioSourceType::ToneSilence:
        default: {
            std::fill_n(buffer, numFrames * 2, 0.0f);
            break;
        }
    }
}

} // namespace fxmobile
