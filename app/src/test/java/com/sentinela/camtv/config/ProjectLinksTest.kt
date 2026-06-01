package com.sentinela.camtv.config

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectLinksTest {
    @Test
    fun usesNyankocoreRepositoryUrls() {
        assertEquals("https://github.com/nyankocore/SentinelaCamTV", ProjectLinks.SITE_URL)
        assertEquals("github.com/nyankocore/SentinelaCamTV", ProjectLinks.SITE_LABEL)
        assertEquals("https://github.com/nyankocore/SentinelaCamTV/issues", ProjectLinks.ISSUES_URL)
        assertEquals(
            "https://api.github.com/repos/nyankocore/SentinelaCamTV/releases/latest",
            ProjectLinks.GITHUB_LATEST_RELEASE_API_URL,
        )
    }
}
