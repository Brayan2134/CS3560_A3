plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.google.code.gson:gson:2.13.2")
}

tasks.test {
    useJUnitPlatform()
}

application {
    // demo entrypoint (you can change/remove)
    mainClass.set("org.example.Main")
}

tasks.named<JavaExec>("run") {
    // Forward your system env var explicitly into the Gradle run environment
    val key = System.getenv("OPENAI_API_KEY") ?: ""
    environment("OPENAI_API_KEY", key)
}