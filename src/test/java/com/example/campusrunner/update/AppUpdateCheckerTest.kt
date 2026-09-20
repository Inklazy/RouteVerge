package com.example.campusrunner.update

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Covers the pure parts of [AppUpdateChecker]: version parsing/comparison,
 * GitHub release JSON parsing and APK asset selection.
 */
class AppUpdateCheckerTest {

    private fun releaseJson(
        tag: String = "v2.1.1",
        htmlUrl: String = "https://github.com/Inklazy/RouteVerge/releases/tag/v2.1.1",
        body: String = "",
        assets: List<Pair<String, String>> = emptyList()
    ): JSONObject {
        val json = JSONObject()
        json.put("tag_name", tag)
        json.put("name", tag)
        json.put("html_url", htmlUrl)
        json.put("body", body)
        val array = JSONArray()
        assets.forEach { (name, url) ->
            array.put(
                JSONObject()
                    .put("name", name)
                    .put("browser_download_url", url)
            )
        }
        json.put("assets", array)
        return json
    }

    @Test
    fun `version comparison is numeric instead of lexicographic`() {
        assertTrue(AppUpdateChecker.compareVersions("2.1.10", "2.1.9") > 0)
        assertTrue(AppUpdateChecker.compareVersions("2.2.0", "2.1.10") > 0)
        assertTrue(AppUpdateChecker.compareVersions("3.0.0", "2.2.0") > 0)
        assertTrue(AppUpdateChecker.compareVersions("2.0.0", "10.0.0") < 0)
        assertEquals(0, AppUpdateChecker.compareVersions("2.1.9", "2.1.9"))
        assertEquals(0, AppUpdateChecker.compareVersions("2.1", "2.1.0"))
    }

    @Test
    fun `version normalization strips prefix and build decoration`() {
        assertEquals("2.1.1", AppUpdateChecker.normalizeVersion("v2.1.1"))
        assertEquals("2.1.1", AppUpdateChecker.normalizeVersion("V2.1.1"))
        assertEquals("2.1.1", AppUpdateChecker.normalizeVersion("2.1.1+build.7"))
        assertEquals("2.1.1", AppUpdateChecker.normalizeVersion("2.1.1-beta"))
        assertEquals("", AppUpdateChecker.normalizeVersion("   "))
    }

    @Test
    fun `version code marker is parsed only when present`() {
        assertEquals(
            22,
            AppUpdateChecker.parseVersionCodeMarker("### 版本信息\n<!-- routeverge-version-code: 22 -->\n")
        )
        assertNull(AppUpdateChecker.parseVersionCodeMarker("Version Code：21"))
        assertNull(AppUpdateChecker.parseVersionCodeMarker(""))
    }

    @Test
    fun `remote version code decides when the release carries a marker`() {
        val newer = AppUpdateChecker.parseLatestRelease(
            releaseJson(tag = "v2.1.0", body = "<!-- routeverge-version-code: 22 -->").toString(),
            currentVersionName = "2.1.0",
            currentVersionCode = 21
        )
        assertTrue(newer.updateRequired)
        assertEquals("2.1.0", newer.latestVersion)

        val same = AppUpdateChecker.parseLatestRelease(
            releaseJson(tag = "v2.1.0", body = "<!-- routeverge-version-code: 21 -->").toString(),
            currentVersionName = "2.1.0",
            currentVersionCode = 21
        )
        assertFalse(same.updateRequired)
    }

    @Test
    fun `version name comparison is the fallback without a marker`() {
        val newer = AppUpdateChecker.parseLatestRelease(
            releaseJson(tag = "v2.1.10").toString(),
            currentVersionName = "2.1.9",
            currentVersionCode = 29
        )
        assertTrue(newer.updateRequired)

        val same = AppUpdateChecker.parseLatestRelease(
            releaseJson(tag = "v2.1.9").toString(),
            currentVersionName = "2.1.9",
            currentVersionCode = 29
        )
        assertFalse(same.updateRequired)
    }

    @Test
    fun `release apk asset wins over the release page`() {
        val apkUrl = "https://github.com/Inklazy/RouteVerge/releases/download/v2.1.1/RouteVerge-v2.1.1-release.apk"
        val json = releaseJson(
            assets = listOf(
                "RouteVerge-v2.1.1-source.zip" to "https://example.invalid/source.zip",
                "RouteVerge-v2.1.1-release.apk" to apkUrl
            )
        )
        assertEquals(apkUrl, AppUpdateChecker.pickApkAssetUrl(json, "v2.1.1"))

        val result = AppUpdateChecker.parseLatestRelease(
            json.toString(),
            currentVersionName = "2.1.0",
            currentVersionCode = 21
        )
        assertEquals(apkUrl, result.downloadUrl)
    }

    @Test
    fun `any apk asset is accepted when the canonical name is absent`() {
        val apkUrl = "https://example.invalid/app-release.apk"
        val json = releaseJson(assets = listOf("app-release.apk" to apkUrl))
        assertEquals(apkUrl, AppUpdateChecker.pickApkAssetUrl(json, "v2.1.1"))
    }

    @Test
    fun `release without apk assets falls back to the release page`() {
        val htmlUrl = "https://github.com/Inklazy/RouteVerge/releases/tag/v2.1.1"
        val json = releaseJson(htmlUrl = htmlUrl)
        assertEquals("", AppUpdateChecker.pickApkAssetUrl(json, "v2.1.1"))

        val result = AppUpdateChecker.parseLatestRelease(
            json.toString(),
            currentVersionName = "2.1.0",
            currentVersionCode = 21
        )
        assertEquals(htmlUrl, result.releaseUrl)
        assertEquals(htmlUrl, result.downloadUrl)
        assertTrue(result.updateRequired)
    }

    @Test
    fun `release payload without a version is rejected`() {
        val json = JSONObject().put("html_url", "https://example.invalid").put("tag_name", "")
        assertThrows(IOException::class.java) {
            AppUpdateChecker.parseLatestRelease(
                json.toString(),
                currentVersionName = "2.1.0",
                currentVersionCode = 21
            )
        }
    }
}