plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    `java-library`
    `maven-publish`
}

group = "com.interaso"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    dokkaHtmlPlugin(libs.dokka.versioning.plugin)
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(11)
    explicitApi()
}

tasks {
    named<Jar>("javadocJar") {
        from(dokkaJavadoc)
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
        }
    }
}
