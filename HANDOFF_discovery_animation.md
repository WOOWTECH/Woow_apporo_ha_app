# Handoff — Discovery screen icon-pulse animation clamp

Same change was applied in this repo and in `Woow_simon_ha_app`. Full handoff document lives in the sibling repo:

**`~/Desktop/Woow_simon_ha_app/HANDOFF_discovery_animation.md`**

TL;DR of the local change here:

- File: `app/src/main/kotlin/io/homeassistant/companion/android/onboarding/serverdiscovery/ServerDiscoveryScreen.kt`
- Function: `AnimatedIcon()`
- Change: `targetValue = 1.15f` → `1.05f` with a two-line comment explaining the geometry constraint
- Unstaged, not committed. Verify with `git diff`.
- Apporo APK not built or installed anywhere yet in this session — the code change exists only.

Read the full handoff for geometry analysis, build/install commands, and open questions before you touch anything.
