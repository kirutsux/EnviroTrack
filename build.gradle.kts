// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // Navigation Safe Args plugin
        val nav_version = "2.9.5"
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$nav_version")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.21")
    }
}

// Top-level build.gradle.kts

plugins {
    // Android Gradle Plugin
    id("com.android.application") version "8.6.0" apply false
    id("com.android.library") version "8.6.0" apply false

    // Kotlin
    id("org.jetbrains.kotlin.jvm") version "1.9.10" apply false
    // Google services
    id("com.google.gms.google-services") version "4.4.2" apply false

    // Navigation Safe Args
    id("androidx.navigation.safeargs") version "2.9.5" apply false
}