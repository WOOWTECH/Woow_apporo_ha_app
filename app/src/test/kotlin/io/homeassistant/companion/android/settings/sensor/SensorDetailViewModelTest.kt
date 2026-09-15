package io.homeassistant.companion.android.settings.sensor

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import io.homeassistant.companion.android.common.sensors.SensorManager
import io.homeassistant.companion.android.database.sensor.SensorDao
import io.homeassistant.companion.android.database.sensor.SensorSetting
import io.homeassistant.companion.android.database.sensor.SensorSettingType
import io.homeassistant.companion.android.sensors.SensorReceiver
import io.homeassistant.companion.android.testing.unit.MainDispatcherJUnit4Rule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the allow list dialog of the app based sensors against losing saved packages.
 *
 * Android package visibility filtering means the app cannot enumerate every installed package, so a
 * package the user allowed earlier can be absent from the current enumeration. If the dialog only
 * offered the packages it can see, saving it would quietly drop the invisible ones from the setting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SensorDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherJUnit4Rule()

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `Given notification allowlist when saving visible apps then unqueryable selections survive`() {
        assertAppSelectionsPreserved(sensorId = LAST_NOTIFICATION_SENSOR_ID)
    }

    @Test
    fun `Given removed notification allowlist when saving visible apps then unqueryable selections survive`() {
        assertAppSelectionsPreserved(sensorId = LAST_REMOVED_NOTIFICATION_SENSOR_ID)
    }

    @Test
    fun `Given next alarm allowlist when saving visible apps then unqueryable selections survive`() {
        assertAppSelectionsPreserved(sensorId = NEXT_ALARM_SENSOR_ID)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S_V2])
    fun `Given legacy package query when saving visible apps then unqueryable selections survive`() {
        assertAppSelectionsPreserved(sensorId = LAST_NOTIFICATION_SENSOR_ID)
    }

    @Test
    fun `Given unknown selected package when loading summary then package name is used`() {
        assertAppSummaryFallback()
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.S_V2])
    fun `Given unknown selected package on legacy API when loading summary then package name is used`() {
        assertAppSummaryFallback()
    }

    private fun assertAppSelectionsPreserved(sensorId: String) = runTest {
        val application = mockk<Application>(relaxed = true)
        val packageManager = mockk<PackageManager>()
        every { application.packageManager } returns packageManager
        val visible = ApplicationInfo().apply { packageName = VISIBLE_PACKAGE }
        val blank = ApplicationInfo().apply { packageName = BLANK_LABEL_PACKAGE }
        val setting = SensorSetting(
            sensorId = sensorId,
            name = ALLOW_LIST_SETTING_NAME,
            value = "$HIDDEN_PACKAGE, $VISIBLE_PACKAGE",
            valueType = SensorSettingType.LIST_APPS,
        )
        val dao = mockk<SensorDao>(relaxed = true)
        mockInstalledApplications(packageManager, listOf(visible, blank))
        every { packageManager.getApplicationLabel(visible) } returns VISIBLE_LABEL
        every { packageManager.getApplicationLabel(blank) } returns " "
        val viewModel = createAppSettingsViewModel(application, dao, sensorId)
        try {
            viewModel.onSettingWithDialogPressed(setting)
            advanceUntilIdle()

            val dialog = requireNotNull(viewModel.sensorSettingsDialog)
            assertEquals(
                listOf(
                    BLANK_LABEL_PACKAGE to BLANK_LABEL_PACKAGE,
                    HIDDEN_PACKAGE to HIDDEN_PACKAGE,
                    VISIBLE_PACKAGE to "$VISIBLE_LABEL\n($VISIBLE_PACKAGE)",
                ),
                dialog.entries,
            )
            // The heart of this test: the package that cannot be queried is still selected.
            assertEquals(listOf(HIDDEN_PACKAGE, VISIBLE_PACKAGE), dialog.entriesSelected)
            coVerify(exactly = 0) { dao.add(any<SensorSetting>()) }
            verifySingleEnumeration(packageManager)

            // Mirror the dialog's Save action without toggling any rows.
            viewModel.submitSettingWithDialog(
                dialog.copy(setting = setting.copy(value = dialog.entriesSelected.joinToString(", "))),
            )
            advanceUntilIdle()

            coVerify(exactly = 1) { dao.add(setting) }
            coVerify(exactly = 0) { dao.setSensorEnabled(any(), any(), any()) }
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun assertAppSummaryFallback() = runTest {
        val sensorId = LAST_NOTIFICATION_SENSOR_ID
        val application = mockk<Application>(relaxed = true)
        val packageManager = mockk<PackageManager>()
        every { application.packageManager } returns packageManager
        val visible = ApplicationInfo().apply { packageName = VISIBLE_PACKAGE }
        mockApplicationInfoLookup(packageManager, visible)
        every { packageManager.getApplicationLabel(visible) } returns VISIBLE_LABEL
        val dao = mockk<SensorDao>(relaxed = true)
        val viewModel = createAppSettingsViewModel(application, dao, sensorId)
        val setting = SensorSetting(
            sensorId = sensorId,
            name = ALLOW_LIST_SETTING_NAME,
            value = "$HIDDEN_PACKAGE, $VISIBLE_PACKAGE",
            valueType = SensorSettingType.LIST_APPS,
        )
        try {
            assertEquals(
                listOf(HIDDEN_PACKAGE, VISIBLE_LABEL),
                viewModel.getSettingEntries(setting, listOf(VISIBLE_PACKAGE, HIDDEN_PACKAGE)),
            )
            // An explicitly empty selection stays empty instead of listing every installed app.
            assertEquals(emptyList<String>(), viewModel.getSettingEntries(setting, emptyList()))
            coVerify(exactly = 0) { dao.add(any<SensorSetting>()) }
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private fun mockInstalledApplications(packageManager: PackageManager, apps: List<ApplicationInfo>) {
        // A second enumeration would have a different order; the dialog must use one snapshot.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            every {
                packageManager.getInstalledApplications(any<PackageManager.ApplicationInfoFlags>())
            } returnsMany listOf(apps, apps.reversed())
        } else {
            @Suppress("DEPRECATION")
            every { packageManager.getInstalledApplications(any<Int>()) } returnsMany listOf(apps, apps.reversed())
        }
    }

    private fun verifySingleEnumeration(packageManager: PackageManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            verify(exactly = 1) { packageManager.getInstalledApplications(any<PackageManager.ApplicationInfoFlags>()) }
        } else {
            @Suppress("DEPRECATION")
            verify(exactly = 1) { packageManager.getInstalledApplications(any<Int>()) }
        }
    }

    private fun mockApplicationInfoLookup(packageManager: PackageManager, visible: ApplicationInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            every {
                packageManager.getApplicationInfo(HIDDEN_PACKAGE, any<PackageManager.ApplicationInfoFlags>())
            } throws PackageManager.NameNotFoundException()
            every {
                packageManager.getApplicationInfo(VISIBLE_PACKAGE, any<PackageManager.ApplicationInfoFlags>())
            } returns visible
        } else {
            @Suppress("DEPRECATION")
            every {
                packageManager.getApplicationInfo(HIDDEN_PACKAGE, any<Int>())
            } throws PackageManager.NameNotFoundException()
            @Suppress("DEPRECATION")
            every { packageManager.getApplicationInfo(VISIBLE_PACKAGE, any<Int>()) } returns visible
        }
    }

    private fun createAppSettingsViewModel(
        application: Application,
        dao: SensorDao,
        sensorId: String,
    ): SensorDetailViewModel {
        every { dao.getSettingsFlow(sensorId) } returns flowOf(emptyList())
        every { dao.getFullFlow(sensorId) } returns flowOf(emptyMap())
        val manager = mockk<SensorManager>(relaxed = true) {
            every { hasSensor(application) } returns true
            coEvery { getAvailableSensors(application) } returns listOf(
                SensorManager.BasicSensor(sensorId, SENSOR_TYPE),
            )
        }
        mockkObject(SensorReceiver.Companion)
        every { SensorReceiver.MANAGERS } returns listOf(manager)
        every { SensorReceiver.updateAllSensors(application) } returns Unit
        return SensorDetailViewModel(
            SavedStateHandle(mapOf(SENSOR_ID_ARGUMENT to sensorId)),
            mockk(relaxed = true),
            dao,
            mockk(relaxed = true),
            mockk(relaxed = true),
            application,
        )
    }
}

private const val SENSOR_ID_ARGUMENT = "id"
private const val SENSOR_TYPE = "sensor"
private const val LAST_NOTIFICATION_SENSOR_ID = "last_notification"
private const val LAST_REMOVED_NOTIFICATION_SENSOR_ID = "last_removed_notification"
private const val NEXT_ALARM_SENSOR_ID = "next_alarm"
private const val ALLOW_LIST_SETTING_NAME = "allow_list"
private const val VISIBLE_PACKAGE = "com.visible"
private const val HIDDEN_PACKAGE = "com.hidden"
private const val BLANK_LABEL_PACKAGE = "com.blank"
private const val VISIBLE_LABEL = "Visible app"
