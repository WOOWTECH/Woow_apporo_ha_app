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
        // This page is not published yet. Before the app is submitted for review, confirm that this
        // address is readable without signing in and that it serves the OAuth client metadata,
        // including a `rel="redirect_uri"` link pointing at the app's auth callback deep link.
        // Home Assistant refuses the authorize request when this address cannot be fetched.
        const val CLIENT_ID = "https://aiot.apporo.ai/android"
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
