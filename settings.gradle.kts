// The first release ships the phone app only: Wear OS and Android Automotive are out of scope.
// The `wear/` and `automotive/` sources are kept intact, but they are not part of the build by
// default, so `./gradlew :wear:assembleRelease` fails with "Project ':wear' not found" instead of
// quietly producing an artifact that nobody is supposed to publish. Relying on the CI and fastlane
// task lists alone would be a convention, not a mechanism: a local build or a hand-run lane would
// still produce those artifacts.
//
// To work on either module again (for example when they are re-evaluated for a later release):
//   ./gradlew -PincludeExcludedFormFactors=true :wear:assembleRelease
// or set `includeExcludedFormFactors=true` in `gradle.properties` for a whole session, which also
// makes Android Studio sync them again.
//
// Safe to exclude because nothing depends on them: no `*.gradle.kts` in this repository declares
// `project(":wear")` or `project(":automotive")`, and `automotive/build.gradle.kts` only mounts
// `../app/src/**` one way, so `:app` and `:common` never reach into `:automotive`.
// Note that `wear/gradle.lockfile` and `automotive/gradle.lockfile` are left in place; Gradle only
// reads a module's lockfile when that module is part of the build, so they stay inert and the
// `lockfiles` CI job (`./gradlew alldependencies --write-locks`) leaves them untouched.
val includeExcludedFormFactors = providers.gradleProperty("includeExcludedFormFactors").orNull.toBoolean()

include(":common", ":app", ":testing-unit", ":lint")

if (includeExcludedFormFactors) {
    include(":wear", ":automotive")
}

rootProject.name = "home-assistant-android"

includeBuild("build-logic")

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

// Reckon plugin doesn't work in git worktrees (JGit doesn't handle worktree .git files).
// Detect worktrees (.git is a file, not a directory) and skip reckon in that case.
val isWorktree = settings.settingsDir.resolve(".git").isFile

plugins {
    // So we can't reach the libs.plugins.* aliases from here so we need to declare them the old way...
    id("org.ajoberstar.reckon.settings").version("1.0.1").apply(false)
}

if (!isWorktree) {
    apply(plugin = "org.ajoberstar.reckon.settings")

    extensions.configure<org.ajoberstar.reckon.gradle.ReckonExtension>("reckon") {
        val isCiBuild = providers.environmentVariable("CI").isPresent

        setDefaultInferredScope("patch")
        if (!isCiBuild) {
            // Use a snapshot version scheme with Reckon when not running in CI, which allows caching to
            // improve performance. Background: https://github.com/home-assistant/android/issues/5220.
            snapshots()
        } else {
            stages("beta", "final")
        }
        setScopeCalc { java.util.Optional.of(org.ajoberstar.reckon.core.Scope.PATCH) }
        setStageCalc(calcStageFromProp())
        setTagWriter { it.toString() }
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
                includeGroupByRegex("org\\.chromium.*")
            }
        }
        mavenCentral()
        google()
        maven("https://jitpack.io")
    }
}
