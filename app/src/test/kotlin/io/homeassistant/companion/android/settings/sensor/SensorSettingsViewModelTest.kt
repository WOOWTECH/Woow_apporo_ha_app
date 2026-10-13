package io.homeassistant.companion.android.settings.sensor

import android.app.Application
import androidx.lifecycle.viewModelScope
import io.homeassistant.companion.android.R
import io.homeassistant.companion.android.common.sensors.SensorManager
import io.homeassistant.companion.android.database.sensor.Sensor
import io.homeassistant.companion.android.database.sensor.SensorDao
import io.homeassistant.companion.android.sensors.SensorReceiver
import io.homeassistant.companion.android.testing.unit.MainDispatcherJUnit4Rule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Covers the sensor list filtering: it has to stay read only, always show the latest query and never
 * hide a sensor that the database has no row for yet.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SensorSettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherJUnit4Rule()

    private val dao = mockk<SensorDao>(relaxed = true)
    private val manager = mockk<SensorManager>()
    private val application = mockk<Application>()
    private val rows = MutableStateFlow(
        listOf(
            Sensor(BATTERY_ID, serverId = 1, enabled = false, state = "old"),
            Sensor(BATTERY_ID, serverId = 2, enabled = true, state = "current"),
            Sensor(ACTIVITY_ID, serverId = 1, enabled = false, state = "still"),
        ),
    )
    private lateinit var viewModel: SensorSettingsViewModel

    @Before
    fun setUp() {
        every { application.getString(MANAGER_NAME_RES) } returns "Sensors 感測器"
        every { application.getString(BATTERY_NAME_RES) } returns "Battery 電池"
        every { application.getString(ACTIVITY_NAME_RES) } returns "Activity 活動"
        every { application.getString(MISSING_NAME_RES) } returns "Missing"
        every { manager.name } returns MANAGER_NAME_RES
        every { manager.hasSensor(application) } returns true
        coEvery { manager.getAvailableSensors(application) } returns listOf(
            SensorManager.BasicSensor(BATTERY_ID, SENSOR_TYPE, name = BATTERY_NAME_RES),
            SensorManager.BasicSensor(ACTIVITY_ID, SENSOR_TYPE, name = ACTIVITY_NAME_RES),
            SensorManager.BasicSensor(MISSING_ID, SENSOR_TYPE, name = MISSING_NAME_RES),
        )
        every { dao.getAllFlow() } returns rows
        mockkObject(SensorReceiver.Companion)
        every { SensorReceiver.MANAGERS } returns listOf(manager)
        viewModel = SensorSettingsViewModel(dao, application, mainDispatcherRule.testDispatcher)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        unmockkAll()
    }

    @Test
    fun `Given English query when filtering and clearing then observed sensors are restored without writes`() {
        assertReadOnlyQuery("bAtTeRy")
    }

    @Test
    fun `Given Chinese query when filtering and clearing then observed sensors are restored without writes`() {
        assertReadOnlyQuery("電池")
    }

    private fun assertReadOnlyQuery(query: String) = runTest {
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID, ACTIVITY_ID), viewModel.sensors.keys)
        assertEquals(setOf(BATTERY_ID, ACTIVITY_ID, MISSING_ID), visibleSensorIds())

        viewModel.setSensorsSearchQuery(query)
        // The query is applied synchronously so a menu rebuilt right now still shows it.
        assertEquals(query, viewModel.searchQuery)
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID), viewModel.sensors.keys)
        assertEquals("current", viewModel.sensors.getValue(BATTERY_ID).state)
        assertEquals(setOf(BATTERY_ID), visibleSensorIds())

        viewModel.setSensorFilterChoice(R.id.action_show_sensors_disabled)
        advanceUntilIdle()
        assertTrue(viewModel.sensors.isEmpty())
        assertTrue(visibleSensorIds().isEmpty())

        viewModel.setSensorFilterChoice(R.id.action_show_sensors_enabled)
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID), viewModel.sensors.keys)
        assertEquals(setOf(BATTERY_ID), visibleSensorIds())

        viewModel.setSensorsSearchQuery("")
        viewModel.setSensorFilterChoice(R.id.action_show_sensors_all)
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID, ACTIVITY_ID), viewModel.sensors.keys)
        assertEquals(setOf(BATTERY_ID, ACTIVITY_ID, MISSING_ID), visibleSensorIds())
        assertNoFilteringSideEffects()
    }

    @Test
    fun `Given rapid queries and changed rows when filtering finishes then only latest state is shown`() = runTest {
        advanceUntilIdle()
        viewModel.setSensorsSearchQuery(BATTERY_ID)
        viewModel.setSensorsSearchQuery(ACTIVITY_ID)
        rows.value = listOf(Sensor(ACTIVITY_ID, serverId = 1, enabled = true, state = "walking"))
        advanceUntilIdle()

        assertEquals(ACTIVITY_ID, viewModel.searchQuery)
        assertEquals(setOf(ACTIVITY_ID), viewModel.sensors.keys)
        assertEquals("walking", viewModel.sensors.getValue(ACTIVITY_ID).state)
        assertEquals(setOf(ACTIVITY_ID), visibleSensorIds())
        assertNoFilteringSideEffects()
    }

    @Test
    fun `Given first empty database emission when listing sensors then metadata is visible without rows`() = runTest {
        // The collector has not run yet, so emptyList is its first emission. No setter triggers filtering.
        rows.value = emptyList()
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID, ACTIVITY_ID, MISSING_ID), visibleSensorIds())
        assertTrue(viewModel.sensors.isEmpty())

        viewModel.setSensorFilterChoice(R.id.action_show_sensors_disabled)
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID, ACTIVITY_ID, MISSING_ID), visibleSensorIds())

        viewModel.setSensorFilterChoice(R.id.action_show_sensors_enabled)
        advanceUntilIdle()
        assertTrue(visibleSensorIds().isEmpty())

        viewModel.setSensorFilterChoice(R.id.action_show_sensors_all)
        viewModel.setSensorsSearchQuery("電池")
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID), visibleSensorIds())

        viewModel.setSensorsSearchQuery("")
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID, ACTIVITY_ID, MISSING_ID), visibleSensorIds())
        assertTrue(viewModel.sensors.isEmpty())
        assertNoFilteringSideEffects()
    }

    @Test
    fun `Given partially populated database when filtering then a sensor without a row stays selectable`() = runTest {
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID, ACTIVITY_ID, MISSING_ID), visibleSensorIds())

        viewModel.setSensorFilterChoice(R.id.action_show_sensors_disabled)
        advanceUntilIdle()
        assertEquals(setOf(ACTIVITY_ID, MISSING_ID), visibleSensorIds())
        assertEquals(setOf(ACTIVITY_ID), viewModel.sensors.keys)

        viewModel.setSensorsSearchQuery(MISSING_ID)
        advanceUntilIdle()
        assertEquals(setOf(MISSING_ID), visibleSensorIds())
        assertTrue(viewModel.sensors.isEmpty())

        viewModel.setSensorFilterChoice(R.id.action_show_sensors_enabled)
        advanceUntilIdle()
        assertTrue(visibleSensorIds().isEmpty())

        viewModel.setSensorsSearchQuery("")
        advanceUntilIdle()
        assertEquals(setOf(BATTERY_ID), visibleSensorIds())

        viewModel.setSensorFilterChoice(R.id.action_show_sensors_all)
        viewModel.setSensorsSearchQuery("Missing")
        advanceUntilIdle()
        assertEquals(setOf(MISSING_ID), visibleSensorIds())
        assertTrue(viewModel.sensors.isEmpty())
        assertNoFilteringSideEffects()
    }

    @Test
    fun `Given sensor activated elsewhere when its row is observed then the enabled list includes it`() = runTest {
        viewModel.setSensorsSearchQuery(MISSING_ID)
        viewModel.setSensorFilterChoice(R.id.action_show_sensors_enabled)
        advanceUntilIdle()
        assertTrue(visibleSensorIds().isEmpty())

        rows.value = rows.value + Sensor(MISSING_ID, serverId = 1, enabled = true, state = "new")
        advanceUntilIdle()
        assertEquals(setOf(MISSING_ID), visibleSensorIds())
        assertEquals("new", viewModel.sensors.getValue(MISSING_ID).state)

        viewModel.setSensorFilterChoice(R.id.action_show_sensors_disabled)
        advanceUntilIdle()
        assertTrue(visibleSensorIds().isEmpty())
        assertNoFilteringSideEffects()
    }

    private fun visibleSensorIds(): Set<String> = viewModel.allSensors.values.flatten().map { it.id }.toSet()

    private fun assertNoFilteringSideEffects() {
        coVerify(exactly = 0) { manager.isEnabled(any(), any()) }
        coVerify(exactly = 0) { manager.checkPermission(any(), any()) }
        coVerify(exactly = 0) { dao.getAnyIsEnabled(any(), any(), any(), any()) }
        coVerify(exactly = 0) { dao.add(any<Sensor>()) }
        coVerify(exactly = 0) { dao.update(any()) }
        coVerify(exactly = 0) { dao.setSensorEnabled(any(), any(), any()) }
    }
}

private const val SENSOR_TYPE = "sensor"
private const val BATTERY_ID = "battery"
private const val ACTIVITY_ID = "activity"
private const val MISSING_ID = "missing"
private const val MANAGER_NAME_RES = 1
private const val BATTERY_NAME_RES = 2
private const val ACTIVITY_NAME_RES = 3
private const val MISSING_NAME_RES = 4
