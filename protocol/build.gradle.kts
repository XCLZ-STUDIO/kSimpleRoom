plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    api(libs.ktor.network)

    testImplementation(kotlin("test-junit"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}