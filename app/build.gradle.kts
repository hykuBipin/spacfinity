plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}

android {
 namespace = "com.bipin.objectdetector"
 compileSdk = 35

 defaultConfig {
  applicationId = "com.bipin.objectdetector"
  minSdk = 24
  targetSdk = 35

  versionCode = 1
  versionName = "1.0"

  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

  // IMPORTANT for emulator ABI issues
  ndk {
   abiFilters += listOf("arm64-v8a", "x86_64")
  }
 }

 buildTypes {
  release {
   isMinifyEnabled = false
   proguardFiles(
    getDefaultProguardFile("proguard-android-optimize.txt"),
    "proguard-rules.pro"
   )
  }
  debug {
   isDebuggable = true
  }
 }

 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }

 kotlinOptions {
  jvmTarget = "17"
 }

 packaging {
  resources {
   excludes += setOf(
    "META-INF/*",
    "META-INF/DEPENDENCIES",
    "META-INF/LICENSE*",
    "META-INF/NOTICE*"
   )
  }

  // FIX for native .so loading / ELF / page-size emulator crashes
  jniLibs {
   useLegacyPackaging = true
  }
 }

 buildFeatures {
  viewBinding = true
 }
}

dependencies {
 implementation("androidx.core:core-ktx:1.13.1")
 implementation("androidx.appcompat:appcompat:1.7.0")
 implementation("com.google.android.material:material:1.12.0")

 implementation("androidx.constraintlayout:constraintlayout:2.2.0")

 // CameraX (if you're using YOLO / camera pipeline)
 val cameraxVersion = "1.3.4"
 implementation("androidx.camera:camera-core:$cameraxVersion")
 implementation("androidx.camera:camera-camera2:$cameraxVersion")
 implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
 implementation("androidx.camera:camera-view:$cameraxVersion")

 // ONNX Runtime for inference
 implementation("com.microsoft.onnxruntime:onnxruntime-android:1.18.0")

 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}