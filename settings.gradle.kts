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
    }
}

rootProject.name = "PayFriend"
include(":app")

// The Ktor backend is a separate Gradle build (see backend/settings.gradle.kts)
// so that building it never requires the Android SDK.
