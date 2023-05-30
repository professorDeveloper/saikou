package ani.saikou.others

import android.app.Activity
import android.app.DownloadManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import ani.saikou.anime.Episode
import ani.saikou.defaultHeaders
import ani.saikou.loadData
import ani.saikou.toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object Download {
    @Suppress("DEPRECATION")
    private fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    private fun getDownloadDir(activity: Activity): File {
        val direct: File
        if (loadData<Boolean>("sd_dl") == true) {
            val arrayOfFiles = ContextCompat.getExternalFilesDirs(activity, null)
            val parentDirectory = arrayOfFiles[1].toString()
            direct = File(parentDirectory)
            if (!direct.exists()) direct.mkdirs()
        }
        else {
            direct = File("storage/emulated/0/${Environment.DIRECTORY_DOWNLOADS}/Saikou/")
            if (!direct.exists()) direct.mkdirs()
        }
        return direct
    }
    fun defaultDownload(activity: Activity, episode: Episode, animeTitle: String) {
        val manager = activity.getSystemService(AppCompatActivity.DOWNLOAD_SERVICE) as DownloadManager
        val extractor = episode.extractors?.find { it.server.name == episode.selectedExtractor } ?: return
        val video =
            if (extractor.videos.size > episode.selectedVideo) extractor.videos[episode.selectedVideo] else return
        val regex = "[\\\\/:*?\"<>|]".toRegex()
        val aTitle = animeTitle.replace(regex, "")
        val request: DownloadManager.Request = DownloadManager.Request(Uri.parse(video.file.url))

        video.file.headers.forEach {
            request.addRequestHeader(it.key, it.value)
        }

        val title = "Episode ${episode.number}${if (episode.title != null) " - ${episode.title}" else ""}".replace(regex, "")
        val name = "$title${if (video.size != null) "(${video.size}p)" else ""}.mp4"
        CoroutineScope(Dispatchers.IO).launch {
            try {
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                val arrayOfFiles = ContextCompat.getExternalFilesDirs(activity, null)
                if (loadData<Boolean>("sd_dl") == true && arrayOfFiles.size > 1 && arrayOfFiles[0] != null && arrayOfFiles[1] != null) {
                    val parentDirectory = arrayOfFiles[1].toString() + "/Anime/${aTitle}/"
                    val direct = File(parentDirectory)
                    if (!direct.exists()) direct.mkdirs()
                    request.setDestinationUri(Uri.fromFile(File("$parentDirectory$name")))
                } else {
                    val direct = File(Environment.DIRECTORY_DOWNLOADS + "/Saikou/Anime/${aTitle}/")
                    if (!direct.exists()) direct.mkdirs()
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "/Saikou/Anime/${aTitle}/$name"
                    )
                }
                request.setTitle("$title:$aTitle")
                manager.enqueue(request)
                toast("Started Downloading\n$title : $aTitle")
            } catch (e: SecurityException) {
                toast("Please give permission to access Files & Folders from Settings, & Try again.")
            } catch (e: Exception) {
                toast(e.toString())
            }
        }
    }
    fun oneDM (activity: Activity, episode: Episode, animeTitle: String) {
        val appName = if (isPackageInstalled("idm.internet.download.manager.plus", activity.packageManager)) {
            "idm.internet.download.manager.plus"
        } else if (isPackageInstalled("idm.internet.download.manager", activity.packageManager)) {
            "idm.internet.download.manager"
        }
        else if (isPackageInstalled("idm.internet.download.manager.adm.lite", activity.packageManager)) {
            "idm.internet.download.manager.adm.lite"
        } else {
            ""
        }
        if (appName.isNotEmpty()) {
            val extractor = episode.extractors?.find { it.server.name == episode.selectedExtractor } ?: return
            val video = if (extractor.videos.size > episode.selectedVideo) extractor.videos[episode.selectedVideo] else return
            val regex = "[\\\\/:*?\"<>|]".toRegex()
            val title = "Episode ${episode.number}${if (episode.title != null) " - ${episode.title}" else ""}".replace(regex, "")
            val name = "$title${if (video.size != null) "(${video.size}p)" else ""}"
            val aTitle = animeTitle.replace(regex, "")
            val bundle = Bundle()
            defaultHeaders.forEach { a -> bundle.putString(a.key, a.value)}
            video.file.headers.forEach { a -> bundle.putString(a.key, a.value)}
            // documentation: https://www.apps2sd.info/idmp/faq?id=35
            val intent = Intent(Intent.ACTION_VIEW).apply {
                component = ComponentName(appName, "idm.internet.download.manager.Downloader")
                data = Uri.parse(video.file.url)
                putExtra("extra_headers", bundle)
                putExtra("extra_filename", "$aTitle - $name")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ContextCompat.startActivity(activity.baseContext, intent, null)
        }
        else {
            ContextCompat.startActivity(
                activity.baseContext,
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=idm.internet.download.manager")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                null
            )
            toast("Please install 1DM")
        }
    }
    fun adm (activity: Activity, episode: Episode, animeTitle: String) {
        if(isPackageInstalled("com.dv.adm", activity.packageManager)) {
            val extractor = episode.extractors?.find { it.server.name == episode.selectedExtractor } ?: return
            val video = if (extractor.videos.size > episode.selectedVideo) extractor.videos[episode.selectedVideo] else return
            val regex = "[\\\\/:*?\"<>|]".toRegex()
            val aTitle = animeTitle.replace(regex, "")
            val title = "Episode ${episode.number}${if (episode.title != null) " - ${episode.title}" else ""}".replace(regex, "")
            val name = "$title${if (video.size != null) "(${video.size}p)" else ""}"
            val bundle = Bundle()
            defaultHeaders.forEach { a -> bundle.putString(a.key, a.value)}
            video.file.headers.forEach { a -> bundle.putString(a.key, a.value)}
            // unofficial documentation: https://pastebin.com/ScDNr2if (there is no official documentation)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                component = ComponentName("com.dv.adm", "com.dv.adm.AEditor")
                putExtra("com.dv.get.ACTION_LIST_ADD", "${video.file.url}<info>$name.mp4")
                putExtra("com.dv.get.ACTION_LIST_PATH", "${getDownloadDir(activity)}/Anime/${aTitle}/")
                putExtra("android.media.intent.extra.HTTP_HEADERS", bundle)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ContextCompat.startActivity(activity.baseContext, intent, null)
            }
        else {
            ContextCompat.startActivity(
                activity.baseContext,
                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.dv.adm")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                null
            )
            toast("Please install ADM")
        }
    }
}