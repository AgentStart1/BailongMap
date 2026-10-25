# Development

Use JDK 21 and Android SDK Platform 37.1 to build the project. The updated Compose
dependencies require compile SDK 37.1; the minimum and target SDK versions remain
33 and 37 respectively.

Run `./gradlew check --no-daemon` for the checks used by CI. On Windows, use
`gradlew.bat` instead of `./gradlew`.

Material3 migration regression tests run without a device using Robolectric:
`./gradlew :app:shared:testAndroidHostTest --tests '*MaterialMigrationTest'`.
They cover independent initially hidden map sheet states, full expansion,
close/reopen, search result selection, and clearing the search query. The three
map sheets share `rememberMapSheetState`, which permits only Hidden and Expanded.

To investigate Gradle compatibility warnings, run
`./gradlew help --no-daemon --no-configuration-cache --warning-mode all -Dorg.gradle.deprecation.trace=true`
(quote the entire `-D...` argument in PowerShell). With Gradle 9.8.1, AGP 9.3.1
calls deprecated `Configuration.setVisible` internally, and Kotlin 2.4.21's
Native toolchain initializes the deprecated `KotlinNativeBundleArtifactsTypes`
attribute in `KotlinNativeBundleArtifactFormat`. These require upstream plugin
fixes before Gradle 11 and 10 respectively; keep warnings visible and recheck
when updating those plugins. The generic warning's recommendation to upgrade
Kotlin to 2.1 does not resolve this project, which already uses a newer version.

To verify Appium test compilation without a device or Appium server, run
`./gradlew :appiumTests:compileTestKotlin --no-daemon`. Running device tests requires
Appium server 3 or newer for Appium Java Client 11.

With `adb` on `PATH`, a connected Android device, and an Appium server running,
run `./scripts/appium-test.sh -PappiumTags=navigation` for navigation end-to-end
tests. The script uses `ANDROID_UDID` when set, otherwise the first connected
device reported by `adb devices`. It installs the debug APK before running the
tests and forwards command-line arguments to both Gradle invocations. An install
failure stops the script; otherwise it returns the test command's exit code.
The script requires no AI plugin or device-lock helper. AI tooling manages any
shared-device locking externally.

Run `./scripts/appium-test.sh -PappiumTags=map-ui` for native-map startup and
bottom-sheet regression tests. They verify initially hidden sheets, settings and
offline sheet dismissal by Back and dragging, and reopening place details.
`APP_PACKAGE` and `APP_ACTIVITY` can target an independently installed test build;
use the fully qualified activity name when its namespace differs from the package.
For an already installed build, run `:appiumTests:test -PappiumTags=map-ui`
with `-x :app:androidApp:installDebug` to skip replacing it.

The navigation test injects mock locations through ADB, selects a three-stage
walking–subway–bicycle option, advances at transfer points, and restores real
location afterward. It uses `adb reverse` with local map-style and OpenTripPlanner
fixtures, so the test does not depend on internet access.

MapLibre Compose uses 0.20.0. The Android app packages the matching OpenGL runtime via
`maplibre-compose-runtime-opengl-android`. The shared API dependency alone only
provides the JNI bridge; omitting the runtime causes a startup
`UnsatisfiedLinkError` for `libmaplibre-native-c.so`. Keep the API and runtime on
the same version as described in the [MapLibre setup guide](https://maplibre.org/maplibre-compose/getting-started/).
Camera commands use `CameraUpdate` and `CameraFit`,
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
