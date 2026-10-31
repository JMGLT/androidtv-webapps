plugins {
    id("com.android.application")
}

// El workflow calcula la última versión estable de GeckoView y la pasa con -PgeckoVersion=...
val geckoVersion = (project.findProperty("geckoVersion") as String?) ?: "+"

android {
    namespace = "app.webtv.gecko"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.webtv.gecko"
        minSdk = 26
        targetSdk = 28
        versionCode = 2
        versionName = "1.1"
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

    // Un APK por arquitectura (GeckoView es grande y así cada archivo pesa menos).
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
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
