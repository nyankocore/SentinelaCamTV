package com.sentinela.camtv.data.update

import org.junit.Assert.assertEquals
import org.junit.Test

class GitHubReleaseParserTest {
    @Test
    fun parseReleaseKeepsOnlyAssetsWithNameAndDownloadUrl() {
        val release = GitHubReleaseParser.parse(
            """
            {
              "tag_name": "v1.0.1",
              "html_url": "https://github.com/nyankocore/SentinelaCamTV/releases/tag/v1.0.1",
              "body": "- Corrige retorno no mosaico vazio.\n- Melhora ONVIF.",
              "assets": [
                {
                  "name": "SentinelaCamTV-v1.0.1-armeabi-v7a.apk",
                  "browser_download_url": "https://example.invalid/armeabi.apk"
                },
                {
                  "name": "SHA256SUMS.txt",
                  "browser_download_url": "https://example.invalid/SHA256SUMS.txt"
                },
                {
                  "name": "",
                  "browser_download_url": "https://example.invalid/ignored.apk"
                },
                {
                  "name": "SentinelaCamTV-v1.0.1-universal.apk",
                  "browser_download_url": ""
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("v1.0.1", release.tagName)
        assertEquals("https://github.com/nyankocore/SentinelaCamTV/releases/tag/v1.0.1", release.htmlUrl)
        assertEquals("- Corrige retorno no mosaico vazio.\n- Melhora ONVIF.", release.body)
        assertEquals(
            listOf(
                GitHubReleaseAsset(
                    name = "SentinelaCamTV-v1.0.1-armeabi-v7a.apk",
                    downloadUrl = "https://example.invalid/armeabi.apk",
                ),
                GitHubReleaseAsset(
                    name = "SHA256SUMS.txt",
                    downloadUrl = "https://example.invalid/SHA256SUMS.txt",
                ),
            ),
            release.assets,
        )
    }
}
