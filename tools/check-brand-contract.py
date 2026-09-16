#!/usr/bin/env python3
"""Offline brand contract tests for Apporo aiot; no Android SDK, no Gradle, no secrets.

Run: python3 tools/check-brand-contract.py
     python3 tools/check-brand-contract.py --oauth-fixture
     python3 tools/check-brand-contract.py --residue
     curl -fsSL -A HomeAssistant https://www.apporo.ai/android |
         python3 tools/check-brand-contract.py --oauth-stdin

WHY THIS FILE EXISTS
--------------------
Ported from the sibling white label `woow_ha_app` (`tools/check-brand-contract.py`), which
had it; this repo did not. That asymmetry cost us on 2026-09-16, when a review found the
Apporo user-visible copy had drifted badly away from the branded baseline:

  * brand words replaced by generic ones — "您的 woowtech aiot 伺服器" had become
    "您的智慧家庭伺服器", so the product name vanished from the onboarding text;
  * `welcome_home_assistant_title` truncated and hard-wrapped — `woowtech aiot app`
    had become `Apporo\naiot`, dropping the word "app" and baking a line break into a
    string the layout is supposed to wrap itself;
  * upstream residue left in shipping copy — `HA: Assist` and friends.

178 Android strings (112 zh-rTW, 66 en) and 13 iOS strings had to be repaired by hand.
None of it was caught by a compiler, a linter or a unit test, because none of them know
what the product is called. This file does: it hard-asserts the exact user-visible brand
strings, the app identity, the deep-link scheme, the OAuth callbacks and the help-centre
host, so the same class of drift fails a 200 ms Python run instead of shipping.

These checks do NOT replace Gradle, the merged manifest, UI tests or a real HA OAuth login.

`KnownOpenDefectTest` at the bottom holds the copy defects this port found and did not fix.
They are real assertions marked `expectedFailure`, so the run stays green while each defect
stays named in the output; fixing one turns it into an UNEXPECTED SUCCESS and reddens the
run, which is the prompt to promote the assertion into the contract above.

DELIBERATE DIFFERENCES FROM THE woowtech VERSION (do not "fix" these back)
-------------------------------------------------------------------------
  * `app_name` in values-zh-rTW is the English "Apporo aiot", not a Chinese trade name.
    woowtech uses "渥屋科技"; Apporo's brand name is not localised.
  * Wear OS callbacks (`https://wear.googleapis.com/3p_auth/...`) are NOT in
    ALLOWED_CALLBACKS. Wear phone sign-in is out of scope for the first Apporo release and
    docs/android/index.html says so explicitly. Add them here and there together.
  * The deep-link scheme is a manifest placeholder (`${deepLinkScheme}`) fed by
    `apporoDeepLinkScheme` in gradle.properties, so release gets `apporoaiot://` and debug
    gets `apporoaiot-dev://` and the two can sit on one device. woowtech hard-codes it, so
    its assertions look for a literal scheme in the source manifest; ours must not.
  * `input_cloud` says "Nabu Casa Cloud". Apporo has no cloud of its own; that toggle
    connects to Nabu Casa's Home Assistant Cloud (see ServerConnectionInfo.kt:20).
  * `open_source_credits` keeps "Home Assistant Companion App (Apache 2.0 License)". That
    is legally required attribution and is the one key excluded from the upstream-name ban.
  * `AppSupportLinks` is a nested catalogue under https://www.apporo.ai/help, not
    woowtech's flat Odoo `blog/help-center-7/<slug>` list, so the link test checks the host
    and base instead of an exact slug table.
  * `HaCarAppService` is still declared in app/src/full — woowtech removed Android Auto from
    the phone app, Apporo keeps the upstream behaviour. Not a branding matter.
"""

import json
from html.parser import HTMLParser
from pathlib import Path
import re
import subprocess
import sys
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
APP = "app/src/main/kotlin/io/homeassistant/companion/android/"
COMMON = "common/src/main/kotlin/io/homeassistant/companion/android/"
ANDROID = "{http://schemas.android.com/apk/res/android}"

BRAND_NAME = "Apporo aiot"
APPLICATION_ID = "com.apporo.aiot"
BRAND_HOST = "www.apporo.ai"
DEEP_LINK_SCHEME = "apporoaiot"
DEBUG_SCHEME_SUFFIX = "-dev"
HELP_CENTER_BASE_URL = f"https://{BRAND_HOST}/help"
OAUTH_CLIENT_ID = f"https://{BRAND_HOST}/android"
PRIVACY_URL = f"https://{BRAND_HOST}/privacy"

# Release and debug builds register different schemes so both can be installed side by side
# without Android raising a disambiguation dialog. Wear is deliberately absent; see the
# module docstring.
ALLOWED_CALLBACKS = (
    f"{DEEP_LINK_SCHEME}://auth-callback",
    f"{DEEP_LINK_SCHEME}{DEBUG_SCHEME_SUFFIX}://auth-callback",
)

