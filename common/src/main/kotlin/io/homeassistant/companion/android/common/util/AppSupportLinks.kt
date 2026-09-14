package io.homeassistant.companion.android.common.util

/**
 * Help centre pages the app links to when the user asks for an explanation of a feature.
 *
 * Only explanatory pages belong here. Addresses the app calls or navigates to as part of a feature,
 * such as the invite and redirect endpoints or the privacy policy, are not help links and are kept
 * next to the code that uses them.
 *
 * **These pages are not published yet.** Every address below is a planned path on the new
 * `aiot.apporo.ai` help centre, not a page that has been visited. Before the app is submitted for
 * review, open each one while signed out of the help centre and confirm that it loads for an
 * anonymous reader: a store reviewer follows these links without an account, and a page that is
 * missing or requires a login reads as a broken link in the submission.
 */
object AppSupportLinks {

    /** Root of the help centre. Kept private so every topic is reached through a named constant. */
    private const val HELP_CENTER_BASE_URL = "https://aiot.apporo.ai/help"

    /** First steps after installing the app: creating an account and adding the first home. */
    const val GETTING_STARTED = "$HELP_CENTER_BASE_URL/getting-started"

    /** Connecting the app to a home server, including local and remote addresses. */
    const val CONNECTION = "$HELP_CENTER_BASE_URL/connection"

    /** How to reach support and what information to include when reporting a problem. */
    const val SUPPORT = "$HELP_CENTER_BASE_URL/support"

    /** What notifications the app can show and how to turn them on. */
    const val NOTIFICATIONS = "$HELP_CENTER_BASE_URL/notifications"

    /** Delivering notifications over the local WebSocket connection instead of the push service. */
    const val LOCAL_PUSH = "$HELP_CENTER_BASE_URL/notifications/local-push"

    /** Advanced notification options such as actions, channels and persistent notifications. */
    const val NOTIFICATIONS_ADVANCED = "$HELP_CENTER_BASE_URL/notifications/advanced"

    /** The sensors the app reports and how to enable or configure each one. */
    const val SENSORS = "$HELP_CENTER_BASE_URL/sensors"

    /** Adding and configuring home screen widgets. */
    const val WIDGETS = "$HELP_CENTER_BASE_URL/widgets"

    /** Creating app shortcuts and assistant shortcuts for frequently used actions. */
    const val SHORTCUTS = "$HELP_CENTER_BASE_URL/shortcuts"

    /** Reading and writing NFC tags to trigger actions. */
    const val NFC = "$HELP_CENTER_BASE_URL/nfc"

    /** Why the app asks for camera and microphone access and how that access is used. */
    const val CAMERA_MICROPHONE = "$HELP_CENTER_BASE_URL/camera-and-microphone"
}
