plugins {
    id("com.android.application")
}

// El workflow calcula la última versión estable de GeckoView y la pasa con -PgeckoVersion=...
val geckoVersion = (project.findProperty("geckoVersion") as String?) ?: "+"

android {
    namespace = "app.webtv.gecko"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.webtv.gecko"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "1.0"
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

dependencies {
    implementation("org.mozilla.geckoview:geckoview:$geckoVersion")
}
