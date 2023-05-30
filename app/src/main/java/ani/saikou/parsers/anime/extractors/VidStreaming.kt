package ani.saikou.parsers.anime.extractors

import ani.saikou.asyncMapNotNull
import ani.saikou.client
import ani.saikou.parsers.VideoContainer
import ani.saikou.parsers.VideoExtractor
import ani.saikou.parsers.VideoServer

class VidStreaming(override val server: VideoServer) : VideoExtractor() {
    override suspend fun extract(): VideoContainer {
        // `streaming.php` is the most likely one to be replaced
        // however sometimes the source will be using `embed` or `load`
        val url =
            server.embed.url
                .replace("streaming.php?", "loadserver.php?")
                .replace("embed.php", "loadserver.php")
                .replace("load.php", "loadserver.php")

        val res = client
                .get(url, mapOf("Referer" to "https://goload.one"))
                .document.select("ul.list-server-items > li.linkserver")

        return VideoContainer(
            res.asyncMapNotNull { server ->
                val src = server.attr("data-video") // link to the source
                val type = server.text().lowercase() // e.g streamsb, vidstreaming, multi quality

                // Using a when makes it easier to expand upon later
                return@asyncMapNotNull when (type) {
                    "streamsb" -> StreamSB(VideoServer(name = "StreamSB", embedUrl = src)).extract().videos
                    "xstreamcdn" -> FPlayer(VideoServer(name = "XStreamCDN", embedUrl = src)).extract().videos
                    else -> null
                }
            }.flatten()
        )
    }
}