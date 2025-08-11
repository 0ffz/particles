@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.shadow)
}

kotlin {
    jvm {
        mainRun {
            mainClass = "me.dvyy.particles.forces.MainKt"
        }
    }
    jvmToolchain(21)

    js {
        browser {
            @OptIn(ExperimentalDistributionDsl::class)
            distribution {
                outputDirectory.set(projectDir.resolve("output"))
            }
            commonWebpackConfig {
                //mode = KotlinWebpackConfig.Mode.PRODUCTION
                mode = KotlinWebpackConfig.Mode.DEVELOPMENT
            }
        }
        compilerOptions {
            target.set("es2015")
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":particles-kool"))
                implementation(project(":particles-ui"))
            }
        }
    }
}

tasks {
    shadowJar {
        archiveBaseName = "particles"
        archiveClassifier = null
        doLast {
            copy {
                from(shadowJar.get().outputs.files)
                into("$rootDir/dist/jvm/")
            }
        }
    }
    clean {
        doLast {
            delete("$rootDir/dist")
        }
    }
    val jsBrowserProductionWebpack by getting
    register("jsDist") {
        dependsOn(jsBrowserProductionWebpack)
        doLast {
            copy {
                from(
                    "$projectDir/build/processedResources/js/main",
                    "$projectDir/build/kotlin-webpack/js/productionExecutable/"
                )
                into("$rootDir/dist/js")
            }
        }
    }
}
