pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "PolyaTV"
include(":app", ":core:model", ":feature:home", ":integration:torrserve", ":integration:lampa", ":integration:num")
