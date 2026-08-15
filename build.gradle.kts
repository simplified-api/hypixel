plugins {
    id("java-library")
    idea
}

group = "dev.sbs"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven(url = "https://central.sonatype.com/repository/maven-snapshots")
    maven(url = "https://jitpack.io")
}

dependencies {
    // Simplified Annotations
    compileOnly(libs.simplified.annotations)
    annotationProcessor(libs.simplified.annotations)
    testCompileOnly(libs.simplified.annotations)
    testAnnotationProcessor(libs.simplified.annotations)

    // Tests
    testImplementation(libs.hamcrest)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.junit.platform.launcher)

    // Sibling API modules (composite-build substitutes by project name)
    api("com.github.simplified-api:skyblock") { version { strictly("2701e81") } }

    // Simplified Libraries (github.com/simplified-dev)
    api("com.github.simplified-dev:collections") { version { strictly("23f01b6") } }
    api("com.github.simplified-dev:utils") { version { strictly("381e317") } }
    api("com.github.simplified-dev:reflection") { version { strictly("d02f3ea") } }
    api("com.github.simplified-dev:gson-extras") { version { strictly("c4bde8d") } }
    api("com.github.simplified-dev:persistence") { version { strictly("a1c9ca2") } }
    api("com.github.simplified-dev:client") { version { strictly("2a3f2fc") } }
    api("com.github.simplified-dev:expression") { version { strictly("6cf527b") } }

    // Minecraft-Library (github.com/minecraft-library)
    api("com.github.minecraft-library:text") { version { strictly("117775e") } }
    api("com.github.minecraft-library:nbt-factory") { version { strictly("f8b5f52") } }

    // Gson - @GsonType-annotated inner classes plus direct Deserializer/TypeAdapter usage
    api(libs.gson)
}

idea {
    module {
        excludeDirs.addAll(listOf(
            layout.projectDirectory.dir(".schema").asFile
        ))
    }
}

tasks {
    test {
        useJUnitPlatform()

        // The characterisation harness reads the corpus out of the sibling skyblock checkout and, on
        // request, rewrites its golden file. Neither reaches a forked test JVM unless it is handed
        // over.
        listOf("skyblock.corpus.root", "profile-stats.golden").forEach { key ->
            System.getProperty(key)?.let { systemProperty(key, it) }
        }
    }
}
