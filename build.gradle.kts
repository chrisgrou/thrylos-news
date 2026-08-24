// Deliberately no root `plugins {}` block: every subproject declares its own
// plugins with explicit versions (see each module's build.gradle.kts). Sharing
// plugin versions via a root `apply false` block caused Gradle to see the
// Kotlin Gradle Plugin "already on the classpath with an unknown version"
// once a subproject applied a different Kotlin variant (jvm vs android) with
// its own explicit version — self-contained subprojects avoid that entirely,
// and it also means `./gradlew :core:model:test :core:sources:test` never
// needs to resolve anything from google() to evaluate the root script.

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
