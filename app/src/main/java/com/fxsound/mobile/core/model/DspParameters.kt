package com.fxsound.mobile.core.model

import org.json.JSONArray
import org.json.JSONObject

data class DspParameters(
    val powerEnabled: Boolean = true,
    val bass: Float = 0.0f,
    val clarity: Float = 0.0f,
    val ambience: Float = 0.0f,
    val surround: Float = 0.0f,
    val dynamicBoost: Float = 0.0f,
    val volumeLeveling: Float = 0.0f,
    val masterGainDb: Float = 0.0f,
    val balance: Float = 0.0f,
    val eqEnabled: Boolean = true,
    val eqBandCount: Int = 10,
    val eqGains: List<Float> = List(10) { 0.0f },
    val eqFrequencies: List<Float> = EqBandTables.TEN,
    val eqQFactors: List<Float> = EqBandTables.qFactors()
) {
    fun toTenBand(): DspParameters {
        if (eqBandCount == EqBandTables.COUNT && eqGains.size == EqBandTables.COUNT &&
            eqFrequencies.size == EqBandTables.COUNT && eqQFactors.size == EqBandTables.COUNT
        ) {
            return this
        }
        val freqs = EqBandTables.TEN
        return copy(
            eqBandCount = EqBandTables.COUNT,
            eqGains = EqBandTables.remapGains(eqFrequencies, eqGains, freqs),
            eqFrequencies = freqs,
            eqQFactors = EqBandTables.qFactors()
        )
    }

    fun toJsonObject(): JSONObject {
        val paramsObj = JSONObject()
        paramsObj.put("powerEnabled", powerEnabled)
        paramsObj.put("bass", bass.toDouble())
        paramsObj.put("clarity", clarity.toDouble())
        paramsObj.put("ambience", ambience.toDouble())
        paramsObj.put("surround", surround.toDouble())
        paramsObj.put("dynamicBoost", dynamicBoost.toDouble())
        paramsObj.put("volumeLeveling", volumeLeveling.toDouble())
        paramsObj.put("masterGainDb", masterGainDb.toDouble())
        paramsObj.put("balance", balance.toDouble())
        paramsObj.put("eqEnabled", eqEnabled)
        paramsObj.put("eqBandCount", eqBandCount)
        paramsObj.put("eqGains", JSONArray().also { arr -> eqGains.forEach { arr.put(it.toDouble()) } })
        paramsObj.put("eqFrequencies", JSONArray().also { arr -> eqFrequencies.forEach { arr.put(it.toDouble()) } })
        paramsObj.put("eqQFactors", JSONArray().also { arr -> eqQFactors.forEach { arr.put(it.toDouble()) } })
        return paramsObj
    }

    companion object {
        val FLAT = DspParameters()

        fun fromJsonObject(paramsObj: JSONObject): DspParameters {
            val eqBandCount = paramsObj.optInt("eqBandCount", 10)
            val defaultFreqs = EqBandTables.frequencies(eqBandCount)
            val defaultQs = EqBandTables.qFactors(eqBandCount)

            val gains = readFloatArray(paramsObj.optJSONArray("eqGains"), defaultFreqs.size)
            val freqs = readFloatArray(paramsObj.optJSONArray("eqFrequencies"), defaultFreqs.size)
                .ifEmpty { defaultFreqs }
            val qs = readFloatArray(paramsObj.optJSONArray("eqQFactors"), defaultFreqs.size)
                .ifEmpty { defaultQs }

            return DspParameters(
                powerEnabled = paramsObj.optBoolean("powerEnabled", true),
                bass = paramsObj.optDouble("bass", 0.0).toFloat(),
                clarity = paramsObj.optDouble("clarity", 0.0).toFloat(),
                ambience = paramsObj.optDouble("ambience", 0.0).toFloat(),
                surround = paramsObj.optDouble("surround", 0.0).toFloat(),
                dynamicBoost = paramsObj.optDouble("dynamicBoost", 0.0).toFloat(),
                volumeLeveling = paramsObj.optDouble("volumeLeveling", 0.0).toFloat(),
                masterGainDb = paramsObj.optDouble("masterGainDb", 0.0).toFloat(),
                balance = paramsObj.optDouble("balance", 0.0).toFloat(),
                eqEnabled = paramsObj.optBoolean("eqEnabled", true),
                eqBandCount = eqBandCount,
                eqGains = gains.ifEmpty { List(eqBandCount) { 0.0f } },
                eqFrequencies = if (freqs.size == eqBandCount) freqs else defaultFreqs,
                eqQFactors = if (qs.size == eqBandCount) qs else defaultQs
            ).toTenBand()
        }

        private fun readFloatArray(array: JSONArray?, expected: Int): List<Float> {
            if (array == null) return emptyList()
            val out = mutableListOf<Float>()
            for (i in 0 until array.length()) {
                out.add(array.getDouble(i).toFloat())
            }
            return if (expected > 0 && out.size > expected) out.take(expected) else out
        }
    }
}
