// NOTE: Android/Hilt/KSP plugins are declared with explicit versions directly in
// :app and :core:data (which are the only modules that need the Android Gradle
// Plugin, resolved from google()). Keeping them out of the root plugins{} block
// means `./gradlew :core:model:test :core:sources:test --configure-on-demand`
// can run in environments without access to Google's Maven repo, since the root
// build script no longer needs to resolve AGP just to evaluate.
plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
