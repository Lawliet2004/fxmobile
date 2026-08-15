package com.fxsound.mobile.core.model

object EqBandTables {
    const val COUNT = 10
    val TEN = listOf(31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)

    fun frequencies(@Suppress("UNUSED_PARAMETER") bandCount: Int = COUNT): List<Float> = TEN

    fun defaultQ(@Suppress("UNUSED_PARAMETER") bandCount: Int = COUNT): Float = 1.414f

    fun qFactors(@Suppress("UNUSED_PARAMETER") bandCount: Int = COUNT): List<Float> = List(COUNT) { defaultQ() }

    fun remapGains(fromFreqs: List<Float>, fromGains: List<Float>, toFreqs: List<Float>): List<Float> {
        if (fromFreqs.isEmpty() || fromGains.isEmpty()) return List(toFreqs.size) { 0f }
        return toFreqs.map { target ->
            var best = 0
            var bestDist = Float.MAX_VALUE
            fromFreqs.forEachIndexed { i, freq ->
                val d = kotlin.math.abs(
                    kotlin.math.ln(freq.coerceAtLeast(1f)) - kotlin.math.ln(target.coerceAtLeast(1f))
                )
                if (d < bestDist) {
                    bestDist = d
                    best = i
                }
            }
            fromGains.getOrElse(best) { 0f }.coerceIn(-12f, 12f)
        }
    }
}
