# ESP32 Controller

Native Android controller app for the ESP32 home automation setup.

This app is built with Kotlin and Jetpack Compose. It is intentionally a single-screen controller with no local settings storage. Commands are sent over HTTP to:

```text
http://192.168.0.108/
```

## Build

```bash
./gradlew assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Checks

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

If local Java or Android SDK components are unavailable, the GitHub Actions workflow builds and uploads the debug APK artifact.
