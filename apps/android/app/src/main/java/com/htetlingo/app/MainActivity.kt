package com.htetlingo.app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import java.util.Locale

data class VocabFlashcard(
    val language: String,
    val locale: Locale,
    val word: String,
    val reading: String,
    val meaningEn: String,
    val meaningMy: String
)

private val SAMPLE_FLASHCARDS = listOf(
    VocabFlashcard("English", Locale.US, "Resilience", "/rɪˈzɪl.jəns/", "Capacity to recover quickly", "ကြံ့ခိုင်စွာ ပြန်လည်နာလန်ထူနိုင်စွမ်း"),
    VocabFlashcard("Japanese", Locale.JAPANESE, "言葉", "ことば (kotoba)", "Word / Language", "စကားလုံး၊ ဘာသာစကား"),
    VocabFlashcard("Chinese", Locale.SIMPLIFIED_CHINESE, "学习", "xuéxí", "To study / learn", "သင်ယူလေ့လာသည်"),
    VocabFlashcard("Korean", Locale.KOREAN, "언어", "eoneo", "Language", "ဘာသာစကား"),
    VocabFlashcard("Burmese", Locale("my", "MM"), "မင်္ဂလာပါ", "ming-ga-la-ba", "Hello", "နှုတ်ခွန်းဆက်စကား")
)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize RevenueCat SDK (Free, Premium, Lifetime, Language Packs)
        runCatching {
            Purchases.configure(
                PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build()
            )
        }

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }

        val dictHelper = DictionaryDbHelper(this)

        setContent {
            MaterialTheme {
                HtetLingoAndroidApp(
                    dictHelper = dictHelper,
                    onSpeak = { text, locale ->
                        tts?.language = locale
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hl_tts")
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtetLingoAndroidApp(
    dictHelper: DictionaryDbHelper,
    onSpeak: (String, Locale) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var xp by remember { mutableIntStateOf(25) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HtetLingo", fontWeight = FontWeight.Bold) },
                actions = {
                    AssistChip(onClick = {}, label = { Text("$xp XP") })
                    Spacer(Modifier.width(12.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Dictionary") },
                    label = { Text("Dict") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.Style, contentDescription = "Vocabulary") },
                    label = { Text("Vocab") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.School, contentDescription = "Grammar") },
                    label = { Text("Grammar") }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Account") },
                    label = { Text("Account") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            when (tab) {
                0 -> DictionaryScreen(dictHelper, onSpeak)
                1 -> VocabScreen(onSpeak = onSpeak, onGainXp = { xp += it })
                2 -> GrammarScreen()
                3 -> AccountScreen(xp = xp, fullOffline = dictHelper.isFullOfflineDbLoaded)
            }
        }
    }
}

@Composable
fun DictionaryScreen(dictHelper: DictionaryDbHelper, onSpeak: (String, Locale) -> Unit) {
    var query by remember { mutableStateOf("") }
    val items = remember(query) { dictHelper.search(query) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search Ornagai EN–MY Dictionary") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { item ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.word, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onSpeak(item.word, Locale.US) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "Listen")
                            }
                        }
                        Text(item.definition, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun VocabScreen(onSpeak: (String, Locale) -> Unit, onGainXp: (Int) -> Unit) {
    var idx by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }
    val card = SAMPLE_FLASHCARDS[idx % SAMPLE_FLASHCARDS.size]

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { flipped = !flipped }
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(card.language, style = MaterialTheme.typography.labelLarge)
                Text(card.word, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text(card.reading, style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = { onSpeak(card.word, card.locale) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Pronounce")
                }
                if (flipped) {
                    HorizontalDivider()
                    Text(card.meaningEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(card.meaningMy, style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text("Tap card to flip", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { flipped = !flipped }) { Text(if (flipped) "Hide" else "Reveal") }
            Button(onClick = {
                onGainXp(10)
                flipped = false
                idx++
            }) { Text("Good (+10 XP)") }
        }
    }
}

@Composable
fun GrammarScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("English · Present Perfect vs. Past Simple", fontWeight = FontWeight.Bold)
                Text("Formula: Subject + have/has + Past Participle (V3)")
                Text("အတိတ်မှာ ပြီးသွားပေမယ့် လက်ရှိအချိန်နဲ့ ဆက်စပ်နေတဲ့ အတွေ့အကြုံတွေအတွက် Present Perfect ကို သုံးပါတယ်။")
            }
        }
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Japanese · は (wa) vs が (ga)", fontWeight = FontWeight.Bold)
                Text("Formula: [Topic] は [Comment] です")
                Text("は (wa) ကို အဓိက Topic အတွက် သုံးပြီး が (ga) ကို အချက်အလက်အသစ်ကို အထူးပြုဖို့ သုံးပါတယ်။")
            }
        }
    }
}

@Composable
fun AccountScreen(xp: Int, fullOffline: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Learning Progress", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Total XP: $xp")
                Text("Offline Ornagai SQLite: ${if (fullOffline) "111,232 words loaded" else "Starter pack active"}")
                Text("Supabase Sync: ${BuildConfig.SUPABASE_URL}")
                Text("RevenueCat Plans: Free · Premium · Lifetime · Language Packs")
            }
        }
    }
}
