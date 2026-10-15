# Nothing Launcher

A Nothing OS–style Android home screen launcher written in Kotlin and Jetpack Compose. It has a dot-matrix font, monochrome round icons, a red/white/black palette, and its own pull-down panel for notifications and quick settings.

## What it replaces

| Part | How |
|---|---|
| **Home screen** | `MainActivity` registers as a `HOME` app. On first launch, Android's *"Set as default home app?"* dialog appears (`RoleManager.ROLE_HOME`). |
| **Pull-down shade (notifications + quick settings)** | `ShadeAccessibilityService` places an invisible strip over the status bar. Swiping down from the top edge **in any app** opens the Nothing-style panel instead of the stock one. Notifications come from `NotificationService` (a `NotificationListenerService`). |
| **App drawer** | Swipe up on home, or tap the search bar. Long-press an app to add it to home, open App info, or uninstall it. |

## Home screen (based on the reference shots)
- "WELCOME BACK / GOOD EVENING" header in the dot-matrix font, with a line about battery temperature, level, and status
- Red free-RAM pill and white date pill
- 10 round app slots. Tap an empty slot or long-press any slot to choose an app.
- Segmented battery widget, red dot-matrix clock, and a Do Not Disturb pill
- A black-and-white music widget appears in place of two app slots while something is playing
- Gestures: swipe down opens the shade, swipe up opens apps, double-tap locks the screen, long-press opens wallpaper and settings

## Quick settings in the shade
Internet, Bluetooth, Torch, DND, Sound/Vibrate/Silent, Auto-rotate, Airplane, Location, Battery saver, Screenshot, Lock, Power menu, a brightness slider with an auto-brightness button, media controls, and swipe-to-dismiss notifications.

> Android does not let normal apps switch Wi-Fi, Bluetooth, mobile data, airplane mode or location on or off directly. Those tiles open the matching system panel. Torch, DND, ringer, rotation and brightness switch directly.

## Setup after installing
Open the app. It asks to become the default home app, then shows a setup checklist (long-press home → *Launcher settings* opens it again):
1. Default home app
2. **Nothing Panel** accessibility service, which replaces the pull-down in every app
3. Notification access
4. Do Not Disturb access
5. Modify system settings (brightness and rotation)

## Build
Requires JDK 17+ and the Android SDK (API 35).
```
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
```
The release build is signed with the debug key so it installs right away.

## Limits (Android platform rules, not this app)
- The **lock screen** cannot be replaced by a third-party app without weakening security, so it stays the system one.
- On Android 13+, sideloaded apps must be allowed to use accessibility: *Settings → Apps → Nothing Launcher → ⋮ → Allow restricted settings*.
- The real status bar icons (clock, signal) are still drawn by the system.
