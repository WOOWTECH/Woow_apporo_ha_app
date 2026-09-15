package io.homeassistant.companion.android.thread

import android.content.Context
import androidx.activity.result.ActivityResult
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

private const val TEST_SERVER_ID = 1
private const val TEST_DATASET_ID = "dataset-1"
private const val TEST_BORDER_AGENT_ID = "0000000000000001"

class ThreadManagerImplTest {

    private val manager = ThreadManagerImpl()

    @Test
    fun `Given Thread is out of scope when querying app support then credential sharing is unsupported`() {
        assertFalse(manager.appSupportsThread())
    }

    @Test
    fun `Given Thread is out of scope when querying core support then the server is never consulted`() = runTest {
        assertFalse(manager.coreSupportsThread(serverId = TEST_SERVER_ID))
    }

    @Test
    fun `Given Thread is out of scope when syncing the preferred dataset then the app reports unsupported`() = runTest {
        assertEquals(
            ThreadManager.SyncResult.AppUnsupported,
            manager.syncPreferredDataset(
                context = mockk<Context>(),
                serverId = TEST_SERVER_ID,
                exportOnly = false,
                scope = backgroundScope,
            ),
        )
    }

    @Test
    fun `Given Thread is out of scope when reading the server dataset then no dataset is returned`() = runTest {
        assertNull(manager.getPreferredDatasetFromServer(serverId = TEST_SERVER_ID))
    }

    @Test
    fun `Given Thread is out of scope when importing a server dataset then nothing is written to the device`() = runTest {
        // The strict mock fails the test if the stub ever reaches out to Google Play Services,
        // which is what "nothing is written" means here.
        manager.importDatasetFromServer(
            context = mockk<Context>(),
            datasetId = TEST_DATASET_ID,
            preferredBorderAgentId = TEST_BORDER_AGENT_ID,
            serverId = TEST_SERVER_ID,
        )
    }

    @Test
    fun `Given Thread is out of scope when reading the device dataset then the call fails instead of prompting`() = runTest {
        val failure = runCatching { manager.getPreferredDatasetFromDevice(mockk<Context>()) }.exceptionOrNull()

        assertInstanceOf(IllegalStateException::class.java, failure)
    }

    @Test
    fun `Given Thread is out of scope when handling an export result then no credential is sent`() = runTest {
        assertNull(
            manager.sendThreadDatasetExportResult(
                result = mockk<ActivityResult>(),
                serverId = TEST_SERVER_ID,
            ),
        )
    }
}
