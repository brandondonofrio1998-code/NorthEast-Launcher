#!/usr/bin/env bash
# UI smoke test on an Android TV emulator (run by .github/workflows/ui-smoke.yml).
# Installs the debug APK, makes it HOME, drives it with D-pad key events, takes screenshots
# and measures cold-start time. Emulator timings are NOT representative of a real stick;
# real numbers come from the device in M3.
set -u
PKG=com.northeast.launcher.debug
ACT=$PKG/com.northeast.launcher.MainActivity
OUT=ui-smoke
mkdir -p "$OUT"

key() { adb shell input keyevent "$@"; sleep 1.2; }
shot() { adb exec-out screencap -p > "$OUT/$1.png"; echo "screenshot: $1"; }

adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd package set-home-activity --user 0 "$ACT" || true
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tee "$OUT/home-resolves-to.txt"

# Cold start, 5 runs.
for i in 1 2 3 4 5; do
  adb shell am force-stop "$PKG"
  sleep 1
  adb shell am start -W -n "$ACT" | grep -E "TotalTime|WaitTime" | tee -a "$OUT/cold-start.txt"
  sleep 2
done

# Stock launchers keep the Home key (see docs/FEASIBILITY.md §3), so open ours directly.
adb shell am start -n "$ACT"
sleep 3
shot 1-home-first-run

# Walk right to the "+ Edit apps" tile at the end of the row and open the app picker.
for _ in 1 2 3 4 5 6; do key KEYCODE_DPAD_RIGHT; done
key KEYCODE_DPAD_CENTER
sleep 1
shot 2-app-picker

# Pin the first three apps in the picker, then go back home.
key KEYCODE_DPAD_CENTER
key KEYCODE_DPAD_RIGHT
key KEYCODE_DPAD_CENTER
key KEYCODE_DPAD_RIGHT
key KEYCODE_DPAD_CENTER
shot 3-app-picker-with-pins
key KEYCODE_BACK
sleep 1
shot 4-home-with-pins
# Back from the app list highlights "Edit apps"; walk left to the first app tile.
for _ in 1 2 3 4 5 6; do key KEYCODE_DPAD_LEFT; done

# Hold OK on the first tile → options; choose Move; move it right once; screenshot; put it down.
adb shell input keyevent --longpress KEYCODE_DPAD_CENTER
sleep 1.5
shot 5-tile-options
key KEYCODE_DPAD_CENTER
key KEYCODE_DPAD_RIGHT
shot 6-move-mode
key KEYCODE_DPAD_CENTER
shot 7-after-move

# Frame stats for a few seconds of D-pad navigation on the home row.
adb shell dumpsys gfxinfo "$PKG" reset > /dev/null
for _ in 1 2 3 4; do key KEYCODE_DPAD_RIGHT; done
for _ in 1 2 3 4; do key KEYCODE_DPAD_LEFT; done
adb shell dumpsys gfxinfo "$PKG" | sed -n '/Total frames rendered/,/Number Frame deadline missed/p' | tee "$OUT/gfxinfo.txt"

# ---- Settings (M3). Row numbers follow the order in SettingsScreen.kt. ----
for _ in 1 2 3 4 5 6; do key KEYCODE_DPAD_LEFT; done   # back to the first tile
key KEYCODE_DPAD_UP                       # row → settings gear
shot 10-gear-focused
key KEYCODE_DPAD_CENTER                   # open settings (no PIN set)
sleep 1
shot 11-settings-top
key KEYCODE_DPAD_RIGHT                    # Color theme: Automatic → Winter
shot 12-settings-color-winter
key KEYCODE_DPAD_LEFT                     # back to Automatic
for _ in 1 2 3 4 5 6; do key KEYCODE_DPAD_DOWN; done   # row 7: Tile size
key KEYCODE_DPAD_RIGHT                    # Medium → Large
for _ in $(seq 10); do key KEYCODE_DPAD_DOWN; done     # row 17: Date
key KEYCODE_DPAD_CENTER                   # Date on
key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER   # row 18: Greeting on
key KEYCODE_DPAD_DOWN; key KEYCODE_DPAD_CENTER   # row 19: Season name on
shot 13-settings-clock-section
for _ in $(seq 15); do key KEYCODE_DPAD_DOWN; done     # row 34: Remote button test
key KEYCODE_DPAD_CENTER
sleep 1
key KEYCODE_MENU
key KEYCODE_GUIDE
key KEYCODE_PROG_RED
shot 14-remote-test
key KEYCODE_BACK
for _ in 1 2 3; do key KEYCODE_DPAD_UP; done           # row 31: Home button setup
key KEYCODE_DPAD_CENTER
sleep 1
shot 15-home-button-setup
key KEYCODE_BACK
for _ in $(seq 10); do key KEYCODE_DPAD_DOWN; done     # to the bottom: reset & about
shot 16-settings-bottom
key KEYCODE_BACK                          # leave settings
sleep 1
shot 17-home-customized

