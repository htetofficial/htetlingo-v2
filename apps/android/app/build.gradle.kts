plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.htetlingo.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.htetlingo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "3.0.0"

        buildConfigField("String", "SUPABASE_URL", "\"https://tqwvzvbnintvijiessgm.supabase.co\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"sb_publishable_kBxm9l-qdy5dMfJIURYCGw_RwKCLV-E\"")
        buildConfigField("String", "REVENUECAT_API_KEY", "\"test_MlxZnHIsSDemVwtetEoTrDHUrrB\"")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.revenuecat.purchases:purchases:8.11.0")
}
