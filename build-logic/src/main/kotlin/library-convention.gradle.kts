plugins {
    kotlin("jvm")
    id("publication-convention")
}

kotlin {
    explicitApi()
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}
