package com.hyrulecompanion.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyrulecompanion.app.data.FishRarity
import com.hyrulecompanion.app.data.StoreRepository
import kotlinx.coroutines.CancellationException

private val fishNotes = mapOf(
    "Hyrule Bass" to "Encontrado em rios e lagos de Hyrule. É um dos peixes mais familiares aos viajantes.",
    "Staminoka Bass" to "Um peixe de água doce que ajuda a recuperar o vigor quando usado em receitas.",
    "Chillfin Trout" to "Prefere águas geladas. Suas escamas azuladas ajudam a preparar pratos contra o calor.",
    "Mighty Porgy" to "Peixe marinho de musculatura forte, usado em pratos que aumentam o poder de ataque.",
    "Armored Porgy" to "As escamas robustas deste peixe marinho inspiram seu nome. Favorece receitas de defesa.",
    "Sizzlefin Trout" to "Vive em águas quentes. Suas escamas vermelhas dão resistência ao frio em receitas.",
    "Voltfin Trout" to "Esta truta de escamas amarelas é usada em pratos que protegem contra eletricidade.",
    "Mighty Carp" to "Carpa de água doce que fortalece o ataque quando usada como ingrediente.",
    "Armored Carp" to "Carpa de escamas resistentes, procurada por quem quer reforçar a defesa.",
    "Glowing Cave Fish" to "Seu brilho natural ilumina as águas escuras das cavernas de Hyrule.",
    "Hearty Bass" to "Um peixe de água doce apreciado por restaurar muitos corações.",
    "Hearty Salmon" to "Um salmão robusto de águas frias, conhecido pelos seus pratos revigorantes.",
    "Stealthfin Trout" to "Peixe noturno cujas escamas ajudam a cozinhar pratos de furtividade.",
    "Sanke Carp" to "Carpa de cores marcantes que nada no lago da área de pesca de Twilight Princess.",
    "Ancient Arowana" to "Peixe ancestral encontrado perto da Corte de Mipha e nas ilhas celestes.",
    "Hylian Loach" to "Peixe comprido e esquivo que aparece no lago de pesca de Ocarina of Time e em Twilight Princess.",
    "Ordon Catfish" to "Bagre de longos barbilhões encontrado nas águas de Ordon em Twilight Princess.",
    "Reekfish" to "Peixe vermelho de cheiro forte que vive perto das Mother-and-Child Rocks, nos domínios dos Zora."
)

@Composable
internal fun AquariumScreen(store: StoreRepository, userId: String?, onBack: () -> Unit) {
    val wallet = store.wallet(userId)
    val fish = wallet?.fish.orEmpty()
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(userId, retry) {
        failed = false
        try { store.refresh(userId) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { failed = true }
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            TextButton(onClick = onBack) { Text("← Voltar") }
            ScreenTitle("Aquário de Hyrule", "Sua coleção de pesca", "◇")
            if (wallet != null) {
                Text("${fish.size} de ${fishNotes.size} espécies pescadas")
                LinearProgressIndicator(progress = { fish.size / fishNotes.size.toFloat() }, Modifier.fillMaxWidth())
            } else if (!failed) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (failed) TextButton(onClick = { retry++ }) { Text("Atualizar coleção") }
        }
        FishRarity.entries.forEach { rarity ->
            item { SmallHeading(rarity.label) }
            items(rarity.species, key = { it }) { name ->
                val caught = name in fish
                ParchmentCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FishIcon(name, Modifier.size(84.dp, 64.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text(fishNotes.getValue(name), style = MaterialTheme.typography.bodySmall)
                            Text(if (caught) "✓ Pescado" else "Ainda não pescado", color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium)
                        }
                        Badge { Text(rarity.label) }
                    }
                }
            }
        }
    }
}
