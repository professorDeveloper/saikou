package ani.saikou.parsers.anime

import ani.saikou.*
import ani.saikou.parsers.*
import ani.saikou.parsers.anime.extractors.StreamTape
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

class NineAnime : AnimeParser() {

    override val name = "9anime"
    override val saveName = "9anime_to"
    override val hostUrl = "https://9animehq.to"
    override val malSyncBackupName = "9anime"
    override val isDubAvailableSeparately = true

    override suspend fun loadEpisodes(animeLink: String, extra: Map<String, String>?): List<Episode> {
        val animeId = client.get(animeLink).document.select("#watch-main").attr("data-id")
        val body = client.get("$hostUrl/ajax/episode/list/$animeId?vrf=${encodeVrf(animeId)}".printIt("a : ")).parsed<Response>().result
        return Jsoup.parse(body).body().select("ul > li > a").mapNotNull {
            val id = it.attr("data-ids").split(",")
                .getOrNull(if (selectDub) 1 else 0) ?: return@mapNotNull null
            val num = it.attr("data-num")
            val title = it.selectFirst("span.d-title")?.text()
            val filler = it.hasClass("filler")
            Episode(num, id, title, isFiller = filler)
        }
    }

    private val embedHeaders = mapOf("referer" to "$hostUrl/")

    override suspend fun loadVideoServers(episodeLink: String, extra: Map<String, String>?): List<VideoServer> {
        val body = client.get("$hostUrl/ajax/server/list/$episodeLink?vrf=${encodeVrf(episodeLink)}").parsed<Response>().result
        val document = Jsoup.parse(body)
        return document.select("li").mapNotNull {
            val name = it.text()
            val encodedStreamUrl = getEpisodeLinks(it.attr("data-link-id"))?.result?.url ?: return@mapNotNull null
            val realLink = FileUrl(decodeVrf(encodedStreamUrl), embedHeaders)
            VideoServer(name, realLink)
        }
    }

    override suspend fun getVideoExtractor(server: VideoServer): VideoExtractor? {
        val extractor: VideoExtractor? = when (server.name) {
            "Vidstream"     -> Extractor(server)
            "MyCloud"       -> Extractor(server)
            "Streamtape"    -> StreamTape(server)
            else            -> null
        }
        return extractor
    }

    class Extractor(override val server: VideoServer) : VideoExtractor() {

        @Serializable
        data class Response (
            val data: Data? = null,
            val rawURL: String? = null
        ) {
            @Serializable
            data class Data(
                val media: Media? = null
            ) {
                @Serializable
                data class Media(
                    val sources: List<Source>? = null
                ) {
                    @Serializable
                    data class Source(
                        val file: String? = null
                    )
                }
            }
        }

        override suspend fun extract(): VideoContainer {
            val slug = server.embed.url.findBetween("e/","?")!!
            val server = if (server.name == "MyCloud") "mcloud" else "vizcloud"
            val url = "https://api.consumet.org/anime/9anime/helper?query=$slug&action=$server"
            val videos =  client.get(url).parsed<Response>().data?.media?.sources?.mapNotNull { s ->
                s.file?.let { Video(null,VideoType.M3U8,it) }
            } ?: emptyList()
            return VideoContainer(videos)
        }
    }

    override suspend fun search(query: String): List<ShowResponse> {
        val vrf = encodeVrf(query)
        val searchLink =
            "$hostUrl/filter?language%5B%5D=${if (selectDub) "dubbed" else "subbed"}&keyword=${encode(query)}&vrf=${vrf}&page=1"
        return client.get(searchLink).document.select("#list-items div.ani.poster.tip > a").map {
            val link = hostUrl + it.attr("href")
            val img = it.select("img")
            val title = img.attr("alt")
            val cover = img.attr("src")
            ShowResponse(title, link, cover)
        }
    }

//    override suspend fun loadByVideoServers(episodeUrl: String, extra: Map<String, String>?, callback: (VideoExtractor) -> Unit) {
//        tryWithSuspend {
//            val servers = loadVideoServers(episodeUrl, extra).map { getVideoExtractor(it) }
//            val mutex = Mutex()
//            servers.asyncMap {
//                tryWithSuspend {
//                    it?.apply {
//                        if (this is VizCloud) {
//                            mutex.withLock {
//                                load()
//                                callback.invoke(this)
//                            }
//                        } else {
//                            load()
//                            callback.invoke(this)
//                        }
//                    }
//                }
//            }
//        }
//    }

    @Serializable
    private data class Links(val result: Url?) {
        @Serializable
        data class Url(val url: String?)
    }

    @Serializable
    data class Response(val result: String)

    private suspend fun getEpisodeLinks(id: String): Links? {
        return tryWithSuspend { client.get("$hostUrl/ajax/server/$id?vrf=${encodeVrf(id)}").parsed() }
    }

    @Serializable
    data class SearchData (
        val url: String
    )
    private suspend fun encodeVrf(text: String): String {
        return client.get("https://api.consumet.org/anime/9anime/helper?query=$text&action=vrf").parsed<SearchData>().url
    }

    private suspend fun decodeVrf(text: String): String {
        return client.get("https://api.consumet.org/anime/9anime/helper?query=$text&action=decrypt").parsed<SearchData>().url
    }
}
