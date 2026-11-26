plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.lichviec.app"
    compileSdk = 35

    val fixedKeystoreFile = file("lichviec-release.jks")
    val hasFixedKeystore = fixedKeystoreFile.exists()
    val buildNum = System.getenv("BUILD_VERSION_CODE")?.toIntOrNull() ?: 1

    defaultConfig {
        applicationId = "com.lichviec.app"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNum
        versionName = "1.0.$buildNum"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasFixedKeystore) {
            create("fixed") {
                storeFile = fixedKeystoreFile
                storePassword = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "lichviec2026"
                keyAlias = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() } ?: "lichviec"
                keyPassword = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() } ?: "lichviec2026"
            }
        }
    }

    buildTypes {
        debug {
            if (hasFixedKeystore) {
                signingConfig = signingConfigs.getByName("fixed")
            }
        }
        release {
            isMinifyEnabled = false
            if (hasFixedKeystore) {
                signingConfig = signingConfigs.getByName("fixed")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
