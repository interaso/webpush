plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

group = "com.interaso"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(11)
    explicitApi()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
        }
    }
}
