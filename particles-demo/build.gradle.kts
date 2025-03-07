import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    application
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.shadow)
}

kotlin {
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
        val commonMain by getting {
            dependencies {
                implementation(project(":particles-kool"))
            }
        }
    }
}

application {
    mainClass = "me.dvyy.particles.forces.MainKt"
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
}
