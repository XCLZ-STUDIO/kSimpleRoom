plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    implementation(project(":korom-protocol"))
    implementation(libs.kotlin.logging)

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