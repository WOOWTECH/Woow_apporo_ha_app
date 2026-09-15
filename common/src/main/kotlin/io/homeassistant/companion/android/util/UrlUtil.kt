package io.homeassistant.companion.android.util

import android.net.Uri
import io.homeassistant.companion.android.common.BuildConfig
import io.homeassistant.companion.android.common.data.MalformedHttpUrlException
import io.homeassistant.companion.android.common.data.authentication.impl.AuthenticationService
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

/**
 * Host that serves the branded web content of the app: universal links, the NFC tag addresses and
 * the help centre.
 *
 * Every branded address in the app is built from this single value so that a future change of
 * domain is a one line change instead of a repository wide search.
 */
const val BRAND_HOST = "aiot.apporo.ai"

/**
 * URL scheme the app registers for its own deep links, for example `apporoaiot://navigate/lovelace`.
 *
 * This value has to stay in step with the `intent-filter` entries declared in the Android manifests.
 * The debug build is installed next to the release build under a different application id, so the
 * two are meant to claim different schemes. Expressing that split needs a generated `BuildConfig`
 * field; until such a field exists this constant is the only place the scheme is spelled out.
 */
/**
 * Deep-link / OAuth-callback scheme for this build type.
 *
 * Supplied by `BuildConfig` so it always matches the `deepLinkScheme` manifest placeholder that
 * declares the intent-filters — both come from `apporoDeepLinkScheme` in `gradle.properties`.
 * Debug builds get a distinct scheme so they do not contend with a release build on the same
 * device. Not a `const`, because the value is only known at build time.
 */
val DEEP_LINK_SCHEME: String = BuildConfig.DEEP_LINK_SCHEME

/** Prefix the frontend puts in front of an in-app navigation target. */
private val NAVIGATE_DEEP_LINK_PREFIX = "$DEEP_LINK_SCHEME://navigate/"

private const val HTTPS_SCHEME = "https"

/** Single path segment that precedes the identifier in an NFC tag address. */
private const val NFC_TAG_PATH_SEGMENT = "tag"

/** An accepted NFC tag address has exactly the two segments `tag` and the identifier. */
private const val TAG_PATH_SEGMENT_COUNT = 2
private const val TAG_PREFIX_SEGMENT_INDEX = 0
private const val TAG_IDENTIFIER_SEGMENT_INDEX = 1

/** Host of tags written by the upstream Home Assistant app, which users may already be carrying. */
private const val LEGACY_NFC_TAG_HOST = "www.home-assistant.io"

/**
 * Hosts accepted when reading an NFC tag: the host this app writes, plus the legacy host so that
 * tags provisioned before the app was rebranded keep working.
 */
private val NFC_TAG_HOSTS = listOf(BRAND_HOST, LEGACY_NFC_TAG_HOST)

object UrlUtil {
    fun formattedUrlString(url: String): String {
        return if (url == "") {
            throw MalformedHttpUrlException()
        } else {
            try {
                val httpUrl = url.toHttpUrl()
                HttpUrl.Builder()
                    .scheme(httpUrl.scheme)
                    .host(httpUrl.host)
                    .port(httpUrl.port)
                    .toString()
            } catch (e: IllegalArgumentException) {
                throw MalformedHttpUrlException(
                    e.message,
                )
            }
        }
    }

    fun buildAuthenticationUrl(url: String): String {
        return url.toHttpUrlOrNull()!!
            .newBuilder()
            .addPathSegments("auth/authorize")
            .addEncodedQueryParameter("response_type", "code")
            .addEncodedQueryParameter("client_id", AuthenticationService.CLIENT_ID)
            .build()
            .toString()
    }

