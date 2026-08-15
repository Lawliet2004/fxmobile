package com.fxsound.mobile.core.presets

import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.model.EqBandTables
import com.fxsound.mobile.core.model.Preset

object BuiltInPresets {

    val GENERAL = factory(
        id = "builtin_general",
        name = "General",
        clarity = 3.9f,
        surround = 1.6f,
        ambience = 0.0f,
        dynamic = 4.7f,
        bass = 4.7f,
        eq = listOf(0f, 0f, 1f, 2f, 0f, -1f, 0f, -1f, -2f, 0f)
    )

    val MUSIC = factory(
        id = "builtin_music",
        name = "Music",
        clarity = 3.9f,
        surround = 2.8f,
        ambience = 2.8f,
        dynamic = 1.6f,
        bass = 4.7f,
        eq = listOf(0f, 2f, 2f, 1f, 0f, 0f, 0f, -1f, 0f, 2f)
    )

    val VOICE = factory(
        id = "builtin_voice",
        name = "Voice",
        clarity = 5.7f,
        surround = 0.0f,
        ambience = 0.0f,
        dynamic = 7.5f,
        bass = 0.0f,
        eq = listOf(0f, -4f, -2f, 2f, 4f, 5f, 3f, 3f, 5f, -11f)
    )

    val VOLUME_BOOST = factory(
        id = "builtin_volume_boost",
        name = "Volume Boost",
        clarity = 2.5f,
        surround = 1.6f,
        ambience = 0.0f,
        dynamic = 8.1f,
        bass = 2.8f,
        eq = listOf(0f, 3f, 2f, 2f, 0f, 0f, 1f, 1f, 2f, 2f)
    )

    val GAMING = factory(
        id = "builtin_gaming",
        name = "Gaming",
        clarity = 2.8f,
        surround = 0.0f,
        ambience = 0.0f,
        dynamic = 6.7f,
        bass = 2.8f,
        eq = listOf(0f, 0f, 2f, 2f, 2f, 0f, -1f, -1f, 2f, 2f)
    )

    val CLASSIC = factory(
        id = "builtin_classic",
        name = "Classic Processing",
        clarity = 4.7f,
        surround = 2.8f,
        ambience = 4.7f,
        dynamic = 4.7f,
        bass = 5.5f,
        eq = List(10) { 0f }
    )

    val LIGHT = factory(
        id = "builtin_light",
        name = "Light Processing",
        clarity = 2.0f,
        surround = 0.0f,
        ambience = 2.8f,
        dynamic = 0.4f,
        bass = 1.6f,
        eq = listOf(0f, -1f, 1f, 1f, -1f, -1f, -2f, -1f, -1f, 1f)
    )

    val BASS_BOOST = factory(
        id = "builtin_bass_boost",
        name = "Bass Boost",
        clarity = 2.4f,
        surround = 2.8f,
        ambience = 2.8f,
        dynamic = 1.6f,
        bass = 5.9f,
        eq = listOf(0f, 3f, 3f, 2f, 1f, -1f, -1f, -1f, -1f, 0f)
    )

    val STREAMING_VIDEO = factory(
        id = "builtin_streaming_video",
        name = "Streaming Video",
        clarity = 2.8f,
        surround = 2.8f,
        ambience = 0.0f,
        dynamic = 4.3f,
        bass = 2.8f,
        eq = listOf(0f, 0f, 0f, 0f, 1f, 1f, 1f, -1f, 0f, 2f)
    )

    val MOVIES = factory(
        id = "builtin_movies",
        name = "Movies",
        clarity = 4.7f,
        surround = 3.9f,
        ambience = 0.0f,
        dynamic = 6.7f,
        bass = 2.8f,
        eq = listOf(0f, 0f, 2f, 0f, 2f, 2f, 1f, -1f, 0f, 2f)
    )

    val TV = factory(
        id = "builtin_tv",
        name = "TV",
        clarity = 3.9f,
        surround = 3.9f,
        ambience = 1.6f,
        dynamic = 4.7f,
        bass = 3.5f,
        eq = listOf(0f, 0f, 1f, 0f, 1f, 0f, 1f, -1f, -1f, 2f)
    )

    val TRANSCRIPTION = factory(
        id = "builtin_transcription",
        name = "Transcription",
        clarity = 7.9f,
        surround = 0.0f,
        ambience = 0.0f,
        dynamic = 9.1f,
        bass = 5.9f,
        eq = listOf(0f, -12f, 7f, 2f, -1f, 7f, 0f, 10f, 3f, -12f)
    )

    val DEFAULT = factory(
        id = "builtin_default",
        name = "Default",
        clarity = 0.0f,
        surround = 0.0f,
        ambience = 0.0f,
        dynamic = 0.0f,
        bass = 0.0f,
        eq = List(10) { 0f }
    )

    val FLAT = DEFAULT.copy(id = "builtin_flat", name = "Flat (Neutral)")

    val ALL = listOf(
        GENERAL,
        MUSIC,
        VOICE,
        VOLUME_BOOST,
        GAMING,
        CLASSIC,
        LIGHT,
        BASS_BOOST,
        STREAMING_VIDEO,
        MOVIES,
        TV,
        TRANSCRIPTION,
        DEFAULT
    )

    val FACTORY_NAMES = ALL.map { it.name }

    private fun factory(
        id: String,
        name: String,
        clarity: Float,
        surround: Float,
        ambience: Float,
        dynamic: Float,
        bass: Float,
        eq: List<Float>
    ): Preset {
        return Preset(
            id = id,
            name = name,
            isBuiltIn = true,
            parameters = DspParameters(
                powerEnabled = true,
                bass = bass,
                clarity = clarity,
                ambience = ambience,
                surround = surround,
                dynamicBoost = dynamic,
                volumeLeveling = 0.0f,
                masterGainDb = 0.0f,
                balance = 0.0f,
                eqEnabled = true,
                eqBandCount = 10,
                eqGains = eq,
                eqFrequencies = EqBandTables.TEN,
                eqQFactors = EqBandTables.qFactors()
            )
        )
    }
}
