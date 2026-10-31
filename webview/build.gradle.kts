plugins {
    id("com.android.application")
}

// Versión de cada app, definida en gradle.properties (version.webview.<servicio>=X.Y.Z)
fun appVersion(service: String): String =
    (project.findProperty("version.webview.$service") as String?) ?: "1.0.0"

fun versionCodeOf(version: String): Int {
    val p = version.split(".").map { it.toInt() }
    return p[0] * 10000 + p[1] * 100 + p[2]
}

android {
    namespace = "app.webtv.webview"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.webtv.webview"
        minSdk = 21
        targetSdk = 28
    }

    // Una app independiente por servicio: así cada una tiene su propio acceso en el launcher
    // y su propio número de versión.
    flavorDimensions += "service"
    productFlavors {
        create("netflix") {
            dimension = "service"
            applicationIdSuffix = ".netflix"
            versionName = appVersion("netflix")
            versionCode = versionCodeOf(appVersion("netflix"))
        }
        create("prime") {
            dimension = "service"
            applicationIdSuffix = ".prime"
            versionName = appVersion("prime")
            versionCode = versionCodeOf(appVersion("prime"))
        }
        create("dazn") {
            dimension = "service"
            applicationIdSuffix = ".dazn"
            versionName = appVersion("dazn")
            versionCode = versionCodeOf(appVersion("dazn"))
        }
        create("sky") {
            dimension = "service"
            applicationIdSuffix = ".sky"
            versionName = appVersion("sky")
            versionCode = versionCodeOf(appVersion("sky"))
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
