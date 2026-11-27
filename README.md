# NorthEast Launcher

A minimal, fast, ad-free home screen for Android TV / Google TV with a Northeast woodland look.
Your pinned apps, nothing else: no ads, no recommendations, no "continue watching", no network access.

> **Status:** M4 — feature-complete for a first release: home screen, ~40 settings, Home-button
> takeover, signed release builds, and a reversible setup/undo script. Everything is verified on an
> Android TV emulator in CI; first real-device test (Home button with a physical remote, startup and
> frame timing) is next.
>
> Docs: [Feasibility](docs/FEASIBILITY.md) · [Settings](docs/SETTINGS.md) ·
> [Make it the home screen](docs/DEFAULT_LAUNCHER.md) · [Release builds](docs/RELEASE.md) ·
> [Troubleshooting](docs/TROUBLESHOOTING.md)

## Using it with the remote

| Do this | To |
|---|---|
| ◀ ▶ then **OK** | Open an app |
| **Hold OK** on an app (or press **Menu**) | Move it or remove it from home |
| In move mode: ◀ ▶, then **OK** or **Back** | Slide the tile, then put it down |
| **+ Edit apps** tile | See every installed app; **OK** adds or removes it from home |
| **▲** from the app row, then **OK** on the gear | Settings (all options are in [docs/SETTINGS.md](docs/SETTINGS.md)) |
| **Back** | Leaves move mode, the app list or settings; never exits the launcher |

On first run it pins YouTube, Crunchyroll, Spotify and HBO Max if they're installed.
The focus outline and the pine trees both follow New England's seasons by the TV's date: frost (Dec–Feb),
lilac (Mar–May), leaf green (Jun–Aug), red maple (Sep–Oct) and copper oak for stick season (Nov).

## Requirements

| | |
|---|---|
| Device | Android TV / Google TV, Android 9 (API 28) or newer. Tested target: Onn 4K Pro (2026). |
| Build machine | Android Studio (recent stable) with JDK 17+, or just the command line with JDK 17. |
| Tools | `adb` (Android SDK Platform-Tools) for sideloading. |

## Build

```bash
git clone https://github.com/brandondonofrio1998-code/NorthEast-Launcher.git
cd NorthEast-Launcher
./gradlew testDebugUnitTest   # unit tests (app list storage/ordering, settings, PIN, home rules)
./gradlew assembleDebug       # → app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press Run with the TV (or a TV emulator) selected.

For the TVs you actually use, build a **signed release** (`./gradlew assembleRelease` with your own
`keystore.properties`, never committed). See [docs/RELEASE.md](docs/RELEASE.md).

**No build machine handy?** Every push is built and tested on GitHub Actions. Open the
repo's **Actions** tab → latest *Build & test* run → download the `northeast-launcher-debug-apk` artifact.

## Sideload onto the TV

1. On the TV device: **Settings → System → About → Android TV OS build**, press OK 7 times to unlock
   Developer options. Then **Settings → System → Developer options → USB debugging / Wireless debugging: On**.
2. Find the device's IP: **Settings → Network & Internet → (your network)**.
3. From your Mac, on the same Wi-Fi:

   ```bash
   adb connect <STICK IP ADDRESS>          # newer Google TV builds may first need: adb pair <ip>:<pairing port>
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   adb shell am start -n com.northeast.launcher.debug/com.northeast.launcher.MainActivity
   ```

   Accept the "Allow debugging?" prompt on the TV the first time.

The debug build installs as `com.northeast.launcher.debug`, so it can sit alongside a release build
(`com.northeast.launcher`).

## Making it the default home screen

```bash
tools/device.sh install app/build/outputs/apk/release/app-release.apk
tools/device.sh make-default            # Home button opens NorthEast
tools/device.sh disable-stock --yes     # optional: stock launcher (and its ads) off
tools/device.sh restore                 # undo everything
```

Google TV needs more than picking a launcher in Settings, so step-by-step instructions, safeguards and
manual commands are in [docs/DEFAULT_LAUNCHER.md](docs/DEFAULT_LAUNCHER.md). Add
`--package com.northeast.launcher.debug` before the command for a debug build.

## Continuous integration

- **Build & test**: unit tests, debug APK and unsigned release APK, a check that neither requests
  network permissions, and a check that no signing secrets are tracked.
- **UI smoke (TV emulator)**: installs on an Android TV 11 emulator and drives it with D-pad key
  events through home, app list, move, settings, remote test and Home-button setup, with screenshots.
  It also runs `tools/device.sh` end to end: make-default, the service binding, disable-stock, **a
  reboot that must land on NorthEast**, and restore. Finally it installs a release build signed with a
  throwaway CI key and checks that it starts. Results are in the run's `ui-smoke` artifact.
  Emulator timings use software rendering. The release build's cold start was ~1.0 s there vs
  ~1.7 s for debug, but real numbers come from the device.

## Privacy

- The manifest explicitly strips `INTERNET` and `ACCESS_NETWORK_STATE`, and CI fails the build if the
  final APK requests any network permission. The launcher cannot phone home even by accident.
- No analytics, crash reporting, or third-party SDKs.
- Your pinned apps and settings are stored locally on the device only (`allowBackup="false"`).
- The settings PIN is stored as a salted SHA-256 hash, never as the digits.
- The Home-button service declares `canRetrieveWindowContent="false"` and subscribes to no accessibility
  events: it receives key presses only, acts on Home (plus one optional extra button), and passes the rest on.

## Project layout

```
app/src/main/java/com/northeast/launcher/
  MainActivity.kt          HOME activity; refreshes on start and on app installs/removals
  apps/AppCatalog.kt       lists launchable apps, loads icons, opens apps (local only)
  data/                    pinned apps, settings, PIN, seasons, home rules (pure Kotlin, unit-tested)
  home/                    Home-button accessibility service, remote key log (test screen only)
  ui/                      Compose for TV home screen and app list, woodland theme
  ui/settings/             settings list, arrange, remote test, Home-button setup, PIN pad
app/src/test/              JUnit tests, run before every commit and in CI
tools/device.sh            install / make-default / disable-stock / restore over ADB
tools/gen_pine_ridge.py    regenerates the backdrop art (see ASSETS.md)
tools/ci/ui-smoke.sh       the emulator walkthrough CI runs
keystore.properties.template   copy to keystore.properties for signed releases (git-ignored)
docs/                      feasibility, settings, default-launcher guide, release, troubleshooting
```

## Roadmap

- [x] M0 — Feasibility & device choice
- [x] M1 — Project setup, storage/ordering logic + tests, CI
- [x] M2 — Home screen: app row, launch, add/remove/reorder, woodland theme (device install pending)
- [x] M3 — Settings, Home-button service, focus polish (emulator-verified)
- [x] M4 — Signed release setup, default-launcher guide + script, troubleshooting
- [ ] On a real device — Home button with a physical remote; startup and frame timing on the stick
