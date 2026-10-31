plugins {
    id("com.android.application")
}

android {
    namespace = "app.webtv.webview"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.webtv.webview"
        minSdk = 21
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
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
}
