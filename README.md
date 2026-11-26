# NorthEast Launcher

A minimal, fast, ad-free home screen for Android TV / Google TV with a Northeast woodland look.
Your pinned apps, nothing else: no ads, no recommendations, no "continue watching", no network access.

> **Status:** M2 — working home screen: a single row of your pinned apps on a spruce-and-pine
> backdrop, with add / remove / reorder by remote. Settings and the Home-button takeover come in M3.
> See [`docs/FEASIBILITY.md`](docs/FEASIBILITY.md) for why this is a launcher (not a custom OS) and
> which devices it targets.

## Using it with the remote

| Do this | To |
|---|---|
| ◀ ▶ then **OK** | Open an app |
| **Hold OK** on an app (or press **Menu**) | Move it or remove it from home |
| In move mode: ◀ ▶, then **OK** or **Back** | Slide the tile, then put it down |
| **+ Edit apps** tile | See every installed app; **OK** adds or removes it from home |
| **Back** | Leaves move mode or the app list; never exits the launcher |

On first run it pins YouTube, Crunchyroll, Spotify and HBO Max if they're installed.
The focus outline color follows New England's seasons by the TV's date: frost (Dec–Feb),
lilac (Mar–May), goldenrod (Jun–Aug), red maple (Sep–Oct) and copper oak for stick season (Nov).

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
  MainActivity.kt          HOME activity; refreshes on start and on app installs/removals
  apps/AppCatalog.kt       lists launchable apps, loads icons, opens apps (local only)
  data/                    pinned-app model, ordering, storage, seasons (pure Kotlin, unit-tested)
  ui/                      Compose for TV screens, woodland theme
app/src/test/              JUnit tests, run before every commit and in CI
tools/gen_pine_ridge.py    regenerates the backdrop art (see ASSETS.md)
docs/                      feasibility study; later: setup guide, troubleshooting
```

## Roadmap

- [x] M0 — Feasibility & device choice
- [x] M1 — Project setup, storage/ordering logic + tests, CI
- [x] M2 — Home screen: app row, launch, add/remove/reorder, woodland theme (device install pending)
- [ ] M3 — Settings, focus polish, performance pass (startup + frame times measured)
- [ ] M4 — Signed release setup, default-launcher guide, troubleshooting
