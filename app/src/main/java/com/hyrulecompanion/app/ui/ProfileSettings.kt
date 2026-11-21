package com.hyrulecompanion.app.ui

import androidx.compose.foundation.selection.toggleable
import android.Manifest
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import com.hyrulecompanion.app.BuildConfig
import com.hyrulecompanion.app.updates.AppUpdatePreferences
import com.hyrulecompanion.app.updates.AppUpdateScheduler
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.hyrulecompanion.app.HyruleViewModel
import com.hyrulecompanion.app.MuralOptions
import com.hyrulecompanion.app.data.*
import java.time.LocalDate

@Composable internal fun Avatar(value:String,reduceMotion:Boolean,modifier:Modifier=Modifier){
    val context = LocalContext.current
    val request = remember(value, context, reduceMotion) {
        val bytes = runCatching {
            Base64.decode(value.substringAfter("base64,", ""), Base64.DEFAULT)
        }.getOrNull()?.takeIf { it.isNotEmpty() }
        bytes?.let { image ->
            coil.request.ImageRequest.Builder(context).data(image).apply {
                if (image.size >= 6 && String(image, 0, 3, Charsets.US_ASCII) == "GIF") {
                    if (reduceMotion) decoderFactory(coil.decode.BitmapFactoryDecoder.Factory())
                    else if (Build.VERSION.SDK_INT >= 28) decoderFactory(coil.decode.ImageDecoderDecoder.Factory())
                    else decoderFactory(coil.decode.GifDecoder.Factory())
                }
            }.build()
        }
    }
    if (request != null) coil.compose.AsyncImage(model = request, contentDescription = "Foto do aventureiro",
        contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = modifier.clip(CircleShape))
    else Surface(modifier=modifier,shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer){Box(contentAlignment=Alignment.Center){Text("🗡️",style=MaterialTheme.typography.headlineMedium)}}
}

