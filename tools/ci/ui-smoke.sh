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

# ---- Home button takeover with the accessibility service, stock launcher still enabled ----
SVC="$PKG/com.northeast.launcher.home.HomeButtonService"
adb shell settings put secure enabled_accessibility_services "$SVC"
adb shell settings put secure accessibility_enabled 1
sleep 3
adb shell dumpsys accessibility | grep -iE "northeast|Bound services|Enabled services" | tee "$OUT/home-key-with-service.txt"
adb shell am start -a android.settings.SETTINGS; sleep 3
shot 18-another-app-open
# (a) Shell-injected Home key.
adb shell input keyevent KEYCODE_HOME; sleep 3
shot 19-home-key-with-service-input
echo "--- after 'input keyevent HOME':" >> "$OUT/home-key-with-service.txt"
adb shell dumpsys activity activities | grep -m2 -E "topResumedActivity|mResumedActivity" | tee -a "$OUT/home-key-with-service.txt"
# (b) Home written straight into a kernel input device (needs root): the same path a real remote
#     takes, so it passes through accessibility key filtering, unlike shell-injected keys.
adb root > /dev/null 2>&1 || true
sleep 3; adb wait-for-device
echo "--- input devices with a Home key:" >> "$OUT/home-key-with-service.txt"
adb shell getevent -pl 2>/dev/null | grep -E "add device|name:|KEY_HOME" | tee -a "$OUT/home-key-with-service.txt" > /dev/null
DEV=$(adb shell getevent -pl 2>/dev/null | tr -d '\r' | awk '/add device/{d=$4} /KEY_HOMEPAGE|KEY_HOME /{print d; exit}')
echo "device: ${DEV:-none}" >> "$OUT/home-key-with-service.txt"
for CODE in 172 102; do   # KEY_HOMEPAGE, KEY_HOME — whichever the keyboard layout maps to HOME
  adb shell am start -a android.settings.SETTINGS; sleep 3
  if [ -n "$DEV" ]; then
    adb shell "sendevent $DEV 1 $CODE 1; sendevent $DEV 0 0 0; sendevent $DEV 1 $CODE 0; sendevent $DEV 0 0 0"
  fi
  sleep 3
  shot "20-home-key-hardware-$CODE"
  echo "--- after hardware key $CODE:" >> "$OUT/home-key-with-service.txt"
  adb shell dumpsys activity activities | grep -m2 -E "topResumedActivity|mResumedActivity" | tee -a "$OUT/home-key-with-service.txt"
done
adb logcat -d | grep -E "NorthEastHome|ActivityTaskManager.*(Background|BAL|Abort)" | tail -20 | tee -a "$OUT/home-key-with-service.txt"
adb shell settings put secure enabled_accessibility_services ""
adb shell settings put secure accessibility_enabled 0

# Emulator only (throwaway device): does the Home key reach us once the stock launcher is out
# of the way? This checks our HOME intent filter, not the recommended setup for real devices.
adb shell input keyevent KEYCODE_HOME; sleep 2
shot 8-home-key-stock-launcher
adb shell pm disable-user --user 0 com.google.android.tvlauncher | tee -a "$OUT/home-key-test.txt"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME; sleep 3
adb shell input keyevent KEYCODE_HOME; sleep 3
shot 9-home-key-stock-disabled
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tee -a "$OUT/home-key-test.txt"
adb shell pm enable com.google.android.tvlauncher | tee -a "$OUT/home-key-test.txt"

ls -l "$OUT"

# Print everything into the log too (short base64 lines): some reviewers can read CI logs but
# not download artifacts. Done here, not in a later step, in case emulator teardown hangs.
for f in "$OUT"/*; do
  echo "=====BEGIN $f"
  base64 -w 2000 "$f"
  echo "=====END $f"
done
