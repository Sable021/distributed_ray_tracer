plugins {
    application
    java
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
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
}
