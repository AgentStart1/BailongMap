# Development

Use JDK 21 and Android SDK Platform 37.1 to build the project. The updated Compose
dependencies require compile SDK 37.1; the minimum and target SDK versions remain
33 and 37 respectively.

Run `./gradlew check --no-daemon` for the checks used by CI. On Windows, use
`gradlew.bat` instead of `./gradlew`.

To verify Appium test compilation without a device or Appium server, run
`./gradlew :appiumTests:compileTestKotlin --no-daemon`. Running device tests requires
Appium server 3 or newer for Appium Java Client 11.

MapLibre Compose uses 0.20.0. Camera commands use `CameraUpdate` and `CameraFit`,
and the location marker uses `LocationIndicatorLayer`. Offline UI observes
`OfflineStorage.state` and distinguishes loading, ready, and failed initialization.
`OfflineHost` owns offline actions on an injected serial background dispatcher;
the screen closes it on disposal and collects its state and effects with its lifecycle.
Offline storage is a native-only API; the shared UI currently builds for Android.

JS, Wasm, and Kotlin web tooling dependency installation share a Gradle build
service with one concurrent user. Yarn Classic's network mutex can return success
without producing `build/wasm/yarn.lock` when installations overlap on Windows.
The service serializes only dependency installation; compilation and tests can
still run in parallel, and lockfile validation remains enabled. Remove it once
the bundled package manager reliably handles concurrent installations.
