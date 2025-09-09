plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.jira.testautomation"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

// IntelliJ Platform configuration
intellij {
    version.set("2023.2.5")
    type.set("IC") // IntelliJ IDEA Community Edition
    plugins.set(listOf(
        "java",
        "gradle",
        "Kotlin",
        "junit",
        "testng",
        "java-ide",
        "java-ide-customization"
    ))
    updateSinceUntilBuild.set(true)
    downloadSources.set(true)
    sandboxDir.set("${rootProject.rootDir}/.sandbox")
}

dependencies {
    // Core Dependencies
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.10")

    // For AI backend communication
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    
    // IntelliJ Platform Dependencies
    implementation("com.intellij:forms_rt:8.1.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.10")
    
    // Add compileOnly for IntelliJ Platform dependencies
    compileOnly("org.jetbrains:annotations:24.0.1")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
