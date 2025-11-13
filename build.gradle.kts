plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
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
    implementation("com.github.librepdf:openpdf:3.0.0")
    implementation("com.lowagie:itext:2.1.7")
    implementation("org.languagetool:language-en:5.9")
    implementation("org.fxmisc.richtext:richtextfx:0.11.0")
}

tasks.test {
    useJUnitPlatform()
}

application {
    // demo entrypoint (you can change/remove)
    mainClass.set("org.example.app.MainApp")
}

tasks.named<JavaExec>("run") {
    // Forward your system env var explicitly into the Gradle run environment
    val key = System.getenv("OPENAI_API_KEY") ?: ""
    environment("OPENAI_API_KEY", key)
}

javafx {
    modules("javafx.controls", "javafx.fxml")
}