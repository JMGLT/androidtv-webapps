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
        versionCode = 2
        versionName = "1.0.0"
    }

    // Una app independiente por servicio: así cada una tiene su propio acceso en el launcher.
    flavorDimensions += "service"
    productFlavors {
        create("netflix") {
            dimension = "service"
            applicationIdSuffix = ".netflix"
        }
        create("prime") {
            dimension = "service"
            applicationIdSuffix = ".prime"
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
}
