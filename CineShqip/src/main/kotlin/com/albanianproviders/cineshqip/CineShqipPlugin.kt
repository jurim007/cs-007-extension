package com.albanianproviders.cineshqip

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class CineShqipPlugin : Plugin() {
    override fun load(context: Context) {
        registerMainAPI(CineShqipProvider())
    }
}
