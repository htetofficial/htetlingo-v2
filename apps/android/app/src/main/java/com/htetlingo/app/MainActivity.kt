package com.htetlingo.app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val db = DictionaryDbHelper(this)

        setContent {
            HtetLingoTheme {
                HtetLingoAndroidRoot(
                    db = db,
                    onSpeak = { text, locale, rate ->
                        tts?.setSpeechRate(rate)
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
fun HtetLingoAndroidRoot(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit
) {
    var mainTab by remember { mutableIntStateOf(0) }
    var xp by remember { mutableIntStateOf(40) }
    var studiedCount by remember { mutableIntStateOf(0) }
    var savedWords by remember { mutableStateOf(setOf("resilience", "language", "culture")) }
    var accountEmail by remember { mutableStateOf("htetmyanmar321@icloud.com") }

    val isVip = accountEmail.trim().lowercase() == "htetmyanmar321@icloud.com"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(TealPrimary, IndigoAccent))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("HL", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                        Column {
                            Text("HtetLingo", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Text(
                                if (isVip) "Founder VIP · Offline Ready" else "Learn · Understand · Connect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        color = WarmGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = WarmGold, modifier = Modifier.size(16.dp))
                            Text("$xp XP", fontWeight = FontWeight.Bold, color = WarmGold, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = mainTab == 0,
                    onClick = { mainTab = 0 },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Dictionary") },
                    label = { Text("Dict") }
                )
                NavigationBarItem(
                    selected = mainTab == 1,
                    onClick = { mainTab = 1 },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = "Speed Vocab") },
                    label = { Text("Vocab") }
                )
                NavigationBarItem(
                    selected = mainTab == 2,
                    onClick = { mainTab = 2 },
                    icon = { Icon(Icons.Default.School, contentDescription = "Grammar") },
                    label = { Text("Grammar") }
                )
                NavigationBarItem(
                    selected = mainTab == 3,
                    onClick = { mainTab = 3 },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = "Quiz") },
                    label = { Text("Quiz") }
                )
                NavigationBarItem(
                    selected = mainTab == 4,
                    onClick = { mainTab = 4 },
                    icon = { Icon(Icons.Default.Apps, contentDescription = "Studio & Hub") },
                    label = { Text("Hub") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (mainTab) {
                0 -> DictionaryTab(
                    db = db,
                    savedWords = savedWords,
                    onToggleSave = { w -> savedWords = if (w in savedWords) savedWords - w else savedWords + w },
                    onSpeak = onSpeak,
                    onXp = { xp += it }
                )
                1 -> SpeedVocabTab(
                    db = db,
                    onSpeak = onSpeak,
                    onStudied = {
                        xp += it
                        studiedCount++
                    }
                )
                2 -> GrammarTab(db = db, onSpeak = onSpeak)
                3 -> InfiniteQuizTab(db = db, onSpeak = onSpeak, onXp = { xp += it })
                4 -> StudioHubTab(
                    db = db,
                    xp = xp,
                    studiedCount = studiedCount,
                    savedCount = savedWords.size,
                    email = accountEmail,
                    isVip = isVip,
                    onEmailChange = { accountEmail = it },
                    onSpeak = onSpeak,
                    onXp = { xp += it }
                )
            }
        }
    }
}

// MARK: - 1. Colorful 3-in-1 Offline Dictionary with US & UK Voices
@Composable
fun DictionaryTab(
    db: DictionaryDbHelper,
    savedWords: Set<String>,
    onToggleSave: (String) -> Unit,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showSavedOnly by remember { mutableStateOf(false) }
    val allMatches = remember(query) { db.searchVocab(query = query, limit = 60) }
    val items = if (showSavedOnly) allMatches.filter { it.word in savedWords } else allMatches

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Colorful Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(TealPrimary, SkyBlue)))
                .padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Offline Multilingual Dictionary", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("15,000+ EN–MY–ZH–EN words · 🇺🇸 US & 🇬🇧 UK Voices", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                }
                FilterChip(
                    selected = showSavedOnly,
                    onClick = { showSavedOnly = !showSavedOnly },
                    label = { Text("Saved (${savedWords.size})", fontSize = 12.sp) }
                )
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Search English, Burmese, or Chinese…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items, key = { it.id }) { item ->
                val isSaved = item.word in savedWords
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(item.word, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                    BadgePill(item.level, IndigoAccent)
                                }
                                if (item.ipa.isNotEmpty()) {
                                    Text("/${item.ipa}/", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                }
                            }
                            IconButton(onClick = { onToggleSave(item.word) }) {
                                Icon(
                                    if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Save",
                                    tint = if (isSaved) CoralOrange else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // US & UK Voice Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VoiceChip("🇺🇸 US", TealPrimary) {
                                onSpeak(item.word, Locale.US, 0.95f)
                                onXp(1)
                            }
                            VoiceChip("🇬🇧 UK", IndigoAccent) {
                                onSpeak(item.word, Locale.UK, 0.95f)
                                onXp(1)
                            }
                            if (item.zhDef.isNotEmpty()) {
                                VoiceChip("🇨🇳 ZH", RosePink) {
                                    onSpeak(item.zhDef.take(18), Locale.SIMPLIFIED_CHINESE, 0.95f)
                                }
                            }
                        }

                        HorizontalDivider()

                        Text("🇲🇲 ${item.myDef}", color = TealPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp)
                        if (item.zhDef.isNotEmpty()) {
                            Text("🇨🇳 ${item.zhDef}", fontSize = 14.sp)
                        }
                        if (item.enDef.isNotEmpty()) {
                            Text("🇬🇧 ${item.enDef}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (item.tag.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                item.tag.split(" ").filter { it.isNotBlank() }.forEach { t ->
                                    BadgePill(t.uppercase(), CoralOrange)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - 2. Speed Vocabulary (15,000 COCA / CET / TOEFL / GRE / Highschool Words)
@Composable
fun SpeedVocabTab(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onStudied: (Int) -> Unit
) {
    val filters = listOf("ALL", "COCA", "HIGHSCHOOL", "CET4", "CET6", "TOEFL", "IELTS", "GRE")
    var selectedFilter by remember { mutableStateOf("ALL") }
    var speedMode by remember { mutableStateOf(true) }
    var useUkVoice by remember { mutableStateOf(false) }
    var autoSpeak by remember { mutableStateOf(true) }
    var index by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }

    val cards = remember(selectedFilter) { db.searchVocab(tagFilter = selectedFilter, limit = 250) }
    val current = cards.getOrNull(index % cards.size.coerceAtLeast(1))

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(CoralOrange, WarmGold)))
                .padding(16.dp)
        ) {
            Column {
                Text("Speed Vocabulary Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("COCA 20k · Highschool · CET4/6 · TOEFL · GRE · IELTS", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }

        // Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            filters.forEach { f ->
                FilterChip(
                    selected = selectedFilter == f,
                    onClick = {
                        selectedFilter = f
                        index = 0
                        flipped = false
                    },
                    label = { Text(f) }
                )
            }
        }

        // Mode & Accent Controls
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = speedMode, onClick = { speedMode = !speedMode }, label = { Text(if (speedMode) "⚡ Speed: ON" else "🃏 Flashcard") })
            FilterChip(selected = autoSpeak, onClick = { autoSpeak = !autoSpeak }, label = { Text(if (autoSpeak) "🔊 Auto-Speak" else "🔇 Silent") })
            FilterChip(selected = useUkVoice, onClick = { useUkVoice = !useUkVoice }, label = { Text(if (useUkVoice) "🇬🇧 UK Accent" else "🇺🇸 US Accent") })
        }

        if (current != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable { flipped = !flipped },
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BadgePill(current.level, IndigoAccent)
                        Text("#${(index % cards.size) + 1} / ${cards.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(current.word, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                    if (current.ipa.isNotEmpty()) {
                        Text("/${current.ipa}/", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        VoiceChip("🇺🇸 US Voice", TealPrimary) { onSpeak(current.word, Locale.US, 0.95f) }
                        VoiceChip("🇬🇧 UK Voice", IndigoAccent) { onSpeak(current.word, Locale.UK, 0.95f) }
                    }

                    if (speedMode || flipped) {
                        HorizontalDivider()
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🇲🇲 ${current.myDef}", color = TealPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            if (current.zhDef.isNotEmpty()) Text("🇨🇳 ${current.zhDef}", fontSize = 15.sp)
                            if (current.enDef.isNotEmpty()) Text("🇬🇧 ${current.enDef}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Text("Tap card to reveal MY · ZH · EN meanings", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onStudied(4)
                        index = (index + 1) % cards.size
                        flipped = false
                        if (autoSpeak) onSpeak(cards[index].word, if (useUkVoice) Locale.UK else Locale.US, 0.95f)
                    }
                ) { Text("Again") }

                Button(
                    modifier = Modifier.weight(1.4f),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                    onClick = {
                        onStudied(8)
                        index = (index + 1) % cards.size
                        flipped = false
                        if (autoSpeak) onSpeak(cards[index].word, if (useUkVoice) Locale.UK else Locale.US, 0.95f)
                    }
                ) { Text("Got It (+8 XP)", fontWeight = FontWeight.Bold) }

                FilledTonalButton(
                    onClick = {
                        index = (0 until cards.size).random()
                        flipped = false
                        if (autoSpeak) onSpeak(cards[index].word, if (useUkVoice) Locale.UK else Locale.US, 0.95f)
                    }
                ) { Icon(Icons.Default.Shuffle, contentDescription = "Random") }
            }
        }
    }
}

