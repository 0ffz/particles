import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xcontext-parameters")
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
        // JVM target platforms, you can remove entries from the list in case you want to target
        // only a specific platform
        val targetPlatforms = listOf("natives-windows", "natives-linux", "natives-macos", "natives-macos-arm64")
        val lwjglVersion = libs.versions.lwjgl.get()

        commonMain {
            dependencies {
                // add additional kotlin multi-platform dependencies here...

                api(libs.kool.core)

                api(project(":particles-config"))
                api(libs.kotlinx.coroutines.core)
                api(libs.koin.core)
                api(libs.kaml)
                implementation(libs.lets.plot.kotlin)
                implementation(libs.lets.plot.image.export)
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.make.observable)
                implementation(libs.multiplatform.settings.coroutines)
                implementation(libs.multiplatform.settings.serialization)
                implementation(libs.filekit.core)
                implementation(libs.filekit.dialogs)
            }
        }

        jvmMain {
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

        jsMain {
            dependencies {
                // add additional js-specific dependencies here...
            }
        }
    }
}
