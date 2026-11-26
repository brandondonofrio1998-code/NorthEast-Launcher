# M0 — Feasibility

*Researched 2026-11-25. Everything below about the current state of devices and firmware was checked against sources listed at the bottom. Launcher behaviour on Google TV changes with OTA updates, so re-check before buying.*

## TL;DR

| Question | Answer |
|---|---|
| Custom firmware on the Roku TV? | **No.** Model not given, so assumed locked. Roku uses signed boot chains, and no current public method exists to replace Roku OS. Even if one did, a self-built OS would not have certified Widevine DRM, so HBO Max / Crunchyroll / etc. would not play in HD (or at all). |
| "Create the OS itself" for all my TVs? | **Not the right target.** What's buildable and useful is a **launcher** (the home-screen app) for Android TV / Google TV. A custom OS (AOSP image) can't get Google certification or Widevine L1, so the streaming apps you listed would break. The launcher runs on any Android TV / Google TV stick or box, and on TVs that ship with Google TV built in (Sony, TCL, Hisense, Philips…). It cannot run on Roku OS, Samsung Tizen or LG webOS. Those TVs get a stick on HDMI. |
| Can a custom launcher fully replace the Google TV home screen in late 2026? | **Yes, but it takes two jobs and it's fragile.** (1) Make our app the resolved HOME activity, and (2) intercept the remote's Home key with a small accessibility service, because Google TV wires that button to its own launcher. Google TV updates have broken this for some users this year. Plan accordingly (see §3). |
| Recommended hardware | **Onn 4K Pro (2026)** (best value), **Google TV Streamer** (fastest Google TV), **NVIDIA Shield TV Pro** (most launcher-friendly, but expensive and old). Details in §4. |

---

## 1. The Roku TV

**Model number:** not provided → treated as *unknown, locked firmware*.

- Roku devices have long used a signed secure-boot chain and a hardened kernel. The only public secure-boot break (GTVHacker, 2013) relied on per-box keys present on one old Broadcom chipset and has no bearing on current Roku TVs.
- Even a hypothetical custom OS would fail the real goal: Netflix-class apps (HBO Max, Crunchyroll, Spotify's video features, YouTube in HD) require a Google-certified device with hardware-backed Widevine. An unofficial OS image doesn't get that.

**What to do with the Roku TV instead (no hacking, fully reversible):**

1. Plug the Google TV device into an HDMI port.
2. On the TV: **Settings → System → Power → Power on → choose that HDMI input** (or "Last used TV input"). The TV then boots straight to the stick and you rarely see Roku's own home screen.
3. Turn on **HDMI-CEC** on both sides so one remote powers both. Caveat: some owners report the power-on input setting is ignored when the TV is woken over CEC. If that happens on your set, wake it with the TV remote or use "Last used input".
4. Optional privacy step on the Roku side: under the TV's **Privacy** settings, turn off the setting that uses information from TV inputs (Roku's ACR / "Smart TV experience"). This is the TV snooping on what the HDMI device shows; our launcher can't block it, only this toggle can.

## 2. Scope: launcher, not OS

We're building **an Android app that registers as a HOME launcher**. That gives you:

- Your own home screen on any Android TV / Google TV device, including future Google TV televisions.
- Every streaming app still runs normally because the device keeps its certified OS, Play Store, and DRM.
- One signed APK you can install on every Android-based TV/stick you own.

What it can't do (and won't pretend to):

