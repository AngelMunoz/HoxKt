plugins {
    kotlin("jvm") version "1.9.21"
}

group = "com.github.angelmunoz"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0-RC2")
    implementation("org.owasp.encoder:encoder:1.2.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(17)
}


tasks.test {
    useJUnitPlatform()
}
