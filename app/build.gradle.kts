plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.example.textocr'
    compileSdk 35

    defaultConfig {
        applicationId "com.example.textocr"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"

        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
        }
    }

    buildFeatures {
        viewBinding true
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    aaptOptions {
        noCompress "tflite" // Prevent model compression
    }

    packagingOptions {
        exclude "META-INF/LICENSE.txt"
        exclude "META-INF/NOTICE.txt"
    }

    sourceSets {
        main {
            java.srcDirs = ['src/main/java']
            res.srcDirs = ['src/main/res']
            manifest.srcFile 'src/main/AndroidManifest.xml'
            assets.srcDirs = ['src/main/assets'] // <-- moved to correct syntax
        }
    }
}

dependencies {
    // AndroidX & Material
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'androidx.activity:activity-ktx:1.8.2'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'com.google.android.material:material:1.11.0'

    // CameraX
    implementation 'androidx.camera:camera-camera2:1.3.0'
    implementation 'androidx.camera:camera-lifecycle:1.3.0'
    implementation 'androidx.camera:camera-view:1.3.0'

    // ML Kit (offline)
    implementation 'com.google.mlkit:text-recognition:16.0.1'
    implementation 'com.google.mlkit:text-recognition-chinese:16.0.1'
    implementation 'com.google.mlkit:text-recognition-devanagari:16.0.1'
    implementation 'com.google.mlkit:text-recognition-japanese:16.0.1'
    implementation 'com.google.mlkit:text-recognition-korean:16.0.1'
    implementation 'com.google.mlkit:image-labeling:17.0.7'
    implementation 'com.google.mlkit:object-detection:17.0.0'

    // TensorFlow Lite (for live object detection using custom model)
    implementation 'org.tensorflow:tensorflow-lite:2.13.0'
    implementation 'org.tensorflow:tensorflow-lite-support:0.3.1'
    implementation 'org.tensorflow:tensorflow-lite-task-vision:0.3.1'

    // Image cropping and preview
    implementation 'com.github.yalantis:ucrop:2.2.8'
    implementation 'com.github.chrisbanes:PhotoView:2.3.0'

    // OpenCV native library
    implementation project(':openCVLibrary')
    implementation libs.androidx.foundation.android
    // Barcode and QR code
    implementation 'com.google.mlkit:barcode-scanning:17.1.0'
    // Testing
    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