    /**
     * Resolves a URL input string against a base URL.
     *
     * @param base The base URL to resolve relative URLs against. Can be null if input is absolute.
     * @param input The URL string to resolve. Supported formats:
     *   - Absolute URL (http://... or https://...)
     *   - Relative path to be resolved against base
     *   - Deep link URL with the [NAVIGATE_DEEP_LINK_PREFIX] prefix, for example
     *     `apporoaiot://navigate/lovelace/default`
     * @return The resolved URL, the base URL if input is invalid, or null if resolution fails
     */
    fun handle(base: URL?, input: String): URL? {
        val normalizedInput = input.removePrefix(NAVIGATE_DEEP_LINK_PREFIX)

        val uri = try {
            URI(normalizedInput)
        } catch (e: Exception) {
            Timber.w(e, "Invalid URI input: $normalizedInput")
            return base
        }

        return when {
            isAbsoluteUrl(input) -> {
                uri.runCatching { toURL() }
                    .onFailure { Timber.w(it, "Failed to convert URI to URL: $normalizedInput") }
                    .getOrNull()
            }

            else -> buildRelativeUrl(base, uri)
        }
    }

    private fun buildRelativeUrl(base: URL?, uri: URI): URL? {
        val builder = base?.toHttpUrlOrNull()?.newBuilder() ?: return null

        return builder.apply {
            uri.path?.takeIf { it.isNotBlank() }?.let {
                addPathSegments(it.trim().removePrefix("/"))
            }
            uri.query?.takeIf { it.isNotBlank() }?.let {
                query(it.trim())
            }
            uri.fragment?.takeIf { it.isNotBlank() }?.let {
                fragment(it.trim())
            }
        }.build().toUrl()
    }

    fun isAbsoluteUrl(it: String?): Boolean {
        return Regex("^https?://").containsMatchIn(it.toString())
    }

    /** @return `true` if both URLs have the same 'base': an equal protocol, host, port and userinfo */
    fun URL.baseIsEqual(other: URL?): Boolean = if (other == null) {
        false
    } else {
        host.equals(other.host, ignoreCase = true) &&
            port.let {
                if (it ==
                    -1
                ) {
                    defaultPort
                } else {
                    it
                }
            } == other.port.let { if (it == -1) defaultPort else it } &&
            protocol.equals(other.protocol, ignoreCase = true) &&
            userInfo == other.userInfo
    }

    /**
     * Reads the tag identifier out of the address stored on an NFC tag.
     *
     * Only an exact `https://<accepted host>/tag/<identifier>` address is accepted. A tag is a piece
     * of hardware that anybody can hand to the user, so the address it carries is untrusted input:
     * anything that merely looks similar, such as a plain HTTP address, a look-alike host, an extra
     * port or user information, a query string, a fragment or additional path segments, is rejected
     * instead of being scanned.
     *
     * @param uri The address read from the tag, or `null` when the tag carried no address.
     * @return The tag identifier, or `null` when [uri] is not an accepted tag address.
     */
    fun splitNfcTagId(uri: Uri?): String? {
        if (uri == null || !uri.isAcceptedNfcTagUri()) {
            return null
        }
        return uri.pathSegments[TAG_IDENTIFIER_SEGMENT_INDEX]
    }

    /**
     * Builds the address written to a newly provisioned NFC tag.
     *
     * @param identifier The tag identifier to embed in the address.
     * @return The `https://<brand host>/tag/<identifier>` address to store on the tag.
     * @throws IllegalArgumentException when [identifier] is blank or carries a path separator or a
     *         control character, which would produce an address that [splitNfcTagId] refuses to read
     *         back.
     */
    fun buildNfcTagUri(identifier: String): Uri {
        require(identifier.isAcceptedNfcTagIdentifier()) { "NFC tag identifier is not usable in an address" }

        return Uri.Builder()
            .scheme(HTTPS_SCHEME)
            .authority(BRAND_HOST)
            .appendPath(NFC_TAG_PATH_SEGMENT)
            .appendPath(identifier)
            .build()
    }

    private fun Uri.isAcceptedNfcTagUri(): Boolean {
        val hasAcceptedOrigin = scheme.equals(HTTPS_SCHEME, ignoreCase = true) &&
            NFC_TAG_HOSTS.any { host.equals(it, ignoreCase = true) } &&
            port == -1 &&
            userInfo == null
        val carriesNoExtraData = query == null && fragment == null

        return hasAcceptedOrigin && carriesNoExtraData && hasExactTagPath()
    }