@Composable fun ProfileScreen(vm:HyruleViewModel,onSettings:()->Unit, openAccount:Boolean=false,onCommunity:()->Unit){
    val revision=vm.revision
    val prefs=vm.repo.preferences()
    val userId = vm.session?.userId
    var communityHandle by remember(userId) { mutableStateOf<String?>(null) }
    var banner by remember(userId) { mutableStateOf<String?>(null) }
    val completed=vm.repo.doneCount()
    val playing=vm.repo.playingCount()
    val started=vm.catalog.games.filter { vm.repo.gameProgress(it.id).status!="Não iniciado" || vm.repo.earnedTriforce(it.id) }
    val perfect=vm.catalog.games.count { vm.repo.gameProgress(it.id).status=="100%" }
    var name by rememberSaveable { mutableStateOf("Herói de Hyrule") }
    var muralOptions by remember { mutableStateOf<MuralOptions?>(null) }
    var exportOptions by remember { mutableStateOf<MuralOptions?>(null) }
    val avatarLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)vm.changeAvatar(uri)}
    val jpegLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/jpeg")){uri -> if(uri!=null)exportOptions?.let{vm.exportMural(uri,it)};exportOptions=null}
    LazyColumn(Modifier.fillMaxSize(),state=androidx.compose.foundation.lazy.rememberLazyListState(initialFirstVisibleItemIndex=if(openAccount) 2 else 0),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item {ScreenTitle("Meu Perfil","Seu registro como aventureiro de Hyrule","♜")}
        item {ParchmentCard {
            ProfileBanner(banner)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)){
                Avatar(prefs.avatar,prefs.reduceMotion,Modifier.size(86.dp))
                Column(Modifier.weight(1f)){
                    Text(communityHandle?.let { "@$it" } ?: "Herói de Hyrule",style=MaterialTheme.typography.titleLarge)
                    Text("Explorador em treinamento")
                    Text("${completed*100/vm.catalog.games.size}% da biblioteca concluída",style=MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick=onSettings){Icon(Icons.Default.Settings,contentDescription="Configurações")}
            }
            OutlinedButton(onClick={avatarLauncher.launch(arrayOf("image/png","image/jpeg","image/webp","image/gif"))}){Text("Escolher foto ou GIF")}
            Text("Formatos: JPG, PNG, WebP e GIF. Limite: 1,8 MB.",style=MaterialTheme.typography.labelSmall)
            if(prefs.avatar.isNotBlank())TextButton(onClick=vm::removeAvatar){Text("Remover foto")}
            if (userId != null) key(userId) {
                ProfileCommunitySettings(vm, userId, prefs.avatar) { handle, image ->
                    communityHandle = handle
                    banner = image
                }
            }
            LinearProgressIndicator(progress={completed.toFloat()/vm.catalog.games.size},modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                StatsTile("${started.size}","Iniciados",Modifier.weight(1f))
                StatsTile("$completed","Concluídos",Modifier.weight(1f))
                StatsTile("$perfect","100%",Modifier.weight(1f))
            }
        }}
        item {AccountPanel(vm)}
        item {ParchmentCard {
            SmallHeading("Comunidade")
            Text("Encontre outros aventureiros e compartilhe a sua jornada.")
            OutlinedButton(onClick=onCommunity,modifier=Modifier.fillMaxWidth()){Text("Abrir comunidade")}
        }}
        item {ParchmentCard {
            SmallHeading("✦ Meu mural de Hyrule")
            Text("Crie uma imagem JPEG com seu progresso e suas aventuras concluídas.")
            Text("$completed de ${vm.catalog.games.size} jogos concluídos • $playing em andamento")
            Button(onClick={muralOptions=MuralOptions(name=name)},modifier=Modifier.fillMaxWidth()){Text("Personalizar e exportar mural")}
        }}
        item{SmallHeading("Minha jornada")}
        if(started.isEmpty())item {ParchmentCard {Text("Você ainda não iniciou nenhuma aventura. Abra a Biblioteca e registre seu primeiro jogo.")}}
        items(started,key={it.id}){g->
            val p=vm.repo.gameProgress(g.id)
            ParchmentCard {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    CoverArt(g,Modifier.size(70.dp,82.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text(g.name,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                            TriforceBadge(vm,g.id,animateNew=true)
                        }
                        StatusTag(p.status)
                        if(p.startDate.isNotBlank())Text("Início: ${p.startDate}",style=MaterialTheme.typography.labelSmall)
                        if(p.finishDate.isNotBlank())Text("Conclusão: ${p.finishDate}",style=MaterialTheme.typography.labelSmall)
                        if(p.notes.isNotBlank())Text(p.notes,style=MaterialTheme.typography.bodySmall,maxLines=3)
                    }
                }
            }
        }
    }
    if(muralOptions!=null) {
        var draft by remember(muralOptions) { mutableStateOf(muralOptions!!) }
        AlertDialog(onDismissRequest={muralOptions=null},title={Text("Exportar minha jornada")},text={
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                OutlinedTextField(draft.name,{draft=draft.copy(name=it)},label={Text("Nome do herói")},singleLine=true)
                Text("Tema",style=MaterialTheme.typography.labelLarge)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    listOf("floresta" to "Floresta","noite" to "Noite","pergaminho" to "Pergaminho","oceano" to "Grande Mar","vulcao" to "Montanha da Morte","crepusculo" to "Crepúsculo").forEach{(k,v)->FilterChip(selected=draft.theme==k,onClick={draft=draft.copy(theme=k)},label={Text(v)})}
                }
                Text("Formato",style=MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    FilterChip(selected=draft.layout=="retrato",onClick={draft=draft.copy(layout="retrato")},label={Text("Retrato")})
                    FilterChip(selected=draft.layout=="paisagem",onClick={draft=draft.copy(layout="paisagem")},label={Text("Paisagem")})
                }
                Row(verticalAlignment=Alignment.CenterVertically){Checkbox(draft.showCovers,onCheckedChange={draft=draft.copy(showCovers=it)});Text("Incluir capas, quando disponíveis")}
            }
        },confirmButton={TextButton(onClick={exportOptions=draft;muralOptions=null;jpegLauncher.launch("mural-hyrule-${LocalDate.now()}.jpg")}){Text("Exportar JPEG")}},dismissButton={TextButton(onClick={muralOptions=null}){Text("Cancelar")}})
    }
}

