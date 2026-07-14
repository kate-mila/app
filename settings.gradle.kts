pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // برای کتابخانه‌ی هسته‌ی V2Ray/Xray (libv2ray)
    }
}

rootProject.name = "V2RayClient"
include(":app")
