plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// ---------------------------------------------------------------------------
// 发布签名配置
//
// 密钥文件本身绝不入库（.gitignore 已排除 *.jks），取值优先级：
//   1) 环境变量 SB_KEYSTORE_FILE —— CI 用，由 GitHub Secret 解码后落地
//   2) 项目根 songbaobao.jks   —— 本机用（同样不入库）
//   3) ~/.android/songbaobao.jks —— 备选位置
// 密码同理：环境变量优先，其次 local.properties（也不入库）。
//
// 本机与 CI 必须指向同一份密钥，否则覆盖安装会报
// INSTALL_FAILED_UPDATE_INCOMPATIBLE（用户升级前只能先卸载）。
// ---------------------------------------------------------------------------
// 直接按行解析 local.properties（Kotlin DSL 顶层作用域下 java.util.Properties
// 会被脚本隐式接收者遮蔽，故不采用 Properties 类）
fun localProp(key: String): String? {
    val f = rootProject.file("local.properties")
    if (!f.exists()) return null
    return f.readLines()
        .firstOrNull { it.trim().startsWith("$key=") }
        ?.substringAfter("=")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
}

fun signingValue(envKey: String, propKey: String, fallback: String = ""): String =
    System.getenv(envKey)?.takeIf { it.isNotBlank() }
        ?: localProp(propKey)
        ?: fallback

val releaseKeystore: File? =
    System.getenv("SB_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }?.let { file(it) }
        ?: listOf(
            rootProject.file("songbaobao.jks"),
            rootProject.file("app/songbaobao.jks"),
            file("${System.getProperty("user.home")}/.android/songbaobao.jks")
        ).firstOrNull { it.exists() }

android {
    namespace = "org.bp.songbaobao"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.bp.songbaobao"
        minSdk = 26          // ML Kit 与通知渠道要求，覆盖绝大多数在用机型
        targetSdk = 34
        versionCode = 10903
        versionName = "1.9.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("releaseSign") {
            val ks = releaseKeystore
            if (ks != null) {
                storeFile = ks
                storePassword = signingValue("SB_STORE_PASSWORD", "sb.storePassword")
                keyAlias = signingValue("SB_KEY_ALIAS", "sb.keyAlias", "songbaobao")
                keyPassword = signingValue("SB_KEY_PASSWORD", "sb.keyPassword")
            } else {
                // 不静默退回 debug 签名：那样产出的包与上一版签名不一致，
                // 用户升级时会安装失败，问题更隐蔽。
                error(
                    """
                    |未找到发布签名密钥 songbaobao.jks。
                    |
                    |本机构建：把密钥放到项目根目录，或在 local.properties 中配置
                    |  sb.storePassword / sb.keyAlias / sb.keyPassword
                    |
                    |CI 构建：需在仓库 Settings → Secrets and variables → Actions 中配置
                    |  SB_KEYSTORE_BASE64 / SB_STORE_PASSWORD / SB_KEY_ALIAS / SB_KEY_PASSWORD
                    |
                    |签名不一致会导致用户升级时必须先卸载旧版本，因此这里直接失败而不是静默降级。
                    """.trimMargin()
                )
            }
        }
    }

    buildTypes {
        release {
            // 商业版开启混淆与资源缩减：减小体积并增加逆向难度
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("releaseSign")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // debug 也用同一份签名：保证本地调试包与正式包可互相覆盖安装
        debug {
            signingConfig = signingConfigs.getByName("releaseSign")
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
