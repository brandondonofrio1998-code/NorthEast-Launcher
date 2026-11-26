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

key KEYCODE_HOME
sleep 2
shot 1-home-first-run

# Open the app picker from the "+" tile (it's focused when nothing is pinned yet).
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

ls -l "$OUT"
