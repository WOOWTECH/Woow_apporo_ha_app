package io.homeassistant.companion.android.matter

import android.content.Context
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

private const val TEST_SERVER_ID = 1
private const val TEST_PAIRING_CODE = "12345678901"
private const val TEST_DEVICE_IP = "192.0.2.1"
private const val TEST_DEVICE_PIN = 20202021L

class MatterManagerImplTest {

    private val manager = MatterManagerImpl()

    @Test
    fun `Given Matter is out of scope when querying app support then commissioning is unsupported`() {
        assertFalse(manager.appSupportsCommissioning())
    }

    @Test
    fun `Given Matter is out of scope when querying core support then the server is never consulted`() = runTest {
        assertFalse(manager.coreSupportsCommissioning(serverId = TEST_SERVER_ID))
    }

    @Test
    fun `Given Matter is out of scope when commissioning a device then no response is returned`() = runTest {
        assertNull(manager.commissionDevice(code = TEST_PAIRING_CODE, serverId = TEST_SERVER_ID))
        assertNull(
            manager.commissionOnNetworkDevice(
                pin = TEST_DEVICE_PIN,
                ip = TEST_DEVICE_IP,
                serverId = TEST_SERVER_ID,
            ),
        )
    }

    @Test
    fun `Given Matter is out of scope when starting a commissioning flow then it fails without an intent`() {
        var failure: Exception? = null

        manager.startNewCommissioningFlow(
            context = mockk<Context>(),
            onSuccess = { fail<Unit>("The commissioning flow must not be offered to the user") },
            onFailure = { failure = it },
        )

        assertInstanceOf(IllegalStateException::class.java, failure)
    }

    @Test
    fun `Given Matter is out of scope when suppressing the discovery sheet then nothing is requested`() {
        // A relaxed mock would record calls; a strict one fails the test if the stub touches
        // Google Play Services, which is exactly what must not happen.
        manager.suppressDiscoveryBottomSheet(mockk<Context>())
    }
}
