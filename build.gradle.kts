import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    alias(libs.plugins.kotlin.multiplatform) apply false
//    alias(libs.plugins.kotlinx.serialization)
//    alias(libs.plugins.shadow)
//    alias(libs.plugins.runtime)
//    alias(libs.plugins.gitarchive.tomarkdown).apply(false)
//    alias(libs.plugins.versions)
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
//
//val applicationMainClass = "me.dvyy.particles.MainKt"
//
//project.setProperty("mainClassName", applicationMainClass)
//
//application {
//    if (hasProperty("openrndr.application")) {
//        mainClass.set("${property("openrndr.application")}")
//    }
//}
//
//tasks {
//    shadowJar {
//        manifest {
//            attributes["Main-Class"] = applicationMainClass
//            attributes["Implementation-Version"] = project.version
//        }
//        minimize {
//            exclude(dependency("org.openrndr:openrndr-gl3:.*"))
//            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
//            exclude(dependency("org.jetbrains.kotlin:kotlin-scripting-jvm-host:.*"))
//            exclude(dependency("org.slf4j:slf4j-simple:.*"))
//            exclude(dependency("org.apache.logging.log4j:log4j-slf4j2-impl:.*"))
//            exclude(dependency("com.fasterxml.jackson.core:jackson-databind:.*"))
//            exclude(dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:.*"))
//            exclude(dependency("org.bytedeco:.*"))
//            exclude(project(":particles-dsl"))
//        }
//    }
//}
//
//class Openrndr {
//    val openrndrVersion = libs.versions.openrndr.get()
//    val orxVersion = libs.versions.orx.get()
//    val ormlVersion = libs.versions.orml.get()
//
//    // choices are "orx-tensorflow-gpu", "orx-tensorflow"
//    val orxTensorflowBackend = "orx-tensorflow"
//
//    val currArch = DefaultNativePlatform("current").architecture.name
//    val currOs = OperatingSystem.current()
//    val os = if (project.hasProperty("targetPlatform")) {
//        val supportedPlatforms = setOf("windows", "macos", "linux-x64", "linux-arm64")
//        val platform: String = project.property("targetPlatform") as String
//        if (platform !in supportedPlatforms) {
//            throw IllegalArgumentException("target platform not supported: $platform")
//        } else {
//            platform
//        }
//    } else when {
//        currOs.isWindows -> "windows"
//        currOs.isMacOsX -> when (currArch) {
//            "aarch64", "arm-v8" -> "macos-arm64"
//            else -> "macos"
//        }
//
//        currOs.isLinux -> when (currArch) {
//            "x86-64" -> "linux-x64"
//            "aarch64" -> "linux-arm64"
//            else -> throw IllegalArgumentException("architecture not supported: $currArch")
//        }
//
//        else -> throw IllegalArgumentException("os not supported: ${currOs.name}")
//    }
//
//    fun orx(module: String) = "org.openrndr.extra:$module:$orxVersion"
//    fun orml(module: String) = "org.openrndr.orml:$module:$ormlVersion"
//    fun openrndr(module: String) = "org.openrndr:openrndr-$module:$openrndrVersion"
//    fun openrndrNatives(module: String) = "org.openrndr:openrndr-$module-natives-$os:$openrndrVersion"
//    fun orxNatives(module: String) = "org.openrndr.extra:$module-natives-$os:$orxVersion"
//
//    init {
//        dependencies {
//            runtimeOnly(openrndr("gl3"))
//            for(platform in listOf("windows", "macos", "linux-x64", "linux-arm64")) {
//                runtimeOnly("org.openrndr:openrndr-gl3-natives-$platform:$openrndrVersion")
//            }
////            runtimeOnly(openrndrNatives("gl3"))
////            runtimeOnly(openrndrNatives("openal"))
//            implementation(openrndr("openal"))
//            implementation(openrndr("application"))
//            implementation(openrndr("svg"))
//            implementation(openrndr("animatable"))
//            implementation(openrndr("extensions"))
//            implementation(openrndr("filter"))
//            implementation(openrndr("dialogs"))
////            if ("video" in openrndrFeatures) {
////                implementation(openrndr("ffmpeg"))
////                runtimeOnly(openrndrNatives("ffmpeg"))
////            }
//            for (feature in orxFeatures) {
//                implementation(orx(feature))
//            }
//            for (feature in ormlFeatures) {
//                implementation(orml(feature))
//            }
//            if ("orx-tensorflow" in orxFeatures) runtimeOnly("org.openrndr.extra:$orxTensorflowBackend-natives-$os:$orxVersion")
//            if ("orx-kinect-v1" in orxFeatures) runtimeOnly(orxNatives("orx-kinect-v1"))
////            if ("orx-olive" in orxFeatures) implementation(libs.kotlin.script.runtime)
//        }
//    }
//}
//
//val openrndr = Openrndr()
