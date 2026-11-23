package com.hyrulecompanion.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hyrulecompanion.app.data.*
import com.hyrulecompanion.app.network.AuthSession
import com.hyrulecompanion.app.network.CloudData
import com.hyrulecompanion.app.network.SupabaseClient
import com.hyrulecompanion.app.updates.AppRelease
import com.hyrulecompanion.app.updates.AppUpdatePreferences
import com.hyrulecompanion.app.updates.AppUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.Instant

data class CloudDecision(val remote: CloudData, val initial: Boolean)

data class MuralOptions(val name: String = "Herói de Hyrule", val theme: String = "floresta", val layout: String = "retrato", val showCovers: Boolean = true)

class HyruleViewModel(application: Application) : AndroidViewModel(application) {
    val catalog = Catalog(application)
    val repo = JourneyRepository(application,catalog)
    private val supabase = SupabaseClient((application as HyruleApplication).communityMediaCache)
    private val appUpdates = AppUpdateRepository()
    internal val store = StoreRepository(this)
    private val updatePreferences = AppUpdatePreferences(application)
    private var lastUpdateCheck = 0L
    var updateChecking by mutableStateOf(false)
        private set
    var updateStatus by mutableStateOf("Verificação automática ao abrir o aplicativo.")
        private set
    var availableRelease by mutableStateOf<AppRelease?>(null)
        private set
    var showUpdateDialog by mutableStateOf(false)
        private set

    var lastSync by mutableStateOf(repo.lastCloudSync())
        private set
    var lastBackup by mutableStateOf(repo.raw("last-backup-export"))
        private set

    private fun recordSync(stamp: String) {
        repo.saveLastCloudSync(stamp)
        lastSync = stamp
    }

    var revision by mutableIntStateOf(0)
        private set
    var session by mutableStateOf(loadSession())
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var pendingCloud by mutableStateOf<CloudDecision?>(null)
        private set
    private var syncJob: Job? = null
    private var syncing = false
    private val sessionRefresh = Mutex()

    init {
        if (session != null) message = "Conta conectada. Sincronize para comparar sua jornada com a nuvem."
    }
    private fun loadSession(): AuthSession? = runCatching { JSONObject(repo.raw(K_SESSION) ?: "{}") }.getOrNull()?.let { AuthSession.fromJson(it) }
    fun notifyChange(sync: Boolean = true) {
        revision++
        if (sync && session != null) {
            syncJob?.cancel()
            syncJob = viewModelScope.launch { delay(1800); sync(automatic=true) }
        }
    }
    fun notifyMessage(value: String) { message=value }
    fun clearMessage() { message="" }
    /** Foreground check is throttled. The manual settings button always checks immediately. */
    fun checkForUpdates(manual: Boolean = false) {
        if (updateChecking) return
        val now = System.currentTimeMillis()
        if (!manual && now - lastUpdateCheck < 60 * 60 * 1000L) return
        lastUpdateCheck = now
        viewModelScope.launch {
            updateChecking = true
            if (manual) updateStatus = "Procurando novas versões..."
            try {
                val latest = appUpdates.latest()
                if (latest != null && latest.isNewer) {
                    availableRelease = latest
                    updateStatus = "Nova versão ${latest.versionName} disponível!"
                    if (manual || updatePreferences.shouldShowDialog(latest.versionCode)) {
                        showUpdateDialog = true
                    }
                } else {
                    availableRelease = null
                    updateStatus = "Você já está na versão mais recente (${BuildConfig.VERSION_NAME})."
                    showUpdateDialog = false
                }
            } catch (e: Exception) {
                updateStatus = e.message ?: "Não foi possível verificar atualizações."
                // Never block the user's journey if Supabase is unreachable.
                if (manual) message = updateStatus
            } finally {
                updateChecking = false
            }
        }
    }

    fun postponeUpdate() {
        availableRelease?.let { updatePreferences.postpone(it.versionCode) }
        showUpdateDialog = false
    }

    /** HTTPS download opens in the browser; Android still asks permission to install the APK. */
    fun openUpdateDownload(context: Context) {
        val release = availableRelease ?: return
        try {
            val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(release.downloadUrl))
                .addCategory(Intent.CATEGORY_BROWSABLE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            showUpdateDialog = false
        } catch (_: Exception) {
            message = "Não foi possível abrir o link de atualização."
        }
    }
    fun hasNewTriforce(id: String) = repo.raw("triforce-pending-$id") == "true"
    fun consumeTriforce(id: String) { repo.erase("triforce-pending-$id") }

