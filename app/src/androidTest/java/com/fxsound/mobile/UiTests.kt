package com.fxsound.mobile

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.fxsound.mobile.audio.AudioEngineController
import com.fxsound.mobile.core.model.DspParameters
import com.fxsound.mobile.core.model.Preset
import com.fxsound.mobile.core.presets.BuiltInPresets
import com.fxsound.mobile.core.presets.PresetRepository
import com.fxsound.mobile.core.presets.StringStore
import com.fxsound.mobile.core.settings.SettingsRepository
import com.fxsound.mobile.ui.home.HomeScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI smoke test for the Home screen.
 *
 * Runs on a real device or emulator (androidTest). Uses a real
 * [AudioEngineController] (no JNI is actually called because the
 * native engine is not started). The fake [StringStore] keeps the
 * [PresetRepository] off the disk.
 *
 * Test setup requirements in app/build.gradle.kts:
 *   - `androidTestImplementation("androidx.compose.ui:ui-test-junit4")`
 *   - `debugImplementation("androidx.compose.ui:ui-test-manifest")`
 *   - For pure-JVM local runs, also add
 *     `testImplementation("org.robolectric:robolectric:4.11.1")` and
 *     `@RunWith(AndroidJUnit4::class)` in this file.
 *
 * NOTE: If [AudioEngineController] is ever refactored to be `open` or
 * to expose an interface, replace the [FakeAudioEngineController] below
 * with a subclass that records the last preset id. Until then, this
 * test verifies the click path indirectly: it checks that after
 * clicking a chip, [AudioEngineController.parameters] reflects the
 * preset's bass value.
 */
@RunWith(AndroidJUnit4::class)
class UiTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_showsEnhancementEffectsAndChipClickAppliesPreset() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val controller = AudioEngineController(context, SettingsRepository(context))
        val repo = PresetRepository(InMemoryStringStoreForAndroidTest())

        try {
            composeTestRule.setContent {
                HomeScreen(
                    audioController = controller,
                    presetRepository = repo,
                    onNavigateToEqualizer = { /* no-op */ }
                )
            }

            // Sanity: a known element of HomeScreen is rendered.
            composeTestRule.onNodeWithText("ENHANCEMENT EFFECTS").assertIsDisplayed()
            composeTestRule.onNodeWithText("Compare").assertIsDisplayed()

            // Click the built-in "Music" preset chip. applyPreset() updates
            // controller.parameters, so we verify the side-effect instead of
            // intercepting a call.
            val music = BuiltInPresets.MUSIC
            composeTestRule.onNodeWithText(music.name).performClick()

            composeTestRule.waitForIdle()

            // Music bass is 4.7 (see BuiltInPresets.MUSIC).
            val appliedBass = controller.parameters.value.bass
            org.junit.Assert.assertEquals(
                "Clicking the Music chip should apply its bass to the controller",
                music.parameters.bass,
                appliedBass,
                0.001f
            )
        } finally {
            runCatching { controller.release() }
        }
    }
}

/**
 * Minimal in-memory [StringStore] for the [PresetRepository] under test.
 * Lives in the androidTest source set so the existing test source set
 * is unaffected.
 */
class InMemoryStringStoreForAndroidTest : StringStore {
    private val strings = mutableMapOf<String, String?>()
    private val sets = mutableMapOf<String, Set<String>>()

    override fun getString(key: String, default: String?): String? =
        if (strings.containsKey(key)) strings[key] else default

    override fun putString(key: String, value: String?) {
        strings[key] = value
    }

    override fun getStringSet(key: String): Set<String> = sets[key] ?: emptySet()

    override fun putStringSet(key: String, values: Set<String>) {
        sets[key] = values.toSet()
    }
}
