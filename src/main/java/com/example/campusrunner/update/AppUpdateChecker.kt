package com.example.campusrunner.update

import com.example.campusrunner.BuildConfig
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.Locale

/**
 * Outcome of a "latest release" lookup.
 *
 * @param latestVersion normalized tag of the newest release, e.g. `2.1.1`
 * @param releaseUrl human readable GitHub Release page
 * @param downloadUrl direct APK asset URL when the release publishes one,
 * otherwise the Release page
 */
data class AppUpdateResult(
    val updateRequired: Boolean,
    val latestVersion: String,
    val releaseUrl: String,
    val downloadUrl: String,
    val message: String = ""
)

/**
 * GitHub Releases based update check.
 *
 * The repository slug is never hard-coded in Kotlin: it comes from
 * [BuildConfig.GITHUB_REPOSITORY], fed by `gradle.properties`
 * (`githubRepository=...`). The local version always comes from
 * [BuildConfig.VERSION_NAME] / [BuildConfig.VERSION_CODE].
 */
object AppUpdateChecker {
    private const val NETWORK_TIMEOUT_MS = 12000
    private const val RELEASE_ASSET_NAME_PREFIX = "routeverge-v"
    private const val RELEASE_ASSET_NAME_SUFFIX = "-release.apk"
    private const val APK_EXTENSION = ".apk"

    /**
     * Machine readable marker written by `.github/workflows/release.yml`.
     * The GitHub API never exposes `versionCode`, so the release body carries
     * it and this checker prefers it over name comparison when present.
     */
    private val versionCodeMarker = Regex("""<!--\s*routeverge-version-code:\s*(\d+)\s*-->""")

    internal val repository: String
        get() = BuildConfig.GITHUB_REPOSITORY.trim()

    internal val latestReleaseApiUrl: String
        get() = "https://api.github.com/repos/$repository/releases/latest"

    internal val releasesPageUrl: String
        get() = "https://github.com/$repository/releases/latest"

    fun checkLatest(): AppUpdateResult {
        val repo = repository
        if (repo.count { it == '/' } != 1 || repo.startsWith("/") || repo.endsWith("/")) {
            throw IOException("GitHub 仓库配置无效: $repo")
        }

        val connection = URL(latestReleaseApiUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = NETWORK_TIMEOUT_MS
        connection.readTimeout = NETWORK_TIMEOUT_MS
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty(
            "User-Agent",
            "RouteVerge-Android/${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
        )

        try {
            val status = connection.responseCode
            val text = readStream(if (status in 200..399) connection.inputStream else connection.errorStream)
            if (status !in 200..399 || text.isBlank()) {
                throw IOException("GitHub HTTP $status")
            }
            return parseLatestRelease(text, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        } finally {
            connection.disconnect()
        }
    }

    /** Pure parsing step, free of I/O so it stays unit testable. */
    internal fun parseLatestRelease(
        jsonBody: String,
        currentVersionName: String,
        currentVersionCode: Int
    ): AppUpdateResult {
        val json = JSONObject(jsonBody)
        val rawTag = json.optString("tag_name").ifBlank { json.optString("name") }
        val latestVersion = normalizeVersion(rawTag)
        if (latestVersion.isBlank()) {
            throw IOException("无法读取最新版本号")
        }

        val releaseUrl = json.optString("html_url").ifBlank { releasesPageUrl }
        val apkUrl = pickApkAssetUrl(json, rawTag)
        val remoteVersionCode = parseVersionCodeMarker(json.optString("body"))
        val currentVersion = normalizeVersion(currentVersionName)

        val updateRequired =
            if (remoteVersionCode != null && currentVersionCode > 0) {
                remoteVersionCode > currentVersionCode
            } else {
                compareVersions(latestVersion, currentVersion) > 0
            }

        return AppUpdateResult(
            updateRequired = updateRequired,
            latestVersion = latestVersion,
            releaseUrl = releaseUrl,
            downloadUrl = apkUrl.ifBlank { releaseUrl }
        )
    }

    /**
     * Prefers the canonical `RouteVerge-v{tag}-release.apk` asset published by
     * the release workflow and falls back to any `*.apk` asset.
     */
    internal fun pickApkAssetUrl(json: JSONObject, rawTag: String): String {
        val assets = json.optJSONArray("assets") ?: return ""
        val expectedName =
            "$RELEASE_ASSET_NAME_PREFIX${normalizeVersion(rawTag)}$RELEASE_ASSET_NAME_SUFFIX".lowercase(Locale.US)
        var fallback = ""
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            val name = asset.optString("name")
            val url = asset.optString("browser_download_url")
            if (name.isBlank() || url.isBlank()) continue
            val lowerName = name.lowercase(Locale.US)
            if (lowerName == expectedName) return url
            if (fallback.isBlank() && lowerName.endsWith(APK_EXTENSION)) fallback = url
        }
        return fallback
    }

    /** Reads the optional `<!-- routeverge-version-code: N -->` marker. */
    internal fun parseVersionCodeMarker(releaseBody: String): Int? {
        val match = versionCodeMarker.find(releaseBody) ?: return null
        return match.groupValues.getOrNull(1)?.toIntOrNull()
    }

    /** Strips the `v` prefix and any `+build` / `-suffix` decoration. */
    internal fun normalizeVersion(value: String): String {
        return value
            .trim()
            .removePrefix("v")
            .removePrefix("V")
            .lowercase(Locale.US)
            .substringBefore("+")
            .substringBefore("-")
            .trim()
    }

    /**
     * Numeric, component-wise comparison — never a lexicographic string
     * compare, so `2.1.9 < 2.1.10 < 2.2.0 < 3.0.0`.
     */
    internal fun compareVersions(left: String, right: String): Int {
        val leftParts = versionParts(left)
        val rightParts = versionParts(right)
        val size = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until size) {
            val diff = (leftParts.getOrNull(index) ?: 0) - (rightParts.getOrNull(index) ?: 0)
            if (diff != 0) return diff
        }
        return 0
    }

    private fun versionParts(version: String): List<Int> {
        return version
            .split('.')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
    }

    private fun readStream(stream: InputStream?): String {
        if (stream == null) return ""
        return BufferedReader(InputStreamReader(stream, StandardCharsets.UTF_8)).use { reader ->
            buildString {
                while (true) {
                    val line = reader.readLine() ?: break
                    append(line)
                }
            }
        }
    }
}