- Replace Roku OS, Tizen, webOS, or Fire OS's newer Vega OS.
- Remove ads *inside* other apps (YouTube's own ads, etc.).
- Stop the TV panel itself (Roku, Samsung, LG) from tracking via ACR. That's a TV setting (§1).

## 3. How launcher replacement works on Google TV right now

Two separate problems, both must be solved:

1. **HOME resolution.** Google TV keeps its stock launcher (`com.google.android.apps.tv.launcherx`) as the preferred HOME even after you "choose" another one. Fix: `adb shell cmd package set-home-activity --user 0 <our.package>/.MainActivity`, then verify with `cmd package resolve-activity`.
2. **The physical Home button.** On Google TV the remote's Home key goes to the stock launcher at a lower level, bypassing the HOME setting. Some remotes don't even send `KEYCODE_HOME` (Xiaomi sends `KEYCODE_ALL_APPS`). Fix: a tiny **accessibility service** in our app whose only job is to catch Home (and any alternate keycode) and bring our launcher forward. This is the same mechanism Projectivy's "Override current launcher" uses.

**Known fragility, 2026:**

- Android Authority (2026) reported a Google TV update that stopped Projectivy's override for some users. Projectivy still opened manually but no longer owned the Home button. It's unclear if that was a bug or intentional.
- Some guides go further and `pm disable-user` the stock launcher plus `com.google.android.tungsten.setupwraith`. That's reversible over ADB, but others report **boot loops / soft-bricks** when the fallback doesn't land on a working launcher. **Our guide will treat disabling launcherx as optional, last, and only after the accessibility method is verified on your exact device.**
- On at least one older Hisense Android TV 10 set, the Home key stopped reaching anything once the stock launcher was out of the way. Built-in Google TV TVs vary by brand.
- Users of Projectivy on the Onn 4K Pro have reported some dedicated remote buttons (settings, star, profile, guide) stop doing anything, because they were handled by the stock launcher.

**Fallback if Google closes the door:** the stock Google TV launcher has an **Apps-only mode** that strips most recommendations and sponsored rows. Our launcher would then still be one button (the remappable star button on Onn remotes) away. That's the honest worst case.

**Design implications for our launcher (feeds M1–M2):**

- Manifest: `MAIN` + `HOME` + `DEFAULT` + `LEANBACK_LAUNCHER` intent categories.
- An optional, minimal `AccessibilityService` with `canRequestFilterKeyEvents`, handling only `KEYCODE_HOME` and configurable alternates. It reads no screen content. On newer Android, sideloaded apps may be blocked from enabling accessibility from the UI, so we'll enable it via ADB (`settings put secure …`, appending, never overwriting).
- `BOOT_COMPLETED` receiver to come up on power-on.
- **No `INTERNET` permission at all.** The OS will enforce "no network calls from the launcher".
- A "Re-link Home button" diagnostic screen in Settings showing which keycode the remote sends, so you can fix it after a Google update without a laptop when possible.

## 4. Recommended devices

All three run YouTube, Crunchyroll, Spotify and HBO Max from the Play Store.

| | **Onn 4K Pro (2026)** — *recommended for the bedroom* | **Google TV Streamer** | **NVIDIA Shield TV Pro** |
|---|---|---|---|
| Price (Nov 2026) | $59.98 (Walmart, US only) | $99.99 | $299.99 (raised from $199.99 on Oct 2, 2026; standard Shield discontinued) |
| Platform | Google TV | Google TV (gets updates first) | Android TV (not Google TV), Android 11 |
| RAM / storage | 3 GB / 32 GB | 4 GB / 32 GB | 3 GB / 16 GB |
| Ports | HDMI, 100 Mbps Ethernet, USB-A | HDMI, Gigabit Ethernet | HDMI, Gigabit Ethernet, 2× USB 3.0 |
| Launcher friendliness | Good. Widely used with Projectivy; needs the accessibility override. Remappable star button is a useful fallback. | Good, same method. Being first in line for updates also means it's first to get anything that breaks overrides. | Best. Classic Android TV, long track record with custom launchers. But old hardware, stuck on Android 11, and expensive now. |
| Why pick it | Cheap, fast enough, mic hardware switch, USB-A for storage/keyboard. | Faster chip, more RAM, better Ethernet. | Strongest hardware, 64-bit, best for local media. Only if you also want it as a Plex/gaming box. |

**My call:** start with **one Onn 4K Pro (2026)** for the bedroom. It's cheap enough to be a test bed, and if the launcher holds up on it, buy more for the other TVs. Skip Fire TV sticks: Amazon has repeatedly broken custom-launcher methods.

## 5. Minimum SDK proposal (decided in M1)

- Onn 4K Pro / Google TV Streamer: Android TV 12–16 (API 31+).
- Shield TV Pro: Android 11 (API 30).
- Older Google TV / Android TV televisions you might own later: Android 9–10 (API 28–29).

**Proposal: `minSdk = 28`, `targetSdk = 35`.** That covers every device above plus most Android-based TVs from 2019 on. Compose for TV supports it, and it costs us almost nothing.

## 6. Risks I'm tracking

| Risk | Mitigation |
|---|---|
| A Google TV OTA breaks Home-button override | Accessibility approach first, keycode diagnostics screen, documented re-enable steps, Apps-only mode + star-button fallback |
| Disabling stock launcher soft-bricks the box | Never required; documented as optional last step with recovery commands kept to hand |
| Some remote buttons (guide, profile) stop working | Document per device; map what we can (e.g. launch Settings from our app) |
| Roku TV ignores power-on input via CEC | Use "Last used input"; wake with TV remote |

---

## Sources

- Android Authority — *A Google TV update may have broken one of the best ways to avoid home screen ads* — https://www.androidauthority.com/google-tv-update-blocks-projectivy-override-3649445
- Mustafa.net — *How to Replace the Google TV Launcher Without Root* (Nov 1, 2026) — https://mustafa.net/2026/11/01/replace-google-tv-launcher-without-root/
- Bitdoze — boot-loop warning on disabling launcherx — https://www.bitdoze.com/md/smart-tv-custom-home-screen-remove-ads.md
- freya.cat — disabling launcherx + setupwraith, soft-brick warning — https://freya.cat/blog/post/2025-03-11
- Android Authority — Projectivy & Apps-only mode — https://www.androidauthority.com/google-tv-ads-projectivy-launcher-3573626
- Android Authority — Onn 4K Pro (2026) review — https://www.androidauthority.com/onn-4k-pro-2026-review-3677202/
- iTechGuides — 2026 streaming device prices — https://www.itechguides.com/?p=353830
- 9to5Google — Shield TV Pro price hike (Oct 2, 2026) — https://9to5google.com/2026/10/02/nvidia-shield-tv-pro-price-hike/
- Wccftech — standard Shield discontinued — https://wccftech.com/nvidia-axes-standard-shield-tv-hikes-shield-tv-pro-to-299-blaming-rising-dram-costs/
- r/Projectivy_Launcher — remote buttons not working on Onn 4K Pro — https://redlib.hackliberty.org/r/Projectivy_Launcher/comments/1lc7ywr/some_buttons_dont_work/my726yf/?context=3
- GTVHacker — *Breaking Secure-Boot on the Roku* (2013) — https://blog.gtvhacker.com/2013/12/
- Roku Community / Philips — Roku TV Power-on input setting — https://community.roku.com/t5/Roku-setup/TCL-75-quot-ROKU-TV-WITH-ROKU-STREAM-BAR-PRO/td-p/840688 , https://www.usa.philips.com/c-f/XC000020535/how-do-i-have-my-philips-roku-tv-always-turn-on-to-the-input-of-my-choice
- AFTVnews — Fire TV launcher workarounds being broken — https://www.aftvnews.com/?p=42631
