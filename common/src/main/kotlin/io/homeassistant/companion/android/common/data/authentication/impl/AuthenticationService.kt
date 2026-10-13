package io.homeassistant.companion.android.common.data.authentication.impl

import io.homeassistant.companion.android.common.data.authentication.impl.entities.Token
import okhttp3.HttpUrl
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

interface AuthenticationService {

    companion object {
        // OAuth client_id. Home Assistant fetches this address from the server side during the
        // authorize request and reads the `rel="redirect_uri"` link tags out of it; the login only
        // succeeds when the app's callback appears there verbatim. An address that does not resolve
        // therefore breaks sign-in outright, and the hard-coded allow list in Home Assistant's
        // indieauth module covers the official client ids only, never a fork's.
        //
        // Home Assistant shows this URL verbatim to the user on the authorization screen, so it
        // has to be a brand domain — a github.io URL reads as someone else's site. The page lives
        // on the customer's Odoo website (`website.page` /android, primary qweb view
        // `apporo_aiot.oauth_android`), which is the same arrangement woowtech aiot uses.
        //
        // HA fetches this URL anonymously and parses `<link rel="redirect_uri">` out of it
        // (`indieauth.fetch_redirect_uris`), so the page must stay anonymously readable and must
        // keep declaring BOTH schemes: `apporoaiot://auth-callback` (release) and
        // `apporoaiot-dev://auth-callback` (debug). Drop either one and that build cannot log in.
        //
        // Scheme changes therefore need the Odoo page edited too — it is not in this repo.
        // Verify with a plain `curl https://www.apporo.ai/android` from outside our network.
        const val CLIENT_ID = "https://www.apporo.ai/android"
        const val GRANT_TYPE_CODE = "authorization_code"
        const val GRANT_TYPE_REFRESH = "refresh_token"
        const val REVOKE_ACTION = "revoke"

        const val SEGMENT_AUTH_TOKEN = "auth/token"
    }

    @FormUrlEncoded
    @POST
    suspend fun getToken(
        @Url url: HttpUrl,
        @Field("grant_type") grandType: String,
        @Field("code") code: String,
        @Field("client_id") clientId: String,
    ): Token

    @FormUrlEncoded
    @POST
    suspend fun refreshToken(
        @Url url: HttpUrl,
        @Field("grant_type") grandType: String,
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
    ): Response<Token>

    @FormUrlEncoded
    @POST
    suspend fun revokeToken(@Url url: HttpUrl, @Field("token") refreshToken: String)

    @FormUrlEncoded
    @POST
    suspend fun revokeTokenLegacy(
        @Url url: HttpUrl,
        @Field("token") refreshToken: String,
        @Field("action") action: String,
    )
}