@Composable private fun AccountPanel(vm:HyruleViewModel){
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val session=vm.session
    ParchmentCard {
        SmallHeading("Conta e Sincronização")
        if(session==null){
            Text("Entre para sincronizar sua jornada com a mesma conta do aplicativo original. Sem login, o modo visitante funciona neste dispositivo.")
            OutlinedTextField(email,{email=it.trim()},label={Text("E-mail")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(password,{password=it},label={Text("Senha")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={vm.signIn(email,password)},enabled=!vm.busy,modifier=Modifier.weight(1f)){Text("Entrar")}
                OutlinedButton(onClick={vm.signIn(email,password,true)},enabled=!vm.busy,modifier=Modifier.weight(1f)){Text("Criar conta")}
            }
        } else {
            Text("Conectado como ${session.email}",fontWeight=FontWeight.SemiBold)
            Text(syncStatus(vm.lastSync),style=MaterialTheme.typography.bodySmall)
            Button(onClick={vm.sync()},enabled=!vm.busy,modifier=Modifier.fillMaxWidth()){
                Icon(Icons.Default.CloudSync,contentDescription=null)
                Spacer(Modifier.width(8.dp))
                Text("Sincronizar agora")
            }
            OutlinedButton(onClick=vm::signOut,modifier=Modifier.fillMaxWidth()){Text("Sair da conta")}
        }
        if(vm.busy)LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
    }
}

@Composable fun SettingsScreen(vm:HyruleViewModel,onBack:()->Unit){
    val revision=vm.revision
    val prefs=vm.repo.preferences()
    val context = LocalContext.current
    val updatePrefs = remember(context) { AppUpdatePreferences(context) }
    var noticesEnabled by remember { mutableStateOf(updatePrefs.backgroundNotices) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        noticesEnabled = granted
        updatePrefs.backgroundNotices = granted
        if(granted) AppUpdateScheduler.checkOnce(context)
    }
    val exportLauncher=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->if(uri!=null)vm.exportBackup(uri)}
    var importUri by remember { mutableStateOf<Uri?>(null) }
    val importLauncher=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->importUri=uri}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item {TextButton(onClick=onBack){Text("← Voltar ao perfil")}}
        item {ScreenTitle("Configurações","Personalize sua experiência em Hyrule","⚙")}
        item {ParchmentCard {
            SmallHeading("Aparência")
            ChoiceSetting("Tema do aplicativo", prefs.theme, listOf("system" to "Usar tema do aparelho", "light" to "Claro", "dark" to "Escuro")) {
                vm.savePreferences(prefs.copy(theme=it))
            }
            ChoiceSetting("Estilo inspirado em Zelda", prefs.palette, gamePalettes) {
                vm.savePreferences(prefs.copy(palette=it))
            }
            Text("O texto acompanha o tamanho escolhido nas configurações do Android.")
            TextButton(onClick={
                val intent = android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                try {
                    context.startActivity(intent)
                } catch (_: android.content.ActivityNotFoundException) {
                    vm.notifyMessage("Abra as configurações de tela do Android para ajustar o tamanho da fonte.")
                }
            }) { Text("Ajustar tamanho do texto no Android") }
            SettingSwitch("Reduzir animações", "Pula a introdução e mostra o primeiro quadro do GIF de perfil.", prefs.reduceMotion) {
                vm.savePreferences(prefs.copy(reduceMotion=it))
            }
        }}
        item {ParchmentCard {
            SmallHeading("Durante o jogo")
            SettingSwitch("Proteção contra spoilers", "Revele cada dica, solução e recompensa quando quiser.", prefs.hideSpoilers) {
                vm.savePreferences(prefs.copy(hideSpoilers=it))
            }
            SettingSwitch("Manter tela ligada", "Enquanto você consulta os guias, as dungeons ou o Atlas no aplicativo.", prefs.keepAwake) {
                vm.savePreferences(prefs.copy(keepAwake=it))
            }
        }}
        item {ParchmentCard {
            SmallHeading("Abertura do aplicativo")
            ChoiceSetting("Tela ao abrir", prefs.startScreen, listOf("home" to "Início", "library" to "Biblioteca", "atlas" to "Atlas", "last" to "Última seção usada")) {
                vm.savePreferences(prefs.copy(startScreen=it))
            }
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){Text("Pular a introdução ao abrir o app");Text("Iniciar diretamente na tela principal",style=MaterialTheme.typography.bodySmall)}
                Switch(checked=prefs.introSkip,onCheckedChange={vm.savePreferences(prefs.copy(introSkip=it))})
            }
            HorizontalDivider()
            Row(verticalAlignment=Alignment.CenterVertically){
                Column(Modifier.weight(1f)){Text("Tocar música da introdução");Text("Trecho de 4 segundos do arquivo original",style=MaterialTheme.typography.bodySmall)}
                Switch(checked=prefs.introMusic,onCheckedChange={vm.savePreferences(prefs.copy(introMusic=it))},enabled=!prefs.introSkip)
            }
            Text("As preferências serão aplicadas na próxima abertura.",style=MaterialTheme.typography.labelSmall)
        }}
        item {ParchmentCard {
            SmallHeading("Backup e sincronização")
            Text(if (vm.session == null) "Modo visitante · sua jornada é salva neste aparelho." else "Conta conectada: ${vm.session?.email}")
            if (vm.session != null) {
                Text(syncStatus(vm.lastSync))
                Button(onClick={vm.sync()},enabled=!vm.busy && vm.pendingCloud == null) { Text("Sincronizar agora") }
            }
            val lastBackup = vm.lastBackup
            Text(if (lastBackup.isNullOrBlank()) "Nenhuma exportação registrada neste aparelho." else "Último backup exportado: ${localTimestamp(lastBackup)}")
            Text("Os arquivos JSON são compatíveis com o formato de backup da Web Edition 1.16.6 (schema 13).")
            Button(onClick={exportLauncher.launch("backup-jornada-hyrule-${LocalDate.now()}.json")},modifier=Modifier.fillMaxWidth()) {Text("⬇ Exportar backup JSON")}
            OutlinedButton(onClick={importLauncher.launch(arrayOf("application/json","text/plain","application/octet-stream"))},modifier=Modifier.fillMaxWidth()){Text("⬆ Restaurar backup JSON")}
            Text("A importação substitui os dados locais da jornada, dos guias, do Atlas, das dungeons e das preferências.",style=MaterialTheme.typography.bodySmall)
        }}
        item {ParchmentCard {
            SmallHeading("▲ Atualizações do aplicativo")
            Text("Versão instalada: ${BuildConfig.VERSION_NAME} (código ${BuildConfig.VERSION_CODE})")
            Text("O Hyrule Companion procura versões novas automaticamente ao abrir. Se houver atualização, aparecerá um aviso com as novidades e um link seguro para baixar o APK.")
            Text(vm.updateStatus,style=MaterialTheme.typography.bodySmall)
            Button(onClick={vm.checkForUpdates(manual=true)},enabled=!vm.updateChecking,modifier=Modifier.fillMaxWidth()) {
                Text(if(vm.updateChecking) "Verificando..." else "↻ Verificar atualizações agora")
            }
            vm.availableRelease?.takeIf { it.isNewer }?.let { release ->
                OutlinedButton(onClick={vm.openUpdateDownload(context)},modifier=Modifier.fillMaxWidth()) {
                    Text("⬇ Baixar ${release.versionName}")
                }
            }
            HorizontalDivider()
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Notificações em segundo plano")
                    Text("Avisar sobre novas versões mesmo sem o app aberto. O Android pode atrasar essas verificações.",style=MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked=noticesEnabled && AppUpdateScheduler.notificationsAllowed(context),
                    onCheckedChange={enabled ->
                        if(!enabled) {
                            noticesEnabled=false
                            updatePrefs.backgroundNotices=false
                        } else if(Build.VERSION.SDK_INT >= 33 && !AppUpdateScheduler.notificationsAllowed(context)) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            noticesEnabled=true
                            updatePrefs.backgroundNotices=true
                            AppUpdateScheduler.checkOnce(context)
                        }
                    }
                )
            }
            Text("A instalação sempre depende da sua confirmação e do APK assinado com a mesma chave da versão anterior.",style=MaterialTheme.typography.labelSmall)
        }}
        item {ParchmentCard {
            SmallHeading("Conectividade e armazenamento")
            Text("Os dados locais funcionam sem internet. Login, download de capas e sincronização precisam de conexão.")
            Text("Armazenamento protegido da aplicação Android; o cache web não é compartilhado automaticamente.")
            Text("Banco remoto: public.hc_user_data • JSON schema 13",style=MaterialTheme.typography.labelSmall)
        }}
    }
    if(importUri!=null)AlertDialog(onDismissRequest={importUri=null},title={Text("Restaurar backup?")},text={Text("Os dados atuais deste dispositivo serão substituídos pelos do arquivo. Se necessário, faça um backup antes de prosseguir.")},confirmButton={TextButton(onClick={importUri?.let(vm::importBackup);importUri=null}){Text("Restaurar")}},dismissButton={TextButton(onClick={importUri=null}){Text("Cancelar")}})
}