// MARK: - 3. Grammar, HSK 1-6 Chinese Grammar (472) & Common English Errors (1,553)
@Composable
fun GrammarTab(db: DictionaryDbHelper, onSpeak: (String, Locale, Float) -> Unit) {
    var subTab by remember { mutableIntStateOf(0) }
    var hskLevel by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }

    val hskItems = remember(hskLevel, search) { db.searchHskGrammar(hskLevel, search, 40) }
    val errItems = remember(search) { db.searchEnglishErrors(search, 50) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(IndigoAccent, DeepPurple)))
                .padding(16.dp)
        ) {
            Column {
                Text("Multilingual Grammar Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Bilingual Lessons · 472 Chinese HSK 1–6 · 1,553 Common Errors", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = subTab == 0, onClick = { subTab = 0 }, label = { Text("Core") })
            FilterChip(selected = subTab == 1, onClick = { subTab = 1 }, label = { Text("🇨🇳 HSK 1–6") })
            FilterChip(selected = subTab == 2, onClick = { subTab = 2 }, label = { Text("⚠️ EN Errors") })
        }

        if (subTab > 0) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text(if (subTab == 1) "Search Chinese grammar patterns…" else "Search misused English words…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
        }

        if (subTab == 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("ALL", "HSK 1", "HSK 2", "HSK 3", "HSK 4", "HSK 5", "HSK 6").forEach { lvl ->
                    FilterChip(selected = hskLevel == lvl, onClick = { hskLevel = lvl }, label = { Text(lvl) })
                }
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (subTab == 0) {
                item {
                    GrammarCard(
                        badge = "🇬🇧 English",
                        title = "Present Perfect vs. Past Simple",
                        formula = "Subject + have/has + Past Participle (V3)",
                        explanation = "အတိတ်မှာ ပြီးသွားပေမယ့် လက်ရှိအချိန်နဲ့ ဆက်စပ်နေတဲ့ အတွေ့အကြုံတွေအတွက် Present Perfect ကို သုံးပါတယ်။",
                        example = "I have studied English for two years.",
                        onListen = { onSpeak("I have studied English for two years.", Locale.UK, 0.95f) }
                    )
                }
                item {
                    GrammarCard(
                        badge = "🇯🇵 Japanese",
                        title = "Topic は (wa) vs Subject が (ga)",
                        formula = "[Topic] は [Comment] です",
                        explanation = "は (wa) ကို အဓိက Topic အတွက် သုံးပြီး が (ga) ကို အချက်အလက်အသစ်ကို အထူးပြုဖို့ သုံးပါတယ်။",
                        example = "私はミャンマー人です。",
                        onListen = { onSpeak("私はミャンマー人です。", Locale.JAPANESE, 0.95f) }
                    )
                }
                item {
                    GrammarCard(
                        badge = "🇰🇷 Korean",
                        title = "Polite Ending -아요 / -어요",
                        formula = "Verb stem + 아요 (ㅏ/ㅗ) | 어요 (other vowels)",
                        explanation = "ကြိယာအမြစ်ရဲ့ နောက်ဆုံးသရက ㅏ သို့မဟုတ် ㅗ ဖြစ်ရင် -아요 ပေါင်းပါတယ်။",
                        example = "안녕하세요, 만나서 반갑습니다.",
                        onListen = { onSpeak("안녕하세요, 만나서 반갑습니다.", Locale.KOREAN, 0.95f) }
                    )
                }
            } else if (subTab == 1) {
                items(hskItems) { h ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                BadgePill(h.level, RosePink)
                                IconButton(onClick = { onSpeak(h.exampleZh, Locale.SIMPLIFIED_CHINESE, 0.9f) }) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = IndigoAccent)
                                }
                            }
                            Text(h.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            if (h.pattern.isNotEmpty()) {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                    Text(h.pattern, modifier = Modifier.padding(8.dp), fontSize = 13.sp)
                                }
                            }
                            Text(h.exampleZh, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(h.examplePy, color = TealPrimary, fontSize = 13.sp)
                            Text(h.exampleEn, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                items(errItems) { err ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(err.wrong, color = RosePink, textDecoration = TextDecoration.LineThrough, fontWeight = FontWeight.SemiBold)
                            Text("→ ${err.right}", color = EmeraldGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - 4. Infinite Auto-Generated Quiz (EN->MY, EN->ZH, Listening US/UK)
@Composable
fun InfiniteQuizTab(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0=EN->MY, 1=EN->ZH, 2=Listen US, 3=Listen UK
    var seed by remember { mutableIntStateOf(0) }
    val pool = remember(seed) { db.randomVocab(45) }
    var step by remember(seed, mode) { mutableIntStateOf(0) }
    var selected by remember(seed, mode, step) { mutableStateOf<String?>(null) }
    var score by remember(seed, mode) { mutableIntStateOf(0) }
    var finished by remember(seed, mode) { mutableStateOf(false) }

    val total = minOf(10, pool.size / 4).coerceAtLeast(1)
    val current = pool.getOrNull(step)
    val options = remember(seed, mode, step) {
        if (current == null) emptyList()
        else {
            val distractors = pool.filter { it.id != current.id }.shuffled().take(3)
            val ans = when (mode) {
                1 -> current.zhDef.ifEmpty { current.myDef }
                2, 3 -> current.word
                else -> current.myDef
            }
            val dists = distractors.map {
                when (mode) {
                    1 -> it.zhDef.ifEmpty { it.myDef }
                    2, 3 -> it.word
                    else -> it.myDef
                }
            }
            (listOf(ans) + dists).shuffled()
        }
    }
    val correctAnswer = current?.let {
        when (mode) {
            1 -> it.zhDef.ifEmpty { it.myDef }
            2, 3 -> it.word
            else -> it.myDef
        }
    } ?: ""

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(EmeraldGreen, TealBright)))
                .padding(16.dp)
        ) {
            Column {
                Text("Infinite Auto-Generated Quiz", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Fresh questions from 15,000 offline words every round!", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = mode == 0, onClick = { mode = 0; seed++ }, label = { Text("🇲🇲 EN→MY") })
            FilterChip(selected = mode == 1, onClick = { mode = 1; seed++ }, label = { Text("🇨🇳 EN→ZH") })
            FilterChip(selected = mode == 2, onClick = { mode = 2; seed++ }, label = { Text("🇺🇸 Listen US") })
            FilterChip(selected = mode == 3, onClick = { mode = 3; seed++ }, label = { Text("🇬🇧 Listen UK") })
        }

        LinearProgressIndicator(
            progress = { (if (finished) total else step).toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
        )

        if (finished) {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(modifier = Modifier.padding(28.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmGold, modifier = Modifier.size(56.dp))
                    Text("Score: $score / $total", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("You earned +${score * 15} XP!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { seed++ }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)) {
                        Text("Generate Fresh 10-Word Quiz")
                    }
                }
            }
        } else if (current != null) {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Question ${step + 1} / $total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val prompt = when (mode) {
                        1 -> "Choose the Chinese meaning of \"${current.word}\":"
                        2 -> "Listen to the 🇺🇸 US voice and select the word:"
                        3 -> "Listen to the 🇬🇧 UK voice and select the word:"
                        else -> "Choose the Burmese meaning of \"${current.word}\":"
                    }
                    Text(prompt, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    if (mode == 2 || mode == 3) {
                        Button(
                            onClick = { onSpeak(current.word, if (mode == 3) Locale.UK else Locale.US, 0.9f) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (mode == 3) IndigoAccent else TealPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (mode == 3) "Play 🇬🇧 UK Audio" else "Play 🇺🇸 US Audio")
                        }
                    }

                    options.forEach { opt ->
                        val isChosen = selected == opt
                        val isCorrect = opt == correctAnswer
                        val containerColor = when {
                            selected == null -> MaterialTheme.colorScheme.surfaceVariant
                            isCorrect -> EmeraldGreen.copy(alpha = 0.22f)
                            isChosen -> RosePink.copy(alpha = 0.22f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Surface(
                            color = containerColor,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().clickable(enabled = selected == null) {
                                selected = opt
                                if (isCorrect) {
                                    score++
                                    onXp(15)
                                }
                            }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(opt, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                                if (selected != null && isCorrect) Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen)
                                if (isChosen && !isCorrect) Icon(Icons.Default.Cancel, null, tint = RosePink)
                            }
                        }
                    }

                    if (selected != null) {
                        Text("${current.word} → 🇲🇲 ${current.myDef}", fontSize = 13.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (step + 1 >= total) finished = true else step++
                            }
                        ) { Text(if (step + 1 >= total) "Finish Quiz" else "Next Question") }
                    }
                }
            }
        }
    }
}

// MARK: - 5. Studio & Hub (Pronunciation US/UK + Translate + Myanmar Hub + VIP Account)
@Composable
fun StudioHubTab(
    db: DictionaryDbHelper,
    xp: Int,
    studiedCount: Int,
    savedCount: Int,
    email: String,
    isVip: Boolean,
    onEmailChange: (String) -> Unit,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    var section by remember { mutableIntStateOf(0) } // 0=Voice, 1=Translate, 2=Myanmar Hub, 3=VIP Account

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = section == 0, onClick = { section = 0 }, label = { Text("🎙️ US/UK Voice") })
            FilterChip(selected = section == 1, onClick = { section = 1 }, label = { Text("🌐 Translate") })
            FilterChip(selected = section == 2, onClick = { section = 2 }, label = { Text("🇲🇲 Myanmar Hub") })
            FilterChip(selected = section == 3, onClick = { section = 3 }, label = { Text("👑 VIP & Stats") })
        }

        when (section) {
            0 -> PronunciationStudioSection(onSpeak, onXp)
            1 -> TranslateSection(onSpeak, onXp)
            2 -> MyanmarHubSection(db)
            3 -> VipAccountSection(db, xp, studiedCount, savedCount, email, isVip, onEmailChange)
        }
    }
}

