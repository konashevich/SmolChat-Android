import java.net.URI
import org.gradle.api.JavaVersion
import org.gradle.api.GradleException

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = URI("https://jitpack.io") }
        maven { url = URI("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
}

// Guard against running the build with an unsupported JDK (e.g. JDK 25 currently breaks Kotlin DSL parsing)
val supportedJdks = setOf(JavaVersion.VERSION_17, JavaVersion.VERSION_21)
val currentJdk = JavaVersion.current()
if (currentJdk !in supportedJdks) {
    throw GradleException("Unsupported JDK ${currentJdk.majorVersion}. Please use JDK 17 (recommended) or 21. Set JAVA_HOME accordingly.")
}

rootProject.name = "SmolChat Android"
include(":app")
include(":smollm")
include(":hf-model-hub-api")
