plugins {
//    application
    kotlin("jvm")
    `maven-publish`
}

//application {
//    mainClass.set("tech.xclz.ApplicationKt")
//
//    val isDevelopment: Boolean = project.ext.has("development")
//    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
//}

dependencies {
    implementation(project(":korom-protocol"))
    implementation(kotlin("reflect"))
    implementation(libs.kotlin.logging)

    testImplementation(project(":korom-client"))
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.logback)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}