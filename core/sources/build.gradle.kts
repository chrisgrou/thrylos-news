import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}

// Emit Java 17 bytecode so this module links cleanly against :app/:core:data
// (which target 17 via AGP's compileOptions) — this cross-targets from
// whatever JDK is running Gradle, it does not require a JDK 17 install.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
}

dependencies {
    api(project(":core:model"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jsoup:jsoup:1.18.1")
    // api: HttpFetcher's public constructor exposes OkHttpClient, and
    // SourceSyncCoordinator's public constructor exposes HttpFetcher — both
    // need to be resolvable from consumer modules like :core:data.
    api("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("net.dankito.readability4j:readability4j:1.0.8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}

tasks.test {
    useJUnitPlatform()
}

