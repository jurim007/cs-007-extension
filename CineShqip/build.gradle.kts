version = 1

cloudstream {
    description = "Filma me titra shqip nga CineShqip"
    authors = listOf("jurim007")
    status = 1
    tvTypes = listOf("Movie")
    requiresResources = true
    language = "sq"
    iconUrl = "https://cineshqip.com/logo.webp"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = false
    }
}
