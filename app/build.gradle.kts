plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "org.bp.songbaobao"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.bp.songbaobao"
        minSdk = 26          // ML Kit 与通知渠道要求，覆盖绝大多数在用机型
        targetSdk = 34
        versionCode = 10200
        versionName = "1.2.0"

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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    // Kotlin 1.9.x 使用 Compose Compiler 扩展版本（Kotlin 2.0 才改用插件）
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // JDK 17 的模块系统会封闭 jdk.compiler 等内部包，kapt 需要显式开放，
    // 否则注解处理阶段会报 "Could not load module <Error module>"。
    kapt {
        correctErrorTypes = true
        javacOptions {
            option("-Xmaxerrs", 500)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    // Room（用 kapt，与 Hilt 同处理器以保证生成类互相可见）
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // Hilt（用 kapt：Hilt 的 KSP 后端在 Kotlin 1.9 下处理 Room 生成的类时会因处理顺序报 error.NonExistentClass）
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    kapt(libs.hilt.compiler)

    // 图片加载
    implementation(libs.coil.compose)

    // 图表（Compose 中通过 AndroidView 包装，API 稳定）
    implementation(libs.mpandroidchart)

    // OCR：中文识别
    implementation(libs.mlkit.text.chinese)

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
