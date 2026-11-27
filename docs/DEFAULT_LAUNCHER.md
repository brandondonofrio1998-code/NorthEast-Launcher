# Making NorthEast the home screen

There are three levels. Stop at whichever one gives you what you want. Every step can be undone with
one command (`tools/device.sh restore`), and none needs root.

| Level | What you get | Risk |
|---|---|---|
| 1. Just install it | NorthEast opens like any app. The stock home screen (with its ads) is still what Home shows. | None |
| 2. **Home button takeover** (recommended) | Home on the remote opens NorthEast. The stock launcher stays installed but you rarely see it. | Very low. A Google TV update can switch it off again; re-run one command. |
| 3. Turn off the stock launcher (optional) | The stock home screen and its ads/recommendations are off completely. The TV boots straight into NorthEast. | Low, with the safeguards below, but it's the one step that has soft-bricked devices for other people. Read step 4 first. |

Why Google TV needs level 2 at all: it keeps the Home button for its own launcher even after you choose
another one. See [FEASIBILITY.md §3](FEASIBILITY.md).

## Before you start

1. **Developer options:** on the TV, **Settings → System → About → Android TV OS build**. Press OK on
   it 7 times until it says you're a developer.
2. **Debugging:** **Settings → System → Developer options** → turn on **USB debugging**, plus
   **Wireless debugging** if your TV has that option.
3. **Find the TV's IP:** **Settings → Network & Internet →** your network.
4. On your Mac, in the project folder:

   ```bash
   adb connect <STICK IP ADDRESS>      # accept "Allow debugging?" on the TV, tick "Always allow"
   tools/device.sh status
   ```

   Some newer Google TV versions want pairing first: in Wireless debugging choose **Pair device with
   pairing code**, then `adb pair <ip>:<pairing port>` and enter the code, then `adb connect` as above.

`tools/device.sh` works on the **release** build (`com.northeast.launcher`). For a debug build add
`--package com.northeast.launcher.debug` before the command.

## Step 1: Install

```bash
tools/device.sh install app/build/outputs/apk/release/app-release.apk
```

(See [RELEASE.md](RELEASE.md) for building it. To try things out first, use a debug APK from the repo's
GitHub Actions page.) Open NorthEast from the TV's app list and make sure your apps launch.

## Step 2: Make it the home screen and take over the Home button

```bash
tools/device.sh make-default
```

This does three things:

1. sets NorthEast as the preferred home activity (`cmd package set-home-activity`);
2. on Android 13 and newer, allows "restricted settings" for NorthEast. Without this, Android won't let
   a sideloaded app's accessibility service be turned on;
3. turns on the **NorthEast Home button** service. It's added to any accessibility services you already
   use; nothing is replaced.

