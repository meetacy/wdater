plugins {
    id("org.gradle.maven-publish")
}

group = "app.meetacy.wdater"

publishing {
    repositories {
        maven {
            name = "Wdater"
            url = uri("https://maven.pkg.github.com/meetacy/wdater")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
