plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.deshark"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.qcloud:cos_api:5.6.245")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
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