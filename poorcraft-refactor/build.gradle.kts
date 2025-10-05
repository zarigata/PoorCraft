plugins {
    kotlin("jvm") version "1.9.22" apply false
    id("org.graalvm.buildtools.native") version "0.10.1" apply false
}

allprojects {
    group = "com.poorcraft"
    version = "0.1.2"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