    fun saveGame(id: String, progress: GameProgress) {
        val previouslyEarned = repo.earnedTriforce(id)
        repo.saveGame(id, progress)
        if (!previouslyEarned && repo.earnedTriforce(id)) repo.storeRaw("triforce-pending-$id", "true")
        notifyChange()
        message = "Jornada salva."
    }
    fun toggleChecked(key: String,id: String,index: Int) { repo.toggleChecked(key,id,index);notifyChange() }
    fun clearChecked(key: String,id: String) { repo.clearChecked(key,id);notifyChange() }
    fun toggleGameFavorite(id: String) { repo.toggleGameFavorite(id);notifyChange() }
    fun favorite(id: String) { repo.toggleFavorite(id);notifyChange() }
    fun note(id: String,value: String) { repo.saveGuideNote(id,value);notifyChange();message="Anotação salva." }
    fun savePreferences(preferences: Preferences) { repo.savePreferences(preferences);notifyChange() }
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(repo.exportBackup().toByteArray(Charsets.UTF_8)) } ?: error("Arquivo indisponível") }
                val stamp = OffsetDateTime.now().toString()
                repo.storeRaw("last-backup-export", stamp)
                lastBackup = stamp
                message="Backup JSON exportado com sucesso."
            } catch (e:Exception) { message=e.message ?: "Falha ao salvar backup." }
        }
    }
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                val text=withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Arquivo indisponível") }
                repo.importBackup(text);notifyChange(sync=false);message="Backup restaurado; confira sua jornada antes de sincronizar."
            } catch(e: Exception) { message=e.message ?: "Backup inválido." }
        }
    }
    fun exportMural(uri: Uri,options: MuralOptions) {
        viewModelScope.launch {
            busy=true
            try {
                withContext(Dispatchers.IO) { MuralRenderer.render(getApplication(),repo,catalog,options,uri) }
                message="Mural JPEG exportado."
            } catch(e: Exception) { message=e.message ?: "Falha ao exportar mural." }
            finally { busy=false }
        }
    }
    var presenceHidden by mutableStateOf<Boolean?>(null)
        private set
    var changingPresence by mutableStateOf(false)
        private set
    private val presenceMutex = Mutex()
    private val presenceDevice = java.util.UUID.randomUUID().toString()

    suspend fun trackPresence(account: AuthSession) {
        try {
            while (session?.userId == account.userId) {
                try {
                    presenceMutex.withLock {
                        val hidden = communityRequest(account.userId) { presenceHeartbeat(it, presenceDevice, true) }
                        if (session?.userId == account.userId) presenceHidden = hidden
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    presenceHidden = null
                }
                delay(25_000)
            }
        } finally {
            val latest = session?.takeIf { it.userId == account.userId } ?: account
            withContext(kotlinx.coroutines.NonCancellable) {
                presenceMutex.withLock {
                    runCatching { supabase.presenceHeartbeat(latest, presenceDevice, false) }
                    presenceHidden = null
                }
            }
        }
    }

    fun setPresenceHidden(hidden: Boolean) {
        val user = session?.userId ?: return
        if (changingPresence) return
        changingPresence = true
        viewModelScope.launch {
            try {
                presenceMutex.withLock {
                    communityRequest(user) { setPresenceHidden(it, hidden) }
                    if (session?.userId == user) presenceHidden = hidden
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "Não foi possível alterar a privacidade da presença."
            } finally {
                changingPresence = false
            }
        }
    }

    suspend fun <T> communityRequest(userId: String, request: suspend SupabaseClient.(AuthSession) -> T): T {
        val account = freshSession()
        check(account.userId == userId) { "A sessão mudou. Tente novamente." }
        return supabase.request(account)
    }

    private suspend fun freshSession(): AuthSession = sessionRefresh.withLock {
        val current = session ?: error("Entre na sua conta para sincronizar.")
        if (current.expiresAt > System.currentTimeMillis() + 60_000) return@withLock current
        val fresh = supabase.refresh(current.refreshToken)
        check(session === current) { "A sessão mudou. Tente novamente." }
        persistSession(fresh)
        fresh
    }
    private fun persistSession(value: AuthSession) { session=value;repo.store(K_SESSION,value.toJson()) }
    private var confirmationJob: Job? = null
    var emailConfirmed by mutableStateOf(false)
        private set

    fun dismissEmailConfirmation() { emailConfirmed = false }

    fun confirmEmail(code: String) {
        if (confirmationJob?.isActive == true) return
        val verifier = repo.raw("email-confirmation-verifier") ?: return
        confirmationJob = viewModelScope.launch {
            try {
                supabase.confirmSignup(code, verifier)
                if (repo.raw("email-confirmation-verifier") == verifier) repo.erase("email-confirmation-verifier")
                emailConfirmed = true
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                message = "Não foi possível concluir a confirmação. Abra o link do e-mail novamente."
            }
        }
    }

    private suspend fun registerEmail(email: String, password: String): AuthSession? {
        val random = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }
        val flags = android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP
        val verifier = android.util.Base64.encodeToString(random, flags)
        val hashed = java.security.MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        val challenge = android.util.Base64.encodeToString(hashed, flags)
        val previous = repo.raw("email-confirmation-verifier")
        repo.storeRaw("email-confirmation-verifier", verifier)
        try {
            return supabase.signUp(email, password, challenge).also {
                if (it != null) repo.erase("email-confirmation-verifier")
            }
        } catch (failure: Exception) {
            if (previous == null) repo.erase("email-confirmation-verifier")
            else repo.storeRaw("email-confirmation-verifier", previous)
            throw failure
        }
    }

    fun signIn(email: String,password: String,register: Boolean=false) {
        if (busy) return
        if (!email.contains('@') || password.length < (if(register)8 else 1)) { message=if(register)"Informe um e-mail válido e senha com ao menos 8 caracteres." else "Informe e-mail e senha.";return }
        viewModelScope.launch {
            busy=true
            try {
                val logged=if(register) registerEmail(email.trim(),password) else supabase.signIn(email.trim(),password)
                if (logged == null) { message="Conta criada. Confirme seu e-mail para entrar." }
                else { persistSession(logged);message="Conta conectada.";initialSync() }
            } catch(e: Exception) { message=e.message ?: "Falha de autenticação." }
            finally { busy=false }
        }
    }
    private suspend fun initialSync() {
        try {
            val s=freshSession()
            val remote=supabase.fetch(s)
            if(remote != null) {
                pendingCloud=CloudDecision(remote,true)
                message="Sua conta já possui uma jornada na nuvem. Escolha qual versão manter."
            } else {
                val stamp=supabase.push(s,repo.snapshot());recordSync(stamp)
                message="Sua jornada local foi salva na nuvem."
            }
        } catch(e: Exception) { message=e.message ?: "Falha ao sincronizar." }
    }
    private fun newerCloud(cloud: String, last: String): Boolean {
        if(last.isBlank()) return true
        return runCatching { OffsetDateTime.parse(cloud).toInstant().isAfter(OffsetDateTime.parse(last).toInstant()) }
            .getOrElse { cloud > last }
    }
    fun sync(automatic: Boolean = false) {
        if (session == null || syncing || pendingCloud != null) {
            if(!automatic && session == null) message="Entre na sua conta para sincronizar."
            return
        }
        viewModelScope.launch {
            syncing=true
            if(!automatic)busy=true
            try {
                val s=freshSession()
                val remote=supabase.fetch(s)
                if(remote != null && newerCloud(remote.updatedAt,repo.lastCloudSync())) {
                    pendingCloud=CloudDecision(remote,false)
                    if(!automatic) message="A nuvem tem uma versão diferente. Escolha qual manter."
                } else {
                    val stamp=supabase.push(s,repo.snapshot())
                    recordSync(stamp)
                    if(!automatic) message="Jornada sincronizada com sucesso."
                }
            } catch(e: Exception) { message=e.message ?: "Não foi possível sincronizar." }
            finally { syncing=false;busy=false }
        }
    }
    fun chooseCloud(useRemote: Boolean) {
        val decision=pendingCloud ?: return
        pendingCloud=null
        viewModelScope.launch {
            busy=true
            try {
                if(useRemote){
                    repo.applySnapshot(decision.remote.payload)
                    recordSync(decision.remote.updatedAt)
                    notifyChange(sync=false)
                    message="Jornada da nuvem recuperada."
                } else {
                    val s=freshSession()
                    val stamp=supabase.push(s,repo.snapshot())
                    recordSync(stamp)
                    message="Jornada local enviada para a nuvem."
                }
            } catch(e:Exception){ message=e.message ?: "Falha ao sincronizar." }
            finally { busy=false }
        }
    }
    fun cancelCloudDecision() { pendingCloud=null;message="Sincronização adiada. Seus dados locais foram preservados." }
    fun signOut() {
        val prev=session
        syncJob?.cancel()
        session=null;repo.erase(K_SESSION)
        recordSync("")
        message="Conta desconectada. Modo visitante disponível."
        if (prev != null) viewModelScope.launch {
            supabase.clearCommunityMedia()
            runCatching { supabase.signOut(prev) }
        }
    }
}
