package com.hyrulecompanion.app.updates

import com.hyrulecompanion.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

/** Public release metadata only. Journey and authentication data are never modified here. */
data class AppRelease(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val downloadUrl: String,
    val mandatory: Boolean
) {
    val isNewer: Boolean get() = versionCode > BuildConfig.VERSION_CODE
}

/** Reads public, admin-managed versions from the existing Supabase project. */
class AppUpdateRepository {
    private val http = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun latest(): AppRelease? = withContext(Dispatchers.IO) {
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') +
            "/rest/v1/hc_app_releases?select=version_code,version_name,notes,download_url,mandatory" +
            "&active=eq.true&order=version_code.desc&limit=1"
        val request = Request.Builder()
            .get()
            .url(url)
            .header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 404 || body.contains("PGRST205")) {
                    throw IOException("O catálogo de atualizações ainda não foi ativado no Supabase.")
                }
                throw IOException("Não foi possível verificar atualizações (HTTP ${response.code}).")
            }
            val releases = JSONArray(body)
            if (releases.length() == 0) return@withContext null
            val row = releases.getJSONObject(0)
            val code = row.getInt("version_code")
            val name = row.getString("version_name").trim()
            val download = row.getString("download_url").trim()
            val uri = runCatching { URI(download) }.getOrNull()
            // The update button only navigates to an explicitly published HTTPS link.
            if (code <= 0 || name.isBlank() || uri?.scheme?.lowercase() != "https" ||
                uri?.host.isNullOrBlank() || uri?.userInfo != null
            ) throw IOException("Os dados da versão publicada são inválidos.")
            AppRelease(
                versionCode = code,
                versionName = name.take(64),
                notes = row.optString("notes", "Correções e melhorias para sua jornada.").take(1500),
                downloadUrl = download,
                mandatory = row.optBoolean("mandatory", false)
            )
        }
    }
}
