package io.homeassistant.companion.android

import android.content.Context

/**
 * Crash telemetry is intentionally disabled for the Apporo aiot production app.
 *
 * The upstream app ships Sentry here. It was removed by owner decision on 2026-09-15, matching
 * what woowtech aiot already does: this is a white-label app whose users are the customer's
 * customers, so every outbound data flow is one more thing to disclose and defend. Both store
 * privacy questionnaires can now answer "no data collected".
 *
 * Local crash saving is a separate mechanism and still runs — see `initCrashSaving`. A user
 * reporting a problem can still export the log from Settings → Troubleshooting, which covers
 * the case this was mostly wanted for.
 *
 * Re-enabling means more than restoring this function: add the dependency back, restore the
 * three `io.sentry.*` meta-data entries in the full manifest and the `sentryDsn` /
 * `sentryRelease` placeholders, set a DSN that belongs to Apporo (never WOOW's), and declare
 * crash/diagnostic data in both store questionnaires and on https://www.apporo.ai/privacy.
 */
@Suppress("UNUSED_PARAMETER")
fun initCrashReporting(context: Context, enabled: Boolean) {
    // No-op by owner decision.
}
