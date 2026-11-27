#!/usr/bin/env bash
# NorthEast Launcher — device helper. Runs on your computer and talks to the TV over ADB.
# Every change it makes is reversible with `restore`. No root needed.
#
#   tools/device.sh status
#   tools/device.sh install <apk>
#   tools/device.sh make-default            # make NorthEast the home screen + turn on the Home-button service
#   tools/device.sh disable-stock --yes     # OPTIONAL, advanced: turn off the stock launcher (and its ads)
#   tools/device.sh restore [--uninstall]   # undo everything above
#
# Options (before the command):
#   --package <id>   com.northeast.launcher (release, default) or com.northeast.launcher.debug
#   -s <serial>      which device, if several are connected (e.g. 192.168.1.50:5555)
#
# Full guide: docs/DEFAULT_LAUNCHER.md
set -u

PKG="com.northeast.launcher"
ADB=(adb)

# Stock home screens this script knows how to turn off and back on.
#   launcherx       – Google TV home screen
#   setupwraith     – Google TV's fallback home (it takes over if launcherx is off)
#   tvlauncher      – Android TV home screen (e.g. Shield, older sticks)
#   leanbacklauncher– very old Android TV home screen
STOCK_PACKAGES=(
  com.google.android.apps.tv.launcherx
  com.google.android.tungsten.setupwraith
  com.google.android.tvlauncher
  com.google.android.leanbacklauncher
)

die() { echo "error: $*" >&2; exit 1; }
say() { echo "==> $*"; }
sh_() { "${ADB[@]}" shell "$@" | tr -d '\r'; }

while [ $# -gt 0 ]; do
  case "$1" in
    --package) PKG="$2"; shift 2 ;;
    -s) ADB=(adb -s "$2"); shift 2 ;;
    -h|--help) sed -n '2,17p' "$0"; exit 0 ;;
    *) break ;;
  esac
done
CMD="${1:-status}"; shift || true

ACTIVITY="$PKG/com.northeast.launcher.MainActivity"
SERVICE="$PKG/com.northeast.launcher.home.HomeButtonService"

need_device() {
  command -v adb > /dev/null || die "adb not found. Install Android SDK Platform-Tools."
  local state
  state=$("${ADB[@]}" get-state 2>/dev/null) || die "no device. Run: adb connect <STICK IP ADDRESS>"
  [ "$state" = "device" ] || die "device state is '$state' (accept the debugging prompt on the TV)"
}

installed() { sh_ pm list packages "$1" | grep -qx "package:$1"; }

home_resolves_to() {
  sh_ cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | tail -1
}

service_list() {
  local v; v=$(sh_ settings get secure enabled_accessibility_services)
  [ "$v" = "null" ] && v=""
  echo "$v"
}

service_on() { service_list | tr ':' '\n' | grep -qx "$SERVICE"; }

cmd_status() {
  need_device
  say "Device: $(sh_ getprop ro.product.manufacturer) $(sh_ getprop ro.product.model), Android $(sh_ getprop ro.build.version.release) (API $(sh_ getprop ro.build.version.sdk))"
  if installed "$PKG"; then say "NorthEast ($PKG): installed, version $(sh_ dumpsys package "$PKG" | grep -m1 versionName | sed 's/.*=//')"
  else say "NorthEast ($PKG): NOT installed"; fi
  say "Home screen resolves to: $(home_resolves_to)"
  if service_on; then say "Home-button service: ON"; else say "Home-button service: off"; fi
  for p in "${STOCK_PACKAGES[@]}"; do
    if installed "$p"; then
      if sh_ pm list packages -d "$p" | grep -qx "package:$p"; then say "Stock: $p (disabled)"
      else say "Stock: $p (enabled)"; fi
    fi
  done
}

cmd_install() {
  need_device
  local apk="${1:-}"
  [ -f "$apk" ] || die "usage: tools/device.sh install <path/to/app.apk>"
  say "Installing $apk"
  "${ADB[@]}" install -r "$apk" || die "install failed (see docs/TROUBLESHOOTING.md)"
}

