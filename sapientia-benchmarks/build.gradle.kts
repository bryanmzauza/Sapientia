plugins {
    id("sapientia.java-conventions")
}

dependencies {
    implementation(project(":sapientia-api"))
    implementation(project(":sapientia-core"))

    compileOnly(libs.paper.api)

    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.ap)
}

// --- JMH runner (T-170 / 1.0.0-beta) -----------------------------------------

val jmhRun = tasks.register<JavaExec>("jmh") {
    group = "benchmark"
    description = "Runs all Sapientia JMH benchmarks and writes a JSON report."
    mainClass.set("org.openjdk.jmh.Main")
    classpath = sourceSets["main"].runtimeClasspath
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    )
    val reportDir = layout.buildDirectory.dir("reports/benchmarks")
    val reportFile = reportDir.map { it.file("result.json") }
    doFirst { reportDir.get().asFile.mkdirs() }
    args = listOf(
        "-rf", "json",
        "-rff", reportFile.get().asFile.absolutePath,
        "-foe", "true",
        "-wi", "1",
        "-i", "3",
        "-f", "1"
    )
}

// --- Baseline comparator (T-171 / 1.0.0-beta) --------------------------------
//
// compareToBaseline reads the latest JMH result.json produced by `:jmh` and
// compares it against `docs/benchmarks/baseline.json`. Any benchmark whose
// score regresses by more than 10 % fails the build. Missing benchmarks in
// the baseline are reported as informational. New baselines are captured with
// `saveBenchmarkBaseline`.

tasks.register("compareToBaseline") {
    group = "verification"
    description = "Compares the latest JMH report against the committed baseline (T-171)."
    dependsOn(jmhRun)

    doLast {
        val resultFile = layout.buildDirectory.file("reports/benchmarks/result.json").get().asFile
        val baselineFile = rootProject.file("docs/benchmarks/baseline.json")
        if (!resultFile.exists()) {
            throw GradleException("No JMH result at ${resultFile.absolutePath}. Run :jmh first.")
        }
        if (!baselineFile.exists()) {
            logger.lifecycle(
                "No baseline at ${baselineFile.path}. Run `./gradlew :sapientia-benchmarks:saveBenchmarkBaseline` to seed one."
            )
            return@doLast
        }
        val current = parseBenchmarkScores(resultFile.readText())
        val baseline = parseBenchmarkScores(baselineFile.readText())
        val regressions = mutableListOf<String>()
        baseline.forEach { (name, base) ->
            val now = current[name] ?: return@forEach
            // Scores are in JMH primary metric units. For AverageTime lower is better, so
            // we treat anything beyond 110 % of the baseline as a regression.
            val ratio = now / base
            if (ratio > 1.10) {
                regressions += "$name regressed: baseline=%.3f current=%.3f (+%.1f %%)"
                    .format(base, now, (ratio - 1.0) * 100.0)
            }
        }
        if (regressions.isNotEmpty()) {
            regressions.forEach { logger.error(it) }
            throw GradleException("Benchmark regression >10 % blocks merge (T-171): ${regressions.size} bench(es).")
        } else {
            logger.lifecycle("All benchmarks within 10 % of baseline.")
        }
    }
}

/**
 * Extracts `benchmark -> score` pairs from a JMH JSON report. Uses a tiny hand-rolled
 * parser so the buildscript has zero runtime dependencies beyond the JDK.
 */
fun parseBenchmarkScores(json: String): Map<String, Double> {
    val out = LinkedHashMap<String, Double>()
    val nameRegex = Regex("\"benchmark\"\\s*:\\s*\"([^\"]+)\"")
    val scoreRegex = Regex("\"score\"\\s*:\\s*([0-9eE.+-]+)")
    val names = nameRegex.findAll(json).map { it.groupValues[1] }.toList()
    val scores = scoreRegex.findAll(json).map { it.groupValues[1].toDouble() }.toList()
    val n = minOf(names.size, scores.size)
    for (i in 0 until n) {
        out[names[i]] = scores[i]
    }
    return out
}

tasks.register<Copy>("saveBenchmarkBaseline") {
    group = "verification"
    description = "Promotes the latest JMH report to docs/benchmarks/baseline.json (T-171)."
    dependsOn(jmhRun)
    from(layout.buildDirectory.file("reports/benchmarks/result.json"))
    into(rootProject.file("docs/benchmarks"))
    rename { "baseline.json" }
}
