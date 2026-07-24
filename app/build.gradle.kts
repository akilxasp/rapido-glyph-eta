plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.akil.rapidoglyph"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.akil.rapidoglyph"
        minSdk = 34
        targetSdk = 36
        versionCode = 3
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
    testImplementation("junit:junit:4.13.2")
}
