plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17)) // 指定 JDK 17
    }
}

group = "com.deshark"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.qcloud:cos_api:5.6.245")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.18")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("modpack-uploader")
    archiveClassifier.set("")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "com.deshark.Main"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}