plugins {
    kotlin("jvm") version "2.3.0"
}

group = "me.znotchill"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.obscure.computer/repository/maven-releases/")
    maven { url = uri("https://jitpack.io") }
    maven("https://repo.znotchill.me/repository/maven-releases/")
    maven("https://redirector.kotlinlang.org/maven/bootstrap")
    maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
        content {
            includeModule("net.minestom", "minestom")
            includeModule("net.minestom", "testing")
        }
    }
    maven("https://repo.smolder.fr/public/")
    maven("https://mvn.everbuild.org/public")

    maven("https://repo.hypera.dev/snapshots/") // spark-minestom
    maven("https://repo.lucko.me/") // spark-common
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    testImplementation(kotlin("test"))
    val lwjglVersion = "3.3.4"
    val lwjglNatives = "natives-windows"

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-opencl")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)

    implementation("dev.hollowcube:polar:1.15.0")
    implementation("net.minestom:minestom:2026.04.13-1.21.11")
    implementation("me.znotchill:blossom:1.5.9")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("net.kyori:adventure-text-minimessage:4.26.1")
    implementation("net.kyori:adventure-text-serializer-ansi:4.26.1")
    implementation("net.kyori:adventure-api:4.26.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")
}

tasks.test {
    useJUnitPlatform()
}