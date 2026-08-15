#pragma once

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct FxEngine FxEngine;

FxEngine* fx_engine_create(float sample_rate, uint32_t max_block);
void fx_engine_destroy(FxEngine* engine);
void fx_engine_prepare(FxEngine* engine, float sample_rate, uint32_t max_block);
void fx_engine_reset(FxEngine* engine);

void fx_engine_process_interleaved(FxEngine* engine, float* audio, uint32_t frames);
void fx_engine_process(FxEngine* engine, const float* input, float* output, uint32_t frames);

void fx_engine_set_power(FxEngine* engine, bool enabled);
bool fx_engine_get_power(const FxEngine* engine);

void fx_engine_set_bass(FxEngine* engine, float amount);
void fx_engine_set_clarity(FxEngine* engine, float amount);
void fx_engine_set_ambience(FxEngine* engine, float amount);
void fx_engine_set_surround(FxEngine* engine, float amount);
void fx_engine_set_dynamic(FxEngine* engine, float amount);
void fx_engine_set_leveling(FxEngine* engine, float amount);
void fx_engine_set_master_db(FxEngine* engine, float gain_db);
void fx_engine_set_balance(FxEngine* engine, float balance);
void fx_engine_set_input_db(FxEngine* engine, float gain_db);

float fx_engine_get_bass(const FxEngine* engine);
float fx_engine_get_clarity(const FxEngine* engine);
float fx_engine_get_ambience(const FxEngine* engine);
float fx_engine_get_surround(const FxEngine* engine);
float fx_engine_get_dynamic(const FxEngine* engine);
float fx_engine_get_leveling(const FxEngine* engine);
float fx_engine_get_master_db(const FxEngine* engine);
float fx_engine_get_balance(const FxEngine* engine);

void fx_engine_set_eq_enabled(FxEngine* engine, bool enabled);
bool fx_engine_get_eq_enabled(const FxEngine* engine);
void fx_engine_set_eq_band_count(FxEngine* engine, uint32_t count);
uint32_t fx_engine_get_eq_band_count(const FxEngine* engine);
void fx_engine_set_eq_gain(FxEngine* engine, uint32_t index, float gain_db);
void fx_engine_set_eq_freq(FxEngine* engine, uint32_t index, float freq);
void fx_engine_set_eq_q(FxEngine* engine, uint32_t index, float q);
float fx_engine_get_eq_gain(const FxEngine* engine, uint32_t index);
float fx_engine_get_eq_freq(const FxEngine* engine, uint32_t index);
float fx_engine_get_eq_q(const FxEngine* engine, uint32_t index);
void fx_engine_reset_eq(FxEngine* engine);

float fx_engine_magnitude_db(const FxEngine* engine, float freq);
void fx_engine_get_meter(FxEngine* engine, float out5[5]);

const char* fx_engine_backend_name(void);

#ifdef __cplusplus
}
#endif
