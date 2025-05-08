package com.movtery.zalithlauncher.game.account.otherserver

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.account.Account
import com.movtery.zalithlauncher.game.account.otherserver.models.AuthRequest
import com.movtery.zalithlauncher.game.account.otherserver.models.AuthResult
import com.movtery.zalithlauncher.game.account.otherserver.models.Refresh
import com.movtery.zalithlauncher.utils.string.StringUtils
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.io.IOException
import java.util.Objects
import java.util.UUID

object OtherLoginApi {
    @OptIn(ExperimentalSerializationApi::class)
    private var client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                explicitNulls = false
                coerceInputValues = true
            })
        }
    }

    private var baseUrl: String? = null

    fun setBaseUrl(baseUrl: String) {
        var url = baseUrl
        if (baseUrl.endsWith("/")) {
            url = baseUrl.dropLast(1)
        }
        OtherLoginApi.baseUrl = url
    }

    @Throws(IOException::class)
    suspend fun login(
        context: Context,
        userName: String,
        password: String,
        onSuccess: suspend (AuthResult) -> Unit = {},
        onFailed: suspend (error: String) -> Unit = {}
    ) {
        if (Objects.isNull(baseUrl)) {
            onFailed(context.getString(R.string.account_other_login_baseurl_not_set))
            return
        }

        val agent = AuthRequest.Agent(
            name = "Minecraft",
            version = 1
        )

        val authRequest = AuthRequest(
            username = userName,
            password = password,
            agent = agent,
            requestUser = true,
            clientToken = UUID.randomUUID().toString().replace("-", "")
        )

        val data = Gson().toJson(authRequest)
        callLogin(data, "/authserver/authenticate", onSuccess, onFailed)
    }

    @Throws(IOException::class)
    suspend fun refresh(
        context: Context,
        account: Account,
        select: Boolean,
        onSuccess: suspend (AuthResult) -> Unit = {},
        onFailed: suspend (error: String) -> Unit = {}
    ) {
        if (Objects.isNull(baseUrl)) {
            onFailed(context.getString(R.string.account_other_login_baseurl_not_set))
            return
        }

        val refresh = Refresh(
            clientToken = account.clientToken,
            accessToken = account.accessToken
        )

        if (select) {
            refresh.selectedProfile = Refresh.SelectedProfile(
                name = account.username,
                id = account.profileId
            )
        }

        val json = Gson().toJson(refresh)
        callLogin(json, "/authserver/refresh", onSuccess, onFailed)
    }

    private suspend fun callLogin(
        data: String,
        url: String,
        onSuccess: suspend (AuthResult) -> Unit = {},
        onFailed: suspend (error: String) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            val response: HttpResponse = client.post(baseUrl + url) {
                contentType(ContentType.Application.Json)
                setBody(data)
            }

            if (response.status == HttpStatusCode.OK) {
                val result: AuthResult = response.body()
                onSuccess(result)
            } else {
                val errorMessage = "(${response.status.value}) ${parseError(response)}"
                Log.e("Other Login", errorMessage)
                onFailed(errorMessage)
            }
        } catch (e: CancellationException) {
            Log.d("Other Login", "Login cancelled")
        } catch (e: Exception) {
            Log.e("Other Login", "Request failed", e)
            onFailed("Request failed: ${e.localizedMessage}")
        }
    }

    private suspend fun parseError(response: HttpResponse): String {
        return try {
            val res = response.bodyAsText()
            val json = JSONObject(res)
            var message = when {
                json.has("errorMessage") -> json.getString("errorMessage")
                json.has("message") -> json.getString("message")
                else -> "Unknown error"
            }
            if (message.contains("\\u")) {
                message = StringUtils.decodeUnicode(message.replace("\\\\u", "\\u"))
            }
            message
        } catch (e: Exception) {
            Log.e("Other Login", "Failed to parse error", e)
            "Unknown error"
        }
    }

    suspend fun getServeInfo(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = client.get(url)
            if (response.status == HttpStatusCode.OK) {
                response.bodyAsText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("Other Login", "Failed to get server info", e)
            null
        }
    }
}