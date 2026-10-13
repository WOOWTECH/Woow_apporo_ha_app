package io.homeassistant.companion.android.util

/**
 * First-release scope for sensor families, expressed as a release policy rather than a user
 * preference.
 *
 * Why a flag instead of deleting the sensor managers: the implementations are upstream code that we
 * want to keep mergeable, and the decision to ship health and fitness data collection is a product
 * and store-review decision that can be revisited. Deleting the managers would make that decision
 * expensive to reverse and would create merge conflicts on every upstream sync.
 *
 * Why the flag is read where the manager list is built (see
 * `io.homeassistant.companion.android.sensors.SensorReceiver.MANAGERS`) rather than inside each
 * manager: every consumer of the sensor subsystem goes through that single list — the broadcast
 * receiver that collects and uploads values, the sensor settings list, the sensor detail screen, and
 * the WebView bridge. Removing an entry from the list therefore disables the sensor everywhere at
 * once, including for a user who had previously opted in, without having to touch the shared
 * `SensorManager` interface in `:common`.
 *
 * The manifest half of this exclusion is already in place: `app/src/main/AndroidManifest.xml`
 * removes the `android.permission.health.*` permissions, `android.permission.ACTIVITY_RECOGNITION`,
 * the `com.google.android.apps.healthdata` package query, and the `HealthConnectPermissionActivity`
 * declaration. Without the flags below, the managers would still be registered while the
 * permissions they ask for can never be granted, and enabling the Health Connect sensor would try to
 * start an activity that is no longer in the manifest.
 *
 * Re-enabling either family requires restoring the matching manifest declarations first, then
 * validating that permissions are actually granted and that values are collected in a signed build.
 *
 * These are deliberately not declared `const`: a compile-time constant would let the compiler fold
 * the checks away and report the disabled branches as unreachable, and it would inline the current
 * value into every call site. Keeping them plain properties keeps the flag readable as a single
 * switch and keeps the guarded code compiled and type-checked.
 */
internal object SensorReleasePolicy {

    /**
     * Health Connect sensors (`HealthConnectSensorManager`).
     *
     * Requires the `android.permission.health.*` permissions and the Health Connect permission
     * activity, both removed from the manifest for the first release.
     */
    val healthConnectEnabled: Boolean = false

    /**
     * Fitness and activity sensors (`StepsSensorManager`, `ActivitySensorManager`).
     *
     * Both require `android.permission.ACTIVITY_RECOGNITION`, which is removed from the manifest for
     * the first release.
     */
    val fitnessSensorsEnabled: Boolean = false
}
