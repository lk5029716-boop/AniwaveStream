dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // nextlib (FFmpeg extension for dual audio / extra subtitle codecs)
        maven("https://jitpack.io")
    }
}