# The merged manifest is a build artifact. It is checked when present and skipped otherwise,
# so this file stays runnable on a machine that has never run Gradle.
MERGED_MANIFEST = ROOT / (
    "app/build/intermediates/merged_manifests/fullRelease/"
    "processFullReleaseManifest/AndroidManifest.xml"
)


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def source_files():
    for module in ("app", "automotive", "common", "wear"):
        for path in (ROOT / module / "src").rglob("*"):
            if path.suffix in (".kt", ".xml"):
                yield path


def localized_strings(locale):
    xml = ET.fromstring(read(f"common/src/main/res/{locale}/strings.xml"))
    return {e.get("name"): "".join(e.itertext()) for e in xml.findall("string")}


class OAuthMetadata(HTMLParser):
    def __init__(self):
        super().__init__()
        self.in_head = False
        self.redirects = []

    def handle_starttag(self, tag, attrs):
        if tag == "head":
            self.in_head = True
        attrs = dict(attrs)
        if tag == "link" and self.in_head and "redirect_uri" in attrs.get("rel", "").split():
            self.redirects.append(attrs.get("href"))

    def handle_endtag(self, tag):
        if tag == "head":
            self.in_head = False


def validate_oauth(html):
    parser = OAuthMetadata()
    parser.feed(html)
    if len(parser.redirects) != len(ALLOWED_CALLBACKS) or set(parser.redirects) != set(ALLOWED_CALLBACKS):
        raise ValueError(
            f"Expected exactly the {len(ALLOWED_CALLBACKS)} approved head redirect_uri links, "
            f"got {parser.redirects}"
        )


def oauth_html(callbacks):
    return '<head>' + ''.join(f'<link rel="redirect_uri" href="{uri}">' for uri in callbacks) + '</head>'


