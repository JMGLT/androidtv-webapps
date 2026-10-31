plugins {
    id("com.android.application")
}

// Versión común definida en gradle.properties (appVersion=MAYOR.MENOR.PARCHE)
val appVersion = (project.findProperty("appVersion") as String?) ?: "1.0.0"
val versionParts = appVersion.split(".").map { it.toInt() }

android {
    namespace = "app.webtv.webview"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.webtv.webview"
        minSdk = 21
        targetSdk = 28
        versionCode = versionParts[0] * 10000 + versionParts[1] * 100 + versionParts[2]
        versionName = appVersion
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