cmd_make_default() {
  need_device
  installed "$PKG" || die "$PKG isn't installed. Install it first."

  say "Setting NorthEast as the preferred home activity"
  sh_ cmd package set-home-activity --user 0 "$ACTIVITY" || true

  # Android 13+ blocks accessibility for sideloaded apps until "restricted settings" are allowed.
  if [ "$(sh_ getprop ro.build.version.sdk)" -ge 33 ] 2>/dev/null; then
    sh_ appops set "$PKG" ACCESS_RESTRICTED_SETTINGS allow > /dev/null 2>&1 || true
  fi

  if service_on; then
    say "Home-button service already on"
  else
    say "Turning on the Home-button service (other accessibility services are kept)"
    local current new
    current=$(service_list)
    if [ -n "$current" ]; then new="$current:$SERVICE"; else new="$SERVICE"; fi
    sh_ settings put secure enabled_accessibility_services "$new"
    sh_ settings put secure accessibility_enabled 1
  fi

  sh_ am start -n "$ACTIVITY" > /dev/null
  say "Done. Press Home on the remote: NorthEast should open."
  say "If Google TV still opens, see 'Home button still opens Google TV' in docs/TROUBLESHOOTING.md."
}

cmd_disable_stock() {
  need_device
  [ "${1:-}" = "--yes" ] || die "this turns off the stock home screen. Read docs/DEFAULT_LAUNCHER.md step 4, then re-run with --yes"
  installed "$PKG" || die "$PKG isn't installed. Never turn off the stock launcher without a working replacement."
  sh_ am start -W -n "$ACTIVITY" | grep -q "Status: ok" || die "NorthEast didn't start, so the stock launcher stays on."
  for p in "${STOCK_PACKAGES[@]}"; do
    if installed "$p"; then
      say "Disabling $p"
      sh_ pm disable-user --user 0 "$p"
    fi
  done
  sh_ cmd package set-home-activity --user 0 "$ACTIVITY" > /dev/null 2>&1 || true
  say "Home now resolves to: $(home_resolves_to)"
  say "To undo at any time: tools/device.sh restore"
}

cmd_restore() {
  need_device
  for p in "${STOCK_PACKAGES[@]}"; do
    if installed "$p"; then
      say "Enabling $p"
      sh_ pm enable --user 0 "$p" > /dev/null
    fi
  done

  if service_on; then
    say "Turning off the Home-button service (other accessibility services are kept)"
    local rest
    rest=$(service_list | tr ':' '\n' | grep -vx "$SERVICE" | grep -v '^$' | paste -sd: -)
    if [ -n "$rest" ]; then
      sh_ settings put secure enabled_accessibility_services "$rest"
    else
      sh_ settings delete secure enabled_accessibility_services > /dev/null
      sh_ settings put secure accessibility_enabled 0
    fi
  fi

  # Point Home back at the stock launcher (whichever this device has).
  local stock_home
  stock_home=$(sh_ cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.HOME \
    | grep -E "^ *(com\.google\.android\.apps\.tv\.launcherx|com\.google\.android\.tvlauncher|com\.google\.android\.leanbacklauncher)/" \
    | head -1 | tr -d ' ')
  if [ -n "$stock_home" ]; then
    say "Setting $stock_home as the home screen again"
    sh_ cmd package set-home-activity --user 0 "$stock_home" > /dev/null 2>&1 || true
  fi

  if [ "${1:-}" = "--uninstall" ] && installed "$PKG"; then
    say "Uninstalling $PKG"
    "${ADB[@]}" uninstall "$PKG"
  fi

  sh_ am start -a android.intent.action.MAIN -c android.intent.category.HOME > /dev/null
  say "Home now resolves to: $(home_resolves_to)"
  say "If NorthEast is still installed and Android asks which home app to use, pick the stock one."
}

case "$CMD" in
  status) cmd_status ;;
  install) cmd_install "$@" ;;
  make-default) cmd_make_default ;;
  disable-stock) cmd_disable_stock "$@" ;;
  restore) cmd_restore "$@" ;;
  *) die "unknown command '$CMD' (try --help)" ;;
esac
