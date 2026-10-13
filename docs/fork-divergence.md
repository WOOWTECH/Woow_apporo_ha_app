# Fork divergence

This repository is a fork of [home-assistant/android](https://github.com/home-assistant/android).

**Fork point:** `cc8fdb027` — *Update github/codeql-action action to v4.32.0 (#6349)*, 2026-01-29.

Upstream commits are picked manually rather than merged, so every difference from upstream has to
be deliberate and recorded here. A difference that is not on this list is either a mistake or an
undocumented decision — both are worth investigating rather than preserving.

## How to check divergence for a file

```bash
# one-time: a local clone of upstream
git clone https://github.com/home-assistant/android.git ~/android

cd ~/android && git fetch origin
diff <(git show origin/main:<path>) <path-in-this-repo>
```

## Intentional divergence

| File / area | Difference | Why | Upstream status |
|---|---|---|---|
| `build-logic/.../AndroidApplicationConventionPlugin.kt` | `applicationId = "com.apporo.aiot"` | Brand identity | Permanent — will never be upstreamed |
| `common/src/main/res/values*/strings.xml` | `app_name` is `Apporo aiot`, plus the brand rewrite of the user-facing strings | Brand identity | Permanent |
| `common/.../util/UrlUtil.kt` | `DEEP_LINK_SCHEME = "apporoaiot"` instead of `homeassistant` | Brand identity; also lets the app coexist with the official app | Permanent |
| `app/src/debug/AndroidManifest.xml`, `automotive/src/debug/AndroidManifest.xml` | Debug builds additionally declare `apporoaiot-dev://` | The debug build installs alongside the release build under `.debug`, so a shared scheme would raise a disambiguation dialog | Permanent, fork-local |
| `common/.../data/authentication/impl/AuthenticationService.kt` | `CLIENT_ID = "https://woowtech.github.io/Woow_apporo_ha_app/android"` | The upstream client_id is on Home Assistant's hard-coded allow list; a fork's is not, so it needs a real page advertising the brand's `redirect_uri`. See the comment on the constant and `docs/android/index.html` | Permanent |
| `docs/android/index.html` | New file — the OAuth client_id page published via GitHub Pages | As above | Fork-local |
| `gradle.properties` | Push and rate-limit endpoints point at `aiot.apporo.ai` | Brand backend | Permanent |
| `.github/mock-google-services.json` | Carries `com.apporo.aiot{,.debug,.minimal,.minimal.debug}` instead of the upstream package names | Needed for debug/minimal builds after the applicationId change | Fixed; not an upstream concern |
| `app/.../util/CrashSaving.kt` | Adds `CrashSavingFailFastHandler`, latch, and diagnostic context | `FailFast` terminates via `exitProcess` without throwing, so its failures never reached the uncaught exception handler and existed only in logcat | **Upstream has the identical gap** — verified byte-identical to `origin/main` before this change. Candidate for an upstream PR |
| `app/.../util/StrictModeDiagnostics.kt` | New file | Build/device context for the above | Same as above |
| `app/.../HomeAssistantApplication.kt` | Registers the FailFast handler before StrictMode | Ordering matters: the first violation happens while the first activity attaches | Same as above |
| `app/src/test/.../IgnoreViolationRulesTest.kt` | New file | Upstream has no tests for the ignore rules; they rely on emulator.wtf instrumentation runs that this fork does not have | Candidate for an upstream PR |
| `app/src/test/.../CrashSavingFailFastHandlerTest.kt` | New file | Covers the divergence above | Same as above |
| `tools/repro-locale-strictmode.sh` | New file | Manual reproduction for an API 31/32-only crash | Fork-local tooling |
| `tools/brand/` | New directory — white-label config, preflight validator and rebrand script | Seeds a new brand from an un-rebranded upstream checkout. Disabled against this repo; see `tools/brand/README.md` | Fork-local tooling |

### Identity history

The brand identity has moved twice, so old commits, plan documents and screenshots refer to values
that are no longer in use. Only the last column is current.

| | applicationId | App name | URL scheme | Server domain |
|---|---|---|---|---|
| Upstream | `io.homeassistant.companion.android` | Home Assistant | `homeassistant` | — |
| woowtech fork | `com.woowtech.home` | woowtech Home | `homeassistant` | `aiot.woowtech.io` |
| Apporo, first rebrand | `com.apporo.home` | Apporo SmartHome / Apporo Home | `apporohome` | `aiot.apporo.io` |
| **Apporo, current** | **`com.apporo.aiot`** | **Apporo aiot** | **`apporoaiot`** | **`aiot.apporo.ai`** |

None of the retired application ids was ever published: there is no installed base to migrate.
The Kotlin namespace stays `io.homeassistant.companion.android` throughout — it is not the
applicationId and is deliberately left alone to keep upstream merges cheap.

The diagnostics work above is deliberately confined to `:app`. It was originally written by
extending `FailFast.failWith()` and `HAStrictMode.enable()` in `:common`, and was reworked so that
those two shared files stay byte-identical to upstream — `:common` has the highest upstream churn
and is the most expensive place to diverge.

## Deliberately NOT diverged

- **`autoStoreLocales=true`** in the app and automotive manifests. Removing it would eliminate real
  main-thread disk I/O on every cold start below API 33, but the manifest is a merge-conflict
  hotspot and the gain is a few milliseconds on a shrinking device population. Upstream chose to
  suppress the resulting StrictMode violation instead; this fork follows that decision.
- **`app/.../util/IgnoreViolationRules.kt`** is kept byte-identical to upstream `origin/main`.
  Do not add fork-local rules to it — if one is ever needed, put it in a separate file so this one
  can keep being replaced wholesale from upstream.

## Known staleness (not divergence — we are simply behind)

| File | What upstream has that we lack |
|---|---|
| `common/.../FailFast.kt` | `failWhen()` returns `Boolean` so it can be used as an inline guard |

## Known pre-existing problems in this fork

These are not caused by any single change and each deserves its own issue:

- `:app:compileMinimalDebugUnitTestKotlin` fails with 64 compile errors — the entire `minimal`
  flavor unit test suite does not compile, so it never runs. Put new tests in a source set that
  actually executes and verify they ran.
- `ConnectionViewModelTest > ...onReceivedError...` fails on a Turbine timeout.
- `ktlintCheck` reports violations in ~10 files unrelated to any recent change.
- `google-services.json` is absent for `:app`, `:automotive` and `:wear`; nothing builds without it.
