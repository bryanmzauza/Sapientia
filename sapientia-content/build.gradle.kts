plugins {
    id("sapientia.java-conventions")
    id("sapientia.test-conventions")
}

dependencies {
    api(project(":sapientia-api"))

    compileOnly(libs.paper.api)
    compileOnly(libs.adventure.minimsg)
    compileOnly(libs.snakeyaml)

    testImplementation(libs.paper.api)
    testImplementation(libs.adventure.minimsg)
    testImplementation(libs.snakeyaml)
}
