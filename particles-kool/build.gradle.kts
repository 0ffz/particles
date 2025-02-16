import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    mavenLocal()
}

kotlin {
    // kotlin multiplatform (jvm + js) setup:
    jvm { }
    jvmToolchain(21)

    js {
        binaries.executable()
        browser {
            @OptIn(ExperimentalDistributionDsl::class)
            distribution {
                outputDirectory.set(File("${rootDir}/dist/js"))
            }
            commonWebpackConfig {
                //mode = KotlinWebpackConfig.Mode.PRODUCTION
                mode = KotlinWebpackConfig.Mode.DEVELOPMENT
            }
        }
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            target.set("es2015")
        }
    }
    
    sourceSets {
        val koolVersion = "0.17.0-SNAPSHOT"
        val lwjglVersion = "3.3.5"
        // JVM target platforms, you can remove entries from the list in case you want to target
        // only a specific platform
        val targetPlatforms = listOf("natives-windows", "natives-linux", "natives-macos", "natives-macos-arm64")

        val commonMain by getting {
            dependencies {
                // add additional kotlin multi-platform dependencies here...

                implementation("de.fabmax.kool:kool-core:$koolVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                implementation(project(":particles-dsl"))
                implementation(libs.kotlinx.coroutines.core)
                implementation("com.charleskorn.kaml:kaml:0.67.0")
//                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.6.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                // add additional jvm-specific dependencies here...

                // add required runtime libraries for lwjgl and physx-jni
                for (platform in targetPlatforms) {
                    // lwjgl runtime libs
                    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$platform")
                    listOf("glfw", "opengl", "jemalloc", "nfd", "stb", "vma", "shaderc").forEach { lib ->
                        runtimeOnly("org.lwjgl:lwjgl-$lib:$lwjglVersion:$platform")
                    }
                }
            }
        }
        
        val jsMain by getting {
            dependencies {
                // add additional js-specific dependencies here...
            }
        }
    }
}

task("runnableJar", Jar::class) {
    dependsOn("jvmJar")

    group = "app"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveAppendix.set("runnable")
    manifest {
        attributes["Main-Class"] = "LauncherKt"
    }

    configurations
        .asSequence()
        .filter { it.name.startsWith("common") || it.name.startsWith("jvm") }
        .map { it.copyRecursive().fileCollection { true } }
        .flatten()
        .distinct()
        .filter { it.exists() }
        .map { if (it.isDirectory) it else zipTree(it) }
        .forEach { from(it) }
    from(layout.buildDirectory.files("classes/kotlin/jvm/main"))

    doLast {
        copy {
            from(layout.buildDirectory.file("libs/${archiveBaseName.get()}-runnable.jar"))
            into("${rootDir}/dist/jvm")
        }
    }
}

task("runApp", JavaExec::class) {
    group = "app"
    dependsOn("jvmMainClasses")

    classpath = layout.buildDirectory.files("classes/kotlin/jvm/main")
    configurations
        .filter { it.name.startsWith("common") || it.name.startsWith("jvm") }
        .map { it.copyRecursive().filter { true } }
        .forEach { classpath += it }

    mainClass.set("LauncherKt")
}

val build by tasks.getting(Task::class) {
    dependsOn("runnableJar")
}

val clean by tasks.getting(Task::class) {
    doLast {
        delete("${rootDir}/dist")
    }
}
