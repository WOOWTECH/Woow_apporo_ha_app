package io.homeassistant.companion.android.thread

import android.content.Context
import android.content.IntentSender
import androidx.activity.result.ActivityResult
import io.homeassistant.companion.android.common.data.websocket.impl.entities.ThreadDatasetResponse
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope

/**
 * Message reported to callers that try to read this device's preferred dataset. Kept as a constant
 * so the wording is defined once and the reason is greppable if Thread is ever restored.
 */
private const val THREAD_UNSUPPORTED_MESSAGE = "Thread credential sharing is not supported in Apporo aiot"

/**
 * [ThreadManager] implementation for a product that does not ship Thread credential management.
 *
 * Thread is out of scope for the first Apporo aiot release, so this lives in the `main` source set
 * and replaces the former Google Play Services backed `full` implementation as well as the
 * `minimal` one: every flavor reports the same lack of support.
 *
 * [appSupportsThread] returning `false` is what hides the developer settings "Sync Thread
 * credentials" entry and makes the WebView external bus advertise
 * `canImportThreadCredentials: false`, so no caller has to branch on the feature being absent.
 */
class ThreadManagerImpl @Inject constructor() : ThreadManager {

    override fun appSupportsThread(): Boolean = false

    override suspend fun coreSupportsThread(serverId: Int): Boolean = false

    override suspend fun syncPreferredDataset(
        context: Context,
        serverId: Int,
        exportOnly: Boolean,
        scope: CoroutineScope,
    ): ThreadManager.SyncResult = ThreadManager.SyncResult.AppUnsupported

    override suspend fun getPreferredDatasetFromServer(serverId: Int): ThreadDatasetResponse? = null

    override suspend fun importDatasetFromServer(
        context: Context,
        datasetId: String,
        preferredBorderAgentId: String?,
        serverId: Int,
    ) {
        // Nothing to import: the device never holds datasets managed by this app.
    }

    override suspend fun getPreferredDatasetFromDevice(context: Context): IntentSender? {
        throw IllegalStateException(THREAD_UNSUPPORTED_MESSAGE)
    }

    override suspend fun sendThreadDatasetExportResult(result: ActivityResult, serverId: Int): String? = null
}
