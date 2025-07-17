import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvm()
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.addAll("-Xexpect-actual-classes", "-Xcontext-parameters")
    }

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
        commonMain {
            dependencies {
                implementation(libs.kaml)
            }
        }
    }
}


//publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            groupId = "me.dvyy"
//            artifactId = "particles-dsl"
//            from(components["java"])
//        }
//    }
//    repositories {
//        maven {
//            name = "mineinabyss"
//            url = uri("https://repo.mineinabyss.com/snapshots/")
//            credentials(PasswordCredentials::class)
//        }
//    }
//}
