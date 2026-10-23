import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinNpmInstallTask
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinToolingSetupTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
}

abstract class YarnInstallService : BuildService<BuildServiceParameters.None>

// Yarn Classic's network mutex can exit successfully without installing when contended
// on Windows. Serialize its callers until the bundled Yarn mutex is reliable:
// https://github.com/yarnpkg/yarn/blob/v1.22.22/src/cli/index.js
val yarnInstallService = gradle.sharedServices.registerIfAbsent("yarnInstall", YarnInstallService::class) {
    maxParallelUsages.set(1)
}

tasks.withType<KotlinNpmInstallTask>().configureEach {
    usesService(yarnInstallService)
}

tasks.withType<KotlinToolingSetupTask>().configureEach {
    usesService(yarnInstallService)
}
