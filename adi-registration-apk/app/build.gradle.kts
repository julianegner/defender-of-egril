plugins {
    id("com.android.application") version "8.13.2"
}

android {
    namespace = "de.egril.adi"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.egril.adi"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }
}
