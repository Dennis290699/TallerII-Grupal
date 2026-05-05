plugins {
    id("java")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0.3"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:3.4")
    implementation("com.formdev:flatlaf-extras:3.4")
}

application {
    mainClass.set("com.programacion.Principal")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveBaseName.set("TallerII")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Implementation-Version"] = project.version
    }
    mergeServiceFiles()
}
