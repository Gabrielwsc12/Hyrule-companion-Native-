package com.hyrulecompanion.app.network

import com.hyrulecompanion.app.data.CatchAttempt
import com.hyrulecompanion.app.data.Cosmetic
import com.hyrulecompanion.app.data.FishRarity
import com.hyrulecompanion.app.data.Wallet
import com.hyrulecompanion.app.data.cosmetics
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

private const val COMMUNITY_FIELDS = "user_id,handle,favorite_game,discoverable,share_journey,journey"

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

class SupabaseClient internal constructor(private val profileMedia: CommunityMediaCache) {
    @Volatile private var mediaRevisionSupported = true

    suspend fun clearCommunityMedia() = withContext(Dispatchers.IO) { profileMedia.clear() }

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
            "DELETE" -> builder.delete()
            else -> error("HTTP method unsupported")
        }
        client.newCall(builder.build()).execute().use { response ->
            val text=response.body?.string().orEmpty()
            if(!response.isSuccessful){
                val j=runCatching { JSONObject(text) }.getOrNull()
                if (j?.optString("error_code") in listOf("user_already_exists", "email_exists")) throw AccountAlreadyExists()
                throw SupabaseError(j?.optString("code").orEmpty(), j?.optString("msg")?.takeIf { it.isNotEmpty() } ?: j?.optString("message")?.takeIf { it.isNotEmpty() } ?: j?.optString("error_description")?.takeIf { it.isNotEmpty() } ?: j?.optString("error")?.takeIf { it.isNotEmpty() } ?: "Erro no Supabase: ${response.code}")
            }
            text
        }
    }
    suspend fun signIn(email: String,password: String): AuthSession {
        val body=JSONObject().put("email",email).put("password",password)
        return AuthSession.fromResponse(JSONObject(request("POST","/auth/v1/token?grant_type=password",body))) ?: error("Resposta de login inválida.")
    }
    suspend fun signUp(email: String, password: String, challenge: String): AuthSession? {
        val body = JSONObject().put("email", email).put("password", password)
            .put("code_challenge", challenge).put("code_challenge_method", "s256")
        val redirect = android.net.Uri.encode(EMAIL_CONFIRMATION_REDIRECT)
        val response = try {
            JSONObject(request("POST", "/auth/v1/signup?redirect_to=$redirect", body))
        } catch (error: IOException) {
            if (error.message.orEmpty().contains("already registered", ignoreCase = true)) throw AccountAlreadyExists()
            throw error
        }
        if ((response.optJSONObject("user") ?: response).optJSONArray("identities")?.length() == 0) {
            throw AccountAlreadyExists()
        }
        return AuthSession.fromResponse(response)
    }

    suspend fun confirmSignup(code: String, verifier: String) {
        val body = JSONObject().put("auth_code", code).put("code_verifier", verifier)
        val result = JSONObject(request("POST", "/auth/v1/token?grant_type=pkce", body))
        check(AuthSession.fromResponse(result) != null) { "Confirmação inválida." }
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
    private suspend fun storeRpc(session: AuthSession, action: String, args: JSONObject = JSONObject()): String =
        request("POST", "/rest/v1/rpc/hc_store_$action", args, session.accessToken)

    internal suspend fun storeState(session: AuthSession): Wallet =
        Wallet.from(JSONObject(storeRpc(session, "state")))

    internal suspend fun storeBuy(session: AuthSession, item: Cosmetic) {
        storeRpc(session, "buy", JSONObject().put("p_item", item.id))
    }

    internal suspend fun storeEquip(session: AuthSession, item: Cosmetic, equipped: Boolean) {
        storeRpc(session, "equip", JSONObject().put("p_item", item.id).put("p_equipped", equipped))
    }

    internal suspend fun storeCast(session: AuthSession, id: String): CatchAttempt {
        val json = JSONObject(storeRpc(session, "cast", JSONObject().put("p_cast", id)))
        return CatchAttempt(id, FishRarity.valueOf(json.getString("rarity")),
            json.getString("species"), json.getLong("wait_ms"))
    }

    internal suspend fun storeClaim(session: AuthSession, id: String): Int =
        storeRpc(session, "claim", JSONObject().put("p_cast", id)).trim().toInt()

    internal suspend fun storeRedeem(session: AuthSession, keys: List<String>): Pair<Boolean, Wallet> {
        val result = JSONObject(storeRpc(session, "redeem", JSONObject().put("p_keys", JSONArray(keys))))
        return result.getBoolean("unlocked") to Wallet.from(result.getJSONObject("wallet"))
    }

    internal suspend fun publicCosmetics(session: AuthSession, userId: String): Set<Cosmetic> =
        cosmetics(JSONArray(storeRpc(session, "public_cosmetics", JSONObject().put("p_profile", userId))))

    suspend fun communityProfile(session: AuthSession): CommunityProfile? =
        communityProfiles(session, "user_id=eq.${session.userId}").firstOrNull()

    suspend fun searchCommunity(session: AuthSession, handle: String): List<CommunityPerson> {
        require(handle.matches(Regex("[a-z0-9_]{1,24}")))
        val prefix = android.net.Uri.encode(literalHandle(handle) + "*")
        val path = "/rest/v1/hc_community_profiles?select=user_id,handle&discoverable=eq.true&user_id=neq.${session.userId}"
        val first = people(JSONArray(request("GET", "$path&handle=like.$prefix&order=handle&limit=18",
            bearer = session.accessToken)))
        if (handle.length < 2 || first.size >= 18) return first
        val related = relatedHandlePatterns(handle).joinToString(",") { "handle.like." + android.net.Uri.encode(it) }
        val extra = people(JSONArray(request("GET",
            "$path&handle=not.like.$prefix&or=($related)&order=handle&limit=${18 - first.size}",
            bearer = session.accessToken)))
        return (first + extra.sortedBy { !it.handle.contains(handle) }).distinctBy { it.userId }
    }

    private fun people(rows: JSONArray): List<CommunityPerson> = List(rows.length()) { i ->
        val row = rows.getJSONObject(i)
        CommunityPerson(row.getString("user_id"), row.getString("handle"))
    }

    private suspend fun communityProfiles(session: AuthSession, filters: String): List<CommunityProfile> {
        val rows = JSONArray(request("GET", "/rest/v1/hc_community_profiles?select=$COMMUNITY_FIELDS&$filters", bearer=session.accessToken))
        return List(rows.length()) { CommunityProfile.fromJson(rows.getJSONObject(it)) }
    }

    suspend fun presenceHeartbeat(session: AuthSession, device: String, active: Boolean): Boolean =
        request("POST", "/rest/v1/rpc/hc_presence_heartbeat",
            JSONObject().put("device", device).put("active", active), session.accessToken).trim().toBooleanStrict()

    suspend fun setPresenceHidden(session: AuthSession, hidden: Boolean) {
        request("POST", "/rest/v1/rpc/hc_presence_set_hidden", JSONObject().put("value", hidden), session.accessToken)
    }

    suspend fun communityOnline(session: AuthSession, userId: String): Boolean =
        request("POST", "/rest/v1/rpc/hc_presence_online",
            JSONObject().put("target", userId), session.accessToken).trim().toBooleanStrict()

    suspend fun communityProfileWithMedia(
        session: AuthSession, userId: String
    ): Pair<CommunityProfile?, CommunityMedia> = withContext(Dispatchers.IO) {
        val id = java.util.UUID.fromString(userId).toString()
        val key = "${session.userId}:$id"
        if (mediaRevisionSupported) {
            try {
                val cached = profileMedia.get(key)
                val args = JSONObject().put("target", id).put("known_media", cached?.revision ?: JSONObject.NULL)
                val response = request("POST", "/rest/v1/rpc/hc_read_community_profile", args, session.accessToken)
                if (response.trim() == "null" || response.isBlank()) {
                    profileMedia.remove(key)
                    return@withContext null to CommunityMedia()
                }
                val row = JSONObject(response)
                val revision = row.getString("media_revision")
                val images = if (cached != null && cached.revision == revision) cached.images
                    else readCommunityMedia(row).also { profileMedia.put(key, CachedMedia(revision, it)) }
                return@withContext CommunityProfile.fromJson(row) to images
            } catch (error: SupabaseError) {
                if (error.code != "PGRST202") throw error
                mediaRevisionSupported = false
            }
        }
        val rows = JSONArray(request("GET",
            "/rest/v1/hc_community_profiles?select=$COMMUNITY_FIELDS,avatar_image,banner_image&user_id=eq.$id",
            bearer = session.accessToken))
        val row = rows.optJSONObject(0) ?: return@withContext null to CommunityMedia()
        CommunityProfile.fromJson(row) to readCommunityMedia(row)
    }

    suspend fun saveCommunityProfile(session: AuthSession, profile: CommunityProfile, media: CommunityMedia? = null) {
        val body = profile.toJson(session.userId)
        media?.let {
            body.put("avatar_image", it.avatar ?: JSONObject.NULL)
            body.put("banner_image", it.banner ?: JSONObject.NULL)
        }
        request("POST", "/rest/v1/hc_community_profiles?on_conflict=user_id",
            body, session.accessToken, "resolution=merge-duplicates,return=minimal")
    }

    suspend fun followedProfiles(session: AuthSession): List<CommunityPerson> {
        val rows = JSONArray(request("GET",
            "/rest/v1/hc_community_follows?select=profile:hc_community_profiles!hc_community_follows_followed_id_fkey!inner(user_id,handle)&order=followed_id",
            bearer=session.accessToken))
        return List(rows.length()) { i ->
            val row = rows.getJSONObject(i).getJSONObject("profile")
            CommunityPerson(row.getString("user_id"), row.getString("handle"))
        }.sortedBy { it.handle }
    }

    suspend fun follows(session: AuthSession, userId: String): Boolean {
        val target = java.util.UUID.fromString(userId)
        val rows = JSONArray(request("GET",
            "/rest/v1/hc_community_follows?select=followed_id&followed_id=eq.$target",
            bearer=session.accessToken))
        return rows.length() > 0
    }

    suspend fun communityReactions(session: AuthSession, userId: String): Map<String, CommunityReaction> {
        val rows = JSONArray(request("POST", "/rest/v1/rpc/hc_community_reaction_counts",
            JSONObject().put("target_profile", userId), session.accessToken))
        return (0 until rows.length()).associate { index ->
            val row = rows.getJSONObject(index)
            row.getString("game_id") to CommunityReaction(row.getInt("applause_count"), row.getBoolean("applauded"))
        }
    }

    suspend fun setCommunityApplause(session: AuthSession, profileId: String, gameId: String, applaud: Boolean) {
        request("POST", "/rest/v1/rpc/hc_community_set_applause",
            JSONObject().put("target_profile", profileId).put("target_game", gameId)
                .put("is_applauded", applaud), session.accessToken)
    }

    suspend fun setFollowing(session: AuthSession, userId: String, follow: Boolean) {
        val target = java.util.UUID.fromString(userId).toString()
        if (follow) {
            request("POST", "/rest/v1/hc_community_follows?on_conflict=follower_id,followed_id",
                JSONObject().put("follower_id", session.userId).put("followed_id", target),
                session.accessToken, "resolution=ignore-duplicates,return=minimal")
        } else {
            request("DELETE", "/rest/v1/hc_community_follows?followed_id=eq.$target", bearer=session.accessToken)
        }
    }

}

