plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.mineinabyss.com/releases")
        maven("https://repo.mineinabyss.com/snapshots")
        mavenLocal()
    }
}

configurations {
    create("docs")
}

dependencies {
    "docs"("me.dvyy:shocky-docs:0.0.8")
}

tasks {
    register<JavaExec>("docsGenerate") {
        classpath = configurations.getByName("docs")
        mainClass.set("me.dvyy.shocky.docs.MainKt")
        args("generate")
    }
    register<JavaExec>("docsServe") {
        classpath = configurations.getByName("docs")
        mainClass.set("me.dvyy.shocky.docs.MainKt")
        args("serve")
    }
}
