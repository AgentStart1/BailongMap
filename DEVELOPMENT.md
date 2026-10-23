# Development

Use JDK 21 and Android SDK Platform 37.1 to build the project. The updated Compose
dependencies require compile SDK 37.1; the minimum and target SDK versions remain
33 and 37 respectively.

Run `./gradlew check --no-daemon` for the checks used by CI. On Windows, use
`gradlew.bat` instead of `./gradlew`.

To verify Appium test compilation without a device or Appium server, run
`./gradlew :appiumTests:compileTestKotlin --no-daemon`. Running device tests requires
Appium server 3 or newer for Appium Java Client 11.

MapLibre Compose remains on 0.17.x because newer releases require migrating the
map camera, location puck, and offline APIs (see [PR #18](https://github.com/storytellerF/BailongMap/pull/18)).
Dependabot permits patch updates but ignores minor and major updates for this
dependency. Remove that exception in `.github/dependabot.yml` when the API migration
is complete and verified.

JS, Wasm, and Kotlin web tooling dependency installation share a Gradle build
service with one concurrent user. Yarn Classic's network mutex can return success
without producing `build/wasm/yarn.lock` when installations overlap on Windows.
The service serializes only dependency installation; compilation and tests can
still run in parallel, and lockfile validation remains enabled. Remove it once
the bundled package manager reliably handles concurrent installations.