data class CommunityProfile(
    val userId: String,
    val handle: String,
    val favoriteGame: String?,
    val discoverable: Boolean,
    val shareJourney: Boolean,
    val journey: Map<String, String> = emptyMap()
) {
    fun toJson(owner: String) = JSONObject()
        .put("user_id", owner)
        .put("handle", handle)
        .put("favorite_game", favoriteGame ?: JSONObject.NULL)
        .put("discoverable", discoverable)
        .put("share_journey", shareJourney)
        .put("journey", JSONObject(if (discoverable && shareJourney) journey else emptyMap<String, String>()))

    companion object {
        fun fromJson(row: JSONObject): CommunityProfile {
            val journey = row.optJSONObject("journey") ?: JSONObject()
            return CommunityProfile(row.getString("user_id"), row.getString("handle"),
                row.optString("favorite_game").takeUnless { row.isNull("favorite_game") || it.isBlank() },
                row.optBoolean("discoverable"), row.optBoolean("share_journey"),
                journey.keys().asSequence().associateWith { journey.optString(it) })
        }
    }
}

data class CommunityPerson(val userId: String, val handle: String)

private fun literalHandle(value: String) = value.replace("_", "\\_")

internal fun relatedHandlePatterns(handle: String): Set<String> = buildSet {
    add("*${literalHandle(handle)}*")
    if (handle.length < 4) return@buildSet
    for (index in handle.indices) {
        val before = literalHandle(handle.take(index))
        val after = literalHandle(handle.drop(index + 1))
        add("${before}_$after*") // One mistyped character.
        add("$before$after*") // One extra character.
        add("${before}_${literalHandle(handle.drop(index))}*") // One missing character.
        if (index + 1 < handle.length) {
            add(before + literalHandle("${handle[index + 1]}${handle[index]}") + literalHandle(handle.drop(index + 2)) + "*")
        }
    }
}

data class CommunityReaction(val count: Int, val applauded: Boolean)

data class CommunityMedia(val avatar: String? = null, val banner: String? = null)


internal const val EMAIL_CONFIRMATION_REDIRECT = "${BuildConfig.APPLICATION_ID}://auth/confirm"
private class SupabaseError(val code: String, message: String) : IOException(message)
internal class AccountAlreadyExists : IOException("Opa, você já está conectado com Hyrule. Por favor faça log in")
