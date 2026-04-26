plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.4.1")
    // Shadow 9.x rewrote RelocatorRemapper in Kotlin and bundles a current ASM,
    // fixing the `mapValue(Handle)` MissingMethodException triggered by
    // invokedynamic + StringConcatFactory on Java 25 class files.
}
