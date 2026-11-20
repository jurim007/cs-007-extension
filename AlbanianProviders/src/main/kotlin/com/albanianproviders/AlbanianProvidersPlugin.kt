package com.albanianproviders

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AlbanianProvidersPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CineShqipProvider())
        registerMainAPI(FilmaOnProvider())
    }
}