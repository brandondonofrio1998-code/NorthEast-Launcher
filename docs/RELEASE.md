# Release builds

Debug builds (`com.northeast.launcher.debug`) are fine for trying things out. For the TVs you actually
use, install a **signed release build** (`com.northeast.launcher`): it's smaller and faster (R8
shrinking and optimization), and it can be updated in place forever as long as you keep the same key.

## 1. Create your signing key (once)

Keep the key **outside** the project folder. The repo's `.gitignore` also blocks `*.jks`, `*.keystore`
and `keystore.properties`, so they can't be committed by accident.

```bash
mkdir -p ~/keys
keytool -genkeypair -v \
  -keystore ~/keys/northeast-release.jks \
  -alias northeast \
  -keyalg RSA -keysize 4096 -validity 10000
```

`keytool` asks for a keystore password, a key password (pressing Enter reuses the keystore password)
and a name. Any name works; it isn't shown anywhere. `keytool` comes with the JDK that Android Studio
bundles. If your shell can't find it, use
`"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"`.

**Back up `northeast-release.jks` and both passwords together** somewhere safe and encrypted (for
example your Proton Drive). If the key is lost, the TVs can't take updates until the app is uninstalled,
which also wipes its settings and app order.

## 2. Point the build at it

```bash
cp keystore.properties.template keystore.properties
```

Edit `keystore.properties` and replace every `[PLACEHOLDER]`:

```properties
storeFile=/Users/<you>/keys/northeast-release.jks
storePassword=[KEYSTORE PASSWORD]
keyAlias=northeast
keyPassword=[KEY PASSWORD]
```

If the file is missing, incomplete, or still has `[PLACEHOLDER]` values, the build still works but the
release APK comes out **unsigned** (`app-release-unsigned.apk`). Gradle prints a warning when that happens.

## 3. Build, check, install

```bash
./gradlew testDebugUnitTest assembleRelease
# → app/build/outputs/apk/release/app-release.apk

# Optional: confirm it's signed with your key
"$ANDROID_HOME"/build-tools/*/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk

adb connect <STICK IP ADDRESS>
adb install -r app/build/outputs/apk/release/app-release.apk
```

Then follow [DEFAULT_LAUNCHER.md](DEFAULT_LAUNCHER.md) to make it the home screen. Use
`--package com.northeast.launcher`, the default, with the helper script.

## Updating later

1. Bump `versionCode` (by 1) and `versionName` in `app/build.gradle.kts`.
2. `./gradlew assembleRelease`, then `adb install -r …/app-release.apk`. Settings and app order are kept.

`INSTALL_FAILED_UPDATE_INCOMPATIBLE` means the APK was signed with a different key than the installed
copy. See [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## What CI does

GitHub Actions never sees your key. On every push it:

- builds an **unsigned** release APK, to catch shrinking/optimization problems, and checks that it
  requests no network permissions;
- on the TV emulator, signs a release build with a **throwaway key generated inside the CI run**,
  installs it, and confirms it starts. That key is discarded with the CI machine.
