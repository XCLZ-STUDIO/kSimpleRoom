
pluginManagement {
    val kotlinVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
    }

    versionCatalogs {
        create("libs") {
            library("ktor-network", "io.ktor:ktor-network:2.0.0-beta-1")
            library("kotlin-logging", "io.github.microutils:kotlin-logging-jvm:2.1.21")
            library("logback", "ch.qos.logback:logback-classic:1.2.11")

        }
    }
}


rootProject.name = "korom"
include("protocol", "server", "client")

project(":protocol").name = "korom-protocol"
project(":server").name = "korom-server"
project(":client").name = "korom-client"
