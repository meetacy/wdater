plugins {
    id("library-convention")
}

version = libs.versions.wdater.get()

dependencies {
    implementation(libs.exposedCore)
    api(projects.wdater)
}
