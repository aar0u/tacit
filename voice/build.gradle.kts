// SPDX-FileCopyrightText: 2026 Tacit contributors
//
// SPDX-License-Identifier: GPL-3.0-or-later

plugins {
    id("com.osfans.trime.app-convention")
}

android {
    namespace = "com.aar0u.tacit.voice"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "com.aar0u.tacit.voice"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    // The ONNX models are copied out of the APK on first run; storing them
    // uncompressed keeps that copy fast and the install size predictable.
    androidResources {
        noCompress += listOf("onnx", "bin", "txt", "model")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation("com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.6")
    testImplementation(libs.junit)
}
