#pragma once

#include <atomic>
#include <cstdint>
#include <cstring>
#include <vector>
#include <algorithm>

namespace fxmobile {

/**
 * Single-producer / single-consumer interleaved stereo float ring.
 * Capacity is power-of-two frames. One slot is left empty to distinguish full/empty.
 * All storage is allocated in the constructor / resetFromNonAudioThread.
 */
class PcmRingBuffer {
public:
    static constexpr size_t kDefaultCapacityFrames = 16384; // must be power of two

    explicit PcmRingBuffer(size_t capacityFrames = kDefaultCapacityFrames)
        : m_capacity(nextPow2(std::max(capacityFrames, size_t(256)))),
          m_mask(m_capacity - 1),
          m_data(m_capacity * 2, 0.0f),
          m_write(0),
          m_read(0) {}

    void reset() {
        m_write.store(0, std::memory_order_relaxed);
        m_read.store(0, std::memory_order_relaxed);
        std::fill(m_data.begin(), m_data.end(), 0.0f);
    }

    size_t capacityFrames() const { return m_capacity; }

    size_t availableRead() const {
        uint32_t w = m_write.load(std::memory_order_acquire);
        uint32_t r = m_read.load(std::memory_order_relaxed);
        return static_cast<size_t>((w - r) & m_mask);
    }

    size_t availableWrite() const {
        uint32_t w = m_write.load(std::memory_order_relaxed);
        uint32_t r = m_read.load(std::memory_order_acquire);
        return static_cast<size_t>((r - w - 1u) & m_mask);
    }

    // Producer (decoder thread). Returns frames written.
    size_t write(const float* interleavedStereo, size_t numFrames) {
        size_t freeFrames = availableWrite();
        size_t toWrite = std::min(numFrames, freeFrames);
        if (toWrite == 0) return 0;

        uint32_t w = m_write.load(std::memory_order_relaxed);
        for (size_t i = 0; i < toWrite; ++i) {
            size_t idx = static_cast<size_t>((w + static_cast<uint32_t>(i)) & m_mask);
            m_data[idx * 2] = interleavedStereo[i * 2];
            m_data[idx * 2 + 1] = interleavedStereo[i * 2 + 1];
        }
        m_write.store(w + static_cast<uint32_t>(toWrite), std::memory_order_release);
        return toWrite;
    }

    // Consumer (audio thread). Returns frames read. Remaining frames are zeroed.
    size_t read(float* interleavedStereo, size_t numFrames) {
        size_t used = availableRead();
        size_t toRead = std::min(numFrames, used);
        uint32_t r = m_read.load(std::memory_order_relaxed);
        for (size_t i = 0; i < toRead; ++i) {
            size_t idx = static_cast<size_t>((r + static_cast<uint32_t>(i)) & m_mask);
            interleavedStereo[i * 2] = m_data[idx * 2];
            interleavedStereo[i * 2 + 1] = m_data[idx * 2 + 1];
        }
        if (toRead < numFrames) {
            std::fill_n(interleavedStereo + toRead * 2, (numFrames - toRead) * 2, 0.0f);
        }
        m_read.store(r + static_cast<uint32_t>(toRead), std::memory_order_release);
        return toRead;
    }

private:
    static size_t nextPow2(size_t v) {
        size_t p = 1;
        while (p < v) p <<= 1;
        return p;
    }

    size_t m_capacity;
    size_t m_mask;
    std::vector<float> m_data;
    std::atomic<uint32_t> m_write;
    std::atomic<uint32_t> m_read;
};

} // namespace fxmobile
