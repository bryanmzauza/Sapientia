plugins {
    id("sapientia.java-conventions")
}

dependencies {
    api(project(":sapientia-api"))
    api(libs.paper.api)
    // MockBukkit integration lands with T-170 — no published artifact targets
    // Paper 26.x yet. Keep the module as a placeholder until then.
}
