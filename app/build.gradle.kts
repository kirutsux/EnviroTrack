plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.ecocp.capstoneenvirotrack"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ecocp.capstoneenvirotrack"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase BOM (Ensures compatibility)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))

    // Firebase Auth & Firestore
    implementation("com.google.firebase:firebase-auth-ktx")   // Firebase Auth
    implementation("com.google.firebase:firebase-firestore-ktx") // Firestore (optional)

    // Firebase Database (optional)
    implementation("com.google.firebase:firebase-database-ktx")
}