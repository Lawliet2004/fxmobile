package com.fxsound.mobile.ui.util

/**
 * Layout math used by Compose screens. Kept as pure functions so JVM unit tests
 * can assert overflow, chip packing, and bottom-nav clearance without a device.
 */
object UiPacking {
    const val SCREEN_PAD_DP = 16
    const val CARD_PAD_DP = 14
    const val CHIP_PAD_H_DP = 14
    const val CHIP_GAP_DP = 8
    const val NAV_BAR_HEIGHT_DP = 80
    const val LABEL_VALUE_GAP_DP = 8
    const val CHECK_BOX_DP = 22
    const val CHECK_MARK_DP = 14
    const val PLAY_CONTROL_DP = 40
    const val MIN_VALUE_COL_DP = 48
    const val TEST_CHIP_PAD_H_DP = 10
    const val TEST_CHIP_GAP_DP = 6
    const val MODE_CHIP_PAD_H_DP = 12
    const val MODE_CHIP_FONT_SP = 13
    const val CHIP_FONT_SP = 12
    const val TELEMETRY_FONT_SP = 12

    /**
     * Extra scroll padding so the last block sits above the floating tab bar
     * (64dp capsule + 8dp gap + gesture-bar breathing room), iOS-style.
     */
    const val SETTINGS_END_CLEARANCE_DP = 100

    data class ChipMetrics(
        val label: String,
        val textWidthDp: Int,
        val chipWidthDp: Int
    ) {
        val fitsInBox: Boolean get() = textWidthDp <= chipWidthDp && chipWidthDp > 0
    }

    data class ChipRowLayout(
        val chips: List<ChipMetrics>,
        val totalWidthDp: Int,
        val availableWidthDp: Int,
        val needsScroll: Boolean
    ) {
        val eachChipFitsItsBox: Boolean get() = chips.isNotEmpty() && chips.all { it.fitsInBox }
    }

    data class LabelValueLayout(
        val label: String,
        val value: String,
        val labelWidthDp: Int,
        val valueWidthDp: Int,
        val valueMaxWidthDp: Int,
        val wrapsValue: Boolean,
        val fits: Boolean
    )

    /**
     * Conservative Latin width in dp at 1:1 density (sp == dp). Slightly wide so
     * packing tests fail closed: if this says it fits, Roboto on a phone will too.
     */
    fun textWidthDp(text: String, fontSizeSp: Int): Int {
        if (text.isEmpty()) return 0
        var width = 0f
        for (ch in text) {
            width += when {
                ch == ' ' -> fontSizeSp * 0.34f
                ch == '.' || ch == ',' || ch == ':' -> fontSizeSp * 0.32f
                ch == '(' || ch == ')' || ch == '/' || ch == '-' -> fontSizeSp * 0.38f
                ch == '*' -> fontSizeSp * 0.48f
                ch.isUpperCase() -> fontSizeSp * 0.70f
                ch.isDigit() -> fontSizeSp * 0.62f
                else -> fontSizeSp * 0.56f
            }
        }
        return kotlin.math.ceil(width.toDouble()).toInt()
    }

    fun contentWidthDp(screenWidthDp: Int, screenPadDp: Int = SCREEN_PAD_DP): Int {
        return (screenWidthDp - screenPadDp * 2).coerceAtLeast(0)
    }

    fun innerCardWidthDp(screenWidthDp: Int): Int {
        return (contentWidthDp(screenWidthDp) - CARD_PAD_DP * 2).coerceAtLeast(0)
    }

    fun chipMetrics(
        label: String,
        fontSizeSp: Int = CHIP_FONT_SP,
        padHDp: Int = CHIP_PAD_H_DP
    ): ChipMetrics {
        val text = textWidthDp(label, fontSizeSp)
        return ChipMetrics(
            label = label,
            textWidthDp = text,
            chipWidthDp = text + padHDp * 2
        )
    }

    fun chipRowLayout(
        names: List<String>,
        availableWidthDp: Int,
        gapDp: Int = CHIP_GAP_DP,
        fontSizeSp: Int = CHIP_FONT_SP,
        padHDp: Int = CHIP_PAD_H_DP
    ): ChipRowLayout {
        val chips = names.map { chipMetrics(it, fontSizeSp, padHDp) }
        val total = if (chips.isEmpty()) {
            0
        } else {
            chips.sumOf { it.chipWidthDp } + gapDp * (chips.size - 1)
        }
        return ChipRowLayout(
            chips = chips,
            totalWidthDp = total,
            availableWidthDp = availableWidthDp,
            needsScroll = total > availableWidthDp
        )
    }

