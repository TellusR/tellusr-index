plugins {
    kotlin("jvm") version "2.1.20"
    `maven-publish`
}

group = "com.tellusr"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    val ktor_version = "3.1.2"
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktor_version}")
    implementation(kotlin("reflect"))

    val serialization_version = "1.6.2"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-hocon:$serialization_version")

    val lucene_version = "10.2.0"
    implementation("org.apache.lucene:lucene-core:$lucene_version")
    implementation("org.apache.lucene:lucene-queryparser:$lucene_version")
    implementation("org.apache.lucene:lucene-queries:$lucene_version")
    implementation("org.apache.lucene:lucene-suggest:$lucene_version")
    implementation("org.apache.lucene:lucene-analysis-common:$lucene_version")
    implementation("org.apache.lucene:lucene-backward-codecs:$lucene_version")

    val logback_version = "1.5.13"
    implementation("ch.qos.logback:logback-classic:${logback_version}")


    testImplementation(kotlin("test"))
}

tasks.withType<JavaExec> {
    jvmArgs(
        "--add-modules", "jdk.incubator.vector",
        "--enable-native-access=ALL-UNNAMED"
    )
}

tasks.test {
    useJUnitPlatform()
    // For test-tasken
    jvmArgs(
        "--add-modules", "jdk.incubator.vector",
        "--enable-native-access=ALL-UNNAMED"
    )
}
kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            groupId = group as String
            artifactId = "tellusr-searchindex"
            version = version as String
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/tellusr/framework")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
