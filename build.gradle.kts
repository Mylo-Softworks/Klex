plugins {
    kotlin("multiplatform") version "2.0.20"
    id("maven-publish")
}

group = "com.mylosoftworks"
version = "1.0"

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["kotlin"])
            groupId = "com.mylosoftworks"
            artifactId = "Klex"
            version = "1.0"
        }
    }
}

dependencies {

}

kotlin {
    jvm()
    js {
        browser()
        nodejs()
    }
}