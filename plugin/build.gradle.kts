plugins {
    kotlin("jvm") version "1.6.21"
    id("org.jetbrains.intellij") version "1.10.1"
}

repositories {
    mavenCentral()
}


// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    //version.set("2022.2")
    type.set("IC") // Target IDE Platform
    localPath.set("/home/matthieu/code/apero-code-1/idea-community/")
    plugins.set(listOf(/* Plugin Dependencies */))
}