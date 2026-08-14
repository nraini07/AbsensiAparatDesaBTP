plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    // Wajib untuk membaca google-services.json (config Firebase project Anda)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.absensiaparatbtp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.absensiaparatbtp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // PDF Library (iText)
    implementation("com.itextpdf:itext-core:8.0.1")

    // Firebase: BoM mengatur semua versi library Firebase otomatis, jadi tidak
    // perlu tulis versi manual di tiap dependency di bawahnya.
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")       // login/register
    implementation("com.google.firebase:firebase-firestore-ktx")  // database absensi & profil pegawai

    // Coroutines untuk memakai Firebase dengan suspend function (await())
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // (opsional) kalau mau unit test / androidTest, bisa tambahkan lagi
}