package io.homeassistant.companion.android.settings.sensor.views

import io.homeassistant.companion.android.database.sensor.SensorSetting
import io.homeassistant.companion.android.database.sensor.SensorSettingType
import io.homeassistant.companion.android.settings.sensor.SensorDetailViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

/**
 * Guards the sensor setting dialog against reintroducing the in place write it used to perform.
 *
 * The dialog used to submit `state.copy().apply { setting.value = input }`. Because `copy()` is a
 * shallow copy, the submitted state kept pointing at the very [SensorSetting] instance the settings
 * list was being drawn from, and the write changed that instance under the list. The instance Room
 * emitted after the save then compared equal to the one already on screen, so Compose considered
 * the row unchanged and the summary kept showing the value from before the save.
 *
 * These tests fail if the original instance is ever touched again.
 */
class SettingDialogStateTest {

    @Test
    fun `Given a list setting when submitting a new value then the original setting instance is unchanged`() {
        val originalSetting = listSetting(value = OLD_VALUE)
        val state = dialogState(originalSetting)

        state.withSettingValue(newValue = NEW_VALUE)

        assertEquals(OLD_VALUE, originalSetting.value)
    }

    @Test
    fun `Given a list setting when submitting a new value then a different setting instance is returned`() {
        val originalSetting = listSetting(value = OLD_VALUE)
        val state = dialogState(originalSetting)

        val updatedState = state.withSettingValue(newValue = NEW_VALUE)

        // Compose compares rows by instance identity first, so a fresh instance is what makes the
        // settings list redraw the summary after the save.
        assertNotSame(originalSetting, updatedState.setting)
    }

    @Test
    fun `Given a list setting when submitting a new value then the returned state carries that value`() {
        val state = dialogState(listSetting(value = OLD_VALUE))

        val updatedState = state.withSettingValue(newValue = NEW_VALUE)

        assertEquals(NEW_VALUE, updatedState.setting.value)
    }

    @Test
    fun `Given a list setting when submitting a new value then the other setting fields are preserved`() {
        val originalSetting = listSetting(value = OLD_VALUE)
        val state = dialogState(originalSetting)

        val updatedSetting = state.withSettingValue(newValue = NEW_VALUE).setting

        assertEquals(originalSetting.copy(value = NEW_VALUE), updatedSetting)
    }

    @Test
    fun `Given a loaded dialog when submitting a new value then the loading flag and entries are preserved`() {
        val state = dialogState(listSetting(value = OLD_VALUE))

        val updatedState = state.withSettingValue(newValue = NEW_VALUE)

        assertEquals(state.copy(setting = updatedState.setting), updatedState)
    }

    private fun listSetting(value: String) = SensorSetting(
        sensorId = SENSOR_ID,
        name = SETTING_NAME,
        value = value,
        valueType = SensorSettingType.LIST,
        enabled = true,
        entries = listOf(OLD_VALUE, NEW_VALUE),
    )

    private fun dialogState(setting: SensorSetting) = SensorDetailViewModel.Companion.SettingDialogState(
        setting = setting,
        loading = false,
        entries = listOf(OLD_VALUE to OLD_VALUE, NEW_VALUE to NEW_VALUE),
        entriesSelected = listOf(OLD_VALUE),
    )
}

private const val SENSOR_ID = "last_used"
private const val SETTING_NAME = "last_used_update_interval"
private const val OLD_VALUE = "fast"
private const val NEW_VALUE = "slow"
