import org.gradle.internal.os.OperatingSystem
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    alias(libs.plugins.kotlin.jvm)
//    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.shadow)
    alias(libs.plugins.runtime)
    alias(libs.plugins.gitarchive.tomarkdown).apply(false)
    alias(libs.plugins.versions)
}

val applicationMainClass = "me.dvyy.particles.MainKt"

/**  ## additional ORX features to be added to this project */
val orxFeatures = setOf<String>(
//  "orx-boofcv",
    "orx-camera",
//  "orx-chataigne",
    "orx-color",
    "orx-compositor",
//  "orx-compute-graph",
//  "orx-compute-graph-nodes",
    "orx-delegate-magic",
//  "orx-dnk3",
//  "orx-easing",
    "orx-envelopes",
//  "orx-expression-evaluator",
//  "orx-file-watcher",
    "orx-fx",
//  "orx-git-archiver",
//  "orx-gradient-descent",
    "orx-gui",
//  "orx-hash-grid",
    "orx-image-fit",
//  "orx-integral-image",
//  "orx-interval-tree",
//  "orx-jumpflood",
//  "orx-kdtree",
//  "orx-keyframer",
//  "orx-kinect-v1",
//  "orx-kotlin-parser",
//  "orx-marching-squares",
//  "orx-mesh-generators",
//  "orx-midi",
//  "orx-minim",
//    "orx-no-clear",
    "orx-noise",
//  "orx-obj-loader",
//    "orx-olive",
//  "orx-osc",
//  "orx-palette",
    "orx-panel",
//  "orx-parameters",
//  "orx-poisson-fill",
//  "orx-property-watchers",
//  "orx-quadtree",
//  "orx-rabbit-control",
//  "orx-realsense2",
//  "orx-runway",
//    "orx-shade-styles",
//  "orx-shader-phrases",
    "orx-shapes",
//  "orx-syphon",
//  "orx-temporal-blur",
//  "orx-tensorflow",
//  "orx-time-operators",
//  "orx-timer",
//  "orx-triangulation",
//  "orx-turtle",
//    "orx-video-profiles",
//    "orx-view-box",
)

/** ## additional ORML features to be added to this project */
val ormlFeatures = setOf<String>(
//    "orml-blazepose",
//    "orml-dbface",
//    "orml-facemesh",
//    "orml-image-classifier",
//    "orml-psenet",
//    "orml-ssd",
//    "orml-style-transfer",
//    "orml-super-resolution",
//    "orml-u2net",
)

/** ## additional OPENRNDR features to be added to this project */
//val openrndrFeatures = setOfNotNull(
//    if (DefaultNativePlatform("current").architecture.name != "arm-v8") "video" else null
//)

/** ## configure the type of logging this project uses */

// ------------------------------------------------------------------------------------------------------------------ //

repositories {
    mavenCentral()
    maven("https://repo.kotlin.link")
}

dependencies {
//    implementation("org.jetbrains.kotlinx:kandy-lets-plot:0.8.0-RC1")
//    implementation("space.kscience:plotlykt-server:0.7.1.1")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    implementation(project(":particles-dsl"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.slf4j.api)
    implementation(libs.kotlin.logging)

    runtimeOnly(libs.slf4j.simple)
    testImplementation(libs.junit)

    // kotlin scripting
    val kotlinVersion = "2.1.0"
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion")
}

// ------------------------------------------------------------------------------------------------------------------ //
kotlin {
    jvmToolchain(21)
}

// ------------------------------------------------------------------------------------------------------------------ //

project.setProperty("mainClassName", applicationMainClass)

application {
    if (hasProperty("openrndr.application")) {
        mainClass.set("${property("openrndr.application")}")
    }
}

tasks {
    shadowJar {
        manifest {
            attributes["Main-Class"] = applicationMainClass
            attributes["Implementation-Version"] = project.version
        }
        minimize {
            exclude(dependency("org.openrndr:openrndr-gl3:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-reflect:.*"))
            exclude(dependency("org.jetbrains.kotlin:kotlin-scripting-jvm-host:.*"))
            exclude(dependency("org.slf4j:slf4j-simple:.*"))
            exclude(dependency("org.apache.logging.log4j:log4j-slf4j2-impl:.*"))
            exclude(dependency("com.fasterxml.jackson.core:jackson-databind:.*"))
            exclude(dependency("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:.*"))
            exclude(dependency("org.bytedeco:.*"))
        }
    }
}

class Openrndr {
    val openrndrVersion = libs.versions.openrndr.get()
    val orxVersion = libs.versions.orx.get()
    val ormlVersion = libs.versions.orml.get()

    // choices are "orx-tensorflow-gpu", "orx-tensorflow"
    val orxTensorflowBackend = "orx-tensorflow"

    val currArch = DefaultNativePlatform("current").architecture.name
    val currOs = OperatingSystem.current()
    val os = if (project.hasProperty("targetPlatform")) {
        val supportedPlatforms = setOf("windows", "macos", "linux-x64", "linux-arm64")
        val platform: String = project.property("targetPlatform") as String
        if (platform !in supportedPlatforms) {
            throw IllegalArgumentException("target platform not supported: $platform")
        } else {
            platform
        }
    } else when {
        currOs.isWindows -> "windows"
        currOs.isMacOsX -> when (currArch) {
            "aarch64", "arm-v8" -> "macos-arm64"
            else -> "macos"
        }

        currOs.isLinux -> when (currArch) {
            "x86-64" -> "linux-x64"
            "aarch64" -> "linux-arm64"
            else -> throw IllegalArgumentException("architecture not supported: $currArch")
        }

        else -> throw IllegalArgumentException("os not supported: ${currOs.name}")
    }

    fun orx(module: String) = "org.openrndr.extra:$module:$orxVersion"
    fun orml(module: String) = "org.openrndr.orml:$module:$ormlVersion"
    fun openrndr(module: String) = "org.openrndr:openrndr-$module:$openrndrVersion"
    fun openrndrNatives(module: String) = "org.openrndr:openrndr-$module-natives-$os:$openrndrVersion"
    fun orxNatives(module: String) = "org.openrndr.extra:$module-natives-$os:$orxVersion"

    init {
        dependencies {
            runtimeOnly(openrndr("gl3"))
            for(platform in listOf("windows", "macos", "linux-x64", "linux-arm64")) {
                runtimeOnly("org.openrndr:openrndr-gl3-natives-$platform:$openrndrVersion")
            }
//            runtimeOnly(openrndrNatives("gl3"))
//            runtimeOnly(openrndrNatives("openal"))
            implementation(openrndr("openal"))
            implementation(openrndr("application"))
            implementation(openrndr("svg"))
            implementation(openrndr("animatable"))
            implementation(openrndr("extensions"))
            implementation(openrndr("filter"))
            implementation(openrndr("dialogs"))
//            if ("video" in openrndrFeatures) {
//                implementation(openrndr("ffmpeg"))
//                runtimeOnly(openrndrNatives("ffmpeg"))
//            }
            for (feature in orxFeatures) {
                implementation(orx(feature))
            }
            for (feature in ormlFeatures) {
                implementation(orml(feature))
            }
            if ("orx-tensorflow" in orxFeatures) runtimeOnly("org.openrndr.extra:$orxTensorflowBackend-natives-$os:$orxVersion")
            if ("orx-kinect-v1" in orxFeatures) runtimeOnly(orxNatives("orx-kinect-v1"))
            if ("orx-olive" in orxFeatures) implementation(libs.kotlin.script.runtime)
        }
    }
}

val openrndr = Openrndr()
