package com.albanianproviders.filmaon

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup

@JsonIgnoreProperties(ignoreUnknown = true)
data class FilmaOnPlayerResponse(
    val embed_url: String? = null,
    val type: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FilmaOnLoadData(
    val postId: String,
    val serverNumbers: List<String>,
    val subtitles: List<String>,
)

class FilmaOnProvider : MainAPI() {
    override var mainUrl = "https://filmaon.live"
    override var name = "FilmaOn"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "sq"
    override val hasMainPage = true
    override val hasQuickSearch = false

    override val mainPage = listOf(
        MainPageData("Filmat e fundit", "latest"),
        MainPageData("Aksion", "genre/action"),
        MainPageData("Aventurë", "genre/adventure"),
        MainPageData("Animacion", "genre/animation"),
        MainPageData("Komedi", "genre/comedy"),
        MainPageData("Krim", "genre/crime"),
        MainPageData("Dramë", "genre/drama"),
        MainPageData("Fantazi", "genre/fantasy"),
        MainPageData("Horor", "genre/horror"),
        MainPageData("Romancë", "genre/romance"),
        MainPageData("Fantashkencë", "genre/science-fiction"),
        MainPageData("Thriller", "genre/thriller"),
        MainPageData("Dokumentar", "genre/documentary"),
        MainPageData("Familje", "genre/family"),
        MainPageData("Luftë", "genre/war"),
        MainPageData("Historik", "genre/history"),
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val pageUrl = if (request.data == "latest") {
            if (page == 1) "$mainUrl/filma/" else "$mainUrl/filma/page/$page/"
        } else {
            val base = "$mainUrl/${request.data}/"
            if (page == 1) base else "$mainUrl/${request.data}/page/$page/"
        }

        val document = app.get(pageUrl).document
        val items = parseCards(document).mapNotNull { it.toSearchResponse() }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        if (query.isBlank()) return emptyList()

        return try {
            val document = app.get(
                "$mainUrl/",
                params = mapOf("s" to query),
            ).document
            parseCards(document).mapNotNull { it.toSearchResponse() }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = try {
            app.get(url).document
        } catch (_: Throwable) {
            return null
        }

        val h1 = document.selectFirst("h1")?.text()?.trim()
            ?: document.selectFirst("meta[property=og:title]")
                ?.attr("content")
                ?.substringBefore(" me Titra Shqip")
            ?: return null
        val title = h1
            .replace(Regex("""\s+HD\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+\(\d{4}\)\s*$"""), "")
            .trim()
            .ifBlank { "Film pa titull" }

        val description = document.selectFirst("meta[property=og:description]")
            ?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?: document.selectFirst("meta[name=description]")
                ?.attr("content")

        val poster = document.select("meta[property=og:image]")
            .map { it.attr("content") }
            .firstOrNull { it.startsWith("http") && it.contains("image.tmdb.org") }
            ?: document.select("meta[property=og:image]")
                .map { it.attr("content") }
                .firstOrNull { it.startsWith("http") }

        val year = Regex("""(?:\(|\b)(20\d{2}|19\d{2})(?:\)|\b)""")
            .find(h1)
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: document.select(".film24-movie-year")
                .text()
                .toIntOrNull()

        val duration = Regex("""(\d+)\s*(?:min|minuta?)""",
            RegexOption.IGNORE_CASE)
            .find(document.select(".film24-movie-meta").text())
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()

        val rating = Regex("""IMDb</b>\s*(\d+(?:[.,]\d+)?)""",
            RegexOption.IGNORE_CASE)
            .find(document.html())
            ?.groupValues
            ?.get(1)
            ?.replace(',', '.')
            ?.toDoubleOrNull()

        val postId = document.select("li.dooplay_player_option[data-post]")
            .firstNotNullOfOrNull { it.attr("data-post").takeIf { id -> id.isNotBlank() } }
            ?: return null

        val serverNumbers = document.select("li.dooplay_player_option[data-nume]")
            .map { it.attr("data-nume") }
            .filter { it.isNotBlank() && it != "trailer" }
            .distinct()

        val subtitles = extractSubtitles(document)
        val loadData = FilmaOnLoadData(postId, serverNumbers, subtitles)
        val data = jacksonObjectMapper().writeValueAsString(loadData)

        val tags = document.select(".film24-card-meta span")
            .map { it.text().trim() }
            .filter { it.isNotBlank() && it.firstOrNull()?.isDigit() != true }
            .distinct()
        val genres = document.select("a[href*=/genre/]")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()

        val actorNames = document.select(".cast_list li, .persons li")
            .mapNotNull { element ->
                element.selectFirst(".name, h2, a")?.text()?.trim()
            }
            .distinct()

        val actors = actorNames.map {
            ActorData(Actor(it, null), ActorRole.Supporting, null, null)
        }

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            data,
        ) {
            this.posterUrl = poster
            this.backgroundPosterUrl = poster
            this.plot = description
            this.year = year
            this.duration = duration
            this.tags = if (genres.isNotEmpty()) genres else tags
            this.actors = actors
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val loadData = try {
            jacksonObjectMapper().readValue<FilmaOnLoadData>(data)
        } catch (_: Throwable) {
            return false
        }

        loadData.subtitles.forEach { subtitle ->
            val name = subtitle.substringAfterLast('/').substringBefore('?')
            val lang = if (name.contains("sq", ignoreCase = true) ||
                name.contains("alban", ignoreCase = true)
            ) "Shqip" else "Titra"
            subtitleCallback(newSubtitleFile(lang, subtitle))
        }

        var found = false
        for (serverNumber in loadData.serverNumbers) {
            try {
                val response = app.post(
                    "$mainUrl/wp-admin/admin-ajax.php",
                    data = mapOf(
                        "action" to "doo_player_ajax",
                        "post" to loadData.postId,
                        "nume" to serverNumber,
                        "type" to "movie",
                    ),
                ).parsed<FilmaOnPlayerResponse>()
                val source = response.embed_url?.trim()?.takeIf { it.isNotBlank() }
                    ?: continue
                found = loadExtractor(source, mainUrl, subtitleCallback, callback) || found
            } catch (_: Throwable) {
                // A failed server should not prevent the remaining servers from loading.
            }
        }
        return found
    }

    private fun parseCards(document: org.jsoup.nodes.Document): List<FilmaOnCard> {
        return document.select("article.film24-card").mapNotNull { card ->
            val link = card.selectFirst("a[href*=/filma/]")?.attr("href")?.trim()
                ?: return@mapNotNull null
            val url = if (link.startsWith("http")) link else "$mainUrl$link"
            val title = card.selectFirst("h3 a")?.text()?.trim()
                ?: card.selectFirst("img[alt]")?.attr("alt")?.trim()
                ?: return@mapNotNull null
            val poster = card.selectFirst("img[src]")?.attr("src")
            val year = card.selectFirst(".film24-year")?.text()?.trim()?.toIntOrNull()
                ?: Regex("""\b(19|20)\d{2}\b""").find(title)?.value?.toIntOrNull()
            val quality = card.selectFirst(".film24-quality")?.text()?.trim()

            FilmaOnCard(title, url, poster, year, quality)
        }.distinctBy { it.url }
    }

    private fun FilmaOnCard.toSearchResponse(): SearchResponse {
        return newMovieSearchResponse(title, url, TvType.Movie) {
            posterUrl = poster
            year = this@toSearchResponse.year
            quality = when (this@toSearchResponse.quality?.uppercase()) {
                "4K", "UHD" -> SearchQuality.UHD
                "1080P", "FULL HD" -> SearchQuality.HD
                "720P", "HD" -> SearchQuality.HD
                "480P", "SD" -> SearchQuality.SD
                else -> null
            }
        }
    }

    private fun extractSubtitles(document: org.jsoup.nodes.Document): List<String> {
        val trackUrls = document.select("track[src]")
            .mapNotNull { it.attr("src").takeIf { src -> src.isNotBlank() } }
        val html = document.html()
        val regexUrls = Regex("""https?[^""\\\s]+?\.(?:srt|vtt)(?:\?[^""\\\s]*)?""",
            RegexOption.IGNORE_CASE)
            .findAll(html)
            .map { it.value.replace("\\/", "/").replace("\\u0026", "&") }
            .toList()
        val urls = (trackUrls + regexUrls).distinct()
        return urls
    }

    private data class FilmaOnCard(
        val title: String,
        val url: String,
        val poster: String?,
        val year: Int?,
        val quality: String?,
    )
}
