version = 3

cloudstream {
    description = "Filma dhe seriale me titra shqip nga CineShqip"
    authors = listOf("jurim007")
    status = 1
    tvTypes = listOf("Movie", "TvSeries")
    requiresResources = true
    language = "sq"
    iconUrl = "https://www.cineshqip.com/favicon.png"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = false
    }
}
