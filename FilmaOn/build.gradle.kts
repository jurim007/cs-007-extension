version = 3

cloudstream {
    description = "Filma dhe seriale me titra shqip nga FilmaOn"
    authors = listOf("jurim007")
    status = 1
    tvTypes = listOf("Movie", "TvSeries")
    requiresResources = true
    language = "sq"
    iconUrl = "https://filmaon.live/wp-content/uploads/2026/06/apple-touch-icon.png"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = false
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
}
