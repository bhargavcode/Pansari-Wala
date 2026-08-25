import java.util.Properties
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
            ?: "https://api.pansariwala.shop"
    val signingProps = Properties().apply {
        listOf(
            rootProject.file("local.properties"),
            file("keystore.properties"),
            rootProject.file("keystore.properties"),
        ).filter { it.exists() }.forEach { source ->
            source.inputStream().use { load(it) }
        }
    }
    fun signingValue(envName: String, vararg propertyKeys: String): String? {
        System.getenv(envName)?.takeIf { it.isNotBlank() }?.let { return it }
        propertyKeys.forEach { key ->
            (project.findProperty(key) as String?)?.takeIf { it.isNotBlank() }?.let { return it }
            signingProps.getProperty(key)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return null
    }
    val defaultKeystore = file("pansariwala.jks").takeIf { it.exists() }
    val keystorePath = signingValue("ANDROID_KEYSTORE_FILE", "ANDROID_KEYSTORE_FILE", "storeFile")
        ?: defaultKeystore?.absolutePath
    val keystorePassword = signingValue("ANDROID_KEYSTORE_PASSWORD", "ANDROID_KEYSTORE_PASSWORD", "storePassword")
    val keyAlias = signingValue("ANDROID_KEY_ALIAS", "ANDROID_KEY_ALIAS", "keyAlias") ?: "pansariwala"
    val keyPassword = signingValue("ANDROID_KEY_PASSWORD", "ANDROID_KEY_PASSWORD", "keyPassword")
        ?: keystorePassword
    val hasReleaseSigning =
        !keystorePath.isNullOrBlank() &&
            file(keystorePath!!).isFile &&
            !keystorePassword.isNullOrBlank() &&
            keyAlias.isNotBlank() &&
            !keyPassword.isNullOrBlank()
    if (!hasReleaseSigning) {
        logger.warn(
            "Android Studio Run will use the debug keystore (Firebase OTP needs pansariwala.jks). " +
                "Add ANDROID_KEYSTORE_PASSWORD / ANDROID_KEY_PASSWORD to local.properties or androidApp/keystore.properties.",
        )
    }
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
            // Sign debug the same as release so Android Studio Run matches Firebase SHA fingerprints.
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
