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
        targetSdk = 34
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

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("com.google.firebase:firebase-database:21.0.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("com.google.firebase:firebase-analytics")
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
//    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
//    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
//    implementation("androidx.room:room-compiler-processing-testing:2.6.1")
//    implementation ("com.firebaseui:firebase-ui-database:8.0.2")
//    implementation ("com.google.firebase:firebase-database:20.3.0")
//    implementation ("com.google.firebase:firebase-storage:20.3.0")
//    implementation ("com.google.firebase:firebase-analytics:21.5.1")
    //implementation 'com.github.rey5137:material:1.3.1'-->
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("io.github.pilgr:paperdb:2.7.2")
    implementation ("de.hdodenhof:circleimageview:3.1.0")
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation("com.vanniktech:android-image-cropper:4.5.0")
    implementation ("com.google.firebase:firebase-core:21.1.1")

}