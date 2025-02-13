plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlinx.serialization)
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.dvyy"
            artifactId = "particles-dsl"
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "mineinabyss"
            url = uri("https://repo.mineinabyss.com/snapshots/")
            credentials(PasswordCredentials::class)
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    val openRndrVersion = libs.versions.openrndr.get()
//    api("org.openrndr:openrndr-color:$openRndrVersion")
    implementation("com.charleskorn.kaml:kaml:0.67.0")
}

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}
