plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val previewApk = providers.gradleProperty("hcPreviewApk").orNull == "true"
val previewRevision = providers.gradleProperty("hcPreviewRevision").orNull

android {
    namespace = "com.hyrulecompanion.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hyrulecompanion.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 11900
        versionName = "1.19.0"
        manifestPlaceholders["hcAppLabel"] = "Hyrule Companion"

        // These are the public project URL + publishable key in the supplied web source.
        // NEVER put a Supabase service_role or secret key in an Android client.
        buildConfigField("String", "SUPABASE_URL", "\"https://kramrknuxdkwxusskmrv.supabase.co\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"sb_publishable_gBTd7h9gqie13ZvpOhdSMA_TZWHkxjD\"")
    }
    if (previewApk) {
        buildTypes.getByName("debug") {
            applicationIdSuffix = ".preview.v${defaultConfig.versionCode}" + (previewRevision?.let { ".$it" } ?: "")
            manifestPlaceholders["hcAppLabel"] = "Hyrule Companion Teste"
        }
    }
    // CI release signatures are supplied through environment variables (never committed).
    // All release APKs MUST use the same keystore to update existing installations.
    val keystorePath = System.getenv("HC_KEYSTORE_PATH")
    val storePasswordValue = System.getenv("HC_KEYSTORE_PASSWORD")
    val keyAliasValue = System.getenv("HC_KEY_ALIAS")
    val keyPasswordValue = System.getenv("HC_KEY_PASSWORD")
    if (!keystorePath.isNullOrBlank() && !storePasswordValue.isNullOrBlank() &&
        !keyAliasValue.isNullOrBlank() && !keyPasswordValue.isNullOrBlank()) {
        signingConfigs {
            create("production") {
                storeFile = file(keystorePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
        buildTypes {
            getByName("release") { signingConfig = signingConfigs.getByName("production") }
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.browser:browser:1.8.0")
    implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    testImplementation("junit:junit:4.13.2")
}