class BrandContractTest(unittest.TestCase):
    def test_application_id_and_namespace(self):
        convention = read("build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt")
        self.assertIn(f'APPLICATION_ID = "{APPLICATION_ID}"', convention)
        self.assertIn('NAMESPACE = "io.homeassistant.companion.android"', convention)
        self.assertIn('applicationIdSuffix = ".debug"', convention)
        flavor = read("build-logic/convention/src/main/kotlin/AndroidFullMinimalFlavorConventionPlugin.kt")
        self.assertRegex(flavor, r'create\("full"\)\s*\{\s*applicationIdSuffix = ""')
        self.assertIn('applicationIdSuffix = ".minimal"', flavor)
        for module in ("app", "automotive", "wear"):
            gradle = read(f"{module}/build.gradle.kts")
            self.assertIn("alias(libs.plugins.homeassistant.android.application)", gradle)
            self.assertNotRegex(gradle, r"applicationId\s*=")
        self.assertIn(f'package_name("{APPLICATION_ID}")', read("fastlane/Appfile"))

    def test_ci_mock_covers_install_ids_without_real_credentials(self):
        mock = json.loads(read(".github/mock-google-services.json"))
        packages = {c["client_info"]["android_client_info"]["package_name"] for c in mock["client"]}
        self.assertEqual(packages, {APPLICATION_ID + s for s in ("", ".debug", ".minimal", ".minimal.debug")})
        self.assertEqual(mock["project_info"]["project_id"], "project_id")
        self.assertTrue(all(c["api_key"][0]["current_key"] == "current_key" for c in mock["client"]))

    def test_deep_link_scheme_is_one_value_fed_from_gradle_properties(self):
        # Kotlin, the manifest and the OAuth page must all derive from this single property.
        properties = read("gradle.properties")
        self.assertIn(f"apporoDeepLinkScheme={DEEP_LINK_SCHEME}", properties)
        self.assertIn(f"apporoDeepLinkSchemeDebugSuffix={DEBUG_SCHEME_SUFFIX}", properties)
        convention = read("build-logic/convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt")
        self.assertIn('providers.gradleProperty("apporoDeepLinkScheme")', convention)
        self.assertIn('manifestPlaceholders["deepLinkScheme"]', convention)
        self.assertIn('const val BRAND_HOST = "www.apporo.ai"', read(COMMON + "util/UrlUtil.kt"))
        self.assertIn('val DEEP_LINK_SCHEME: String = BuildConfig.DEEP_LINK_SCHEME', read(COMMON + "util/UrlUtil.kt"))

    def test_callback_matches_reference_metadata(self):
        connection = read(APP + "onboarding/connection/ConnectionViewModel.kt")
        self.assertIn('AUTH_CALLBACK_SCHEME = DEEP_LINK_SCHEME', connection)
        self.assertIn('AUTH_CALLBACK_HOST = "auth-callback"', connection)
        self.assertIn(f'CLIENT_ID = "{OAUTH_CLIENT_ID}"',
                      read(COMMON + "common/data/authentication/impl/AuthenticationService.kt"))
        validate_oauth(read("docs/android/index.html"))

    def test_mobile_app_registration_identity_and_push_boundary(self):
        registration = read(COMMON + "common/data/integration/impl/IntegrationRepositoryImpl.kt")
        self.assertIn(f'APP_ID = "{APPLICATION_ID}"', registration)
        self.assertIn(f'APP_NAME = "{BRAND_NAME}"', registration)
        self.assertIn('request.appId = APP_ID', registration)
        self.assertIn('request.appName = APP_NAME', registration)
        self.assertIn('deviceRegistration.appVersion ?: oldDeviceRegistration.appVersion', registration)
        self.assertIn('appData["push_url"] = PUSH_URL', registration)
        self.assertIn('private const val PUSH_URL = BuildConfig.PUSH_URL', registration)
        serializer = read(COMMON + "common/data/integration/impl/entities/RegisterDeviceRequest.kt")
        self.assertIn('element("app_id", String.serializer().descriptor)', serializer)
        self.assertIn('element("app_name", String.serializer().descriptor)', serializer)
        self.assertNotIn('APP_ID = "io.homeassistant.companion.android"', registration)

    def test_push_and_rate_limit_endpoints_stay_on_the_brand_host(self):
        properties = read("gradle.properties")
        self.assertIn(f"homeAssistantAndroidPushUrl=https://{BRAND_HOST}/", properties)
        self.assertIn(f"homeAssistantAndroidRateLimitUrl=https://{BRAND_HOST}/", properties)

    def test_oauth_validator_rejects_old_scheme_and_body_metadata(self):
        for html in (
            '<head><link rel="redirect_uri" href="homeassistant://auth-callback"></head>',
            f'<body><link rel="redirect_uri" href="{ALLOWED_CALLBACKS[0]}"></body>',
            '<head></head>',
        ):
            with self.subTest(html=html), self.assertRaises(ValueError):
                validate_oauth(html)

    def test_oauth_validator_accepts_exact_callbacks_in_any_order(self):
        validate_oauth(oauth_html(ALLOWED_CALLBACKS))
        validate_oauth(oauth_html(reversed(ALLOWED_CALLBACKS)))

    def test_oauth_validator_rejects_missing_callbacks_including_release_only_deployment(self):
        for callback in ALLOWED_CALLBACKS:
            with self.subTest(missing=callback), self.assertRaises(ValueError):
                validate_oauth(oauth_html(uri for uri in ALLOWED_CALLBACKS if uri != callback))
        with self.assertRaises(ValueError):
            validate_oauth(oauth_html(ALLOWED_CALLBACKS[:1]))

    def test_oauth_validator_rejects_extra_duplicate_wildcard_or_body_callbacks(self):
        # The Wear entry is rejected on purpose: turning Wear sign-in back on means editing
        # ALLOWED_CALLBACKS and docs/android/index.html in the same change, not one of them.
        for extra in (ALLOWED_CALLBACKS[0],
                      f'https://wear.googleapis.com/3p_auth/{APPLICATION_ID}',
                      f'{DEEP_LINK_SCHEME}://*',
                      'homeassistant://auth-callback'):
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                validate_oauth(oauth_html((*ALLOWED_CALLBACKS, extra)))
        with self.assertRaises(ValueError):
            validate_oauth(oauth_html(ALLOWED_CALLBACKS).replace('head', 'body'))

    def test_all_source_xml_parses(self):
        for path in source_files():
            if path.suffix == ".xml":
                with self.subTest(path=str(path.relative_to(ROOT))):
                    ET.parse(path)

    def test_manifest_identity_and_functional_deep_links(self):
        for module in ("app", "automotive"):
            manifest = ET.fromstring(read(f"{module}/src/main/AndroidManifest.xml"))
            schemes = {e.get(ANDROID + "scheme") for e in manifest.iter("data")}
            # The literal scheme is injected per build type, so the source manifest must
            # carry the placeholder and never a hard-coded scheme of its own.
            self.assertIn("${deepLinkScheme}", schemes)
            self.assertNotIn("homeassistant", schemes)
            self.assertNotIn(DEEP_LINK_SCHEME, schemes)
            for provider in manifest.iter("provider"):
                authority = provider.get(ANDROID + "authorities")
                self.assertTrue(authority.startswith("${applicationId}"), authority)
            for element in manifest.iter():
                if ANDROID + "taskAffinity" in element.attrib:
                    self.assertTrue(element.get(ANDROID + "taskAffinity").startswith("${applicationId}"))
            for path in ("/tag/", "/invite/", "/redirect/"):
                self.assertTrue(any(e.get(ANDROID + "host") == BRAND_HOST and e.get(ANDROID + "pathPrefix") == path
                                    for e in manifest.iter("data")), f"{module}: missing {path}")
            tag_reader = next(
                activity for activity in manifest.iter("activity")
                if activity.get(ANDROID + "name") == ".nfc.TagReaderActivity"
            )
            ndef_hosts = {
                data.get(ANDROID + "host")
                for intent_filter in tag_reader.findall("intent-filter")
                if any(
                    action.get(ANDROID + "name") == "android.nfc.action.NDEF_DISCOVERED"
                    for action in intent_filter.findall("action")
                )
                for data in intent_filter.findall("data")
            }
            self.assertEqual(ndef_hosts, {BRAND_HOST, "www.home-assistant.io"})
        self.assertIn('import io.homeassistant.companion.android.util.DEEP_LINK_SCHEME',
                      read(APP + "launch/link/LinkHandler.kt"))
        self.assertIn('DEEP_LINK_SCHEME -> handleDeepLink(uri)', read(APP + "launch/link/LinkHandler.kt"))
        nfc_urls = read(COMMON + "util/UrlUtil.kt")
        self.assertIn('NAVIGATE_DEEP_LINK_PREFIX = "$DEEP_LINK_SCHEME://navigate/"', nfc_urls)
        self.assertIn('LEGACY_NFC_TAG_HOST = "www.home-assistant.io"', nfc_urls)
        self.assertIn('NFC_TAG_HOSTS = listOf(BRAND_HOST, LEGACY_NFC_TAG_HOST)', nfc_urls)
        self.assertIn('UrlUtil.buildNfcTagUri(nfcTagToWriteUUID!!)', read(APP + "nfc/NfcSetupActivity.kt"))
        self.assertIn('BASE_INVITE_URL = "https://$BRAND_HOST/invite/#"',
                      read(APP + "settings/server/ServerSettingsFragment.kt"))

    def test_merged_release_manifest_resolves_to_the_branded_scheme(self):
        # Build artifact: checked when a fullRelease manifest has already been produced.
        if not MERGED_MANIFEST.exists():
            self.skipTest("merged fullRelease manifest not built; run the app build to cover this")
        manifest = ET.parse(MERGED_MANIFEST).getroot()
        self.assertEqual(manifest.get("package"), APPLICATION_ID)
        schemes = {e.get(ANDROID + "scheme") for e in manifest.iter("data")}
        self.assertIn(DEEP_LINK_SCHEME, schemes)
        for rejected in ("homeassistant", "woowtech", f"{DEEP_LINK_SCHEME}{DEBUG_SCHEME_SUFFIX}", "${deepLinkScheme}"):
            self.assertNotIn(rejected, schemes)
        hosts = {e.get(ANDROID + "host") for e in manifest.iter("data")}
        self.assertIn(BRAND_HOST, hosts)
        self.assertNotIn("aiot.woowtech.io", hosts)

    def test_wear_signin_stays_paired_with_branded_phone(self):
        watch = read("wear/src/main/kotlin/io/homeassistant/companion/android/onboarding/OnboardingActivity.kt")
        self.assertIn('WEAR_PHONE_SIGN_IN_HOST = "wear-phone-signin"', watch)
        self.assertIn('"$DEEP_LINK_SCHEME://$WEAR_PHONE_SIGN_IN_HOST', watch)
        phone = read("app/src/full/kotlin/io/homeassistant/companion/android/settings/wear/SettingsWearActivity.kt")
        self.assertIn('it.scheme == DEEP_LINK_SCHEME', phone)
        self.assertIn('it.host == "wear-phone-signin"', phone)
        for presenter in ('OnboardingPresenterImpl.kt', 'manual/ManualSetupPresenterImpl.kt'):
            source = read('wear/src/main/kotlin/io/homeassistant/companion/android/onboarding/' + presenter)
            self.assertIn('OAuthRequest.Builder(context)', source)
            self.assertNotIn('setRedirectUrl', source)
            self.assertNotIn('://auth-callback', source)

    def test_published_topic_catalog_and_contact_button(self):
        catalog = read(COMMON + "common/util/AppSupportLinks.kt")
        self.assertIn(f'HELP_CENTER_BASE_URL = "{HELP_CENTER_BASE_URL}"', catalog)
        # Every published link must be built from the base, never from a second host.
        constants = dict(re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', catalog))
        self.assertTrue(constants, "AppSupportLinks exposes no constants")
        for name, value in constants.items():
            with self.subTest(link=name):
                self.assertRegex(
                    value,
                    r'^(https://www\.apporo\.ai/help|\$(HELP_CENTER_BASE_URL|CONNECTION|NOTIFICATIONS_ADVANCED'
                    r'|NFC|SENSORS|WEAR_OS|WEAR_OS_SENSORS|PAGE_URL|AppSupportLinks\.SENSORS))',
                )
                self.assertNotIn("home-assistant.io", value)
                self.assertNotIn("woowtech", value)
        for url in re.findall(r'https?://[^\s"\')]+', catalog):
            self.assertTrue(url.startswith(HELP_CENTER_BASE_URL), url)
        screen = read(APP + "onboarding/connection/ConnectionErrorScreen.kt")
        self.assertIn("AppSupportLinks.SUPPORT.toUri()", screen)
        self.assertIn("Icons.Outlined.SupportAgent", screen)
        self.assertIn("connection_error_support_content_description", screen)
        for old in ("discord", "github", "URL_COMMUNITY_FORUM"):
            self.assertNotIn(old, screen)

    def test_primary_locale_identity_and_provenance(self):
        for locale in ("values", "values-zh-rTW"):
            strings = localized_strings(locale)
            taiwan = locale == "values-zh-rTW"
            # Apporo's brand name is not localised; both locales carry the English name.
            self.assertEqual(strings["app_name"], BRAND_NAME)
            self.assertEqual(strings["welcome_home_assistant_title"], f"{BRAND_NAME} app")
            self.assertEqual(strings["welcome_details"], (
                r"隨時隨地存取您的 Apporo aiot 伺服器。\n\nApporo aiot 倡導隱私資安保護並於您的家中進行本地端運行。"
                if taiwan else
                r"Access your Apporo aiot server on the go.\n\nApporo aiot puts privacy and security first and runs locally in your home."
            ))
            self.assertEqual(strings["welcome_connect_to_ha"],
                             "連線至我的 Apporo aiot" if taiwan else "Connect to my Apporo aiot")
            # Apporo runs no cloud of its own; this setting reaches Nabu Casa's HA Cloud.
            self.assertEqual(strings["input_cloud"],
                             "使用 Nabu Casa 雲端服務" if taiwan else "Use Nabu Casa Cloud")
            # Required upstream attribution, and the only key exempt from the name ban below.
            self.assertEqual(strings["open_source_credits"], (
                "基於 Home Assistant Companion App（Apache 2.0 授權）" if taiwan else
                "Based on Home Assistant Companion App (Apache 2.0 License)"
            ))
            self.assertEqual(strings["companion_app"], BRAND_NAME)
            self.assertEqual(strings["privacy_url"], PRIVACY_URL)
            self.assertIn(BRAND_NAME, strings["connection_check_home_assistant"])
            self.assertIn(BRAND_NAME, strings["connection_error_support_content_description"])
            # Deliberately retained unused resources; the push relay rate-limit path uses them.
            for key in ("rate_limit_notification_title", "rate_limit_notification_body"):
                self.assertIn(key, strings)
        self.assertEqual(read("fastlane/metadata/android/en-US/title.txt").strip(), BRAND_NAME)
        description = read("fastlane/metadata/android/en-US/full_description.txt")
        for unsupported_claim in ("Home Green", "Home Cloud", "1 million", "Matter", "Thread"):
            self.assertNotIn(unsupported_claim, description)

    def test_visible_resource_names_keep_only_legal_upstream_attribution(self):
        # Inspect values, not resource keys, styles, endpoints or Kotlin identifiers.
        upstream_name = re.compile(r"home(?:\s|\\n)*assistant|\bHA\b", re.IGNORECASE)
        for path in source_files():
            if path.suffix != ".xml" or not path.parent.name.startswith("values"):
                continue
            for element in ET.parse(path).getroot():
                if element.tag not in ("string", "string-array", "plurals"):
                    continue
                if element.get("name") == "open_source_credits":
                    continue
                value = "".join(element.itertext())
                value = re.sub(r"https?://[^\s<>]+", "", value)
                with self.subTest(path=str(path.relative_to(ROOT)), key=element.get("name")):
                    self.assertNotRegex(value, upstream_name)
                    if element.get("name") == "welcome_home_assistant_title":
                        # Exact equality: the 2026-09-16 drift wrote "Apporo\naiot" here.
                        self.assertEqual(value, f"{BRAND_NAME} app")
                        self.assertNotIn("\n", value)
                        self.assertNotIn(r"\n", value)

    def test_shared_ios_copy_for_onboarding_settings_notifications_and_assist(self):
        # Canonical shared meanings from iOS Localizable.strings, not iOS permission instructions.
        expected = {
            "name_your_device_title": ("How would you like to name this device?", "為裝置命名？"),
            "name_your_device_content": (f"This is used to identify your device in your {BRAND_NAME}.",
                                         f"用以於 {BRAND_NAME} 中識別裝置。"),
            "manual_server_title": (f"What is your {BRAND_NAME} address?", f"{BRAND_NAME} 網址是什麼"),
            "set_home_network_title": ("What is your home network?", "您的家庭網路名稱？"),
            "connection_error_more_details": ("More details", "更多資訊"),
            "clear_webview_cache": ("Reset frontend cache", "重置前端快取"),
            "confirm_delete_all_notification_title": (
                "Are you sure you want to clear the notification history?", "確定要清除通知記錄嗎？"),
            "confirm_delete_all_notification_message": ("This cannot be undone.", "此動作無法復原。"),
            # "HA: Assist" was the 2026-09-16 residue here.
            "assist": ("Assist", "助理"),
            "gestures_category_app": ("App", "App"),
            "sensors": ("Sensors", "感測器"),
            "widget_todo_label": ("To-do List", "待辦事項列表"),
        }
        for index, locale in enumerate(("values", "values-zh-rTW")):
            strings = localized_strings(locale)
            for key, values in expected.items():
                with self.subTest(locale=locale, key=key):
                    self.assertEqual(strings[key], values[index])
        # 位址輸入框的提示。**這一條刻意與 woowtech 不同**：
        # woowtech 兩個語系都寫 `https://<ipaddress>:8123`，Apporo 用 `http://`。
        # 理由是功能性的 —— 區網 IP 幾乎不可能有有效憑證，提示寫 https 會把使用者
        # 導向憑證驗證失敗。Owner 於 2026-09-16 拍板。
        # 兩個語系必須一字不差（先前 en 與 zh-rTW 曾經各寫各的）。
        for locale in ("values", "values-zh-rTW"):
            with self.subTest(locale=locale, key="input_url_hint"):
                self.assertEqual(localized_strings(locale)["input_url_hint"],
                                 "http://<ipaddress>:8123")

        # ⚠️ 上線流程第一個輸入框的提示**曾經被寫死在 Kotlin 裡**
        # （`ManualServerScreen.kt` 的 placeholder = "https://<ipaddress>"），
        # 繞過字串資源。後果是改 `input_url_hint` 對那個畫面完全無效 ——
        # 2026-09-16 在 Pixel 實機跑正式簽章版才發現，而它是使用者看到的第一個欄位。
        # 這條斷言擋住「把可見文案寫死」這個類別的回歸，不只擋這一個字串。
        screen = (ROOT / (APP + "onboarding/manualserver/ManualServerScreen.kt")
                  ).read_text(encoding="utf-8")
        self.assertNotIn('"https://<ipaddress>"', screen,
                         "ManualServerScreen 又把位址提示寫死了；請改用 commonR.string.input_url_hint")
        self.assertIn("stringResource(commonR.string.input_url_hint)", screen,
                      "ManualServerScreen 的 placeholder 必須讀 input_url_hint")

        # Compliance wording the permission screen has to keep saying.
        default = localized_strings("values")
        self.assertIn("Wi-Fi network detection requires location permission on supported Android versions.",
                      default["location_secure_connection_hint"])
        self.assertIn("Location sensor sharing is configured separately.",
                      default["location_secure_connection_hint"])
        self.assertIn("Sensors will update on a 15 minute interval.",
                      read("common/src/main/res/values/strings.xml"))
        # `add_action_data_field` exists in both locales; the upstream literal "Field" showed a
        # bare English word in the middle of a translated screen until 2026-09-16.
        widget = read(APP + "widgets/button/ButtonWidgetConfigureActivity.kt")
        self.assertIn(".setTitle(commonR.string.add_action_data_field)", widget)
        self.assertNotIn('.setTitle("Field")', widget)

    def test_visible_branding_preserves_format_arguments_and_android_escaping(self):
        placeholders = {
            "matter_shared_status_confirmation_named": ["%1$s"],
            "matter_shared_status_failure_code": ["%1$d"],
            "no_assist_support": ["%1$s", "%2$s"],
        }
        for locale in ("values", "values-zh-rTW"):
            strings = localized_strings(locale)
            for key, expected in placeholders.items():
                with self.subTest(locale=locale, key=key):
                    self.assertEqual(re.findall(r"%\d+\$[sd]", strings[key]), expected)
            if locale == "values":
                self.assertIn(r"\'Mobile App\'", strings["error_with_registration"])
                self.assertIn(r"couldn\'t", strings["assist_connnect"])
                for key, quote_count in (("sensor_description_location_accurate", 6),
                                         ("sensor_description_location_background", 2),
                                         ("sensor_description_location_zone", 2),
                                         ("sensor_description_wifi_bssid", 2)):
                    with self.subTest(key=key):
                        self.assertEqual(strings[key].count(r'\"'), quote_count)

    def test_visible_branding_preserves_welcome_layout_legal_and_protocol_boundaries(self):
        welcome = read(APP + "onboarding/welcome/WelcomeScreen.kt")
        for value in ("ICON_SIZE = 120.dp", "R.drawable.ic_apporo_branding",
                      "stringResource(commonR.string.welcome_home_assistant_title)",
                      "style = HATextStyle.Headline", "style = HATextStyle.Body"):
            self.assertIn(value, welcome)
        for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"):
            self.assertTrue((ROOT / f"app/src/main/res/drawable-{density}/ic_apporo_branding.png").exists(), density)
        self.assertIn("Apache License", read("LICENSE.md"))
        self.assertIn("Version 2.0, January 2004", read("LICENSE.md"))
        # Protocol-level identifiers keep the upstream name: they are wire values, not copy.
        checker = read(COMMON + "common/data/connectivity/DefaultConnectivityChecker.kt")
        self.assertIn('HOME_ASSISTANT_NAME = "Home Assistant"', checker)
        self.assertIn('name == HOME_ASSISTANT_NAME', checker)
        searcher = read(APP + "onboarding/serverdiscovery/HomeAssistantSearcher.kt")
        self.assertIn('SERVICE_TYPE = "_home-assistant._tcp"', searcher)
        apis = read(COMMON + "common/data/HomeAssistantApis.kt")
        self.assertIn('"Home Assistant/${BuildConfig.VERSION_NAME} ($ANDROID_DETAILS)"', apis)

    def test_matter_thread_bindings_are_unsupported_in_all_flavors(self):
        for feature, name, method in (("matter", "Matter", "appSupportsCommissioning"),
                                      ("thread", "Thread", "appSupportsThread")):
            manager = read(APP + f"{feature}/{name}ManagerImpl.kt")
            self.assertIn(f"override fun {method}(): Boolean = false", manager)
            self.assertNotIn("com.google.android.gms", manager)
            for flavor in ("full", "minimal"):
                self.assertFalse(
                    (ROOT / f"app/src/{flavor}/kotlin/io/homeassistant/companion/android/{feature}/{name}ManagerImpl.kt").exists()
                )
        for module in ("app", "automotive"):
            manifest = read(f"{module}/src/full/AndroidManifest.xml")
            self.assertNotIn(".matter.MatterCommissioning", manifest)
            self.assertNotIn('android:name="home:0:preferred"', manifest)

    def test_obsolete_native_commissioning_sources_and_icons_are_removed(self):
        for path in source_files():
            if path.suffix == '.kt':
                source = path.read_text(encoding='utf-8')
                self.assertNotIn('com.google.android.gms.home', source, str(path))
                self.assertNotIn('com.google.android.gms.threadnetwork', source, str(path))
                self.assertNotRegex(source, r'\bMatterCommissioning(Activity|Service|ViewModel|View|ViewPreviewStates)\b', str(path))
        for directory in ('drawable', 'drawable-night'):
            self.assertFalse((ROOT / 'app/src/main/res' / directory / 'ic_matter.xml').exists())

    def test_preserved_features_and_google_dependencies(self):
        deps = read("build-logic/convention/src/main/kotlin/AndroidApplicationDependenciesConventionPlugin.kt")
        for dependency in ("libs.play.services.location", "libs.firebase.messaging", "libs.play.services.wearable"):
            self.assertIn(f'"fullImplementation"({dependency})', deps)
        self.assertIn("libs.androidx.health.services.client", read("wear/build.gradle.kts"))
        self.assertIn("HealthConnectSensorManager", read(APP + "sensors/HealthConnectSensorManager.kt"))
        manifest = read("app/src/main/AndroidManifest.xml")
        for feature in ("widgets.camera.CameraWidget", "TagReaderActivity", "android.permission.CAMERA",
                        "android.permission.RECORD_AUDIO", "android.permission.ACCESS_BACKGROUND_LOCATION"):
            self.assertIn(feature, manifest)
        full = read("app/src/full/AndroidManifest.xml")
        for feature in ("FirebaseCloudMessagingService", "HighAccuracyLocationService", "WearDnsRequestListener"):
            self.assertIn(feature, full)
        # Unlike woowtech, Apporo keeps Android Auto on the phone app as well as automotive.
        self.assertIn("HaCarAppService", read("automotive/src/main/AndroidManifest.xml"))
        self.assertTrue((ROOT / APP / "vehicle/HaCarAppService.kt").exists())
        self.assertTrue((ROOT / APP / "settings/websocket/WebsocketSettingFragment.kt").exists())

    def test_no_stale_app_identity_in_runtime_sources(self):
        # `\n` is resolved first so a hard-wrapped string cannot hide a stale identity.
        # "Apporo Home" was the brand name before the 2026-09-14 rename to "Apporo aiot";
        # ten colour-token comments still carried it until 2026-09-16.
        stale = ("com.apporo.home", "Apporo Home", "apporo Home", "com.woowtech",
                 "woowtech://", "aiot.woowtech.io", "homeassistant://")
        findings = []
        for path in source_files():
            for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
                resolved_line = line.replace(r"\n", " ")
                if any(old in resolved_line for old in stale):
                    findings.append(f"{path.relative_to(ROOT)}:{number}")
        self.assertEqual(findings, [])


class KnownOpenDefectTest(unittest.TestCase):
    """Apporo defects this port found and did not fix, pinned so they cannot be forgotten.

    Every test below is a real assertion about how the app should read, marked
    `expectedFailure` because the repo does not satisfy it yet. The run therefore stays
    green while the defect stays visible in the output as an "expected failure".

    When one is fixed the test starts passing, unittest reports it as an UNEXPECTED SUCCESS
    and the run goes red — that is the signal to delete the decorator and move the
    assertion up into the contract above. Do not delete the test instead.
    """

    def test_zh_location_hint_keeps_the_english_compliance_wording(self):
        """values-zh-rTW `location_secure_connection_hint` says something else entirely.

        en: "Wi-Fi network detection requires location permission on supported Android
        versions. Location sensor sharing is configured separately." — what the permission
        actually is and what it is not.
        zh-rTW: 「此資料永遠不會與 Apporo 或第三方分享。」 — a data-sharing promise that the
        English never makes and that the app cannot substantiate on this screen.

        woowtech translates the English faithfully:
        「在適用的 Android 版本上，偵測 Wi-Fi 網路需要定位權限。位置感測器分享需另外設定。」
        Fixing this is a strings.xml change and belongs to the copy owner, not to this file.
        """
        chinese = localized_strings("values-zh-rTW")["location_secure_connection_hint"]
        self.assertIn("Wi-Fi", chinese)
        self.assertIn("定位權限", chinese)
        self.assertNotIn("永遠不會", chinese)


def check_public_links():
    """Fetch every help-centre address the app links to and confirm it is anonymously readable."""
    catalog = read(COMMON + "common/util/AppSupportLinks.kt")
    resolved = {}
    for name, value in re.findall(r'const val (\w+)\s*=\s*"([^"]+)"', catalog):
        url = value
        for _ in range(4):
            url = re.sub(r'\$\{?(\w+(?:\.\w+)?)\}?',
                         lambda m: resolved.get(m.group(1).split(".")[-1],
                                                HELP_CENTER_BASE_URL if "HELP_CENTER" in m.group(1) else m.group(0)),
                         url)
        resolved[name] = url
    pages = sorted({url.split("#")[0] for url in resolved.values() if url.startswith("http")})
    for url in pages:
        html = subprocess.check_output(
            ["curl", "--fail", "--silent", "--show-error", "--location", "--max-time", "30", "-A", "HomeAssistant", url],
            text=True,
        )
        if BRAND_NAME not in html:
            raise ValueError(f"{url}: expected brand content missing")
        print(f"PASS: {url}: anonymous HTTP success and brand content")
    missing_anchors = []
    for name, url in resolved.items():
        if "#" not in url:
            continue
        page, anchor = url.split("#", 1)
        html = subprocess.check_output(
            ["curl", "--fail", "--silent", "--show-error", "--location", "--max-time", "30", "-A", "HomeAssistant", page],
            text=True,
        )
        if anchor not in set(re.findall(r'\bid=["\']([^"\']+)["\']', html)):
            missing_anchors.append(f"{name} -> {url}")
    if missing_anchors:
        raise ValueError(f"missing anchors: {sorted(missing_anchors)}")


def print_residue():
    """Report remaining URL candidates; comments/previews are not necessarily user actions."""
    findings = []
    for path in source_files():
        if "/test/" in str(path):
            continue
        for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            urls = re.findall(r'https://(?:www\.apporo\.ai|aiot\.apporo\.ai|mobile-apps\.home-assistant\.io)[^\s"<>]*', line)
            for url in urls:
                if not url.startswith(HELP_CENTER_BASE_URL):
                    findings.append({"file": str(path.relative_to(ROOT)), "line": number, "url": url})
    print(json.dumps({"remaining_urls_require_semantic_review": findings}, indent=2))


if __name__ == "__main__":
    if sys.argv[1:] == ["--oauth-stdin"]:
        validate_oauth(sys.stdin.read())
        print(f"PASS: live endpoint HTML contains exactly the {len(ALLOWED_CALLBACKS)} approved callbacks; "
              "HA login remains untested")
    elif sys.argv[1:] == ["--oauth-fixture"]:
        validate_oauth(read("docs/android/index.html"))
        print(f"PASS: local fixture contains exactly the {len(ALLOWED_CALLBACKS)} approved callbacks; "
              "live deployment is NOT verified")
    elif sys.argv[1:] == ["--residue"]:
        print_residue()
    elif sys.argv[1:] == ["--public-links"]:
        check_public_links()
    else:
        unittest.main(verbosity=2)
