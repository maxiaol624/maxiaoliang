plugins {
    id("com.android.application")
    id("com.google.devtools.ksp") version "2.3.11"
}

android {
    namespace = "com.maxiaol624.wordtrail"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.maxiaol624.wordtrail"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
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
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.room:room-runtime:2.8.5")
    implementation("androidx.work:work-runtime:2.12.0")
    implementation("androidx.datastore:datastore-preferences:1.3.0")
    ksp("androidx.room:room-compiler:2.8.5")
    testImplementation("junit:junit:4.13.2")
}

ksp {
    arg("room.schemaLocation", file("schemas").path)
}
