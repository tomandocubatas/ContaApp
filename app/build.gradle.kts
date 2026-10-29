plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// URL base por defecto; se puede sobreescribir con -P<nombre>=... o en gradle.properties.
fun apiBaseUrl(property: String, fallback: String): String =
    "\"${providers.gradleProperty(property).getOrElse(fallback)}\""

// Versión semántica: fix -> patch, mejora -> minor, cambio incompatible -> major.
val appVersionMajor = 0
val appVersionMinor = 24
val appVersionPatch = 1
val appVersionName = "$appVersionMajor.$appVersionMinor.$appVersionPatch"

// El APK sale como ContaApp-<versión>-<tipo>.apk.
base.archivesName.set("ContaApp-$appVersionName")

android {
    namespace = "com.contaapp.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.contaapp.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = appVersionMajor * 10000 + appVersionMinor * 100 + appVersionPatch
        versionName = appVersionName

        vectorDrawables {
            useSupportLibrary = true
        }

    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("String", "API_BASE_URL", apiBaseUrl("contaApiBaseUrlDebug", "http://10.0.2.2:4000/api/"))
        }
        release {
            buildConfigField("String", "API_BASE_URL", apiBaseUrl("contaApiBaseUrl", "https://10.0.2.2:4000/api/"))
            isMinifyEnabled = true
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
        buildConfig = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.okhttp)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
}
