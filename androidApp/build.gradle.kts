import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.uiToolingPreview)
    implementation(libs.koin.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.razorpay.checkout)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "org.bhargav.pansariwala"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.bhargav.pansariwala"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    flavorDimensions += "product"
    productFlavors {
        create("pos") {
            dimension = "product"
            applicationId = "org.bhargav.pansariwala"
            resValue("string", "app_name", "Pansari PoS")
            buildConfigField("String", "APP_PRODUCT", "\"POS\"")
        }
        create("user") {
            dimension = "product"
            applicationId = "org.bhargav.pansariwala.user"
            resValue("string", "app_name", "Pansari Market")
            buildConfigField("String", "APP_PRODUCT", "\"USER\"")
        }
        create("delivery") {
            dimension = "product"
            applicationId = "org.bhargav.pansariwala.delivery"
            resValue("string", "app_name", "Pansari Partner")
            buildConfigField("String", "APP_PRODUCT", "\"DELIVERY\"")
        }
    }
    val apiBaseUrl =
        System.getenv("API_BASE_URL")
            ?: (project.findProperty("API_BASE_URL") as String?)
            ?: "http://35.172.232.196:8080"
    val keystorePath = System.getenv("ANDROID_KEYSTORE_FILE")
    val keystorePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
    val keyAlias = System.getenv("ANDROID_KEY_ALIAS")
    val keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    val hasReleaseSigning =
        !keystorePath.isNullOrBlank() &&
            !keystorePassword.isNullOrBlank() &&
            !keyAlias.isNullOrBlank() &&
            !keyPassword.isNullOrBlank()
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }
    buildTypes {
        debug {
            // Always use the default Android debug keystore — never the production keystore.
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
