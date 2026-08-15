#include <jni.h>
#include <memory>
#include "dsp/DspEngine.hpp"
#include "audio/OboeAudioStream.hpp"
#include "fxengine.h"

using namespace fxmobile;

static DspEngine* engineOf(jlong ptr) {
    return reinterpret_cast<DspEngine*>(ptr);
}

static OboeAudioStream* streamOf(jlong ptr) {
    return reinterpret_cast<OboeAudioStream*>(ptr);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_createEngine(
        JNIEnv* env, jobject /* this */, jfloat sampleRate, jint maxBlockSize) {
    auto* engine = new DspEngine(sampleRate, static_cast<size_t>(maxBlockSize));
    return reinterpret_cast<jlong>(engine);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_destroyEngine(
        JNIEnv* env, jobject /* this */, jlong enginePtr) {
    delete engineOf(enginePtr);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setPowerEnabled(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jboolean enabled) {
    if (auto* engine = engineOf(enginePtr)) engine->setPowerEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setBass(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloat amount) {
    if (auto* engine = engineOf(enginePtr)) engine->setBass(amount);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setClarity(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloat amount) {
    if (auto* engine = engineOf(enginePtr)) engine->setClarity(amount);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setAmbience(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloat amount) {
    if (auto* engine = engineOf(enginePtr)) engine->setAmbience(amount);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setSurround(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloat amount) {
    if (auto* engine = engineOf(enginePtr)) engine->setSurround(amount);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setDynamicBoost(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloat amount) {
    if (auto* engine = engineOf(enginePtr)) engine->setDynamicBoost(amount);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setVolumeLeveling(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloat amount) {
    if (auto* engine = engineOf(enginePtr)) engine->setVolumeLeveling(amount);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setMasterGainDb(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloat gainDb) {
    if (auto* engine = engineOf(enginePtr)) engine->setMasterGainDb(gainDb);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setBalance(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloat balance) {
    if (auto* engine = engineOf(enginePtr)) engine->setBalance(balance);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setEqEnabled(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jboolean enabled) {
    if (auto* engine = engineOf(enginePtr)) engine->setEqEnabled(enabled);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setEqBandCount(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jint count) {
    if (auto* engine = engineOf(enginePtr)) engine->setEqBandCount(static_cast<size_t>(count));
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setEqBandGain(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jint bandIndex, jfloat gainDb) {
    if (auto* engine = engineOf(enginePtr)) engine->setEqBandGain(static_cast<size_t>(bandIndex), gainDb);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setEqBandFrequency(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jint bandIndex, jfloat freq) {
    if (auto* engine = engineOf(enginePtr)) engine->setEqBandFrequency(static_cast<size_t>(bandIndex), freq);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setEqBandQ(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jint bandIndex, jfloat q) {
    if (auto* engine = engineOf(enginePtr)) engine->setEqBandQ(static_cast<size_t>(bandIndex), q);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_resetAllEqBands(
        JNIEnv* env, jobject /* this */, jlong enginePtr) {
    if (auto* engine = engineOf(enginePtr)) engine->resetAllEqBands();
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_getMagnitudeResponse(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloatArray inFreqs, jfloatArray outDbs, jint count) {
    auto* engine = engineOf(enginePtr);
    if (!engine || count <= 0) return;

    jfloat* freqs = env->GetFloatArrayElements(inFreqs, nullptr);
    jfloat* dbs = env->GetFloatArrayElements(outDbs, nullptr);

    if (freqs && dbs) {
        for (int i = 0; i < count; ++i) {
            dbs[i] = engine->getCompositeMagnitudeResponseDb(freqs[i]);
        }
    }

    if (freqs) env->ReleaseFloatArrayElements(inFreqs, freqs, JNI_ABORT);
    if (dbs) env->ReleaseFloatArrayElements(outDbs, dbs, 0);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_getMeterData(
        JNIEnv* env, jobject /* this */, jlong enginePtr, jfloatArray outValues) {
    auto* engine = engineOf(enginePtr);
    if (!engine) return;

    jfloat* values = env->GetFloatArrayElements(outValues, nullptr);
    if (values) {
        float pL = 0.0f, pR = 0.0f, rL = 0.0f, rR = 0.0f;
        bool clipping = false;
        engine->getMeterData(pL, pR, rL, rR, clipping);

        values[0] = pL;
        values[1] = pR;
        values[2] = rL;
        values[3] = rR;
        values[4] = clipping ? 1.0f : 0.0f;

        env->ReleaseFloatArrayElements(outValues, values, 0);
    }
}

JNIEXPORT jlong JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_createAudioStream(
        JNIEnv* env, jobject /* this */, jlong enginePtr) {
    auto* engine = engineOf(enginePtr);
    if (!engine) return 0;

    // Aliased shared_ptr: stream must not free the engine (JNI owns it).
    std::shared_ptr<DspEngine> engineShared(std::shared_ptr<DspEngine>{}, engine);
    auto* stream = new OboeAudioStream(engineShared);
    return reinterpret_cast<jlong>(stream);
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setAudioStreamLowPowerIo(
        JNIEnv* /* env */, jobject /* this */, jlong streamPtr, jboolean enabled) {
    if (auto* stream = streamOf(streamPtr)) {
        stream->setLowPowerIo(enabled == JNI_TRUE);
    }
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_destroyAudioStream(
        JNIEnv* env, jobject /* this */, jlong streamPtr) {
    delete streamOf(streamPtr);
}

JNIEXPORT jboolean JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_startAudioStream(
        JNIEnv* env, jobject /* this */, jlong streamPtr) {
    auto* stream = streamOf(streamPtr);
    return stream ? stream->start() : false;
}

JNIEXPORT jboolean JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_stopAudioStream(
        JNIEnv* env, jobject /* this */, jlong streamPtr) {
    auto* stream = streamOf(streamPtr);
    return stream ? stream->stop() : false;
}

JNIEXPORT jboolean JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_pauseAudioStream(
        JNIEnv* env, jobject /* this */, jlong streamPtr) {
    auto* stream = streamOf(streamPtr);
    return stream ? stream->pause() : false;
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_setAudioStreamSource(
        JNIEnv* env, jobject /* this */, jlong streamPtr, jint sourceType, jfloat frequency) {
    if (auto* stream = streamOf(streamPtr)) {
        stream->setSourceType(static_cast<AudioSourceType>(sourceType));
        stream->setToneFrequency(frequency);
    }
}

JNIEXPORT jint JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_writePcmFrames(
        JNIEnv* env, jobject /* this */, jlong streamPtr, jfloatArray data, jint offsetFrames, jint numFrames) {
    auto* stream = streamOf(streamPtr);
    if (!stream || numFrames <= 0 || offsetFrames < 0) return 0;
    jsize len = env->GetArrayLength(data);
    jint maxFrames = len / 2;
    if (offsetFrames >= maxFrames) return 0;
    if (offsetFrames + numFrames > maxFrames) {
        numFrames = maxFrames - offsetFrames;
    }
    jfloat* samples = env->GetFloatArrayElements(data, nullptr);
    if (!samples) return 0;
    jint written = static_cast<jint>(stream->writePcmFrames(
            samples + (offsetFrames * 2), static_cast<size_t>(numFrames)));
    env->ReleaseFloatArrayElements(data, samples, JNI_ABORT);
    return written;
}

JNIEXPORT jint JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_pcmAvailableWrite(
        JNIEnv* env, jobject /* this */, jlong streamPtr) {
    auto* stream = streamOf(streamPtr);
    return stream ? static_cast<jint>(stream->pcmAvailableWrite()) : 0;
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_resetPcmRing(
        JNIEnv* env, jobject /* this */, jlong streamPtr) {
    if (auto* stream = streamOf(streamPtr)) stream->resetPcmRing();
}

JNIEXPORT jstring JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_getDspBackend(
        JNIEnv* env, jobject /* this */) {
    const char* name = fx_engine_backend_name();
    return env->NewStringUTF(name ? name : "Rust DSP");
}

JNIEXPORT void JNICALL
Java_com_fxsound_mobile_audio_NativeDspBridge_getAudioStreamTelemetry(
        JNIEnv* env, jobject /* this */, jlong streamPtr, jfloatArray outTelemetry) {
    auto* stream = streamOf(streamPtr);
    if (!stream) return;

    jfloat* telemetry = env->GetFloatArrayElements(outTelemetry, nullptr);
    if (telemetry) {
        telemetry[0] = static_cast<float>(stream->getSampleRate());
        telemetry[1] = static_cast<float>(stream->getChannelCount());
        telemetry[2] = static_cast<float>(stream->getBufferSize());
        telemetry[3] = static_cast<float>(stream->getFramesPerBurst());
        telemetry[4] = static_cast<float>(stream->getCpuLoad());
        telemetry[5] = stream->isAAudio() ? 1.0f : 0.0f;

        env->ReleaseFloatArrayElements(outTelemetry, telemetry, 0);
    }
}

} // extern "C"
