plugins {
    id("com.android.application")
}

android {
    namespace = "com.maxiaol624.wordtrail"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.maxiaol624.wordtrail"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
}
