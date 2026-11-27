# NorthEast Launcher

A minimal, fast, ad-free home screen for Android TV / Google TV with a Northeast woodland look.
Your pinned apps, nothing else: no ads, no recommendations, no "continue watching", no network access.

> **Status:** M3 — home screen, ~40 settings, Home-button takeover service, remote button test,
> settings PIN and layout lock. Verified on an Android TV emulator; real-device testing and timing
> are next. See [`docs/FEASIBILITY.md`](docs/FEASIBILITY.md) for why this is a launcher (not a custom
> OS) and which devices it targets, and [`docs/SETTINGS.md`](docs/SETTINGS.md) for every setting.

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

Google TV requires more than picking it in Settings (see FEASIBILITY.md §3). NorthEast includes a small
**Home-button service** (an accessibility service that only listens for the Home button — it can't read
the screen). Turn it on from **Settings → Home button setup**, which shows the exact two ADB commands for
your build. The full step-by-step, reversible guide lands in `docs/` in M4.

What CI has already confirmed on an Android TV 11 emulator (`.github/workflows/ui-smoke.yml`):

- `cmd package set-home-activity` reports success but the stock launcher still owns HOME and the Home key.
- With the stock launcher disabled, HOME resolves to NorthEast and the Home key lands on it, so the
  launcher's HOME registration itself works. (The emulator test re-enables the stock launcher afterwards.)

## Continuous integration

- **Build & test** — unit tests, debug APK, and a check that the APK requests no network permissions.
- **UI smoke (TV emulator)** — installs on an Android TV emulator, drives it with D-pad key events,
  screenshots each screen, and records cold-start time. Results are in the run's `ui-smoke` artifact.
  Emulator timings use software rendering and are not representative; device numbers come in M3.

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
tools/gen_pine_ridge.py    regenerates the backdrop art (see ASSETS.md)
docs/                      feasibility study, settings guide; later: setup guide, troubleshooting
```

## Roadmap

- [x] M0 — Feasibility & device choice
- [x] M1 — Project setup, storage/ordering logic + tests, CI
- [x] M2 — Home screen: app row, launch, add/remove/reorder, woodland theme (device install pending)
- [~] M3 — Settings, Home-button service, focus polish (done, emulator-verified); performance numbers on the real device still to measure
- [ ] M4 — Signed release setup, default-launcher guide, troubleshooting
