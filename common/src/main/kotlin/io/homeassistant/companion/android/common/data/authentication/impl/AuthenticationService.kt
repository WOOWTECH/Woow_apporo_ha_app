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
        // This deliberately points at the GitHub Pages copy of `docs/android/index.html`, which is
        // live and anonymously readable today, rather than at the brand domain. `aiot.apporo.ai`
        // has no DNS record yet, so pointing at it would make every sign-in fail.
        //
        // Migration condition — switch to "https://aiot.apporo.ai/android" only once all of these
        // hold: the host resolves, it serves the same `rel="redirect_uri"` link tags as
        // docs/android/index.html, and it is readable anonymously (no auth, no interstitial) from
        // outside our network. Verify with a plain `curl` from an unrelated machine first.
        //
        // Note that GitHub Pages publishes from the default branch, so a change to the redirect
        // scheme in docs/android/index.html only takes effect after it is merged to `main`.
        const val CLIENT_ID = "https://woowtech.github.io/Woow_apporo_ha_app/android"
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