@Composable
fun PronunciationStudioSection(onSpeak: (String, Locale, Float) -> Unit, onXp: (Int) -> Unit) {
    val voices = listOf(
        Triple("🇺🇸 US English", Locale.US, "Learn languages, understand words, connect cultures."),
        Triple("🇬🇧 UK English", Locale.UK, "Good morning! Would you like a cup of tea?"),
        Triple("🇯🇵 Japanese", Locale.JAPANESE, "新しい言葉を毎日学びます。"),
        Triple("🇨🇳 Chinese", Locale.SIMPLIFIED_CHINESE, "我每天学习中文和英语。"),
        Triple("🇰🇷 Korean", Locale.KOREAN, "안녕하세요, 만나서 반갑습니다."),
        Triple("🇫🇷 French", Locale.FRENCH, "Bonjour, je veux apprendre le français."),
        Triple("🇩🇪 German", Locale.GERMAN, "Sprache verbindet Menschen weltweit.")
    )
    var selectedIdx by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf(voices[0].third) }
    var speed by remember { mutableFloatStateOf(0.95f) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(RosePink, DeepPurple))).padding(16.dp)
        ) {
            Column {
                Text("Pronunciation & Accent Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Switch between 🇺🇸 American & 🇬🇧 British English + 5 global languages", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            voices.forEachIndexed { i, v ->
                FilterChip(
                    selected = selectedIdx == i,
                    onClick = { selectedIdx = i; text = v.third },
                    label = { Text(v.first) }
                )
            }
        }

        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("Phrase to pronounce") })

        Text("Voice Speed: ${"%.2f".format(speed)}x", style = MaterialTheme.typography.labelMedium)
        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.6f..1.35f)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                onClick = { onSpeak(text, Locale.US, speed); onXp(5) }
            ) { Text("🇺🇸 Speak US") }

            Button(
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                onClick = { onSpeak(text, Locale.UK, speed); onXp(5) }
            ) { Text("🇬🇧 Speak UK") }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RosePink),
            onClick = { onSpeak(text, voices[selectedIdx].second, speed); onXp(5) }
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Speak in ${voices[selectedIdx].first}")
        }
    }
}

