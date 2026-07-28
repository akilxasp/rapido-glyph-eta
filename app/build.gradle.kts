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
        versionCode = 9
        versionName = "0.4.3"

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
    implementation(files("libs/glyph-matrix-sdk-2.0.aar"))
    testImplementation("junit:junit:4.13.2")
}
