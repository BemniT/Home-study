plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.home_study"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.home_study"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions{
        jvmTarget = "17"
    }
}



dependencies {

    // AndroidX / UI
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    
    // Utilities & libs
    implementation("com.github.barteksc:androidpdfviewer:3.1.0-beta.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation("com.github.bumptech.glide:glide:5.0.5")
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.5")

    implementation("com.intuit.sdp:sdp-android:1.0.6")
    implementation("com.intuit.ssp:ssp-android:1.0.6")
    implementation("com.facebook.shimmer:shimmer:0.5.0")


    implementation("io.github.pilgr:paperdb:2.7.2")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("com.squareup.picasso:picasso:2.71828")
    implementation("com.vanniktech:android-image-cropper:4.6.0")
    implementation("com.github.yalantis:ucrop:2.2.6")

   // Firebase BOM – keep versions consistent via BOM
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))

    // Firebase modules (use boM-managed versions)
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-storage")
//    implementation("com.google.firebase:firebase-analytics")

    // Firestore (use ktx)
    implementation("com.google.firebase:firebase-firestore")
    // Kotlin runtime required by Firestore internals (even if app is Java)
//    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")

    // AndroidX DataStore (preferences) required by Firestore heartbeat/storage
    implementation("androidx.datastore:datastore-preferences-core:1.1.1")

    // Optional: DataStore core if you use it elsewhere (kept compatible)
    implementation("androidx.datastore:datastore-core:1.1.1")

    // Other test / tooling
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

}