#!/usr/bin/env bash
set -euo pipefail

adb wait-for-device
for attempt in {1..90}; do
  package_service="$(adb shell service check package 2>/dev/null || true)"
  if [[ "$package_service" == *found* ]]; then
    echo "Android Package Manager is ready."
    exec ./gradlew :app:connectedFullDebugAndroidTest :app:connectedMinimalDebugAndroidTest :common:connectedDebugAndroidTest
  fi
  sleep 2
done

echo "Android Package Manager did not become ready within 180 seconds." >&2
exit 1
