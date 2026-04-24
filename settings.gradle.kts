rootProject.name = "sapientia"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
        maven("https://repo.opencollab.dev/maven-releases/")       { name = "opencollab-releases" }
        maven("https://repo.opencollab.dev/maven-snapshots/")      { name = "opencollab-snapshots" }
        maven("https://jitpack.io")                                { name = "jitpack" }
    }
}

include(
    "sapientia-api",
    "sapientia-core",
    "sapientia-content",
    "sapientia-bedrock",
    "sapientia-testkit",
    "sapientia-benchmarks",
)
