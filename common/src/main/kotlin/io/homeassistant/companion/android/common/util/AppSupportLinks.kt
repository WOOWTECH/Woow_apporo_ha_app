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

    /**
     * The security levels a connection can be held to, and what the app refuses to do at each one.
     *
     * Explains why a plain text server is blocked unless the user is on the home network, which is
     * the question the onboarding and the block screen send the reader here to answer.
     */
    const val CONNECTION_SECURITY_LEVEL = "$CONNECTION/security-level"

    /**
     * Setting up a client certificate so the server can authenticate the app with mutual TLS.
     *
     * Reached from the watch onboarding, which cannot present a certificate of its own and has to
     * explain why the phone finished the pairing instead.
     */
    const val CONNECTION_CLIENT_CERTIFICATE = "$CONNECTION/client-certificate"

    /**
     * Naming the Wi-Fi networks that count as home so the app uses the internal address on them.
     *
     * Includes what to do when the app keeps choosing the external address on the home network.
     */
    const val HOME_NETWORK = "$CONNECTION/home-network"

    /** How to reach support and what information to include when reporting a problem. */
    const val SUPPORT = "$HELP_CENTER_BASE_URL/support"

    /** What notifications the app can show and how to turn them on. */
    const val NOTIFICATIONS = "$HELP_CENTER_BASE_URL/notifications"

    /** Delivering notifications over the local WebSocket connection instead of the push service. */
    const val LOCAL_PUSH = "$HELP_CENTER_BASE_URL/notifications/local-push"

    /** Advanced notification options such as actions, channels and persistent notifications. */
    const val NOTIFICATIONS_ADVANCED = "$HELP_CENTER_BASE_URL/notifications/advanced"

    /**
     * The section of [NOTIFICATIONS_ADVANCED] about channels.
     *
     * Channels are where the user changes the sound, vibration and importance of one kind of
     * notification without silencing the rest, which is what the channel settings screen links to.
     */
    const val NOTIFICATION_CHANNELS = "$NOTIFICATIONS_ADVANCED#notification-channels"

    /** The sensors the app reports and how to enable or configure each one. */
    const val SENSORS = "$HELP_CENTER_BASE_URL/sensors"

    /**
     * Why location updates stop arriving and how to get the device tracker updating again.
     *
     * Covers the battery optimisation and background permission settings that silently stop
     * location reporting, which is the most common reason a tracker goes stale.
     */
    const val LOCATION_TROUBLESHOOTING = "$HELP_CENTER_BASE_URL/troubleshooting/location"

    /** Adding and configuring home screen widgets. */
    const val WIDGETS = "$HELP_CENTER_BASE_URL/widgets"

    /** Creating app shortcuts and assistant shortcuts for frequently used actions. */
    const val SHORTCUTS = "$HELP_CENTER_BASE_URL/shortcuts"

    /** Exposing entities as Android device controls in the power menu and on the lock screen. */
    const val DEVICE_CONTROLS = "$HELP_CENTER_BASE_URL/device-controls"

    /** Adding entities to the Quick Settings panel as tiles. */
    const val QUICK_SETTINGS_TILES = "$HELP_CENTER_BASE_URL/quick-settings-tiles"

    /** Running an action by swiping on the app, and which gestures can be assigned. */
    const val GESTURES = "$HELP_CENTER_BASE_URL/gestures"

    /** The app on the car screen: which entities it can show and how to choose the favourites. */
    const val ANDROID_AUTO = "$HELP_CENTER_BASE_URL/android-auto"

    /** Reading and writing NFC tags to trigger actions. */
    const val NFC = "$HELP_CENTER_BASE_URL/nfc"

    /**
     * The address format written onto a tag, and how the app is opened by scanning one.
     *
     * The same links work from any app that opens web addresses, so this page also covers sharing
     * a tag address outside of NFC.
     */
    const val NFC_UNIVERSAL_LINKS = "$NFC/universal-links"

    /** Why the app asks for camera and microphone access and how that access is used. */
    const val CAMERA_MICROPHONE = "$HELP_CENTER_BASE_URL/camera-and-microphone"

    /** The app running on a Meta Quest headset, including the sensors that are exclusive to it. */
    const val META_QUEST = "$HELP_CENTER_BASE_URL/meta-quest"

    /** The watch app: pairing it with the phone, and the settings, tiles and favourites it offers. */
    const val WEAR_OS = "$HELP_CENTER_BASE_URL/wear-os"

    /** The sensors reported by the watch app, which are a different set from the phone sensors. */
    const val WEAR_OS_SENSORS = "$WEAR_OS/sensors"

    /** The watch sensors that read from Wear OS Health Services, such as heart rate and calories. */
    const val WEAR_OS_HEALTH_SERVICES = "$WEAR_OS_SENSORS#health-services"

    /**
     * Anchors into the single [SENSORS] page, one per sensor the app can report.
     *
     * Each constant is the section of that page that explains what the sensor measures, what it
     * costs in battery, and which permission it needs. They are grouped in their own namespace
     * because there is one entry per sensor and they would otherwise crowd out the topic links
     * above.
     *
     * Writing the page means writing one section per anchor below: an anchor with no matching
     * heading silently drops the reader at the top of the page instead of at their sensor.
     */
    object Sensors {

        /** The page every anchor below points into. Kept private so call sites use a named sensor. */
        private const val PAGE_URL = AppSupportLinks.SENSORS

        /** Android version, security patch level and other read-only properties of the system. */
        const val ANDROID_OS = "$PAGE_URL#android-os-sensors"

        /** The installed version of this app. */
        const val CURRENT_VERSION = "$PAGE_URL#current-version-sensor"

        /** How much space the app itself occupies, split between code, data and cache. */
        const val APP_DATA = "$PAGE_URL#app-data-sensors"

        /** Heap memory currently used by the app. */
        const val APP_MEMORY = "$PAGE_URL#app-memory-sensor"

        /** How long the app has been in the foreground and how often it was started. */
        const val APP_USAGE = "$PAGE_URL#app-usage-sensors"

        /** Whether Android currently treats the app as foreground, cached or stopped. */
        const val APP_IMPORTANCE = "$PAGE_URL#app-importance-sensor"

        /** Speaker and microphone state: volume levels, ringer mode and headphone connection. */
        const val AUDIO = "$PAGE_URL#audio-sensors"

        /** Charge level, charging state, temperature and power source of the battery. */
        const val BATTERY = "$PAGE_URL#battery-sensors"

        /** Bluetooth radio state and the devices currently connected to it. */
        const val BLUETOOTH = "$PAGE_URL#bluetooth-sensors"

        /** Whether Do Not Disturb is on and which interruption filter it applies. */
        const val DO_NOT_DISTURB = "$PAGE_URL#do-not-disturb-sensor"

        /** Every sensor read from the screen, listed together. */
        const val DISPLAY = "$PAGE_URL#display-sensors"

        /** Current screen brightness and whether the device sets it automatically. */
        const val SCREEN_BRIGHTNESS = "$PAGE_URL#screen-brightness-sensor"

        /** How long the device waits before turning the screen off. */
        const val SCREEN_OFF_TIMEOUT = "$PAGE_URL#screen-off-timeout-sensor"

        /** Whether the screen is currently portrait or landscape. */
        const val SCREEN_ORIENTATION = "$PAGE_URL#screen-orientation-sensor"

        /** The rotation the device reports, independent of what the app chooses to display. */
        const val SCREEN_ROTATION = "$PAGE_URL#screen-rotation-sensor"

        /** Whether the device is locked, and whether a secure lock screen is set up at all. */
        const val KEYGUARD = "$PAGE_URL#keyguard-sensors"

        /** When the device last started up. */
        const val LAST_REBOOT = "$PAGE_URL#last-reboot-sensor"

        /** What caused the most recent sensor update, useful when debugging missing updates. */
        const val LAST_UPDATE_TRIGGER = "$PAGE_URL#last-update-trigger-sensor"

        /** Ambient light measured by the light sensor, in lux. */
        const val LIGHT = "$PAGE_URL#light-sensor"

        /** Whether mobile data is enabled and whether the device is roaming. */
        const val MOBILE_DATA = "$PAGE_URL#mobile-data-sensors"

        /** The public address the device reaches the internet from. */
        const val PUBLIC_IP = "$PAGE_URL#public-ip-sensor"

        /** Which network the device is on and the details of the Wi-Fi it is joined to. */
        const val NETWORK_TYPE = "$PAGE_URL#network-type-sensor"

        /** Whether the active connection is Wi-Fi, mobile, Ethernet or none. */
        const val CONNECTION_TYPE = "$PAGE_URL#connection-type-sensor"

        /** The next alarm the device will ring and which app scheduled it. */
        const val NEXT_ALARM = "$PAGE_URL#next-alarm-sensor"

        /** Whether the NFC radio is switched on. */
        const val NFC_STATE = "$PAGE_URL#nfc-state-sensor"

        /** Whether a call is ringing, active or idle. */
        const val PHONE_STATE = "$PAGE_URL#phone-state-sensor"

        /** The mobile network operator the device is registered with. */
        const val CELLULAR_PROVIDER = "$PAGE_URL#cellular-provider-sensor"

        /** Whether the screen is on and the device is being used. */
        const val INTERACTIVE = "$PAGE_URL#interactive-sensor"

        /** Whether Android has put the device into Doze to save power. */
        const val DOZE = "$PAGE_URL#doze-sensor"

        /** Whether battery saver is switched on. */
        const val POWER_SAVE = "$PAGE_URL#power-save-sensor"

        /** Atmospheric pressure measured by the barometer. */
        const val PRESSURE = "$PAGE_URL#pressure-sensor"

        /** Distance to the nearest object in front of the proximity sensor. */
        const val PROXIMITY = "$PAGE_URL#proximity-sensor"

        /** Steps counted by the built-in pedometer. */
        const val PEDOMETER = "$PAGE_URL#pedometer-sensors"

        /** Free and total space on internal and external storage. */
        const val STORAGE = "$PAGE_URL#storage-sensor"

        /** The time zone the device is currently set to. */
        const val TIME_ZONE = "$PAGE_URL#current-time-zone-sensor"

        /** Bytes sent and received over mobile and Wi-Fi since the device last started. */
        const val TRAFFIC_STATS = "$PAGE_URL#traffic-stats-sensor"

        /** Whether a managed work profile exists on the device. */
        const val WORK_PROFILE = "$PAGE_URL#work-profile-sensor"

        /** The accent colour Android derives from the wallpaper on Android 12 and newer. */
        const val DYNAMIC_COLOR = "$PAGE_URL#dynamic-color-sensor"

        /**
         * Where the device is: background updates, zone entry and exit, and the single accurate
         * reading the server can ask for on demand.
         */
        const val LOCATION = "$PAGE_URL#location-sensors"

        /** The address the current location resolves to. */
        const val GEOCODED_LOCATION = "$PAGE_URL#geocoded-location-sensor"

        /** Health and fitness readings the app reads from Health Connect. */
        const val HEALTH_CONNECT = "$PAGE_URL#health-connect-sensors"

        /** The app that was most recently in the foreground. */
        const val LAST_USED_APP = "$PAGE_URL#last-used-app-sensor"

        /** Every sensor read from the notification shade, listed together. */
        const val NOTIFICATION = "$PAGE_URL#notification-sensors"

        /** The most recent notification the device received. */
        const val LAST_NOTIFICATION = "$PAGE_URL#last-notification"

        /** The most recent notification the user dismissed. */
        const val LAST_REMOVED_NOTIFICATION = "$PAGE_URL#last-removed-notification"

        /** How many notifications are currently showing. */
        const val ACTIVE_NOTIFICATION_COUNT = "$PAGE_URL#active-notification-count"

        /** What is playing, in which app, and how far through it is. */
        const val MEDIA_SESSION = "$PAGE_URL#media-session-sensor"

        /** What the device thinks the user is doing: walking, driving, still and so on. */
        const val ACTIVITY = "$PAGE_URL#activity-sensors"
    }
}