private fun localTimestamp(value: String): String = runCatching {
    val local = java.time.OffsetDateTime.parse(value).atZoneSameInstant(java.time.ZoneId.systemDefault())
    val format = if (local.toLocalDate() == LocalDate.now()) "'hoje às' HH:mm" else "dd/MM/yyyy 'às' HH:mm"
    local.format(java.time.format.DateTimeFormatter.ofPattern(format))
}.getOrDefault(value)

private fun syncStatus(timestamp: String) = if (timestamp.isBlank()) "Aguardando a primeira sincronização."
    else "Última sincronização: ${localTimestamp(timestamp)}"

@Composable private fun ChoiceSetting(title: String, value: String, choices: List<Pair<String,String>>, onChange: (String)->Unit) {
    var expanded by remember { mutableStateOf(false) }
    Text(title, style=MaterialTheme.typography.labelLarge)
    Box {
        OutlinedButton(onClick={expanded=true}) { Text(choices.find { it.first==value }?.second ?: choices.first().second) }
        DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
            choices.forEach { (id,label) -> DropdownMenuItem(text={Text(label)},onClick={expanded=false; onChange(id)}) }
        }
    }
}

@Composable private fun SettingSwitch(title:String, description:String, checked:Boolean, onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value=checked,role=androidx.compose.ui.semantics.Role.Switch,onValueChange=onChange),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title); Text(description,style=MaterialTheme.typography.bodySmall) }
        Switch(checked=checked,onCheckedChange=null)
    }
}