    private fun Uri.hasExactTagPath(): Boolean {
        val segments = pathSegments

        return segments.size == TAG_PATH_SEGMENT_COUNT &&
            segments[TAG_PREFIX_SEGMENT_INDEX] == NFC_TAG_PATH_SEGMENT &&
            segments[TAG_IDENTIFIER_SEGMENT_INDEX].isAcceptedNfcTagIdentifier()
    }

    private fun String.isAcceptedNfcTagIdentifier(): Boolean {
        return isNotBlank() && '/' !in this && none { character -> character.isISOControl() }
    }
}

/**
 * Determines if this URL is publicly accessible using Fully Qualified Domain Name (FQDN) or a public IP.
 *
 * A URL is considered to be publicly accessible if:
 * 1. Its hostname does NOT end with a known local TLD (`.local`, `.lan`, `.home`, `.internal`,
 *    `.localdomain`), AND
 * 2. When resolved via DNS, ALL of its IP addresses are public (not private RFC 1918 addresses,
 *    not loopback, not link-local, and not any-local addresses).
 *
 * This function performs DNS resolution on the IO dispatcher and may block briefly while
 * resolving the hostname.
 *
 * @return `true` if the URL is publicly accessible, `false` if it is local/private or
 *         if DNS resolution fails.
 */
suspend fun URL.isPubliclyAccessible(): Boolean {
    return isPubliclyAccessible(host)
}

private suspend fun isPubliclyAccessible(fqdn: String): Boolean {
    // Check TLD
    val localTlds = listOf(".local", ".lan", ".home", ".internal", ".localdomain")
    if (localTlds.any { fqdn.endsWith(it, ignoreCase = true) }) {
        return false
    }

    // Resolve and check IP
    return try {
        val addresses = withContext(Dispatchers.IO) {
            InetAddress.getAllByName(fqdn)
        }
        addresses.none { it.isPrivateOrLocal() }
    } catch (e: UnknownHostException) {
        false
    }
}

private fun InetAddress.isPrivateOrLocal(): Boolean {
    return this.isSiteLocalAddress ||
        // Private IP ranges (RFC 1918)
        this.isLoopbackAddress ||
        // 127.0.0.0/8 or ::1
        this.isLinkLocalAddress ||
        // 169.254.0.0/16 or fe80::/10
        this.isAnyLocalAddress // 0.0.0.0 or ::
}

/**
 * Checks if this URL has the same origin (scheme, host, and port) as the other URL.
 *
 * @param other the URL to compare against
 * @return `true` if both URLs have the same scheme, host, and port
 */
fun HttpUrl.hasSameOrigin(other: HttpUrl): Boolean {
    return scheme.equals(other.scheme, ignoreCase = true) &&
        host.equals(other.host, ignoreCase = true) &&
        port == other.port
}

/**
 * Checks if this Uri has the same origin (scheme, host, and port) as the other Uri.
 * Default ports (443 for HTTPS, 80 for HTTP) are normalized for comparison.
 *
 * @param other the Uri to compare against
 * @return `true` if both URIs have the same scheme, host, and port
 */
fun Uri.hasSameOrigin(other: Uri?): Boolean {
    if (other == null) return false
    return scheme.equals(other.scheme, ignoreCase = true) &&
        host.equals(other.host, ignoreCase = true) &&
        effectivePort == other.effectivePort
}

private val Uri.effectivePort: Int
    get() = when {
        port != -1 -> port
        scheme.equals("https", ignoreCase = true) -> 443
        scheme.equals("http", ignoreCase = true) -> 80
        else -> -1
    }

/**
 * Checks if this Uri has a non root path (not empty, not just "/").
 *
 * @return `true` if the Uri has a path that is not blank and not just "/"
 */
fun Uri.hasNonRootPath(): Boolean {
    val path = this.path ?: return false
    return path.isNotBlank() && path != "/"
}
