plugins {
    id("sapientia.java-conventions")
    id("sapientia.test-conventions")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.adventure.minimsg)

    testImplementation(libs.paper.api)
    testImplementation(libs.adventure.minimsg)
}