# ---- M4: tools/device.sh end to end (make-default → disable-stock → reboot → restore) ----
# A physical Home press can't be simulated here: shell-injected keys skip accessibility key filtering
# by design, and the Android TV emulator image has no root for writing to input devices. The Home
# takeover itself is checked on a real device with its remote; here we check that the service binds.
DEV="bash tools/device.sh --package $PKG"
$DEV status | tee "$OUT/device-status-before.txt"
$DEV make-default | tee "$OUT/device-make-default.txt"
sleep 4
adb shell dumpsys accessibility | grep -E "Bound services|Enabled services" | tee "$OUT/home-service.txt"
adb logcat -d | grep "NorthEastHome" | tail -5 | tee -a "$OUT/home-service.txt"
SERVICE_BOUND=0
grep -q "NorthEast Home button" "$OUT/home-service.txt" && SERVICE_BOUND=1

$DEV disable-stock --yes | tee "$OUT/device-disable-stock.txt"
adb shell input keyevent KEYCODE_HOME; sleep 3
shot 21-home-key-stock-disabled
# Reboot: with the stock launcher off, the TV must boot straight into NorthEast (no soft-brick).
adb reboot
adb wait-for-device
for _ in $(seq 90); do [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = 1 ] && break; sleep 2; done
sleep 15
shot 22-after-reboot-stock-disabled
adb shell dumpsys activity activities | grep -m1 -E "mResumedActivity|topResumedActivity" | tee "$OUT/after-reboot.txt"
{
  echo "--- disabled packages after reboot:"; adb shell pm list packages -d
  echo "--- tvlauncher state:"; adb shell dumpsys package com.google.android.tvlauncher | grep -iE "enabled|User 0:" | head -5
  echo "--- home resolves to:"; adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
  echo "--- boot log about the launcher:"; adb logcat -d | grep -iE "tvlauncher.*(enabl|disabl)|setEnabledSetting|PackageManager.*enable" | tail -15
} 2>&1 | tr -d '\r' | tee -a "$OUT/after-reboot.txt"
grep -q "$PKG" "$OUT/after-reboot.txt" || echo "NOTE: NorthEast was not the home screen after reboot (see after-reboot.txt)"

$DEV restore | tee "$OUT/device-restore.txt"
sleep 3
shot 23-after-restore
$DEV status | tee "$OUT/device-status-after.txt"
grep -q "tvlauncher (enabled)" "$OUT/device-status-after.txt" || { echo "::error::restore did not re-enable the stock launcher"; FAILED=1; }
grep -q "Home-button service: off" "$OUT/device-status-after.txt" || { echo "::error::restore did not turn the service off"; FAILED=1; }
grep -q "resolves to: com.google.android.tvlauncher" "$OUT/device-status-after.txt" || { echo "::error::restore did not give Home back to the stock launcher"; FAILED=1; }

# ---- Release build (signed in CI with a throwaway key): installs, starts, cold-start timing ----
REL=app/build/outputs/apk/release/app-release.apk
if [ -f "$REL" ]; then
  adb install -r "$REL"
  for i in 1 2 3; do
    adb shell am force-stop com.northeast.launcher; sleep 1
    adb shell am start -W -n com.northeast.launcher/com.northeast.launcher.MainActivity | grep -E "Status|TotalTime" | tee -a "$OUT/release-cold-start.txt"
    sleep 2
  done
  shot 24-release-build
  grep -q "Status: ok" "$OUT/release-cold-start.txt" || { echo "::error::release build did not start"; FAILED=1; }
else
  echo "::error::signed release APK missing"; FAILED=1
fi

ls -l "$OUT"
if [ "$SERVICE_BOUND" != 1 ]; then echo "::error::Home-button service did not bind"; FAILED=1; fi

# Print everything into the log too (short base64 lines): some reviewers can read CI logs but
# not download artifacts. Done here, not in a later step, in case emulator teardown hangs.
for f in "$OUT"/*; do
  echo "=====BEGIN $f"
  base64 -w 2000 "$f"
  echo "=====END $f"
done

exit "${FAILED:-0}"
