import java.util.Properties

// Load API keys from local.properties so they never get committed to source control
val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

apply(plugin = "kotlin-parcelize")

android {
    namespace = "com.lasallecollegevancouver.gameinventoryapp"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.lasallecollegevancouver.gameinventoryapp"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject API key as a BuildConfig constant — access as BuildConfig.PRICE_CHARTING_API_KEY
        buildConfigField("String", "PRICE_CHARTING_API_KEY",
            "\"${localProperties.getProperty("priceChartingApiKey", "")}\"")

        // Pokémon TCG API key — stored in local.properties, never committed
        buildConfigField("String", "POKEMON_TCG_API_KEY",
            "\"${localProperties.getProperty("pokemonTcgApiKey", "")}\"")

        // RAWG.io API key — game cover art and metadata
        buildConfigField("String", "RAWG_API_KEY",
            "\"${localProperties.getProperty("rawgApiKey", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        // Required to generate the BuildConfig class with our API key fields
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)

    // Room — persistent local database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Navigation Component — NavGraph, NavController, back button handling
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lifecycle — provides lifecycleScope for running suspend functions in fragments
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Retrofit + OkHttp — HTTP networking for PriceCharting API calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // CameraX — camera preview for barcode scanning
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // ML Kit — barcode scanning and photo OCR
    implementation(libs.mlkit.barcode)
    implementation(libs.mlkit.text.recognition)

    // Glide — image loading for TCG card artwork
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Splash screen API — shows themed launch screen before first frame
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Pull-to-refresh on list screens
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
