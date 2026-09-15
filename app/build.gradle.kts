plugins {
    alias(libs.plugins.homeassistant.android.application)
    alias(libs.plugins.homeassistant.android.flavor)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.google.services)
    alias(libs.plugins.homeassistant.android.dependencies)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    useLibrary("android.car")

    defaultConfig {
        manifestPlaceholders["sentryRelease"] = "$applicationId@$versionName"
        manifestPlaceholders["sentryDsn"] = System.getenv("SENTRY_DSN") ?: ""

        bundle {
            language {
                // We want to keep the translations in the final AAB for all the language
                enableSplit = false
            }
        }
    }

    lint {
        // Until we fully migrate to Material3 this lint issue is too verbose https://github.com/home-assistant/android/issues/5420
        disable += listOf("UsingMaterialAndMaterial3Libraries")
    }
}

firebaseAppDistributionDefault {
    serviceCredentialsFile = "firebaseAppDistributionServiceCredentialsFile.json"
    releaseNotesFile = "./app/build/outputs/changelogBeta"
    groups = "continuous-deployment"
}

dependencies {
    // Most of the dependencies are coming from the convention plugin to avoid duplication with `:automotive` module.
    "fullImplementation"(libs.car.projected)
}

// Disable to fix memory leak and be compatible with the configuration cache.
googleServices {
    disableVersionCheck = true
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        // 首版對外版號固定 1.0,與 Reckon 由 git tag 推導出的
        // 2026.9.x 內部版號及 flavor 尾碼脫鉤。
        // versionCode 仍由 CI 的 VERSION_CODE 環境變數決定,不受影響。
        variant.outputs.forEach { output ->
            output.versionName.set("1.0")
        }
    }
}