@Composable
fun TranslateSection(onSpeak: (String, Locale, Float) -> Unit, onXp: (Int) -> Unit) {
    var input by remember { mutableStateOf("Hello") }
    var target by remember { mutableStateOf("my") }
    var output by remember { mutableStateOf("မင်္ဂလာပါ") }
    var provider by remember { mutableStateOf("Offline Phrasebook") }
    var loading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(SkyBlue, IndigoAccent))).padding(16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Multilingual Translator", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    Text("Cloudflare Edge + Instant Offline Phrasebook", color = Color.White.copy(alpha = 0.88f), fontSize = 12.sp)
                }
                BadgePill(provider, Color.White)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("my" to "🇲🇲 MY", "ja" to "🇯🇵 JA", "zh" to "🇨🇳 ZH", "ko" to "🇰🇷 KO", "th" to "🇹🇭 TH", "fr" to "🇫🇷 FR", "de" to "🇩🇪 DE").forEach { (code, label) ->
                FilterChip(selected = target == code, onClick = { target = code }, label = { Text(label) })
            }
        }

        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), minLines = 3, label = { Text("English text") })

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !loading,
            onClick = {
                loading = true
                val txt = input
                val dst = target
                thread {
                    val (res, prov) = MyanmarUtils.translateSync(txt, "en", dst)
                    output = res
                    provider = prov
                    loading = false
                    onXp(5)
                }
            }
        ) { Text(if (loading) "Translating…" else "Translate Now") }

        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(output, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoiceChip("🇺🇸 Source", TealPrimary) { onSpeak(input, Locale.US, 0.95f) }
                    VoiceChip("🔊 Result", IndigoAccent) {
                        val loc = when (target) {
                            "ja" -> Locale.JAPANESE
                            "zh" -> Locale.SIMPLIFIED_CHINESE
                            "ko" -> Locale.KOREAN
                            "fr" -> Locale.FRENCH
                            "de" -> Locale.GERMAN
                            else -> Locale.US
                        }
                        onSpeak(output, loc, 0.95f)
                    }
                }
            }
        }
    }
}

