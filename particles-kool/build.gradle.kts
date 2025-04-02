import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    jvm {
        withJava()
    }
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

                api("de.fabmax.kool:kool-core:$koolVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                api(project(":particles-config"))
                api(libs.kotlinx.coroutines.core)
                api(libs.koin.core)
                api("com.charleskorn.kaml:kaml:0.67.0")
                implementation("org.jetbrains.lets-plot:lets-plot-kotlin:4.10.0")
                implementation("org.jetbrains.lets-plot:lets-plot-image-export:4.6.1")
                implementation("com.russhwolf:multiplatform-settings:1.3.0")
                implementation("com.russhwolf:multiplatform-settings-make-observable:1.3.0")
                implementation("com.russhwolf:multiplatform-settings-coroutines:1.3.0")
                implementation("com.russhwolf:multiplatform-settings-serialization:1.3.0")
                implementation("io.github.vinceglb:filekit-core:0.10.0-beta01")
                implementation("io.github.vinceglb:filekit-dialogs:0.10.0-beta01")
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
