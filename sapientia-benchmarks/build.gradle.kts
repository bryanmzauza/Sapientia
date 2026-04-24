plugins {
    id("sapientia.java-conventions")
}

dependencies {
    implementation(project(":sapientia-api"))
    // Benchmarks can depend on core internals when added later.

    compileOnly(libs.paper.api)

    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.ap)
}
