import java.time.*

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.nexus.publish)
    alias(libs.plugins.git.versioning)
    `java-library`
    `maven-publish`
    signing
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

kotlin {
    jvmToolchain(11)
    explicitApi()
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Jar>("javadocJar") {
    from(tasks.dokkaJavadoc)
}

signing {
    isRequired = false
    sign(publishing.publications)
    useInMemoryPgpKeys(
        providers.gradleProperty("gpgKey").orNull,
        providers.gradleProperty("gpgPassword").orNull,
    )
}

nexusPublishing.repositories.sonatype {
    nexusUrl = uri("https://s01.oss.sonatype.org/service/local/")
    snapshotRepositoryUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")

    username = providers.gradleProperty("sonatypeUsername").orNull
    password = providers.gradleProperty("sonatypePassword").orNull
}

nexusPublishing.transitionCheckOptions {
    maxRetries = 10
    delayBetween = Duration.ofSeconds(5)
}

publishing.publications.create<MavenPublication>("release") {
    from(components["java"])
    pom {
        name = "WebPush"
        description = "Lightweight library for sending web push notifications with zero dependencies."
        url = "https://github.com/interaso/webpush"
        licenses {
            license {
                name = "The MIT License (MIT)"
                url = "https://opensource.org/licenses/MIT"
            }
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
        scm {
            connection = "scm:git:https://github.com/interaso/webpush.git"
            developerConnection = "scm:git:ssh://git@github.com/interaso/webpush.git"
            url = "https://github.com/interaso/webpush"
        }
    }
}
