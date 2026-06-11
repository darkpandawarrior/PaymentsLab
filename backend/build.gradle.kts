plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

application {
    // fun main() lives in Application.kt → file class MainKt.
    mainClass.set("com.paymentslab.backend.MainKt")
}

dependencies {
    // Depend on the shared wire contract (JVM target) so the server serves the EXACT DTOs the client sends.
    implementation(project(":core:protocol"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.cors)
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
}

kotlin {
    jvmToolchain(17)
}
