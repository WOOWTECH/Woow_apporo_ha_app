# Apporo Home

Apporo Home is the official Android smart home control app.

## Overview

Apporo Home connects to your smart home server at `https://aiot.apporo.io`.
It provides local control and privacy-first home automation, built on Home Assistant open source technology.

## Features

- **Remote access** — control your smart home from anywhere
- **Local control** — connect directly to the home server on the same network
- **Sensors** — background sensor collection for automation triggers
- **Notifications** — push notifications for alerts and automations
- **Widgets** — home screen widgets for quick device control
- **Wear OS** — smartwatch support
- **Android Auto** — in-car control

## Brand

| Property | Value |
|---|---|
| Primary color | #8B6B24 |
| App name | Apporo Home |
| Package ID | com.apporo.home |
| Server URL | https://aiot.apporo.io |
| URL scheme | apporohome:// |

## Build

Requirements: JDK 17+, Android SDK, Gradle.

```bash
./gradlew assembleFullDebug     # debug
./gradlew assembleFullRelease   # release
```

Output: `app/build/outputs/apk/full/debug/app-full-debug.apk`

## Rebranding

This branch was generated from `main` with:

```bash
git checkout -b brand/apporo main
bash tools/brand/rebrand.sh tools/brand/apporo.conf
```

See `docs/brand/WHITE_LABEL_SOP.md`.

## License

Based on Home Assistant Companion for Android (Apache 2.0).

---

**Apporo Home**
