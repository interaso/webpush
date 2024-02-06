import com.vanniktech.maven.publish.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.git.versioning)
    alias(libs.plugins.maven.publish)
}

group = "com.interaso"
version = "0.0.0-SNAPSHOT"

gitVersioning.apply {
    describeTagPattern = "v(?<version>[0-9]+\\.[0-9]+\\.[0-9]+)"
    refs.tag(describeTagPattern) {
        version = "\${ref.version}"
    }
    refs.branch(describeTagPattern.removePrefix("v")) {
        version = "\${ref.version}-SNAPSHOT"
    }
    rev {
        version = "\${describe.tag.version.major}.\${describe.tag.version.minor.next}.0-SNAPSHOT"
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.server.html.builder)
    testImplementation(libs.slf4j.simple)
    testImplementation(libs.playwright)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

kotlin {
    jvmToolchain(11)
    explicitApi()
}

tasks.test {
    useJUnitPlatform()
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01, true)
    signAllPublications()
    pom {
        name = "WebPush"
        description = "Lightweight Kotlin library for sending web push notifications with zero external dependencies."
        url = "https://github.com/interaso/webpush"
        scm {
            connection = "scm:git:https://github.com/interaso/webpush.git"
            developerConnection = "scm:git:ssh://git@github.com/interaso/webpush.git"
            url = "https://github.com/interaso/webpush"
        }
        organization {
            name = "Interactive Solutions s.r.o."
            url = "https://interaso.com"
        }
        developers {
            developer {
                id = "morki"
                name = "Lukáš Moravec"
                email = "morki@morki.cz"
            }
        }
        licenses {
            license {
                name = "The MIT License (MIT)"
                url = "https://opensource.org/licenses/MIT"
            }
        }
    }
}
