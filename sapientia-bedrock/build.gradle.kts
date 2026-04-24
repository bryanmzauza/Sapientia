plugins {
    id("sapientia.java-conventions")
    id("sapientia.test-conventions")
}

dependencies {
    api(project(":sapientia-api"))

    compileOnly(libs.paper.api)
    compileOnly(libs.floodgate.api)

    testImplementation(libs.paper.api)
    testImplementation(libs.floodgate.api)
}
