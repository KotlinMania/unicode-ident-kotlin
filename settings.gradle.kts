pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

val localMavenValidation =
    providers.gradleProperty("project.repositories.mavenLocal")
        .map { it.trim().toBooleanStrict() }
        .getOrElse(false)

dependencyResolutionManagement {
    if (localMavenValidation) {
        val localMavenPath = providers.systemProperty("maven.repo.local").orNull
        require(localMavenPath != null && File(localMavenPath).isAbsolute) {
            "Local Maven validation requires -Dmaven.repo.local=/absolute/path/to/isolated-repository"
        }
        // The Kotlin/JS plugin always adds its own Node.js/Yarn distribution
        // repositories from plugin code; FAIL_ON_PROJECT_REPOS rejects those as
        // "added by unknown code" and breaks kotlinNodeJsSetup. PREFER_SETTINGS
        // keeps every dependency resolution routed through the settings repos
        // below while ignoring project-level additions instead of failing.
        repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    }
    repositories {
        if (localMavenValidation) {
            exclusiveContent {
                forRepository {
                    mavenLocal { name = "kotlinManiaLocalValidation" }
                }
                filter { includeGroup("io.github.kotlinmania") }
            }
        }
        google()
        mavenCentral()
        // Declared here (rather than by the Kotlin/JS plugin as a project-level
        // repository) so the build works with RepositoriesMode.FAIL_ON_PROJECT_REPOS.
        ivy("https://nodejs.org/dist") {
            name = "Node Distributions"
            patternLayout { artifact("v[revision]/[artifact](-v[revision]-[classifier]).[ext]") }
            metadataSources { artifact() }
            content { includeModule("org.nodejs", "node") }
        }
        ivy("https://github.com/yarnpkg/yarn/releases/download") {
            name = "Yarn Distributions"
            patternLayout { artifact("v[revision]/[artifact]-v[revision].[ext]") }
            metadataSources { artifact() }
            content { includeModule("com.yarnpkg", "yarn") }
        }
    }
}

rootProject.name = "unicode-ident-kotlin"
