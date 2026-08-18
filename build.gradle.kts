import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

plugins {
    id("java")
    id("org.jetbrains.intellij.platform")
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
    intellijPlatform {
        create(IntelliJPlatformType.IntellijIdeaCommunity, libs.versions.intellijPlatform)

        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("com.intellij.modules.json")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("org.jetbrains.idea.maven.model")
        bundledPlugin("org.jetbrains.idea.maven.server.api")

        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
        testFramework(TestFrameworkType.Plugin.Maven)
    }

    // The spy jar has to exist on disk: it is injected into the Maven JVM with -Dmaven.ext.class.path.
    implementation(project(":event-spy"))

    implementation(libs.commonsLang3)

    implementation(libs.gson) {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }

    compileOnly(libs.errorProneAnnotations)

    testCompileOnly(libs.errorProneAnnotations)

    testImplementation(libs.junit)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = libs.versions.pluginSinceBuild
            untilBuild = provider { null }
        }
    }

    pluginVerification {
        // EXPERIMENTAL_API_USAGES and DEPRECATED_API_USAGES stay out of failureLevel: com.intellij.build
        // is entirely @Experimental, and getThreeStateCheckBox(), which replaces the deprecated
        // CheckboxTreeBase.getCheckbox(), does not exist in 2024.3.
        failureLevel =
            listOf(
                FailureLevel.COMPATIBILITY_PROBLEMS,
                FailureLevel.INTERNAL_API_USAGES,
                FailureLevel.INVALID_PLUGIN,
                FailureLevel.MISSING_DEPENDENCIES,
                FailureLevel.NON_EXTENDABLE_API_USAGES,
                FailureLevel.OVERRIDE_ONLY_API_USAGES,
                FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
                FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
            )

        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, libs.versions.intellijPlatform)
            create(IntelliJPlatformType.IntellijIdea, libs.versions.intellijPlatformNewest)
        }
    }
}

tasks.assemble {
    dependsOn(tasks.buildPlugin)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.buildSearchableOptions {
    val logFile = layout.buildDirectory.file("logs/buildSearchableOptions.log")

    outputs.file(logFile)

    doFirst {
        val log = logFile.get().asFile

        log.parentFile.mkdirs()

        standardOutput = log.outputStream()
        errorOutput = standardOutput
    }
}

// Cannot share a JVM. The importing tests close the reused light project and the two action gates assert on
// a project Maven has not taken over, which any reimport ends. The last two ask for a Java light project,
// and that switch closes the shared one, disposing a MavenProjectsManager the suite left uninitialized.
val isolatedTests =
    listOf(
        "*ImportingTest",
        "*BuildProfilesActionPlatformTest",
        "*MavenPrimeActionGroupPlatformTest",
        "*RunTestWithMavenPrimeGutterPlatformTest",
        "*TestSelectionsPlatformTest")

fun Test.platformTestDefaults() {
    useJUnit()

    maxHeapSize = "2g"

    // The platform sets java.system.class.loader, so CDS has nothing to share and warns on every fork.
    jvmArgs("-Xshare:off")

    systemProperty("java.awt.headless", "true")

    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
    }
}

tasks.test {
    platformTestDefaults()

    filter { isolatedTests.forEach { excludeTestsMatching(it) } }
}

intellijPlatformTesting {
    testIde {
        register("isolatedTest") {
            testFramework(TestFrameworkType.Platform)
            testFramework(TestFrameworkType.Plugin.Java)
            testFramework(TestFrameworkType.Plugin.Maven)

            task {
                platformTestDefaults()

                forkEvery = 1

                filter { isolatedTests.forEach { includeTestsMatching(it) } }
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.named("isolatedTest"))
}
