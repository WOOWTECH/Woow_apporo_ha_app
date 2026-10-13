package io.homeassistant.companion.android.matter

import android.content.Context
import android.content.IntentSender
import io.homeassistant.companion.android.common.data.websocket.impl.entities.MatterCommissionResponse
import javax.inject.Inject

/**
 * Message reported to callers that try to start a commissioning flow. Kept as a constant so the
 * wording is defined once and the reason is greppable if Matter is ever restored.
 */
private const val MATTER_UNSUPPORTED_MESSAGE = "Matter commissioning is not supported in Apporo aiot"

/**
 * [MatterManager] implementation for a product that does not ship Matter commissioning.
 *
 * Matter is out of scope for the first Apporo aiot release, so this lives in the `main` source set
 * and replaces the former Google Play Services backed `full` implementation as well as the
 * `minimal` one: every flavor reports the same lack of support. Every capability query answers
 * `false`/`null`, which is what callers such as the WebView external bus already handle, so
 * nothing needs to branch on the feature being absent.
 */
class MatterManagerImpl @Inject constructor() : MatterManager {

    override fun appSupportsCommissioning(): Boolean = false

    override suspend fun coreSupportsCommissioning(serverId: Int): Boolean = false

    override fun suppressDiscoveryBottomSheet(context: Context) {
        // Nothing to suppress: the app never registers with the Google Home commissioning flow.
    }

    override fun startNewCommissioningFlow(
        context: Context,
        onSuccess: (IntentSender) -> Unit,
        onFailure: (Exception) -> Unit,
    ) {
        onFailure(IllegalStateException(MATTER_UNSUPPORTED_MESSAGE))
    }

    override suspend fun commissionDevice(code: String, serverId: Int): MatterCommissionResponse? = null

    override suspend fun commissionOnNetworkDevice(pin: Long, ip: String, serverId: Int): MatterCommissionResponse? =
        null
}
