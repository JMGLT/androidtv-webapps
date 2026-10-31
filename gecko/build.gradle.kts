plugins {
    id("com.android.application")
}

// El workflow calcula la última versión estable de GeckoView y la pasa con -PgeckoVersion=...
val geckoVersion = (project.findProperty("geckoVersion") as String?) ?: "+"

// Versión común definida en gradle.properties (appVersion=MAYOR.MENOR.PARCHE)
val appVersion = (project.findProperty("appVersion") as String?) ?: "1.0.0"
val versionParts = appVersion.split(".").map { it.toInt() }

android {
    namespace = "app.webtv.gecko"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.webtv.gecko"
        minSdk = 26
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
        create("dazn") {
            dimension = "service"
            applicationIdSuffix = ".dazn"
        }
        create("sky") {
            dimension = "service"
            applicationIdSuffix = ".sky"
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
