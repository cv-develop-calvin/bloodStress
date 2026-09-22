/**
 * 仓库配置。
 *
 * 默认（本机 / 国内网络）走阿里云镜像加速。
 * CI 或境外网络可设环境变量 USE_MIRROR=false，只用官方源
 * （GitHub Actions 在境外，访问阿里云反而更慢）。
 */
val useMirror: Boolean = System.getenv("USE_MIRROR")?.toBoolean() ?: true

pluginManagement {
    repositories {
        if (useMirror) {
            // ---- 阿里云镜像（Gradle 插件 / Android / 公共库）----
            maven("https://maven.aliyun.com/repository/gradle-plugin")
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }

        // ---- 官方源兜底 ----
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (useMirror) {
            // ---- 阿里云镜像 ----
            maven("https://maven.aliyun.com/repository/google")
            maven("https://maven.aliyun.com/repository/public")
        }

        // ---- 官方源兜底 ----
        google()
        mavenCentral()

        // MPAndroidChart 发布在 JitPack，无国内镜像，需走官方
        maven("https://jitpack.io")
    }
}

rootProject.name = "SongBaoBao"
include(":app")
