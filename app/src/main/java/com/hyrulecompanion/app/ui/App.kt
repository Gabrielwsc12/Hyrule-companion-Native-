package com.hyrulecompanion.app.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.activity.compose.BackHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.R
import kotlinx.coroutines.delay

private data class Tab(val id:String, val title:String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
private val tabs=listOf(
    Tab("home","Início",Icons.Default.Home),
    Tab("timeline","Cronologia",Icons.Default.Schedule),
    Tab("atlas","Atlas",Icons.Default.Map),
    Tab("library","Biblioteca",Icons.Default.CollectionsBookmark),
    Tab("guides","Guias",Icons.Default.AutoStories),
    Tab("profile","Perfil",Icons.Default.Person)
)

@Composable private fun isOnline(): Boolean {
    val context=LocalContext.current
    val manager=remember(context) { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }
    fun connected():Boolean = manager.getNetworkCapabilities(manager.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)==true
    var online by remember { mutableStateOf(connected()) }
    DisposableEffect(manager) {
        val callback=object:ConnectivityManager.NetworkCallback(){
            override fun onAvailable(network: Network){online=connected()}
            override fun onLost(network: Network){online=connected()}
            override fun onCapabilitiesChanged(network:Network,capabilities:NetworkCapabilities){online=connected()}
        }
        runCatching { manager.registerDefaultNetworkCallback(callback) }
        onDispose { runCatching { manager.unregisterNetworkCallback(callback) } }
    }
    return online
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun HyruleApp(vm:HyruleViewModel) {
    val revision=vm.revision
    val prefs=vm.repo.preferences()
    var screen by rememberSaveable {
        val target = if (prefs.startScreen == "last") vm.repo.raw("last-section") else prefs.startScreen
        mutableStateOf(tabs.find { it.id == target }?.id ?: "home")
    }
    LaunchedEffect(screen) {
        if (tabs.any { it.id == screen }) vm.repo.storeRaw("last-section", screen)
    }
    var gameId by rememberSaveable { mutableStateOf<String?>(null) }
    var guideGame by rememberSaveable { mutableStateOf("") }
    var guideSequence by rememberSaveable { mutableIntStateOf(0) }
    var intro by remember { mutableStateOf(!prefs.introSkip && !prefs.reduceMotion) }
    var previous by remember { mutableStateOf("library") }
    val view = androidx.compose.ui.platform.LocalView.current
    val awake = prefs.keepAwake && !intro && gameId == null && screen in listOf("guides", "atlas")
    DisposableEffect(view, awake) {
        val original = view.keepScreenOn
        if (awake) view.keepScreenOn = true
        onDispose { if (awake) view.keepScreenOn = original }
    }
    val online=isOnline()
    val lifecycleOwner = LocalLifecycleOwner.current
    val appContext = LocalContext.current
    val welcomePrefs = remember { appContext.getSharedPreferences("welcome", Context.MODE_PRIVATE) }
    var invite by remember { mutableStateOf(!welcomePrefs.getBoolean("seen", false) && vm.session == null) }
    var openAccount by rememberSaveable { mutableStateOf(false) }
    var atlasGame by rememberSaveable { mutableStateOf("oot") }
    fun closeInvite() {
        invite = false
        welcomePrefs.edit().putBoolean("seen", true).apply()
    }
    // Check when first opened and whenever brought back from the background.
    DisposableEffect(lifecycleOwner, intro) {
        val observer = LifecycleEventObserver { _, event ->
            if (!intro && event == Lifecycle.Event.ON_START) vm.checkForUpdates()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (!intro) vm.checkForUpdates()
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(gameId!=null || screen=="settings" || screen=="community") {
        if(gameId!=null){gameId=null;screen=previous}else screen="profile"
    }
    HyruleTheme(prefs.theme=="dark" || (prefs.theme=="system" && androidx.compose.foundation.isSystemInDarkTheme())) {
        if(intro) {
            IntroScreen(prefs.introMusic,onFinish={intro=false})
        } else {
            if (invite && vm.session == null) AlertDialog(
                onDismissRequest = { closeInvite() },
                title = { Text("Quer salvar sua jornada em uma conta?") },
                text = { Text("Criar uma conta é opcional. Com ela você pode sincronizar seu progresso entre dispositivos. Como visitante, sua jornada fica salva neste aparelho.") },
                confirmButton = { TextButton(onClick={ closeInvite(); openAccount=true; screen="profile" }) { Text("Criar conta") } },
                dismissButton = { TextButton(onClick={ closeInvite() }) { Text("Continuar como visitante") } }
            )
            Scaffold(
                topBar={
                    Column {
                        TopAppBar(title={ Text("▲ Hyrule Companion",style=MaterialTheme.typography.titleLarge) },actions={
                            IconButton(onClick={gameId=null;screen="settings"}) { Icon(Icons.Default.Settings,contentDescription="Configurações") }
                        },colors=TopAppBarDefaults.topAppBarColors(containerColor=MaterialTheme.colorScheme.primaryContainer))
                        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal=16.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(if(online)"● Online" else "● Offline",color=if(online)Color(0xFF4E976C) else Color(0xFFBA723B),style=MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.weight(1f))
                            Text(if(vm.session!=null)"☁ Conta conectada" else "♜ Modo visitante",style=MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                bottomBar={
                    if(screen!="settings" && screen!="community" && gameId==null) NavigationBar(containerColor=MaterialTheme.colorScheme.primaryContainer,tonalElevation=7.dp) {
                        tabs.forEach { tab -> NavigationBarItem(selected=screen==tab.id,onClick={openAccount=false;screen=tab.id},icon={Icon(tab.icon,contentDescription=tab.title)},label={Text(tab.title,maxLines=1,style=MaterialTheme.typography.labelSmall)},alwaysShowLabel=false) }
                    }
                }
            ){ inner ->
                Column(Modifier.fillMaxSize().padding(inner)) {
                    if(vm.message.isNotBlank()) Surface(Modifier.fillMaxWidth().clickable{vm.clearMessage()},color=MaterialTheme.colorScheme.secondaryContainer) {
                        Row(Modifier.padding(horizontal=14.dp,vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(vm.message,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
                            IconButton(onClick=vm::clearMessage,modifier=Modifier.size(28.dp)) { Icon(Icons.Default.Close,contentDescription="Fechar",modifier=Modifier.size(16.dp)) }
                        }
                    }
                    when {
                        gameId!=null -> GameDetailScreen(vm,gameId!!,onBack={gameId=null;screen=previous},onAtlas={id->atlasGame=id;gameId=null;screen="atlas"},onGuides={id->gameId=null;guideGame=id;guideSequence++;screen="guides"})
                        screen=="home" -> HomeScreen(vm,onNavigate={screen=it},onGame={previous="home";gameId=it})
                        screen=="timeline" -> TimelineScreen(vm,onGame={previous="timeline";gameId=it})
                        screen=="atlas" -> AtlasScreen(vm,atlasGame,onGame={previous="atlas";gameId=it})
                        screen=="library" -> LibraryScreen(vm,onGame={previous="library";gameId=it})
                        screen=="guides" -> key(guideSequence){
                            CompositionLocalProvider(LocalSpoilerProtection provides prefs.hideSpoilers) {
                                GuidesScreen(vm,guideGame,onGame={previous="guides";gameId=it})
                            }
                        }
                        screen=="profile" -> ProfileScreen(vm,openAccount=openAccount,onSettings={screen="settings"},onCommunity={screen="community"})
                        screen=="community" -> CommunityScreen(vm,onBack={screen="profile"},onAccount={openAccount=true;screen="profile"})
                        screen=="settings" -> SettingsScreen(vm,onBack={screen="profile"})
                    }
                }
            }
        }
        if (!intro && !invite && vm.pendingCloud == null && vm.showUpdateDialog) {
            vm.availableRelease?.let { release ->
                AlertDialog(
                    onDismissRequest = vm::postponeUpdate,
                    icon = { Icon(Icons.Default.SystemUpdate, contentDescription = null) },
                    title = { Text("Nova versão de Hyrule Companion!") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Versão ${release.versionName} disponível (atual: ${com.hyrulecompanion.app.BuildConfig.VERSION_NAME}).")
                            if (release.notes.isNotBlank()) Text(release.notes)
                            Text("Toque em Atualizar agora para baixar o APK. O Android solicitará a confirmação da instalação.", style = MaterialTheme.typography.bodySmall)
                            if (release.mandatory) Text("Atualização importante recomendada.", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    confirmButton = {
                        Button(onClick = { vm.openUpdateDownload(appContext) }) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Atualizar agora")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = vm::postponeUpdate) { Text("Lembrar depois") }
                    }
                )
            }
        }
        vm.pendingCloud?.let { decision ->
            AlertDialog(
                onDismissRequest=vm::cancelCloudDecision,
                icon={Icon(Icons.Default.CloudSync,contentDescription=null)},
                title={Text(if(decision.initial)"Jornada encontrada no Supabase" else "Escolha a versão da jornada")},
                text={Text("Uma jornada está salva nesta conta. Selecione Recuperar nuvem para substituir os dados deste aparelho ou Enviar local para substituir a cópia na nuvem. Recomenda-se exportar um backup JSON antes de escolher.")},
                confirmButton={TextButton(onClick={vm.chooseCloud(true)}){Text("Recuperar nuvem")}},
                dismissButton={Column {TextButton(onClick={vm.chooseCloud(false)}){Text("Enviar local")};TextButton(onClick=vm::cancelCloudDecision){Text("Decidir depois")}}}
            )
        }
    }
}

@Composable private fun IntroScreen(withMusic:Boolean,onFinish:()->Unit) {
    val context=LocalContext.current
    DisposableEffect(withMusic) {
        val player=if(withMusic) runCatching { MediaPlayer.create(context,R.raw.hyrule_intro)?.apply { setOnCompletionListener{onFinish()};start() } }.getOrNull() else null
        onDispose { player?.stop();player?.release() }
    }
    LaunchedEffect(Unit) { delay(4200);onFinish() }
    Column(Modifier.fillMaxSize().background(DeepForest).padding(24.dp),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){
        Canvas(Modifier.size(190.dp)) {
            val gold=Gold
            fun triangle(centerX:Float,topY:Float,side:Float){
                val path=Path().apply { moveTo(centerX,topY);lineTo(centerX-side/2,topY+side*.866f);lineTo(centerX+side/2,topY+side*.866f);close() }
                drawPath(path,gold)
            }
            val side=size.width*.47f
            triangle(size.width*.5f,size.height*.06f,side)
            triangle(size.width*.26f,size.height*.51f,side)
            triangle(size.width*.74f,size.height*.51f,side)
        }
        Spacer(Modifier.height(28.dp))
        Text("HYRULE",fontFamily=FontFamily.Serif,fontSize=45.sp,color=Gold,fontWeight=FontWeight.Bold)
        Text("COMPANION",fontFamily=FontFamily.Serif,fontSize=30.sp,color=Parchment,fontWeight=FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text("Explore · Complete · Conquiste",color=Gold,textAlign=TextAlign.Center)
        Spacer(Modifier.height(80.dp))
        OutlinedButton(onClick=onFinish){Text("Pular introdução",color=Parchment)}
    }
}
