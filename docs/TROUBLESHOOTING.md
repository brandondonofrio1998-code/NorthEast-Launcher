# Troubleshooting

Start with `tools/device.sh status`. It shows the device, whether NorthEast is installed, what Home
currently opens, whether the Home-button service is on, and which stock launchers are enabled.

## Connecting and installing

**`adb connect` times out or is refused**
- The Mac and the TV must be on the same network, and some routers isolate devices on guest Wi-Fi.
- Check **Developer options → USB debugging** (and **Wireless debugging**, if present) are on.
- Newer Google TV: pair first. Go to **Wireless debugging → Pair device with pairing code**, run
  `adb pair <ip>:<pairing port>`, then `adb connect <ip>:<port shown under Wireless debugging>`.
- `adb devices` shows `unauthorized`: accept the prompt on the TV (tick **Always allow**). If it never
  appears, run `adb kill-server`, connect again, and watch the TV.

**`INSTALL_FAILED_UPDATE_INCOMPATIBLE`**
The installed copy was signed with a different key (for example a CI build replaced by your own release
build). Run `adb uninstall com.northeast.launcher` and install again. That wipes NorthEast's settings
and app order. If the stock launcher is turned off, run `tools/device.sh restore` **first**.

**Release APK is named `app-release-unsigned.apk`**
`keystore.properties` is missing, incomplete, or still has `[PLACEHOLDER]` values. See
[RELEASE.md](RELEASE.md).

## Home button

**Home button still opens Google TV**
1. `tools/device.sh status`. Is the Home-button service **ON**? If not, run `tools/device.sh make-default`.
   Google TV updates sometimes switch accessibility services off.
2. In NorthEast: **Settings → Home button takeover** must be **On**.
3. **Settings → Remote button test**, then press Home. With the service on it should appear as `HOME`
   "via service". If your remote's Home sends something else, that name appears instead: choose
   **Use … as an extra Home button**.
4. Still nothing? Some Google TV builds stopped third-party launchers from owning Home in 2026. The
   remaining no-root option is step 4 of [DEFAULT_LAUNCHER.md](DEFAULT_LAUNCHER.md) (turn off the stock
   launcher). Developer logs: `adb logcat -s NorthEastHome` prints a line each time the service handles Home.

**The accessibility switch for NorthEast is greyed out on the TV**
That's Android's "restricted setting" for sideloaded apps. Use `tools/device.sh make-default`, which
lifts the restriction for NorthEast only and turns the service on over ADB.

**A remote button stopped working after switching launchers** (dashboard, profile, Guide, ★…)
Those were handled by the stock home screen. Use **Settings → Remote button test** to see what the button
sends, and make it an extra Home button if that's useful. **Settings → Open Android TV settings** replaces
the stock settings shortcut.

**Long-pressing Home no longer opens the dashboard / Assistant**
With the takeover on, NorthEast receives Home presses (short and long). Turn **Home button takeover** off
in settings to get the stock behavior back without undoing anything else.

## Home screen

**An app is missing from the app list**
- **Settings → App list shows** may be set to *TV apps only*. Switch it to *All apps*.
- Apps with no launcher entry at all (background services, some system apps) can't be shown by any
  launcher.

**An app I pinned disappeared from the home screen**
It was uninstalled, or it's mid-update. Its spot is remembered and it comes back when reinstalled.

**Tiles show icon + name even though "App banner" is selected**
That app doesn't ship a TV banner, so NorthEast falls back to its icon.

**The edges of the screen are cut off**
**Settings → Screen edge padding** (try Medium). Or turn off overscan in the TV's picture settings
("Just Scan", "Screen fit", "1:1", depending on the brand).

**It feels slow on a cheap stick**
In settings, set **Highlight zoom → Off** and **Scenery → Front trees only** (or None), and use
**Tile artwork → Icon + name**. Everything else is already static.

**Colors are wrong for the season**
**Color theme** follows the device's date. Check **Settings → Open Android TV settings → System → Date &
time**. Or lock a season in **Color theme**.

## PIN and lock

**Forgot the settings PIN**
There's no back door in the app. Clearing NorthEast's data removes the PIN, settings and app order (your
apps themselves are untouched):

```bash
adb shell pm clear com.northeast.launcher
```

**Can't move or remove apps / no "Edit apps" tile**
**Lock home screen layout** is on. Turn it off in settings, or edit apps from **Settings → Edit home
screen apps** while it stays locked.

## The TV itself (Roku TV with a stick)

**The Roku TV starts on Roku's own home screen instead of the stick**
On the Roku TV: **Settings → System → Power → Power on** → choose the stick's HDMI input (or **Last used
TV input**).

**One remote doesn't turn both on**
Turn on HDMI-CEC on both: on the Roku TV, **Settings → System → Control other devices (CEC)**; on the
stick, **Settings → Display & Sound → HDMI-CEC**. Some Roku TVs ignore the power-on input when woken
over CEC; use **Last used TV input**.

## Getting the stock setup back

```bash
tools/device.sh restore               # stock launcher on, Home back to it, service off
tools/device.sh restore --uninstall   # and remove NorthEast
```

If the device won't boot to any home screen, see the end of [DEFAULT_LAUNCHER.md](DEFAULT_LAUNCHER.md).
