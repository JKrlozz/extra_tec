plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "itsm.extra_tec"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "itsm.extra_tec"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // direccion de la api en Azure; se cambia en gradle.properties (API_URL)
        val apiUrl = providers.gradleProperty("API_URL").getOrElse("https://extratec-api-movil-h9fyaka4d3eagta6.westus3-01.azurewebsites.net")
        buildConfigField("String", "API_URL", "\"$apiUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            res.directories.addAll(
                listOf(
                    "src/main/vistas/Comun/res",
                    "src/main/vistas/Alumno/res",
                    "src/main/vistas/Docente/res",
                    "src/main/vistas/Administrador/res",
                )
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}