    fun labelValueLayout(
        label: String,
        value: String,
        availableWidthDp: Int,
        fontSizeSp: Int = TELEMETRY_FONT_SP,
        gapDp: Int = LABEL_VALUE_GAP_DP
    ): LabelValueLayout {
        val labelW = textWidthDp(label, fontSizeSp)
        val valueW = textWidthDp(value, fontSizeSp)
        val valueMax = (availableWidthDp - labelW - gapDp).coerceAtLeast(MIN_VALUE_COL_DP)
        val overflowsSingleLine = labelW + gapDp + valueW > availableWidthDp
        val wrapsValue = overflowsSingleLine
        val roomForLabel = labelW + gapDp + MIN_VALUE_COL_DP <= availableWidthDp
        val valueCanWrap = valueMax >= MIN_VALUE_COL_DP
        val fits = availableWidthDp > 0 && ( !overflowsSingleLine || (roomForLabel && valueCanWrap) )
        return LabelValueLayout(
            label = label,
            value = value,
            labelWidthDp = labelW,
            valueWidthDp = valueW,
            valueMaxWidthDp = valueMax,
            wrapsValue = wrapsValue,
            fits = fits
        )
    }

    fun modeChipFits(label: String, availableWidthDp: Int): Boolean {
        val chip = chipMetrics(label, MODE_CHIP_FONT_SP, MODE_CHIP_PAD_H_DP)
        val withTick = chip.chipWidthDp + CHECK_BOX_DP + 8
        return withTick <= availableWidthDp && chip.fitsInBox && checkMarkFits()
    }

    fun testChipRowLayout(
        names: List<String>,
        availableWidthDp: Int,
        playControlDp: Int = PLAY_CONTROL_DP
    ): ChipRowLayout {
        val chipArea = (availableWidthDp - playControlDp - 8).coerceAtLeast(0)
        return chipRowLayout(
            names = names,
            availableWidthDp = chipArea,
            gapDp = TEST_CHIP_GAP_DP,
            fontSizeSp = 11,
            padHDp = TEST_CHIP_PAD_H_DP
        )
    }

    fun checkMarkFits(boxDp: Int = CHECK_BOX_DP, markDp: Int = CHECK_MARK_DP): Boolean {
        return markDp > 0 && boxDp >= markDp + 6
    }

    fun settingsBottomClearanceDp(navAlreadyInset: Boolean = true): Int {
        return if (navAlreadyInset) SETTINGS_END_CLEARANCE_DP else NAV_BAR_HEIGHT_DP + SETTINGS_END_CLEARANCE_DP
    }

    /**
     * Last Settings block is reachable when the scroll end padding plus usable
     * viewport (screen minus nav) can bring the whole block on-screen.
     */
    fun lastSectionReachable(
        viewportHeightDp: Int,
        navBarHeightDp: Int,
        lastBlockHeightDp: Int,
        bottomPaddingDp: Int
    ): Boolean {
        val usable = (viewportHeightDp - navBarHeightDp).coerceAtLeast(0)
        if (usable <= 0 || lastBlockHeightDp <= 0) return false
        if (lastBlockHeightDp <= usable && bottomPaddingDp >= SETTINGS_END_CLEARANCE_DP / 2) return true
        return bottomPaddingDp >= navBarHeightDp.coerceAtMost(SETTINGS_END_CLEARANCE_DP) &&
            lastBlockHeightDp <= usable + bottomPaddingDp
    }

    fun faderLabelFits(label: String, columnWidthDp: Int, fontSizeSp: Int = 9): Boolean {
        if (columnWidthDp <= 0) return false
        return textWidthDp(label, fontSizeSp) <= columnWidthDp
    }

    fun typicalPresetNames(): List<String> = listOf(
        "General",
        "Music",
        "Voice",
        "Volume Boost",
        "Gaming",
        "Classic Processing",
        "Light Processing",
        "Bass Boost",
        "Flat",
        "Compare"
    )

    fun typicalTelemetryPairs(): List<Pair<String, String>> = listOf(
        "Audio Backend" to "AAudio",
        "Native Sample Rate" to "48000 Hz",
        "Channels" to "2 (Stereo PCM Float32)",
        "Buffer Size" to "192 frames (192 burst)",
        "Estimated Latency" to "4.00 ms",
        "DSP CPU Load" to "2.40%",
        "Active Output Device" to "Speaker",
        "Android Version" to "Android 14 (API 34)"
    )

    fun typicalModeChipLabels(): List<String> = listOf(
        "Internal engine (full DSP)",
        "Player session (limited)"
    )

    fun typicalTestChipLabels(): List<String> = listOf("Sine", "Sweep", "Noise")
}
