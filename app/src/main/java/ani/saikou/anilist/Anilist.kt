package ani.saikou.anilist

import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import ani.saikou.client
import ani.saikou.openLinkInBrowser
import ani.saikou.tryWithSuspend
import java.io.File
import java.util.*

object Anilist {
    val query: AnilistQueries = AnilistQueries()
    val mutation: AnilistMutations = AnilistMutations()

    var token: String? = null
    var username: String? = null
    var adult: Boolean = false
    var userid: Int? = null
    var avatar: String? = null
    var bg: String? = null
    var episodesWatched: Int? = null
    var chapterRead: Int? = null

    var genres: ArrayList<String>? = null
    var tags: Map<Boolean, List<String>>? = null

    val sortBy = mapOf(
        "Score" to "SCORE_DESC",
        "Popular" to "POPULARITY_DESC",
        "Trending" to "TRENDING_DESC",
        "A-Z" to "TITLE_ENGLISH",
        "Z-A" to "TITLE_ENGLISH_DESC",
        "What?" to "SCORE",
    )

    val seasons = listOf(
        "WINTER", "SPRING", "SUMMER", "FALL"
    )

    val anime_formats = listOf(
        "TV", "TV SHORT", "MOVIE", "SPECIAL", "OVA", "ONA", "MUSIC"
    )

    val manga_formats = listOf(
        "MANGA", "NOVEL", "ONE SHOT"
    )

    val authorRoles = listOf(
        "Original Creator", "Story & Art", "Story"
    )

    private val cal: Calendar = Calendar.getInstance()
    private val currentYear = cal.get(Calendar.YEAR)
    private val currentSeason: Int = when (cal.get(Calendar.MONTH)) {
        0, 1, 2   -> 0
        3, 4, 5   -> 1
        6, 7, 8   -> 2
        9, 10, 11 -> 3
        else      -> 0
    }

    private fun getSeason(next: Boolean): Pair<String, Int> {
        var newSeason = if (next) currentSeason + 1 else currentSeason - 1
        var newYear = currentYear
        if (newSeason > 3) {
            newSeason = 0
            newYear++
        } else if (newSeason < 0) {
            newSeason = 3
            newYear--
        }
        return seasons[newSeason] to newYear
    }

    val currentSeasons = listOf(
        getSeason(false),
        seasons[currentSeason] to currentYear,
        getSeason(true)
    )

    fun loginIntent(context: Context) {
        val clientID = 6818
        try {
            CustomTabsIntent.Builder().build().launchUrl(
                context,
                Uri.parse("https://anilist.co/api/v2/oauth/authorize?client_id=$clientID&response_type=token")
            )
        } catch (e: ActivityNotFoundException) {
            openLinkInBrowser("https://anilist.co/api/v2/oauth/authorize?client_id=$clientID&response_type=token")
        }
    }

    fun getSavedToken(context: Context): Boolean {
        if ("anilistToken" in context.fileList()) {
            token = File(context.filesDir, "anilistToken").readText()
            return true
        }
        return false
    }

    fun removeSavedToken(context: Context) {
        token = null
        username = null
        adult = false
        userid = null
        avatar = null
        bg = null
        episodesWatched = null
        chapterRead = null
        if ("anilistToken" in context.fileList()) {
            File(context.filesDir, "anilistToken").delete()
        }
    }

    suspend inline fun <reified T : Any> executeQuery(
        query: String,
        variables: String = "",
        force: Boolean = false,
        useToken: Boolean = true,
        show: Boolean = false,
        cache: Int? = null
    ): T? {
        return tryWithSuspend {
            val data = mapOf(
                "query" to query,
                "variables" to variables
            )
            val headers = mutableMapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )

            if (token != null || force) {
                if (token != null && useToken) headers["Authorization"] = "Bearer $token"

                val json = client.post("https://graphql.anilist.co/", headers, data = data, cacheTime = cache ?: 10)
                if (!json.text.startsWith("{")) throw Exception("Seems like Anilist is down, maybe try using a VPN or you can wait for it to comeback.")
                if (show) println("Response : ${json.text}")
                json.parsed()
            } else null
        }
    }
}
