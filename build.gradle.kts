plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
}

allprojects {
    repositories {
        mavenCentral()
        mavenLocal()
//        maven("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
