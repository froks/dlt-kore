import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jreleaser.model.Active
import org.jreleaser.model.Http

plugins {
    kotlin("jvm").version("2.0.0")
    `java-library`
    `maven-publish`
    id("org.jreleaser") version "1.12.0"
}

group = "io.github.froks"
version = "0.1.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(21)
    explicitApi()
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
    }
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    targetCompatibility = "1.8"
    sourceCompatibility = "1.8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                name.set("dlt-core")
                description.set("Kotlin based parser library for autosar dlt files")
                url.set("https://github.com/froks/dlt-core")
                developers {
                    developer {
                        id.set("froks")
                        name.set("Florian Roks")
                        email.set("flo.github@debugco.de")
                    }
                }
                scm {
                    url.set("https://github.com/froks/dlt-core")
                    developerConnection.set("scm:git:ssh://github.com:froks/dlt-core.git")
                    connection.set("scm:git:git://github.com/froks/dlt-core.git")
                }
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            setUrl(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    release {
        github {
            skipRelease = true
            skipTag = true
        }
    }
    signing {
        active = Active.ALWAYS
        armored = true
        verify = false
    }
    deploy {
        maven {
            mavenCentral.create("sonatype") {
                active = Active.ALWAYS
                username = System.getenv("JRELEASER_MAVENCENTRAL_USERNAME")
                url = "https://central.sonatype.com/api/v1/publisher"
                authorization = Http.Authorization.BEARER
                stagingRepository(layout.buildDirectory.dir("staging-deploy").get().toString())
            }
        }
    }
}
