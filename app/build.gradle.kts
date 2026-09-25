plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "org.bp.songbaobao"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.bp.songbaobao"
        minSdk = 26          // ML Kit 与通知渠道要求，覆盖绝大多数在用机型
        targetSdk = 34
        versionCode = 10500
        versionName = "1.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 统一签名：本机与 CI 必须共用同一份 keystore。
    // 否则每次构建都会换新签名，覆盖安装时报 INSTALL_FAILED_UPDATE_INCOMPATIBLE，
    // 用户升级前只能先卸载旧版（数据也会跟着没了）。
    // 后续若要换成私有密钥，把三个值改用 secrets / 环境变量覆盖即可，无需再改脚本
    // （变量名：SB_STORE_PASSWORD、SB_KEY_ALIAS、SB_KEY_PASSWORD）。
    signingConfigs {
        create("unified") {
            storeFile = file("signing/debug.keystore")
            storePassword = System.getenv("SB_STORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("SB_KEY_ALIAS") ?: "androiddebugkey"
            keyPassword = System.getenv("SB_KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("unified")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfig = signingConfigs.getByName("unified")
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
        // AGP 8.x 默认不生成 BuildConfig，VersionRepository 依赖它
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
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
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // 图片加载
    implementation(libs.coil.compose)

    // 图表（Compose 中通过 AndroidView 包装，API 稳定）
    implementation(libs.mpandroidchart)

    // OCR：中文识别
    implementation(libs.mlkit.text.chinese)

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // 单元测试：验证版本比较等纯逻辑
    testImplementation("junit:junit:4.13.2")
}
