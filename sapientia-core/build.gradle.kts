plugins {
    id("sapientia.shadow-conventions")
    id("sapientia.test-conventions")
    alias(libs.plugins.runpaper)
}

dependencies {
    api(project(":sapientia-api"))
    implementation(project(":sapientia-bedrock"))
    implementation(project(":sapientia-content"))

    compileOnly(libs.paper.api)
    compileOnly(libs.adventure.minimsg)

    // Embedded and relocated via shadow.
    implementation(libs.hikari)
    implementation(libs.sqlite.jdbc)
    implementation(libs.snakeyaml)
    implementation(libs.kryo)

    // Cloud command framework — catalog entries reserved for T-103 (0.2.0). Not
    // wired into any implementation() today to keep the shaded jar lean.

    // Floodgate soft-dep: only compileOnly so absent servers still load.
    compileOnly(libs.floodgate.api)

    testImplementation(libs.paper.api)
    testImplementation(libs.adventure.minimsg)
    testImplementation(libs.archunit.junit5)
    // MockBukkit is deferred to T-170 (sapientia-testkit) — no matching artifact
    // for Paper 26.x is published yet.
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.named<xyz.jpenilla.runpaper.task.RunServer>("runServer") {
    minecraftVersion("26.1.2")
}
