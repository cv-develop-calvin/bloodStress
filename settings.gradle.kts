/**
 * 仓库配置：国内镜像在前，官方源在后兜底。
 * 若镜像不可用或你希望使用官方源，把镜像那几行注释掉即可。
 */

pluginManagement {
    repositories {
        // ---- 阿里云镜像（Gradle 插件 / Android / 公共库）----
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")

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
        // ---- 阿里云镜像 ----
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")

        // ---- 官方源兜底 ----
        google()
        mavenCentral()

        // MPAndroidChart 发布在 JitPack，无国内镜像，需走官方
        maven("https://jitpack.io")
    }
}

rootProject.name = "SongBaoBao"
include(":app")
