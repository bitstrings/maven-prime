plugins {
    id("java")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.javaToolchain.get())
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }

    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(emptyList<String>())
    }
}

dependencies {
    compileOnly(libs.mavenCore)

    testImplementation(libs.junit)
    testImplementation(libs.mavenCore)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"

    // Maven 3.9 still runs on Java 8, so the spy targets 8. -Xlint:-options silences only the obsolete
    // target warning, which -Werror would otherwise turn into a failure.
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-options", "-Werror"))

    options.release = 8
}

tasks.compileTestJava {
    options.release = null
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.jar {
    archiveBaseName = "maven-prime-event-spy"
}
