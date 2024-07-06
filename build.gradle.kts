plugins {
    id("org.openjfx.javafxplugin") version "0.0.13"
    kotlin("jvm") version "1.9.21"

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}

javafx {
    version = "21"
    modules("javafx.controls", "javafx.fxml")
}