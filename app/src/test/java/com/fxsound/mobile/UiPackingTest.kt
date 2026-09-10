package com.fxsound.mobile

import com.fxsound.mobile.core.presets.BuiltInPresets
import com.fxsound.mobile.ui.util.UiPacking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiPackingTest {

    private fun homeChipNames(): List<String> {
        return BuiltInPresets.FACTORY_NAMES + listOf("Flat", "Compare")
    }

    @Test
    fun bassBoostChipFitsItsBoxAtTypicalPhoneWidths() {
        val names = homeChipNames()
        assertTrue("shipped factory list must include Bass Boost", names.contains("Bass Boost"))
        for (width in intArrayOf(360, 400)) {
            val available = UiPacking.contentWidthDp(width)
            val layout = UiPacking.chipRowLayout(names, available)
            val bass = layout.chips.find { it.label == "Bass Boost" }
            assertTrue("Bass Boost chip missing at ${width}dp", bass != null)
            assertTrue(
                "Bass Boost text ${bass!!.textWidthDp}dp must fit chip ${bass.chipWidthDp}dp at ${width}dp",
                bass.fitsInBox
            )
            assertTrue("every preset chip must contain its full label", layout.eachChipFitsItsBox)
            if (layout.totalWidthDp > available) {
                assertTrue("overflowing preset row must use horizontal scroll", layout.needsScroll)
            }
        }
    }

    @Test
    fun presetChipRowScrollsInsteadOfClippingLongNames() {
        val available = UiPacking.contentWidthDp(360)
        val layout = UiPacking.chipRowLayout(homeChipNames(), available)
        assertTrue("real preset names exceed 360dp content and must scroll", layout.needsScroll)
        assertTrue(layout.chips.all { it.textWidthDp <= it.chipWidthDp })
        assertTrue(layout.chips.sumOf { it.chipWidthDp } + UiPacking.CHIP_GAP_DP * (layout.chips.size - 1) == layout.totalWidthDp)
    }

    @Test
    fun settingsTelemetryPairsFitInsideCardAtTypicalWidths() {
        for (width in intArrayOf(360, 400)) {
            val inner = UiPacking.innerCardWidthDp(width)
            for ((label, value) in UiPacking.typicalTelemetryPairs()) {
                val row = UiPacking.labelValueLayout(label, value, inner)
                assertTrue(
                    "$label / $value must fit inner ${inner}dp at screen ${width}dp (label=${row.labelWidthDp} value=${row.valueWidthDp} max=${row.valueMaxWidthDp})",
                    row.fits
                )
                if (row.wrapsValue) {
                    assertTrue(row.valueMaxWidthDp >= UiPacking.MIN_VALUE_COL_DP)
                } else {
                    assertTrue(row.labelWidthDp + UiPacking.LABEL_VALUE_GAP_DP + row.valueWidthDp <= inner)
                }
            }
        }
    }

    @Test
    fun modeAndTestChipsFitInsideSettingsContentWidth() {
        for (width in intArrayOf(360, 400)) {
            val inner = UiPacking.innerCardWidthDp(width)
            for (label in UiPacking.typicalModeChipLabels()) {
                assertTrue(
                    "mode chip '$label' must fit ${inner}dp with selection tick",
                    UiPacking.modeChipFits(label, inner)
                )
            }
            val tests = UiPacking.testChipRowLayout(UiPacking.typicalTestChipLabels(), inner)
            assertTrue("Sine/Sweep/Noise chips must keep full text in their boxes", tests.eachChipFitsItsBox)
            if (tests.totalWidthDp > tests.availableWidthDp) {
                assertTrue("test chips wrap or scroll when the play control leaves too little row room", tests.needsScroll)
            }
        }
    }

    @Test
    fun settingsLastSectionClearsBottomNav() {
        val padding = UiPacking.settingsBottomClearanceDp(navAlreadyInset = true)
        assertTrue("scroll end padding must clear the nav comfortably", padding >= 48)
        assertTrue(
            UiPacking.lastSectionReachable(
                viewportHeightDp = 800,
                navBarHeightDp = UiPacking.NAV_BAR_HEIGHT_DP,
                lastBlockHeightDp = 160,
                bottomPaddingDp = padding
            )
        )
        assertTrue(
            UiPacking.lastSectionReachable(
                viewportHeightDp = 640,
                navBarHeightDp = UiPacking.NAV_BAR_HEIGHT_DP,
                lastBlockHeightDp = 200,
                bottomPaddingDp = padding
            )
        )
        assertFalse(
            UiPacking.lastSectionReachable(
                viewportHeightDp = 640,
                navBarHeightDp = UiPacking.NAV_BAR_HEIGHT_DP,
                lastBlockHeightDp = 200,
                bottomPaddingDp = 0
            )
        )
    }

    @Test
    fun selectionTickFitsInsideItsBox() {
        assertTrue(UiPacking.checkMarkFits())
        assertTrue(UiPacking.checkMarkFits(boxDp = 24, markDp = 16))
        assertFalse(UiPacking.checkMarkFits(boxDp = 16, markDp = 16))
        assertEquals(22, UiPacking.CHECK_BOX_DP)
        assertEquals(14, UiPacking.CHECK_MARK_DP)
    }

    @Test
    fun contentWidthSubtractsScreenPadding() {
        assertEquals(328, UiPacking.contentWidthDp(360))
        assertEquals(368, UiPacking.contentWidthDp(400))
        assertEquals(300, UiPacking.innerCardWidthDp(360))
    }

    @Test
    fun tenBandFaderLabelsFitTypicalPhoneColumns() {
        val gaps = 2 * 9
        val column = (UiPacking.contentWidthDp(360) - gaps) / 10
        for (label in arrayOf("0", "+12", "-12", "31", "125", "1k", "16k")) {
            assertTrue(
                "fader '$label' must fit ${column}dp column",
                UiPacking.faderLabelFits(label, column)
            )
        }
        assertFalse(UiPacking.faderLabelFits("16000Hz", 12))
        assertFalse(UiPacking.faderLabelFits("16k", 0))
    }
}
