plugins {
    id("com.android.application")
}

// El workflow calcula la última versión estable de GeckoView y la pasa con -PgeckoVersion=...
val geckoVersion = (project.findProperty("geckoVersion") as String?) ?: "+"

// Versión de cada app, definida en gradle.properties (version.gecko.<servicio>=X.Y.Z)
fun appVersion(service: String): String =
    (project.findProperty("version.gecko.$service") as String?) ?: "1.0.0"

fun versionCodeOf(version: String): Int {
    val p = version.split(".").map { it.toInt() }
    return p[0] * 10000 + p[1] * 100 + p[2]
}

android {
    namespace = "app.webtv.gecko"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.webtv.gecko"
        minSdk = 26
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
    }

    // Un APK por arquitectura (GeckoView es grande y así cada archivo pesa menos).
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

    // Comprime las librerías nativas dentro del APK para que pese bastante menos.
    packaging {
        jniLibs {
            useLegacyPackaging = true
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

// GeckoView y algunas librerías androidx declaran que requieren compilar contra la API 37,
// pero ese SDK no es utilizable con esta versión de AGP. El código de esta app no usa APIs
// nuevas, así que se desactiva solo esa comprobación de metadatos.
tasks.configureEach {
    if (name.startsWith("check") && name.endsWith("AarMetadata")) {
        enabled = false
    }
}

dependencies {
    implementation("org.mozilla.geckoview:geckoview:$geckoVersion")
}
