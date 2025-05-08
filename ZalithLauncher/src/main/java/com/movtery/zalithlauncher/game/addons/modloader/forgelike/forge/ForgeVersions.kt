package com.movtery.zalithlauncher.game.addons.modloader.forgelike.forge

import android.util.Log
import com.movtery.zalithlauncher.game.addons.modloader.ResponseTooShortException
import com.movtery.zalithlauncher.path.UrlManager
import com.movtery.zalithlauncher.utils.network.withRetry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * [Reference PCL2](https://github.com/Hex-Dragon/PCL2/blob/44aea3e/Plain%20Craft%20Launcher%202/Modules/Minecraft/ModDownload.vb#L626-L697)
 */
object ForgeVersions {
    private const val TAG = "ForgeVersions"
    private const val FORGE_FILE_URL = "https://files.minecraftforge.net/maven/net/minecraftforge/forge"

    private val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = UrlManager.TIME_OUT.first.toLong()
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * 获取 Forge 版本列表
     */
    suspend fun fetchForgeList(mcVersion: String): List<ForgeVersion>? = withContext(Dispatchers.Default) {
        val url = "https://files.minecraftforge.net/maven/net/minecraftforge/forge/index_${
            mcVersion.replace("-", "_")
        }.html"

        try {
            val html = withContext(Dispatchers.IO) {
                withRetry(TAG, maxRetries = 2) {
                    client.get(url) {
                        headers {
                            append(HttpHeaders.UserAgent, "Mozilla/5.0...")
                        }
                    }.body<String>()
                }
            }

            if (html.length < 100) throw ResponseTooShortException("Response too short")

            val versions = html.split("<td class=\"download-version")
                .drop(1)
                .mapNotNull { parseVersionFromHtml(it, mcVersion) }

            if (versions.isEmpty()) throw Exception("No versions found")

            versions
        } catch (e: CancellationException) {
            Log.d(TAG, "Client cancelled.")
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch forge list!", e)
            throw e
        }
    }

    /**
     * 获取 Forge 对应版本的下载链接
     */
    fun getDownloadUrl(version: ForgeVersion) =
        "$FORGE_FILE_URL/${version.inherit}-${version.fileVersion}/forge-${version.inherit}-${version.fileVersion}-${version.category}.${version.fileExtension}"

    private fun parseVersionFromHtml(html: String, mcVersion: String): ForgeVersion? {
        return try {
            val name = Regex("""(?<=\D)\d+(\.\d+)+""").find(html)?.value ?: return null
            //                      <i class="promo-latest  fa" aria-hidden="true"></i>     <i class="promo-recommended  fa" aria-hidden="true"></i>
            val isRecommended = html.contains("promo-latest  fa" /* 最新推荐标签 */) || html.contains("promo-recommended  fa" /* 推荐标签 */)
            val branch = Regex("""(?<=-$name-)[^-"]+(?=-[a-z]+\.[a-z]{3})""").find(html)?.value
            val timeStr = Regex("""(?<="download-time" title=")[^"]+""").find(html)?.value ?: return null
            
            val dateTime = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
            
            val (category, hash) = when {
                html.contains("classifier-installer") -> parseInstaller(html)
                html.contains("classifier-universal") -> parseUniversal(html)
                html.contains("client.zip") -> parseClient(html)
                else -> return null
            } ?: return null

            ForgeVersion(
                versionName = name,
                branch = branch,
                inherit = mcVersion,
                releaseTime = dateTime,
                hash = hash,
                isRecommended = isRecommended,
                category = category,
                fileVersion = "$name${branch?.let { "-$it" } ?: ""}"
            )
        } catch (e: Exception) {
            null
        }
    }

    //'类型为 installer.jar，支持范围 ~753 (~ 1.6.1 部分), 738~684 (1.5.2 全部)
    private fun parseInstaller(html: String): Pair<String, String>? {
        val section = html.substringAfter("installer.jar")
        val hash = Regex("""(?<=MD5:</strong> )[^<]+""").find(section)?.value?.trim()
        return hash?.let { "installer" to it }
    }

    //'类型为 universal.zip，支持范围 751~449 (1.6.1 部分), 682~183 (1.5.1 ~ 1.3.2 部分)
    private fun parseUniversal(html: String): Pair<String, String>? {
        val section = html.substringAfter("universal.zip")
        val hash = Regex("""(?<=MD5:</strong> )[^<]+""").find(section)?.value?.trim()
        return hash?.let { "universal" to it }
    }

    //'类型为 client.zip，支持范围 182~ (1.3.2 部分 ~)
    private fun parseClient(html: String): Pair<String, String>? {
        val section = html.substringAfter("client.zip")
        val hash = Regex("""(?<=MD5:</strong> )[^<]+""").find(section)?.value?.trim()
        return hash?.let { "client" to it }
    }
}