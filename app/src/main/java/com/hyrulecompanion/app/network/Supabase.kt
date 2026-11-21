package com.hyrulecompanion.app.network

import com.hyrulecompanion.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private val JSON_TYPE="application/json; charset=utf-8".toMediaType()

data class AuthSession(val accessToken: String, val refreshToken: String, val expiresAt: Long, val userId: String, val email: String) {
    fun toJson() = JSONObject().put("access_token",accessToken).put("refresh_token",refreshToken).put("expires_at",expiresAt).put("user", JSONObject().put("id",userId).put("email",email))
    companion object {
        fun fromJson(json: JSONObject): AuthSession? {
            val user = json.optJSONObject("user") ?: return null
            val access=json.optString("access_token")
            val refresh=json.optString("refresh_token")
            val id=user.optString("id")
            if(access.isBlank() || refresh.isBlank() || id.isBlank()) return null
            return AuthSession(access,refresh,json.optLong("expires_at",0),id,user.optString("email"))
        }
        fun fromResponse(json: JSONObject): AuthSession? {
            val user=json.optJSONObject("user") ?: return null
            if (json.optString("access_token").isBlank()) return null
            return AuthSession(json.getString("access_token"),json.optString("refresh_token"),System.currentTimeMillis() + json.optLong("expires_in",3600)*1000,user.getString("id"),user.optString("email"))
        }
    }
}

data class CloudData(val payload: JSONObject, val updatedAt: String)

class SupabaseClient {
    private val client = OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).build()
    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val apiKey = BuildConfig.SUPABASE_ANON_KEY
    private suspend fun request(method: String, endpoint: String, body: JSONObject? = null, bearer: String? = null, prefer: String? = null): String = withContext(Dispatchers.IO) {
        val builder=Request.Builder().url(base+endpoint).header("apikey",apiKey).header("Accept","application/json")
        if(bearer != null) builder.header("Authorization","Bearer $bearer")
        if(prefer != null) builder.header("Prefer",prefer)
        val payload=(body?.toString() ?: "{}").toRequestBody(JSON_TYPE)
        when(method){
            "POST" -> builder.post(payload)
            "GET" -> builder.get()
            else -> error("HTTP method unsupported")
        }
        client.newCall(builder.build()).execute().use { response ->
            val text=response.body?.string().orEmpty()
            if(!response.isSuccessful){
                val j=runCatching { JSONObject(text) }.getOrNull()
                throw IOException(j?.optString("msg")?.takeIf { it.isNotEmpty() } ?: j?.optString("message")?.takeIf { it.isNotEmpty() } ?: j?.optString("error_description")?.takeIf { it.isNotEmpty() } ?: j?.optString("error")?.takeIf { it.isNotEmpty() } ?: "Erro no Supabase: ${response.code}")
            }
            text
        }
    }
    suspend fun signIn(email: String,password: String): AuthSession {
        val body=JSONObject().put("email",email).put("password",password)
        return AuthSession.fromResponse(JSONObject(request("POST","/auth/v1/token?grant_type=password",body))) ?: error("Resposta de login inválida.")
    }
    suspend fun signUp(email: String,password: String): AuthSession? {
        val body=JSONObject().put("email",email).put("password",password)
        return AuthSession.fromResponse(JSONObject(request("POST","/auth/v1/signup",body)))
    }
    suspend fun refresh(token: String): AuthSession = AuthSession.fromResponse(JSONObject(request("POST","/auth/v1/token?grant_type=refresh_token",JSONObject().put("refresh_token",token)))) ?: error("Não foi possível renovar a sessão.")
    suspend fun signOut(session: AuthSession) { request("POST","/auth/v1/logout",bearer=session.accessToken) }
    suspend fun fetch(session: AuthSession): CloudData? {
        val text=request("GET","/rest/v1/hc_user_data?select=payload,updated_at&user_id=eq.${session.userId}",bearer=session.accessToken)
        val rows=JSONArray(text)
        if(rows.length()==0) return null
        val row=rows.getJSONObject(0)
        return CloudData(row.getJSONObject("payload"),row.optString("updated_at"))
    }
    suspend fun push(session: AuthSession,payload: JSONObject): String {
        val now=java.time.OffsetDateTime.now().toString()
        request("POST","/rest/v1/hc_user_data?on_conflict=user_id",JSONObject().put("user_id",session.userId).put("payload",payload).put("updated_at",now),bearer=session.accessToken,prefer="resolution=merge-duplicates,return=minimal")
        return now
    }
}
