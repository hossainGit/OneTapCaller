plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt") // Add this line for kapt
}

android {
    namespace = "com.example.elder_phone"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.elder_phone"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

// C:/app Dev/elder_phone2/app/build.gradle.kts

dependencies {

    /// C:/app Dev/elder_phone2/app/build.gradle.kts

//
// DELETE the entire old dependencies { ... } block and REPLACE it with this:
//
    dependencies {
        // Core Android libraries from the version catalog
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.appcompat)
        implementation(libs.material)
        implementation(libs.androidx.activity)
        implementation(libs.androidx.constraintlayout)
        implementation(libs.androidx.recyclerview)

        // Testing
        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)

        // Room Database
        implementation(libs.androidx.room.runtime)
        implementation(libs.androidx.room.ktx)
        kapt(libs.androidx.room.compiler) // Use kapt for the compiler

        // Lifecycle Components (ViewModel, LiveData)
        implementation(libs.androidx.lifecycle.viewmodel.ktx)
        implementation(libs.androidx.lifecycle.livedata.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)

        // Activity and Fragment KTX for modern APIs
        implementation(libs.androidx.activity.ktx)
        implementation(libs.androidx.fragment.ktx)

        // Image Loading
        implementation(libs.coil)

        // ExifInterface for image rotation metadata
        implementation(libs.androidx.exifinterface)

        // Kotlin Coroutines (You had this hardcoded, it's good practice to add it to the TOML file, but we can leave it for now)
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    }


}
