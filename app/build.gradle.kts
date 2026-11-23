plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.lemovi.tv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lemovi.tv"
        minSdk = 23
        targetSdk = 28
        versionCode = 6
        versionName = "1.0.6"
    }

    flavorDimensions += "device"
    productFlavors {
        create("tv") {
            dimension = "device"
            resValue("string", "app_name", "Lemovi TV")
        }
        create("mobile") {
            dimension = "device"
            applicationIdSuffix = ".mobile"
            resValue("string", "app_name", "Lemovi Mobile")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}


dependencies {
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
}
