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
    api("com.github.simplified-api:skyblock") { version { strictly("d566734") } }

    // Simplified Libraries (github.com/simplified-dev)
    api("com.github.simplified-dev:collections") { version { strictly("9696ca5") } }
    api("com.github.simplified-dev:utils") { version { strictly("3d8af56") } }
    api("com.github.simplified-dev:reflection") { version { strictly("158edbc") } }
    api("com.github.simplified-dev:gson-extras") { version { strictly("ed1d77e") } }
    api("com.github.simplified-dev:persistence") { version { strictly("2d6b0e7") } }
    api("com.github.simplified-dev:client") { version { strictly("2ced9a4") } }
    api("com.github.simplified-dev:expression") { version { strictly("2092f2e") } }

    // Minecraft-Library (github.com/minecraft-library)
    api("com.github.minecraft-library:text") { version { strictly("84f8f1a") } }
    api("com.github.minecraft-library:nbt-factory") { version { strictly("c2f5f8c") } }

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
