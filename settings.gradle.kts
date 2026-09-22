/**
 * 仓库配置。
 *
 * 重要：pluginManagement / dependencyResolutionManagement 是 Gradle 的隔离块，
 * **不能引用脚本顶层变量**（会报 Unresolved reference），
 * 因此这里在每个块内直接内联读取环境变量 USE_MIRROR。
 *
 * 默认（本机 / 国内网络）走阿里云镜像；
 * CI 或境外网络设 USE_MIRROR=false 则只用官方源。
 */

pluginManagement {
    repositories {
        if (System.getenv("USE_MIRROR")?.toBoolean() ?: true) {
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
        if (System.getenv("USE_MIRROR")?.toBoolean() ?: true) {
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
