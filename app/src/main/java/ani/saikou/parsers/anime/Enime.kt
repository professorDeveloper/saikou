package ani.saikou.parsers.anime

import ani.saikou.FileUrl
import ani.saikou.client
import ani.saikou.getSize
import ani.saikou.parsers.*
import ani.saikou.printIt
import kotlinx.serialization.Serializable

class Enime : AnimeParser() {

    override val name = "Consumet Enime"
    override val saveName = "consumet_enime"
    override val hostUrl = "https://api.consumet.org/anime/enime"
    override val isDubAvailableSeparately = false

    override suspend fun search(query: String): List<ShowResponse> {
        return client.get("$hostUrl/$query").parsed<SearchResponse>().results.map {
            ShowResponse(it.title, it.id, it.image)
        }
    }

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        return client.get("$hostUrl/info?id=$animeLink").parsed<InfoResponse>().episodes.map {
            Episode(
                it.number.toString(),
                "$hostUrl/watch?episodeId=${it.id}"
            )
        }
    }

    override suspend fun loadVideoServers(episodeLink: String, extra: Map<String, String>?): List<VideoServer> {
        return listOf(
            VideoServer("gogocdn", episodeLink),
        )
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor = Consumet9AnimeExtractor(server)

    class Consumet9AnimeExtractor(override val server: VideoServer) : VideoExtractor() {
        override suspend fun extract(): VideoContainer {
            val res = client.get(server.embed.url.printIt("Aa : ")).parsed<EpisodeResponse>()
            res.sources ?: throw Exception(res.message!!)

            return VideoContainer(res.sources.map {
                val link = FileUrl(it.url, res.headers ?: mapOf())
                Video(
                    null,
                    if (it.isM3U8) VideoType.M3U8 else VideoType.CONTAINER,
                    link,
                    if (!it.isM3U8) getSize(link) else null,
                    it.quality
                )
            })
        }
    }

    @Serializable
    data class SearchResponse(
        val results: List<Result>
    ) {
        @Serializable
        data class Result(
            val id: String,
            val title: String,
            val image: String
        )
    }

    @Serializable
    data class InfoResponse(
        val episodes: List<Episode>
    ) {
        @Serializable
        data class Episode(
            val id: String,
            val number: Long
        )
    }

    @Serializable
    data class EpisodeResponse(
        val message: String? = null,
        val headers: Map<String, String>? = null,
        val sources: List<Source>? = null,
    ) {
        @Serializable
        data class Source(
            val url: String,
            val quality: String? = null,
            val isM3U8: Boolean
        )
    }
}