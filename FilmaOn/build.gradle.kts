version = 1

cloudstream {
    description = "Filma me titra shqip nga FilmaOn"
    authors = listOf("jurim007")
    status = 1
    tvTypes = listOf("Movie")
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
