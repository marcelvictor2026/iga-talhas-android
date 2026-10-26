plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "br.com.giga.talhas"
    compileSdk = 35
    defaultConfig {
        applicationId = "br.com.giga.talhas"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.6"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
