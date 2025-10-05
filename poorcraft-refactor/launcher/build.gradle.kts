plugins {
    kotlin("jvm")
    application
    id("org.graalvm.buildtools.native")
}

dependencies {
    implementation(project(":engine"))
    implementation(kotlin("stdlib"))
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
}

application {
    mainClass.set("com.poorcraft.launcher.LauncherKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("PoorCraftRefactor")
            mainClass.set("com.poorcraft.launcher.LauncherKt")
            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--initialize-at-build-time=org.slf4j,ch.qos.logback")
            buildArgs.add("--initialize-at-run-time=org.lwjgl")
            buildArgs.add("-H:IncludeResources=.*\\.png")
            buildArgs.add("-H:IncludeResources=.*\\.lua")
            buildArgs.add("-H:IncludeResources=.*\\.json")
            buildArgs.add("-H:IncludeResources=.*\\.glsl")
            buildArgs.add("-H:IncludeResources=.*\\.vert")
            buildArgs.add("-H:IncludeResources=.*\\.frag")
            buildArgs.add("-H:+AddAllCharsets")
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(17))
            })
        }
    }
}

tasks.register<JavaExec>("runDev") {
    group = "application"
    description = "Run in development mode with live reload"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.poorcraft.launcher.LauncherKt")
    args("--dev-mode")
}

tasks.register<JavaExec>("runPortable") {
    group = "application"
    description = "Run in portable mode"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.poorcraft.launcher.LauncherKt")
    args("--portable")
}

tasks.register<JavaExec>("runHeadless") {
    group = "application"
    description = "Run in headless mode for testing"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.poorcraft.launcher.LauncherKt")
    args("--headless")
}
