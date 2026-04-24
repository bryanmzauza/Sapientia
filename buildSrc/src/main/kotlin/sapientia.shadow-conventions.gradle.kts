import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("sapientia.java-conventions")
    id("com.gradleup.shadow")
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    mergeServiceFiles()

    // Relocate embedded libraries to avoid clashes with other plugins on the server.
    val base = "dev.brmz.sapientia.libs"
    relocate("com.zaxxer.hikari",           "$base.hikari")
    relocate("org.sqlite",                   "$base.sqlite")
    relocate("org.incendo.cloud",            "$base.cloud")
    relocate("org.yaml.snakeyaml",           "$base.snakeyaml")
    relocate("com.esotericsoftware.kryo",    "$base.kryo")
    relocate("com.esotericsoftware.minlog",  "$base.minlog")
    relocate("com.esotericsoftware.reflectasm", "$base.reflectasm")
}

tasks.named("jar") {
    enabled = false
}

tasks.named("assemble") {
    dependsOn("shadowJar")
}

tasks.named("build") {
    dependsOn("shadowJar")
}
