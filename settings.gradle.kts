pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ContaApp"
include(":app")

gradle.projectsLoaded {
    rootProject.layout.projectDirectory.dir(".gradle-build").asFile.mkdirs()
}
