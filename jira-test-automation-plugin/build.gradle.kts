plugins {
    id("org.jetbrains.intellij") version "1.17.0"

    kotlin("jvm") version "1.8.0"
}

group = "com.cts"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("org.json:json:20230227")
    implementation("org.jfree:jfreechart:1.5.3")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-core:2.13.3")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.13.3")




}

intellij {
    version.set("2023.1")
    plugins.set(listOf("java"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("231")
        untilBuild.set("241.*")
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}