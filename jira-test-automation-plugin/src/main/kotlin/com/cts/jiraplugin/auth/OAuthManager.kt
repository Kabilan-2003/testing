package com.cts.jiraplugin.auth

import com.cts.jiraplugin.PluginSettings
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object OAuthManager {
    private const val CLIENT_ID = "0giKrzpgRLaRcpxDgF9bODYxSZxsIE59"
    private const val CLIENT_SECRET = "ATOAo79ucQIOgcQ8wu4zvBczdTE7ZYXRTmels2AeiZqjLHAedHx50NIb9nmGoFYq4-ax72FD891D"
    private const val TOKEN_URL = "https://auth.atlassian.com/oauth/token"
    private val mapper = jacksonObjectMapper()

    fun exchangeCodeForTokens(code: String, redirectUri: String) {
        val params = mapOf(
            "grant_type" to "authorization_code",
            "client_id" to CLIENT_ID,
            "client_secret" to CLIENT_SECRET,
            "code" to code,
            "redirect_uri" to redirectUri
        )

        val response = postForm(TOKEN_URL, params)
        val json = mapper.readTree(response)

        val settings = PluginSettings.getInstance()
        settings.accessToken = json["access_token"].asText()
        settings.refreshToken = json["refresh_token"].asText()
        settings.tokenExpiry = System.currentTimeMillis() + (json["expires_in"].asLong() * 1000)
    }

    fun refreshAccessToken(): Boolean {
        val settings = PluginSettings.getInstance()
        val refreshToken = settings.refreshToken ?: return false

        val params = mapOf(
            "grant_type" to "refresh_token",
            "client_id" to CLIENT_ID,
            "client_secret" to CLIENT_SECRET,
            "refresh_token" to refreshToken
        )

        val response = postForm(TOKEN_URL, params)
        val json = mapper.readTree(response)

        settings.accessToken = json["access_token"].asText()
        settings.refreshToken = json["refresh_token"].asText()
        settings.tokenExpiry = System.currentTimeMillis() + (json["expires_in"].asLong() * 1000)

        return true
    }

    private fun postForm(urlStr: String, params: Map<String, String>): String {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.doOutput = true

        val body = params.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, StandardCharsets.UTF_8)}=${URLEncoder.encode(it.value, StandardCharsets.UTF_8)}"
        }

        connection.outputStream.use { os ->
            os.write(body.toByteArray(StandardCharsets.UTF_8))
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}
