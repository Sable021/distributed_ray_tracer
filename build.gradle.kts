import java.security.MessageDigest

import com.raytracer.ci.ChangeabilityIndexTask

plugins {
    application
    java
    jacoco
    id("org.openjfx.javafxplugin") version "0.1.0"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

javafx {
    version = "21.0.5"
    modules = listOf("javafx.controls")
}

application {
    mainClass = "com.raytracer.Main"
    applicationDefaultJvmArgs = listOf("-Xss4m")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
    finalizedBy(tasks.named("jacocoTestReport"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("test"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

// ---- Changeability Index (ISO/IEC 25010) ------------------------------------
// Computes a [1,100] ease-of-change score from source, test, and JaCoCo artefacts.
//   ./gradlew test jacocoTestReport changeabilityIndex   # full computation
//   ./gradlew changeabilityFloor -Pci.floor=80           # gate the build on a minimum
val changeabilityOutput = layout.buildDirectory.dir("reports/changeability")

tasks.register<ChangeabilityIndexTask>("changeabilityIndex") {
    group = "verification"
    description = "Compute the ISO 25010 Changeability Index from source, test, and coverage artefacts."
    projectRoot.set(layout.projectDirectory)
    outputDir.set(changeabilityOutput)
    enforceFloor.set(false)
    floor.set(80.0)
    // No hard dependency (keeps the task fast on re-runs), but when coverage IS requested
    // in the same invocation it must run first so 5.5/7.7 read a fresh report.
    mustRunAfter(tasks.named("jacocoTestReport"))
}

tasks.register<ChangeabilityIndexTask>("changeabilityFloor") {
    group = "verification"
    description = "Fail the build if the Changeability Index drops below the floor (default 80, override with -Pci.floor)."
    projectRoot.set(layout.projectDirectory)
    outputDir.set(changeabilityOutput)
    enforceFloor.set(true)
    floor.set((project.findProperty("ci.floor") as String?)?.toDouble() ?: 80.0)
    mustRunAfter(tasks.named("jacocoTestReport"))
}

// ---- Golden-image parity gate -----------------------------------------------
// Renders the four canonical configurations into build/verify/, hashes each
// PPM, and compares the hash against tools/golden/<config>.ppm.sha256.
// Any drift = task fails. Use ./gradlew regenGoldens to rewrite the goldens
// (only at the very start of a refactor, never silently).

val verifyConfigs = listOf(
    "quick"    to listOf("--headless", "--quick"),
    "dof"      to listOf("--headless", "--mode=dof", "--quick"),
    "classic"  to listOf("--headless", "--quick", "--scene=classic.scene.json"),
    "cylinder" to listOf("--headless", "--quick", "--scene=cylinder.scene.json"),
)

fun sha256(path: java.io.File): String {
    val md = MessageDigest.getInstance("SHA-256")
    path.inputStream().use { stream ->
        val buf = ByteArray(8192)
        while (true) {
            val n = stream.read(buf); if (n <= 0) break
            md.update(buf, 0, n)
        }
    }
    return md.digest().joinToString("") { "%02x".format(it) }
}

verifyConfigs.forEach { (name, args) ->
    tasks.register<JavaExec>("renderGolden_$name") {
        group = "verification"
        description = "Render the $name golden image into build/verify/golden_$name.ppm"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass = "com.raytracer.Main"
        workingDir = rootProject.projectDir
        val outFile = layout.buildDirectory.file("verify/golden_$name.ppm")
        outputs.file(outFile)
        doFirst {
            outFile.get().asFile.parentFile.mkdirs()
            this@register.args = args + listOf("--out=${outFile.get().asFile.absolutePath}")
        }
    }
}

tasks.register("verifyImage") {
    group = "verification"
    description = "Render the four canonical configs and assert each PPM hash matches tools/golden/<cfg>.ppm.sha256."
    dependsOn(verifyConfigs.map { (n, _) -> "renderGolden_$n" })
    doLast {
        val failures = mutableListOf<String>()
        verifyConfigs.forEach { (name, _) ->
            val ppm = layout.buildDirectory.file("verify/golden_$name.ppm").get().asFile
            val expected = file("tools/golden/$name.ppm.sha256").readText().trim()
            val actual = sha256(ppm)
            if (expected != actual) {
                failures += "  $name: expected $expected, got $actual"
            } else {
                println("[verifyImage] $name OK ($actual)")
            }
        }
        if (failures.isNotEmpty()) {
            throw GradleException("Golden image hash mismatch:\n${failures.joinToString("\n")}")
        }
    }
}

tasks.register("regenGoldens") {
    group = "verification"
    description = "REWRITE tools/golden/<cfg>.ppm.sha256 from current renders. Use only intentionally."
    dependsOn(verifyConfigs.map { (n, _) -> "renderGolden_$n" })
    doLast {
        verifyConfigs.forEach { (name, _) ->
            val ppm = layout.buildDirectory.file("verify/golden_$name.ppm").get().asFile
            val target = file("tools/golden/$name.ppm.sha256")
            target.parentFile.mkdirs()
            target.writeText(sha256(ppm) + "\n")
            println("[regenGoldens] wrote ${target.relativeTo(rootDir)} = ${sha256(ppm)}")
        }
    }
}
