plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadow)
    application
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("org.bhargav.pansariwala.server.ApplicationKt")
    applicationDefaultJvmArgs = listOf(
        "-Djdk.tls.client.protocols=TLSv1.2",
    )
}

tasks.shadowJar {
    archiveBaseName.set("pansari-server")
    archiveClassifier.set("all")
    archiveVersion.set("")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "org.bhargav.pansariwala.server.ApplicationKt"
    }
}

tasks.named<JavaExec>("run") {
    // Real environment variables win over server/.env (gitignored local secrets).
    val envFile = file(".env")
    if (envFile.isFile) {
        envFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
            .forEach { line ->
                val key = line.substringBefore('=').trim()
                val value = line.substringAfter('=').trim()
                if (value.isNotEmpty() && System.getenv(key).isNullOrBlank()) environment(key, value)
            }
    }
}

tasks.named("build") {
    dependsOn(tasks.shadowJar)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.compression)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.mongodb.driver.kotlin.sync)
    implementation(libs.bson.kotlinx)
    implementation(libs.logback.classic)
    implementation(libs.apache.poi.ooxml)
    testImplementation(libs.kotlin.testJunit)
}