Now open any app and press **Home** on the remote. NorthEast should come up. If the stock home screen
appears instead, see *Home button still opens Google TV* in [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

**About the service:** it is an accessibility service because that's the only no-root way an app can
see the Home button on Google TV. It is configured to receive key presses only. It cannot read the
screen (`canRetrieveWindowContent="false"`), subscribes to no accessibility events, and the app has no
internet permission at all. It acts on Home (plus one optional extra button you choose) and passes every
other button through untouched. Settings → **Home button takeover** switches it off without
uninstalling anything.

### Doing step 2 by hand instead

```bash
adb shell cmd package set-home-activity --user 0 com.northeast.launcher/com.northeast.launcher.MainActivity
adb shell appops set com.northeast.launcher ACCESS_RESTRICTED_SETTINGS allow        # Android 13+ only
adb shell settings get secure enabled_accessibility_services                         # note what's there
adb shell settings put secure enabled_accessibility_services "<what was there>:com.northeast.launcher/com.northeast.launcher.home.HomeButtonService"
adb shell settings put secure accessibility_enabled 1
```

If nothing was there (`null`), leave out the `<what was there>:` part. NorthEast's **Settings → Home
button setup** screen shows these commands with the right package name for whichever build you have
installed.

## Step 3: Check it

- Press **Home** from inside YouTube (or any app): NorthEast appears.
- Press **Home** on the home screen itself: focus goes to your first app. This is configurable in Settings.
- **Settings → Remote button test** shows what each remote button sends. Some remotes have extra buttons
  (a "Google TV" or settings key, Guide, a star key) that the stock launcher handled. If one of them
  stops doing anything, you can make it a second Home button from that screen.

Most people can stop here.

## Step 4 (optional): Turn off the stock launcher and its ads

This removes the stock home screen, with its ads, recommendations and "continue watching", from the
picture entirely. Your apps, the Play Store and system settings keep working. Extras that live inside
the stock home screen may stop working: its quick-settings/dashboard panel, profile switching, and some
dedicated remote buttons. NorthEast's settings include **Open Android TV settings** for that reason.

**Safeguards. Read before running:**

- Only do this after step 3 works. The script refuses to continue if NorthEast isn't installed or
  doesn't start.
- On Google TV, the script also turns off `com.google.android.tungsten.setupwraith`, the fallback home
  that would otherwise take over. A device with **no** working home app is what causes the "soft
  brick" reports online; that's why NorthEast must be installed and working first.
- **Never uninstall NorthEast while the stock launcher is off.** Run `restore` first.
- On a **TV with Google TV built in** (not a stick), the stock launcher may also handle switching HDMI
  inputs. If inputs stop working, run `restore`.
- Keep ADB set up afterwards. It's how you undo this.

```bash
tools/device.sh disable-stock --yes
```

The packages it turns off (only those present on your device):

| Package | What it is |
|---|---|
| `com.google.android.apps.tv.launcherx` | Google TV home screen |
| `com.google.android.tungsten.setupwraith` | Google TV fallback home |
| `com.google.android.tvlauncher` | Android TV home screen (e.g. Shield) |
| `com.google.android.leanbacklauncher` | Older Android TV home screen |

It uses `pm disable-user`, which turns a package off for your user without uninstalling anything. A
factory reset also turns them back on.

**Wait about 30 seconds before restarting the TV.** Android saves package on/off states to disk
a few seconds after the command. In testing, rebooting right away lost the change and the stock launcher
came back. That's harmless, but confusing. After the wait, a reboot goes straight to NorthEast.

## Undo everything

```bash
tools/device.sh restore               # stock launcher back on, Home given back to it, service off
tools/device.sh restore --uninstall   # same, then uninstall NorthEast
```

By hand:

```bash
adb shell pm enable com.google.android.apps.tv.launcherx           # Google TV
adb shell pm enable com.google.android.tungsten.setupwraith        # Google TV
adb shell pm enable com.google.android.tvlauncher                  # Android TV
adb shell settings put secure enabled_accessibility_services "<the list without NorthEast's entry>"
adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME
```

If the TV ever boots to a blank screen: ADB over the network usually still connects, so run
`tools/device.sh restore`. If even ADB is unreachable, a factory reset from the device's recovery menu
restores everything. How to reach it varies by device; for the Onn 4K Pro it's in Walmart/Onn's support
pages.

## Without a computer

You can't turn on the service from the TV alone on most current Google TV versions: the switch in
**Settings → System → Accessibility** is greyed out for sideloaded apps ("restricted setting"). What
works without a computer:

- Open NorthEast from the app list (or put it first in Google TV's app row).
- On Onn remotes, the programmable ★ button can be set to open NorthEast.
- Google TV's **Apps only mode** (Settings → Accounts & sign-in → your profile) removes most
  recommendation rows from the stock screen. Google has added ads to that mode in some countries.

What CI checks automatically on an Android TV 11 emulator: `make-default` binds the service,
`disable-stock` followed by a **reboot** lands on NorthEast, and `restore` turns the stock launcher back
on, gives Home back to it and turns the service off. The emulator runs Android TV's launcher
(`tvlauncher`); Google TV's `launcherx` and `setupwraith` follow the same commands but are first tested
on a real Google TV device.
