plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.home_study"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.home_study"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    // Refresher
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("androidx.activity:activity:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("com.github.barteksc:androidpdfviewer:3.1.0-beta.1")
//    implementation ("com.github.User:Repo:Tag")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    implementation ("com.github.bumptech.glide:glide:5.0.5")
    annotationProcessor ("com.github.bumptech.glide:compiler:5.0.5")

    //    For responsiveness
    implementation("com.intuit.sdp:sdp-android:1.0.6")
    implementation("com.intuit.ssp:ssp-android:1.0.6")

    implementation("com.github.jitpack:gradle-simple:1.0")
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))
//    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
//    implementation("androidx.room:room-compiler-processing-testing:2.6.1")
//    implementation ("com.firebaseui:firebase-ui-database:8.0.2")

    // Firebase Dependency
    implementation ("com.google.firebase:firebase-core:21.1.1")
    implementation("com.google.firebase:firebase-database:22.0.1")
    implementation("com.google.firebase:firebase-firestore:26.0.2")
    implementation("com.google.firebase:firebase-storage:22.0.1")
//    implementation ("com.google.firebase:firebase-database:20.3.0")
//    implementation ("com.google.firebase:firebase-storage:20.3.0")
    implementation ("com.google.firebase:firebase-analytics")
    //implementation 'com.github.rey5137:material:1.3.1'-->
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("io.github.pilgr:paperdb:2.7.2")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation("com.vanniktech:android-image-cropper:4.6.0")
    implementation ("com.github.yalantis:ucrop:2.2.6")


}