@Composable
fun MyanmarHubSection(db: DictionaryDbHelper) {
    var query by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("09791234567") }
    var mode by remember { mutableIntStateOf(0) } // 0=Names, 1=Postal, 2=Tools

    val names = remember(query) { db.searchMyanmarNames(query, 40) }
    val postals = remember(query) { db.searchPostalCodes(query, 40) }
    val today = remember { MyanmarUtils.todayMyanmarDate() }
    val phoneCheck = MyanmarUtils.checkPhone(phone)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("Names (12k)") })
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("Postal (955)") })
            FilterChip(selected = mode == 2, onClick = { mode = 2 }, label = { Text("Calendar & Phone") })
        }

        if (mode < 2) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text(if (mode == 0) "Search Myanmar name (Htet, Aung, အောင်)…" else "Search Yangon, Mandalay, 11181…") },
                singleLine = true
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (mode == 0) {
                    items(names) { n ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(n.en, fontWeight = FontWeight.Bold)
                                Text(n.mm, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    items(postals) { p ->
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(p.township, fontWeight = FontWeight.Bold)
                                    Text(p.region, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                BadgePill(p.code, DeepPurple)
                            }
                        }
                    }
                }
            }
        } else {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Myanmar Calendar (mmcal)", fontWeight = FontWeight.Bold)
                    Text("မြန်မာသက္ကရာဇ် ${today.first} ခုနှစ်", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DeepPurple)
                    Text("${today.second} · ${today.third}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Myanmar Phone Operator Checker", fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(phoneCheck.operator, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TealPrimary)
                        BadgePill(if (phoneCheck.valid) "Valid · ${phoneCheck.network}" else "Invalid", if (phoneCheck.valid) EmeraldGreen else RosePink)
                    }
                }
            }
        }
    }
}

