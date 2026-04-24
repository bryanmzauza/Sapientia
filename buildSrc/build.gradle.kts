plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.5")
    // Shadow 8.3.5 bundles ASM 9.7.1, which doesn't recognise Java 25 class files
    // (major version 69). Force the plugin classpath to a newer ASM until we can
    // upgrade to Shadow 9.x (which requires Kotlin 2.2+ and therefore Gradle 9).
    constraints {
        implementation("org.ow2.asm:asm:9.8")
        implementation("org.ow2.asm:asm-commons:9.8")
        implementation("org.ow2.asm:asm-tree:9.8")
        implementation("org.ow2.asm:asm-analysis:9.8")
    }
}
