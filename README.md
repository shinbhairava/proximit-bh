# Proximit

A tiny, hidden Android app that turns the screen off when you cover the
proximity sensor — **without locking the device**. There is **no launcher icon**;
the only entry point is a **Quick Settings tile**. The app is built entirely in
GitHub Actions, so no Android SDK is needed on your computer.

## How it works

| Component | Purpose |
| --- | --- |
| `ProximityTileService` | Quick Settings tile. Toggles the gesture on/off. |
| `ProximityService` | Foreground service that holds a proximity screen-off wake lock. |
| `SetupActivity` | Hidden setup screen (no launcher entry). |

The service uses the public
`PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK` level, which requires only the
normal `WAKE_LOCK` permission. It is armed after the sensor first reports
**far**, so enabling the tile with a finger over the sensor does not immediately
turn the screen off. After that:

- **Cover the sensor** → the display turns off.
- **Uncover the sensor** → the display turns back on, still unlocked.

No device admin, accessibility service, root, or Shizuku is involved. The device
is not locked and no keyguard is shown by the app.

## Install

1. Open the **Actions** tab and pick the latest successful **Android CI** run.
2. Download the `proximit-debug-apk` artifact and install the APK on your phone
   (allow "Install unknown apps" for the app you use to open it).
3. Pull down Quick Settings, tap the edit/pencil button, find **Proximit**, and
   drag it into the active tiles.
4. Tap the **Proximit** tile to enable screen-off mode.
5. Cover the proximity sensor (near the earpiece) to turn the screen off.

The tile is the on/off switch. If you want the ongoing notification and the
hidden setup screen, tap **Allow notifications** when Android asks (Android 13+).
The notification also opens `SetupActivity`, where you can enable/disable the
service, grant notification permission, and see sensor support.

## Screen off, not locked

Android has no public API that lets a normal app permanently turn the screen off
while keeping the device unlocked. The closest supported behaviour is the
proximity screen-off wake lock used here:

- The screen goes off only while the sensor is covered and comes back on when it
  is uncovered.
- The device stays unlocked, so there is no PIN/pattern/biometric prompt when
  the screen returns.
- Because the wake lock is held while the sensor is covered, Android may keep
  the CPU awake during that time on some devices. Disable the tile when you do
  not need it, especially if the phone will sit in a pocket for a long time.
- Some devices do not expose this wake lock level. The setup screen reports
  `Screen-off mode: not supported` and the tile is marked unavailable.

To uninstall, simply uninstall Proximit. There is no device administrator to
remove first.

## Notes and limitations

- The app is hidden from the launcher but still appears in Android Settings.
- The Quick Settings tile must be added manually on most devices.
- Some OEM skins (Xiaomi, Samsung, etc.) may kill background services. If the
  gesture stops working, allow Proximit to run in the background / disable
  battery optimization for it.
- No internet permission is requested.

## Build (GitHub Actions only)

The workflow in `.github/workflows/android.yml`:

1. Installs JDK 17 and uses the Android SDK platform 35 and build-tools 35.0.0
   that are preinstalled on the GitHub Actions runner.
2. Generates the Gradle wrapper (`gradlew` + wrapper JAR) with Gradle 8.9.
3. Builds `app-debug.apk` with AGP 8.7.3.
4. Uploads the APK as an artifact.

Push to `main`, open a pull request, or run the workflow manually from the
Actions tab. Pushing a tag such as `v1.1.1` also publishes the APK on a GitHub
Release:

```bash
git tag v1.1.1
git push origin v1.1.1
```

The Gradle wrapper JAR is intentionally not committed — it is a binary, and CI
generates it each run. If you later build locally, install Gradle 8.9 once and
run `gradle wrapper`, then `./gradlew assembleDebug`.
