plugins {
    base
}

allprojects {
    group = "dev.brmz.sapientia"
    version = "1.0.0"
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

/**
 * CI gate T-105 — fails if en.yml and pt_BR.yml diverge on key set.
 * See docs/i18n-strategy.md and ROADMAP 0.2.0.
 */
tasks.register("verifyTranslations") {
    group = "verification"
    description = "Verifies en.yml and pt_BR.yml have the same key set."

    val langDir = file("sapientia-core/src/main/resources/lang")
    inputs.dir(langDir)

    doLast {
        val yaml = org.yaml.snakeyaml.Yaml()
        fun flatten(prefix: String, node: Any?, out: MutableSet<String>) {
            if (node is Map<*, *>) {
                for ((k, v) in node) {
                    val key = if (prefix.isEmpty()) k.toString() else "$prefix.$k"
                    if (v is Map<*, *>) flatten(key, v, out) else out.add(key)
                }
            }
        }
        fun load(name: String): Set<String> {
            val f = langDir.resolve(name)
            if (!f.exists()) throw GradleException("Missing catalog: $name")
            val raw: Any? = f.inputStream().use { yaml.load<Any?>(it) }
            val out = linkedSetOf<String>()
            flatten("", raw, out)
            return out
        }
        val en = load("en.yml")
        val pt = load("pt_BR.yml")
        val missingInPt = en - pt
        val missingInEn = pt - en
        if (missingInPt.isNotEmpty() || missingInEn.isNotEmpty()) {
            val sb = StringBuilder("Translation catalogs diverge:\n")
            if (missingInPt.isNotEmpty()) {
                sb.append("  Missing in pt_BR.yml (${missingInPt.size}):\n")
                missingInPt.sorted().forEach { sb.append("    - ").append(it).append('\n') }
            }
            if (missingInEn.isNotEmpty()) {
                sb.append("  Missing in en.yml (${missingInEn.size}):\n")
                missingInEn.sorted().forEach { sb.append("    - ").append(it).append('\n') }
            }
            throw GradleException(sb.toString())
        }
        logger.lifecycle("verifyTranslations: ${en.size} keys match between en and pt_BR.")
    }
}

buildscript {
    dependencies {
        classpath("org.yaml:snakeyaml:2.3")
    }
    repositories {
        mavenCentral()
    }
}

tasks.named("check") {
    dependsOn("verifyTranslations")
}
