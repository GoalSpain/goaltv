package com.goaltv

import com.goaltv.data.network.NetworkModule
import com.goaltv.data.repository.XtreamRepositoryImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamPlaybackTest {

    @Test
    fun testPlayerOkHttpClientConfiguration() {
        val playerClient = NetworkModule.getPlayerOkHttpClient()
        assertNotNull(playerClient)
        assertEquals(20000, playerClient.connectTimeoutMillis)
        assertEquals(20000, playerClient.readTimeoutMillis)
        assertTrue(playerClient.followRedirects)
        assertTrue(playerClient.followSslRedirects)
    }

    @Test
    fun testXtreamStreamUrlBuilding() {
        val repo = XtreamRepositoryImpl()
        val serverUrl = "http://zeta.proxy-pass.xyz:80"
        val user = "testuser"
        val pass = "testpass"
        val streamId = "12345"

        val liveUrlTs = repo.buildLiveStreamUrl(serverUrl, user, pass, streamId, "ts")
        assertEquals("http://zeta.proxy-pass.xyz/live/testuser/testpass/12345.ts", liveUrlTs)

        val liveUrlM3u8 = repo.buildLiveStreamUrl(serverUrl, user, pass, streamId, "m3u8")
        assertEquals("http://zeta.proxy-pass.xyz/live/testuser/testpass/12345.m3u8", liveUrlM3u8)

        val liveUrlRaw = repo.buildLiveStreamUrl(serverUrl, user, pass, streamId, "")
        assertEquals("http://zeta.proxy-pass.xyz/live/testuser/testpass/12345", liveUrlRaw)
    }
}
