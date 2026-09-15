# Apporo aiot

Apporo aiot is the Android smart home control app for Apporo, built on the
[Home Assistant Companion](https://github.com/home-assistant/android) open source app.

## Overview

Apporo aiot connects to an Apporo smart home server, with local control on the home network and
remote access from anywhere.

## Features

- **Remote access** — control your smart home from anywhere
- **Local control** — connect directly to the home server on the same network
- **Sensors** — background sensor collection for automation triggers
- **Notifications** — push notifications for alerts and automations
- **Widgets** — home screen widgets for quick device control

Wear OS, Android Auto / Automotive, and health and fitness tracking are out of scope for the first
release. The upstream source for those features is still in the tree but is not part of the
shipped build.

## Brand

| Property | Value |
|---|---|
| App name | Apporo aiot |
| Package ID | `com.apporo.aiot` (debug: `com.apporo.aiot.debug`) |
| Primary color | `#8B6B24` — see [ADR-0001](docs/adr/0001-apporo-primary-color.md) |
| Server domain | `aiot.apporo.ai` — **not yet delegated; no DNS record exists** |
| URL scheme | `apporoaiot://` (debug builds also answer `apporoaiot-dev://`) |
| OAuth `client_id` | https://woowtech.github.io/Woow_apporo_ha_app/android |

The brand values above are recorded in [`tools/brand/apporo.conf`](tools/brand/apporo.conf), which
is the single source of truth. After merging upstream, check that nothing overwrote them:

```bash
python3 tools/brand/preflight.py --verify-repo tools/brand/apporo.conf
```

### Why the OAuth client_id is a github.io address

Home Assistant fetches the `client_id` URL from the server side during sign-in and reads the
`rel="redirect_uri"` link tags out of it; sign-in only succeeds on an exact match. That address has
to be live and anonymously readable *now*. `aiot.apporo.ai` has no DNS record yet, so the client_id
points at the GitHub Pages copy of [`docs/android/index.html`](docs/android/index.html) instead.
The conditions for moving it to the brand domain are written in the comment above `CLIENT_ID` in
`common/.../data/authentication/impl/AuthenticationService.kt`.

Because GitHub Pages publishes from the default branch, a change to `docs/android/index.html` only
reaches the live page once it is merged to `main`. A branch that changes the app's URL scheme
cannot sign in until that merge happens.

## Build

Requirements: JDK 17+, Android SDK, Gradle.

```bash
./gradlew assembleFullDebug     # debug
./gradlew assembleFullRelease   # release
```

Output: `app/build/outputs/apk/full/debug/app-full-debug.apk`

A real `google-services.json` is not in the tree; see
[`docs/fork-divergence.md`](docs/fork-divergence.md) for the current build caveats.

## Relationship to upstream

This repository is a fork of `home-assistant/android`. Every deliberate difference is recorded in
[`docs/fork-divergence.md`](docs/fork-divergence.md) — a difference that is not on that list is
worth investigating rather than preserving.

The Apporo identity was originally produced by the white-label script in
[`tools/brand/`](tools/brand/README.md). **That script no longer runs against this repository** and
will refuse to start; read `tools/brand/README.md` before touching anything in there.

## License

Based on Home Assistant Companion for Android (Apache 2.0).
