# Proximit

A tiny, hidden Android app that locks the screen when you cover the proximity
sensor. There is **no launcher icon**; the only entry point is a **Quick
Settings tile**. The app is built entirely in GitHub Actions, so no Android SDK
is needed on your computer.

## How it works

| Component | Purpose |
| --- | --- |
| `ProximityTileService` | Quick Settings tile. Toggles the gesture on/off. |
| `ProximityService` | Foreground service that listens to `TYPE_PROXIMITY`. |
| `AdminReceiver` | Device admin used only for `DevicePolicyManager.lockNow()`. |
| `SetupActivity` | Hidden setup screen (no launcher entry). |

The gesture fires only when the screen is on and the sensor goes from **far to
near**. The service starts disarmed, so tapping the tile with a finger over the
sensor will not immediately lock the phone. A short 300 ms delay avoids
accidental locks, and a cooldown prevents repeated requests.

## Install

1. Open the **Actions** tab and pick the latest successful **Android CI** run.
2. Download the `proximit-debug-apk` artifact and install the APK on your phone
   (allow "Install unknown apps" for the app you use to open it).
3. Pull down Quick Settings, tap the edit/pencil button, find **Proximit**, and
   drag it into the active tiles.
4. Tap the **Proximit** tile once. The hidden setup screen opens:
   - **Enable device admin** — required to lock the screen.
   - **Allow notifications** — optional; shows the ongoing status notification.
   - **Start gesture** — starts listening.
5. Cover the proximity sensor (near the earpiece) for a moment to lock.

After setup, the tile is the on/off switch. Tapping the ongoing notification
opens the setup screen again; tapping the tile while device admin is disabled
also opens setup.

## Why device admin?

Stock Android has no public API for a normal app to turn the screen off without
locking the device. The standard supported approach is
`DevicePolicyManager.lockNow()`, which locks the device. Unlocking then requires
your normal PIN, pattern, or biometric. The app requests only the `force-lock`
policy; it cannot erase or change the device.

To uninstall: disable the gesture from the tile, then remove Proximit from
**Settings → Security → Device admin apps**, then uninstall normally.

## Notes and limitations

- The app is hidden from the launcher but still appears in Android Settings.
- The Quick Settings tile must be added manually on most devices.
- Some OEM skins (Xiaomi, Samsung, etc.) may kill background services. If the
  gesture stops working, allow Proximit to run in the background / disable
  battery optimization for it.
- The gesture is ignored while the screen is off, so a phone in a pocket will
  not lock itself repeatedly.
- No internet permission is requested.

## Build (GitHub Actions only)

The workflow in `.github/workflows/android.yml`:

1. Installs JDK 17, Android SDK platform 35, and build-tools 35.0.0.
2. Generates the Gradle wrapper (`gradlew` + wrapper JAR) with Gradle 8.9.
3. Builds `app-debug.apk` with AGP 8.7.3.
4. Uploads the APK as an artifact.

Push to `main`, open a pull request, or run the workflow manually from the
Actions tab. Pushing a tag such as `v1.0.0` also publishes the APK on a GitHub
Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The Gradle wrapper JAR is intentionally not committed — it is a binary, and CI
generates it each run. If you later build locally, install Gradle 8.9 once and
run `gradle wrapper`, then `./gradlew assembleDebug`.
