plugins {
    base
}

allprojects {
    group = "dev.brmz.sapientia"
    version = "0.1.0"
}

/**
 * Root convenience task that produces the distributable plugin jar (shadowed
 * sapientia-core). See docs/module-breakdown.md §6.
 */
tasks.register("buildPluginJar") {
    group = "build"
    description = "Assembles the shaded Sapientia plugin jar."
    dependsOn(":sapientia-core:shadowJar")
}
