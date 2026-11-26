# NorthEast Launcher

A minimal, fast, ad-free home screen for Android TV / Google TV with a Northeast woodland look.
Your pinned apps, nothing else: no ads, no recommendations, no "continue watching", no network access.

> **Status:** M1 — project scaffold. It builds, installs and registers as a HOME launcher, but the
> screen is a placeholder. The pinned-app home screen arrives in M2. See [`docs/FEASIBILITY.md`](docs/FEASIBILITY.md)
> for why this is a launcher (not a custom OS) and which devices it targets.

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
./gradlew testDebugUnitTest   # unit tests (pinned-app storage + ordering)
./gradlew assembleDebug       # → app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press Run with the TV (or a TV emulator) selected.

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

Not yet — this needs the Home-button handling from M2/M3, and Google TV requires more than picking it
in Settings (see FEASIBILITY.md §3). Step-by-step, reversible ADB instructions land in `docs/` in M4.
Until then, launch it like any app.

## Privacy

- The manifest explicitly strips `INTERNET` and `ACCESS_NETWORK_STATE`, and CI fails the build if the
  final APK requests any network permission. The launcher cannot phone home even by accident.
- No analytics, crash reporting, or third-party SDKs.
- Your pinned apps are stored locally on the device only (`allowBackup="false"`).

## Project layout

```
app/src/main/java/com/northeast/launcher/
  MainActivity.kt          HOME activity (placeholder UI in M1)
  data/                    pinned-app model, ordering, storage (pure Kotlin, unit-tested)
app/src/test/              JUnit tests, run before every commit and in CI
docs/                      feasibility study; later: setup guide, troubleshooting
```

## Roadmap

- [x] M0 — Feasibility & device choice
- [x] M1 — Project setup, storage/ordering logic + tests, CI
- [ ] M2 — Home screen: app grid, launch, add/remove/reorder, woodland theme
- [ ] M3 — Settings, focus polish, performance pass (startup + frame times measured)
- [ ] M4 — Signed release setup, default-launcher guide, troubleshooting
