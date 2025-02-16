import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlinx.serialization)
}

repositories {
    mavenCentral()
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

        val commonMain by getting {
            dependencies {
                implementation("com.charleskorn.kaml:kaml:0.67.0")
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
