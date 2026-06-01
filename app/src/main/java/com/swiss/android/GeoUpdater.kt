package com.swiss.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

// GEO UPDATER — delete this file to remove geo update support.
// Also remove the marked blocks in VpnViewModel, SwissVpnService, and MainActivity.
object GeoUpdater {
    const val ENABLED = false

    private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000

    private val FILES = listOf(
        "geoip.dat" to "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geoip.dat",
        "geosite.dat" to "https://github.com/Loyalsoldier/v2ray-rules-dat/releases/latest/download/geosite.dat",
    )

    fun isStale(filesDir: File): Boolean = FILES.any { (name, _) ->
        val f = File(filesDir, name)
        !f.exists() || System.currentTimeMillis() - f.lastModified() > MAX_AGE_MS
    }

    suspend fun download(filesDir: File) = withContext(Dispatchers.IO) {
        FILES.forEach { (name, url) ->
            val tmp = File(filesDir, "$name.tmp")
            try {
                URL(url).openStream().use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
                tmp.renameTo(File(filesDir, name))
            } finally {
                if (tmp.exists()) tmp.delete()
            }
        }
    }
}
