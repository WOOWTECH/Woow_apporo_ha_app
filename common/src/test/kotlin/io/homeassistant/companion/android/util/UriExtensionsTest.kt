package io.homeassistant.companion.android.util

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [Uri] extension functions and for the NFC tag helpers of [UrlUtil], defined in UrlUtil.kt.
 *
 * This test class uses Robolectric (JUnit 4) because [Uri] is an Android framework class
 * that requires the Android runtime to function properly. The main [UrlUtilTest] uses
 * JUnit 5 for tests that don't require Android classes.
 */
@RunWith(RobolectricTestRunner::class)
class UriExtensionsTest {

    @Test
    fun `hasSameOrigin returns expected value`() {
        assertHasSameOrigin("https://example.com", "https://example.com", true)
        assertHasSameOrigin("https://example.com/path", "https://example.com", true)
        assertHasSameOrigin("https://example.com?query=1", "https://example.com", true)
        assertHasSameOrigin("https://example.com:443", "https://example.com", true)
        assertHasSameOrigin("http://example.com:80", "http://example.com", true)
        assertHasSameOrigin("https://example.com:8123", "https://example.com:8123", true)
        assertHasSameOrigin("https://example.com", "https://other.com", false)
        assertHasSameOrigin("https://example.com", "http://example.com", false)
        assertHasSameOrigin("https://example.com:8123", "https://example.com:8124", false)
        assertHasSameOrigin("https://example.com", "https://example.com.evil.com", false)
        assertHasSameOrigin("https://sub.example.com", "https://example.com", false)
    }

    private fun assertHasSameOrigin(url1: String, url2: String, expected: Boolean) {
        val uri1 = Uri.parse(url1)
        val uri2 = Uri.parse(url2)
        assertEquals("hasSameOrigin($url1, $url2)", expected, uri1.hasSameOrigin(uri2))
    }

    @Test
    fun `hasSameOrigin returns false when other is null`() {
        val uri = Uri.parse("https://example.com")
        assertFalse(uri.hasSameOrigin(null))
    }

    @Test
    fun `hasNonRootPath returns expected value`() {
        assertHasNonRootPath("https://example.com/path", true)
        assertHasNonRootPath("https://example.com/lovelace/default", true)
        assertHasNonRootPath("https://example.com/a", true)
        assertHasNonRootPath("https://example.com/", false)
        assertHasNonRootPath("https://example.com", false)
    }

    private fun assertHasNonRootPath(url: String, expected: Boolean) {
        val uri = Uri.parse(url)
        assertEquals("hasNonRootPath($url)", expected, uri.hasNonRootPath())
    }

    @Test
    fun `Given tag address on an accepted host when calling splitNfcTagId then returns the identifier`() {
        assertTagId("https://aiot.apporo.ai/tag/$TAG_IDENTIFIER", TAG_IDENTIFIER)
        assertTagId("https://www.home-assistant.io/tag/$TAG_IDENTIFIER", TAG_IDENTIFIER)
    }

    @Test
    fun `Given tag address with a differently cased scheme or host when calling splitNfcTagId then returns the identifier`() {
        assertTagId("HTTPS://AIOT.APPORO.AI/tag/$TAG_IDENTIFIER", TAG_IDENTIFIER)
    }

    @Test
    fun `Given tag address with an escaped character in the identifier when calling splitNfcTagId then returns the decoded identifier`() {
        assertTagId("https://aiot.apporo.ai/tag/tag%20one", "tag one")
    }

    @Test
    fun `Given address that is not secure when calling splitNfcTagId then returns null`() {
        assertNotATagAddress("http://aiot.apporo.ai/tag/$TAG_IDENTIFIER")
    }

    @Test
    fun `Given address on an unexpected host when calling splitNfcTagId then returns null`() {
        assertNotATagAddress("https://aiot.apporo.example/tag/$TAG_IDENTIFIER")
        assertNotATagAddress("https://aiot.apporo.ai.example.com/tag/$TAG_IDENTIFIER")
        assertNotATagAddress("https://example.com/tag/$TAG_IDENTIFIER")
        assertNotATagAddress("https://home-assistant.io/tag/$TAG_IDENTIFIER")
    }

    @Test
    fun `Given tag address carrying a port or user information when calling splitNfcTagId then returns null`() {
        assertNotATagAddress("https://aiot.apporo.ai:8443/tag/$TAG_IDENTIFIER")
        assertNotATagAddress("https://someone@aiot.apporo.ai/tag/$TAG_IDENTIFIER")
    }

    @Test
    fun `Given tag address carrying a query or a fragment when calling splitNfcTagId then returns null`() {
        assertNotATagAddress("https://aiot.apporo.ai/tag/$TAG_IDENTIFIER?server=1")
        assertNotATagAddress("https://aiot.apporo.ai/tag/$TAG_IDENTIFIER#section")
    }

    @Test
    fun `Given address whose path is not exactly one tag identifier when calling splitNfcTagId then returns null`() {
        assertNotATagAddress("https://aiot.apporo.ai/tag/$TAG_IDENTIFIER/extra")
        assertNotATagAddress("https://aiot.apporo.ai/redirect/tag/$TAG_IDENTIFIER")
        assertNotATagAddress("https://aiot.apporo.ai/tag/")
        assertNotATagAddress("https://aiot.apporo.ai/tag")
        assertNotATagAddress("https://aiot.apporo.ai/")
    }

    @Test
    fun `Given no address when calling splitNfcTagId then returns null`() {
        assertNull(UrlUtil.splitNfcTagId(null))
    }

    @Test
    fun `Given an identifier when calling buildNfcTagUri then returns the address read back by splitNfcTagId`() {
        val uri = UrlUtil.buildNfcTagUri(TAG_IDENTIFIER)

        assertEquals("https://aiot.apporo.ai/tag/$TAG_IDENTIFIER", uri.toString())
        assertEquals(TAG_IDENTIFIER, UrlUtil.splitNfcTagId(uri))
    }

    @Test
    fun `Given an identifier that cannot be read back when calling buildNfcTagUri then throws`() {
        assertRejectedIdentifier("")
        assertRejectedIdentifier("   ")
        assertRejectedIdentifier("tag/extra")
        assertRejectedIdentifier("tag\u0000one")
    }

    private fun assertTagId(url: String, expected: String) {
        assertEquals("splitNfcTagId($url)", expected, UrlUtil.splitNfcTagId(Uri.parse(url)))
    }

    private fun assertNotATagAddress(url: String) {
        assertNull("splitNfcTagId($url)", UrlUtil.splitNfcTagId(Uri.parse(url)))
    }

    private fun assertRejectedIdentifier(identifier: String) {
        val failure = runCatching { UrlUtil.buildNfcTagUri(identifier) }.exceptionOrNull()

        assertTrue("buildNfcTagUri($identifier) should reject the identifier", failure is IllegalArgumentException)
    }
}

private const val TAG_IDENTIFIER = "5f0ba733-172f-430d-a7f8-e4ad940c88d7"
