package com.albanianproviders.cineshqip

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.Jsoup

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineCatalogItem(
    val id: Any? = null,
    val tmdbId: Int? = null,
    val title: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val genres: List<String>? = null,
    val overview: String? = null,
    val runtime: Int? = null,
    val slug: String? = null,
    val type: String? = null,
    val mediaType: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineCatalogResponse(
    val items: List<CineCatalogItem>? = null,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineGenre(
    val id: Int,
    val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineGenreResponse(
    val genre: CineGenre? = null,
    val items: List<CineCatalogItem>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineHomepage(
    val genres: List<CineGenre>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineMovieDetails(
    val id: Int? = null,
    val title: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val genres: List<String>? = null,
    val overview: String? = null,
    val runtime: Int? = null,
    val director: String? = null,
    val cast: List<String>? = null,
    val quality: String? = null,
    val trailerUrl: String? = null,
    val slug: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineTvDetails(
    val id: Int? = null,
    val title: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: Int? = null,
    val genres: List<String>? = null,
    val overview: String? = null,
    val cast: List<String>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineTvEpisode(
    val id: Any? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val title: String? = null,
    val overview: String? = null,
    val runtime: Int? = null,
    val stillUrl: String? = null,
    val videoSources: List<CineVideoSource>? = null,
    val subtitleTracks: List<JsonNode>? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineVideoSource(
    val url: String? = null,
    val sourceType: String? = null,
    val resolvedUrl: String? = null,
    val active: Boolean? = null,
)

class CineShqipProvider : MainAPI() {
    override var mainUrl = "https://www.cineshqip.com"
    override var name = "CineShqip"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "sq"
    override val hasMainPage = true
    override val hasQuickSearch = false

    private val genreNames = mapOf(
        28 to "Aksion",
        12 to "Aventurë",
        16 to "Animacion",
        35 to "Komedi",
        80 to "Krim",
        99 to "Dokumentar",
        18 to "Dramë",
        10751 to "Familje",
        14 to "Fantazi",
        36 to "Historik",
        27 to "Horor",
        10402 to "Muzikë",
        9648 to "Mister",
        10749 to "Romancë",
        878 to "Fantashkencë",
        10770 to "Filmat TV",
        53 to "Thriller",
        10752 to "Luftë",
        37 to "Western",
    )

    override val mainPage = listOf(
        MainPageData("Të rejat", "latest"),
        MainPageData("Serialet e fundit", "tv-shows"),
        MainPageData("Më të shikuarat", "popular"),
        MainPageData("Më të vlerësuarat", "top-rated"),
        MainPageData("Aksion", "genre/28"),
        MainPageData("Aventurë", "genre/12"),
        MainPageData("Animacion", "genre/16"),
        MainPageData("Komedi", "genre/35"),
        MainPageData("Krim", "genre/80"),
        MainPageData("Dramë", "genre/18"),
        MainPageData("Fantazi", "genre/14"),
        MainPageData("Horor", "genre/27"),
        MainPageData("Romancë", "genre/10749"),
        MainPageData("Fantashkencë", "genre/878"),
        MainPageData("Thriller", "genre/53"),
        MainPageData("Dokumentar", "genre/99"),
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = when {
            request.data == "latest" ->
                "$mainUrl/api/catalog/latest?page=$page&limit=30"
            request.data == "tv-shows" ->
                "$mainUrl/api/catalog/tv-shows?page=$page&limit=30"
            request.data.startsWith("genre/") ->
                "$mainUrl/api/catalog/${request.data}?page=$page&limit=30"
            else ->
                "$mainUrl/api/catalog/${request.data}?page=$page"
        }

        val items = if (request.data.startsWith("genre/")) {
            app.get(url).parsed<CineGenreResponse>().items.orEmpty()
        } else {
            app.get(url).parsed<CineCatalogResponse>().items.orEmpty()
        }
        val home = newHomePageResponse(
            request.name,
            items.mapNotNull { it.toSearchResponse(request.data == "tv-shows") },
        )
        return home
    }

    override suspend fun search(query: String): List<SearchResponse> {
        if (query.isBlank()) return emptyList()

        return try {
            app.get(
                "$mainUrl/api/catalog/search",
                params = mapOf("q" to query, "page" to "1", "limit" to "30"),
            ).parsed<CineCatalogResponse>().items
                .orEmpty()
                .mapNotNull { it.toSearchResponse() }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val tvId = Regex("""/tv/(\d+)""").find(url)?.groupValues?.get(1)
        if (tvId != null) return loadTvSeries(url, tvId)

        val id = Regex("""/movie/(\d+)""").find(url)?.groupValues?.get(1) ?: return null

        val html = try {
            app.get(url).text
        } catch (_: Throwable) {
            ""
        }

        val details = try {
            app.get("$mainUrl/api/catalog/movie/$id").parsed<CineMovieDetails>()
        } catch (_: Throwable) {
            return null
        }

        val title = details.title?.ifBlank { "Film pa titull" } ?: "Film pa titull"
        val poster = details.posterUrl?.takeIf { it.startsWith("http") }
        val backdrop = details.backdropUrl?.takeIf { it.startsWith("http") }

        val sources = extractVideoSources(html)

        val subtitleUrls = extractSubtitleUrls(html)

        val data = CineLoadData(
            id = id,
            sources = sources,
            subtitles = subtitleUrls,
        )

        val actors = details.cast.orEmpty()
            .map { ActorData(Actor(it, null), null, null, null) }

        return newMovieLoadResponse(
            title,
            url,
            TvType.Movie,
            data,
        ) {
            this.posterUrl = poster
            this.backgroundPosterUrl = backdrop
            this.plot = details.overview
            this.year = details.year
            this.tags = details.genres
            this.duration = details.runtime
            this.actors = actors
        }
    }

    private suspend fun loadTvSeries(url: String, id: String): LoadResponse? {
        val html = try {
            app.get(url).text
        } catch (_: Exception) {
            return null
        }
        val details = try {
            app.get("$mainUrl/api/catalog/tv/$id").parsed<CineTvDetails>()
        } catch (_: Exception) {
            return null
        }

        val episodes = extractTvEpisodes(html).mapNotNull { episode ->
            val episodeTitle = episode.title?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val sources = episode.videoSources.orEmpty()
                .filter { it.active != false && it.sourceType in setOf("embed", "direct") }
                .mapNotNull { source ->
                    val url = if (source.sourceType == "direct") {
                        source.resolvedUrl ?: source.url
                    } else {
                        source.url ?: source.resolvedUrl
                    }
                    url?.takeIf { it.startsWith("http") }
                }
                .distinct()
            if (sources.isEmpty()) return@mapNotNull null

            val subtitles = episode.subtitleTracks.orEmpty().mapNotNull { track ->
                listOf("url", "src", "file", "subtitleUrl", "fileUrl")
                    .firstNotNullOfOrNull { key ->
                        track.get(key)?.takeIf { it.isTextual }?.asText()
                    }
                    ?.takeIf { it.startsWith("http") }
            }.distinct()
            val episodeData = CineLoadData(
                id = episode.id?.toString()
                    ?: "$id-${episode.seasonNumber}-${episode.episodeNumber}",
                sources = sources,
                subtitles = subtitles,
            )

            newEpisode(jacksonObjectMapper().writeValueAsString(episodeData)) {
                this.name = episodeTitle
                this.season = episode.seasonNumber
                this.episode = episode.episodeNumber
                this.posterUrl = episode.stillUrl
                this.description = episode.overview
                this.runTime = episode.runtime
            }
        }
        if (episodes.isEmpty()) return null

        val title = details.title?.takeIf { it.isNotBlank() } ?: "Serial pa titull"
        val poster = details.posterUrl?.takeIf { it.startsWith("http") }
        val actors = details.cast.orEmpty()
            .map { ActorData(Actor(it, null), null, null, null) }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.backgroundPosterUrl = details.backdropUrl?.takeIf { it.startsWith("http") }
            this.plot = details.overview
            this.year = details.year
            this.tags = details.genres
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
            jacksonObjectMapper().readValue<CineLoadData>(data)
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
        loadData.sources.forEach { source ->
            try {
                found = loadExtractor(source, mainUrl, subtitleCallback, callback) || found
            } catch (_: Throwable) {
                // Continue to the next available source.
            }
        }

        return found
    }

    private fun CineCatalogItem.toSearchResponse(isTvShow: Boolean = false): SearchResponse? {
        val contentId = tmdbId ?: id?.toString()?.toIntOrNull()
        val contentTitle = title?.takeIf { it.isNotBlank() } ?: return null
        if (contentId == null) return null

        val isTv = isTvShow || type == "tv-show" || mediaType == "tv"
        val contentUrl = if (!slug.isNullOrBlank()) {
            "$mainUrl/${if (isTv) "tv" else "movie"}/$contentId-$slug"
        } else {
            "$mainUrl/${if (isTv) "tv" else "movie"}/$contentId"
        }

        return if (isTv) {
            newTvSeriesSearchResponse(contentTitle, contentUrl, TvType.TvSeries) {
                this.posterUrl = posterUrl
                this.year = year
            }
        } else {
            newMovieSearchResponse(contentTitle, contentUrl, TvType.Movie) {
                this.posterUrl = posterUrl
                this.year = year
            }
        }
    }

    private fun extractTvEpisodes(html: String): List<CineTvEpisode> {
        val mapper = jacksonObjectMapper()
        // The catalog API omits episodes; Next.js Flight includes their playback sources.
        val flightData = Regex(
            """self\.__next_f\.push\(\[1,("(?:\\.|[^"\\])*")\]\)"""
        ).findAll(html)

        for (match in flightData) {
            val payload = try {
                mapper.readValue<String>(match.groupValues[1])
            } catch (_: Exception) {
                continue
            }
            val episodesProperty = payload.indexOf("\"episodes\":[")
            if (episodesProperty < 0) continue

            val arrayStart = payload.indexOf('[', episodesProperty)
            val arrayEnd = findJsonArrayEnd(payload, arrayStart) ?: continue
            val episodes = try {
                mapper.readValue<List<CineTvEpisode>>(payload.substring(arrayStart, arrayEnd + 1))
            } catch (_: Exception) {
                continue
            }
            if (episodes.isNotEmpty()) return episodes
        }

        return emptyList()
    }

    private fun findJsonArrayEnd(json: String, start: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false

        // Ignore brackets inside episode descriptions while finding the array boundary.
        for (index in start until json.length) {
            val char = json[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    char == '\\' -> escaped = true
                    char == '"' -> inString = false
                }
            } else {
                when (char) {
                    '"' -> inString = true
                    '[' -> depth++
                    ']' -> {
                        depth--
                        if (depth == 0) return index
                    }
                }
            }
        }

        return null
    }

    private fun extractSubtitleUrls(html: String): List<String> {
        val document = Jsoup.parse(html)
        val trackUrls = document.select("track[src]")
            .mapNotNull { it.attr("src").takeIf { src -> src.isNotBlank() } }

        val regexUrls = Regex("""https?[^""\\\s]+?\.(?:srt|vtt)(?:\?[^""\\\s]*)?""",
            RegexOption.IGNORE_CASE)
            .findAll(html)
            .map { it.value.replace("\\/", "/").replace("\\u0026", "&") }
            .toList()

        return (trackUrls + regexUrls).distinct()
    }

    private fun extractVideoSources(html: String): List<String> {
        val payloadSources = Regex(
            """\\"label\\":\\"(.*?)\\",\\"url\\":\\"(https?://.*?)\\",\\"sourceType\\":\\"(.*?)\\".*?\\"active\\":(true|false)"""
        ).findAll(html)
            .mapNotNull { match ->
                if (match.groupValues[4] != "true") return@mapNotNull null
                val sourceType = match.groupValues[3]
                val sourceUrl = match.groupValues[2]
                    .replace("\\/", "/")
                    .replace("\\u0026", "&")
                sourceUrl.takeIf {
                    sourceType in setOf("embed", "direct") && it.startsWith("http")
                }
            }
            .toList()

        val iframeSources = if (html.isNotBlank()) {
            Jsoup.parse(html).select("iframe[src]")
                .mapNotNull { it.attr("src").takeIf { src -> src.startsWith("http") } }
                .filterNot { it.contains("youtube.com") }
        } else {
            emptyList()
        }

        return (payloadSources + iframeSources).distinct()
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CineLoadData(
    val id: String,
    val sources: List<String>,
    val subtitles: List<String>,
)