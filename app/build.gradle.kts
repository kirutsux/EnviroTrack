import java.util.Properties

kotlin
plugins {
    id("com.android.application") // do NOT specify version
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt")
}


android {
    namespace = "com.ecocp.capstoneenvirotrack"
    compileSdk = 35

    val properties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        properties.load(localPropertiesFile.inputStream())
    }
    
    defaultConfig {
        applicationId = "com.ecocp.capstoneenvirotrack"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            properties.getProperty("OPENAI_API_KEY") ?: ""
        )
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA",
                "META-INF/DEPENDENCIES",
                "META-INF/io.netty.versions.properties",
                "META-INF/NOTICE",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE.txt",
                "META-INF/ASL2.0"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.5")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    //OkHTTP
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Firebase (BOM manages versions)
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
    implementation("com.google.firebase:firebase-common-ktx") // ✅ add this
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("de.hdodenhof:circleimageview:3.1.0")

    //completed
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")


    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.filament.android)
    implementation(libs.google.material)
    implementation(libs.androidx.animation.core)
    implementation ("com.google.firebase:firebase-messaging-ktx")
    implementation(libs.volley)


    kapt("com.github.bumptech.glide:compiler:4.16.0")

    // Stripe
    implementation("com.stripe:stripe-android:21.18.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Global exclusions
configurations.all {
    exclude(group = "com.google.protobuf", module = "protobuf-java")
    exclude(group = "com.google.api.grpc", module = "proto-google-common-protos")
}