@Composable
fun VipAccountSection(
    db: DictionaryDbHelper,
    xp: Int,
    studiedCount: Int,
    savedCount: Int,
    email: String,
    isVip: Boolean,
    onEmailChange: (String) -> Unit
) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Brush.horizontalGradient(listOf(WarmGold, CoralOrange))).padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (isVip) "👑 Founder VIP · Lifetime Free Unlocked" else "HtetLingo Free Plan", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("All Premium features & Language Packs unlocked permanently for htetmyanmar321@icloud.com", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Learning Progress", fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { (xp.coerceAtMost(100)) / 100f }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape))
                Text("XP: $xp · Studied Cards: $studiedCount · Bookmarks: $savedCount")
                OutlinedTextField(value = email, onValueChange = onEmailChange, modifier = Modifier.fillMaxWidth(), label = { Text("Account Email (Supabase)") }, singleLine = true)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Bundled 100% Offline SQLite Database", fontWeight = FontWeight.Bold)
                Text("• Trilingual Dictionary & Speed Vocab: ${db.countTable("vocab")} words")
                Text("• Chinese HSK 1–6 Grammar: ${db.countTable("hsk_grammar")} lessons")
                Text("• Common English Errors: ${db.countTable("english_errors")} items")
                Text("• Myanmar Names EN↔MY: ${db.countTable("myanmar_names")} pairs")
                Text("• Myanmar Postal Codes: ${db.countTable("postal_codes")} townships")
            }
        }
    }
}

@Composable
fun GrammarCard(badge: String, title: String, formula: String, explanation: String, example: String, onListen: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BadgePill(badge, IndigoAccent)
                IconButton(onClick = onListen) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = TealPrimary) }
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Text(formula, modifier = Modifier.padding(10.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Text(explanation, fontSize = 14.sp)
            Text("Example: $example", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun VoiceChip(label: String, tint: Color, onClick: () -> Unit) {
    Surface(
        color = tint.copy(alpha = 0.14f),
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
            Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
fun BadgePill(text: String, tint: Color) {
    Surface(color = tint.copy(alpha = 0.15f), shape = RoundedCornerShape(999.dp)) {
        Text(text, color = tint, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}
