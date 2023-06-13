enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "wdater"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

includeBuild("build-logic")

include("auto-migrations")
