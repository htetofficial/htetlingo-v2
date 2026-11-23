package com.htetlingo.app

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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

// MARK: - Reusable Compact Search Bar (Saves vertical space on mobile)
@Composable
fun CompactSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: () -> Unit = { onValueChange("") }
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            )
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HtetLingoAndroidRoot(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit
) {
    var mainTab by remember { mutableIntStateOf(0) }
    var xp by remember { mutableIntStateOf(65) }
    var hearts by remember { mutableIntStateOf(5) }
    var gems by remember { mutableIntStateOf(350) }
    var streak by remember { mutableIntStateOf(7) }
    var studiedCount by remember { mutableIntStateOf(12) }
    var savedWords by remember { mutableStateOf(setOf("resilience", "language", "culture")) }
    var accountEmail by remember { mutableStateOf("htetmyanmar321@icloud.com") }
    var showCefrExam by remember { mutableStateOf(false) }

    val isVip = accountEmail.trim().lowercase() == "htetmyanmar321@icloud.com"

    // Full Screen Authentic CEFR Exam Dialog
    if (showCefrExam) {
        CefrExamScreen(
            db = db,
            onSpeak = onSpeak,
            onFinish = { score, total, band ->
                xp += score * 20
                gems += 50
            },
            onDismiss = { showCefrExam = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(SakuraPink, LilacPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🌸", fontSize = 18.sp)
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("HtetLingo", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
                                BadgePill("🌸 Quest", SakuraPink)
                            }
                            Text(
                                if (isVip) "👑 Founder VIP · All Unlocked" else "Learn · Bloom · Connect",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // CEFR Exam Quick Button
                    Surface(
                        color = LilacPurple.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, LilacPurple.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { showCefrExam = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.School, contentDescription = null, tint = LilacPurple, modifier = Modifier.size(15.dp))
                            Text("Exam", fontWeight = FontWeight.Bold, color = LilacPurple, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.width(6.dp))

                    // Hearts Indicator
                    Surface(
                        color = SakuraPink.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, SakuraPink.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("❤️", fontSize = 13.sp)
                            Text("$hearts", fontWeight = FontWeight.ExtraBold, color = SakuraPink, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Gems Indicator
                    Surface(
                        color = SkyBlue.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, SkyBlue.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text("💎", fontSize = 13.sp)
                            Text("$gems", fontWeight = FontWeight.ExtraBold, color = SkyBlue, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = mainTab == 0,
                    onClick = { mainTab = 0 },
                    icon = { Text("🌸", fontSize = 20.sp) },
                    label = { Text("Quest", fontWeight = if (mainTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                NavigationBarItem(
                    selected = mainTab == 1,
                    onClick = { mainTab = 1 },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Dictionary") },
                    label = { Text("Dict") }
                )
                NavigationBarItem(
                    selected = mainTab == 2,
                    onClick = { mainTab = 2 },
                    icon = { Icon(Icons.Default.Bolt, contentDescription = "Speed Vocab") },
                    label = { Text("Vocab") }
                )
                NavigationBarItem(
                    selected = mainTab == 3,
                    onClick = { mainTab = 3 },
                    icon = { Icon(Icons.Default.School, contentDescription = "Grammar") },
                    label = { Text("Grammar") }
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
                0 -> InteractiveQuestPathTab(
                    db = db,
                    hearts = hearts,
                    gems = gems,
                    streak = streak,
                    onSpeak = onSpeak,
                    onHeartLost = { hearts = (hearts - 1).coerceAtLeast(0) },
                    onRefillHearts = {
                        if (gems >= 30) {
                            gems -= 30
                            hearts = 5
                        } else {
                            hearts = 5
                        }
                    },
                    onRewardEarned = { gainedXp, gainedGems ->
                        xp += gainedXp
                        gems += gainedGems
                        streak += 1
                    }
                )
                1 -> DictionaryTab(
                    db = db,
                    savedWords = savedWords,
                    onToggleSave = { w -> savedWords = if (w in savedWords) savedWords - w else savedWords + w },
                    onSpeak = onSpeak,
                    onXp = { xp += it }
                )
                2 -> SpeedVocabTab(
                    db = db,
                    onSpeak = onSpeak,
                    onStudied = {
                        xp += it
                        studiedCount++
                    }
                )
                3 -> GrammarTab(db = db, onSpeak = onSpeak, onXp = { xp += it })
                4 -> StudioHubTab(
                    db = db,
                    xp = xp,
                    studiedCount = studiedCount,
                    savedCount = savedWords.size,
                    email = accountEmail,
                    isVip = isVip,
                    onEmailChange = { accountEmail = it },
                    onSpeak = onSpeak,
                    onXp = { xp += it },
                    onOpenCefrExam = { showCefrExam = true }
                )
            }
        }
    }
}

// MARK: - 1. Dictionary (Compact layout, Maximum Screen Real Estate)
@Composable
fun DictionaryTab(
    db: DictionaryDbHelper,
    savedWords: Set<String>,
    onToggleSave: (String) -> Unit,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var dictMode by remember { mutableIntStateOf(0) } // 0 = Trilingual (15k), 1 = English-to-English (49,537)
    var showSavedOnly by remember { mutableStateOf(false) }
    val allMatches = remember(query) { db.searchVocab(query = query, limit = 60) }
    val enEnMatches = remember(query, dictMode) { if (dictMode == 1) db.searchEnEn(query, 60) else emptyList() }
    val items = if (showSavedOnly) allMatches.filter { it.word in savedWords } else allMatches

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Slim Header Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (dictMode == 0) "Trilingual Dictionary" else "EN–EN Dictionary",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            BadgePill(if (dictMode == 0) "15k Words · 🇺🇸 🇬🇧" else "49,537 Words", TealPrimary)
        }

        // Mode Filters
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(
                selected = dictMode == 0,
                onClick = { dictMode = 0 },
                label = { Text("🇲🇲🇨🇳 Trilingual (15k)") }
            )
            FilterChip(
                selected = dictMode == 1,
                onClick = { dictMode = 1 },
                label = { Text("🇬🇧 English–English (49.5k)") }
            )
            if (dictMode == 0) {
                FilterChip(
                    selected = showSavedOnly,
                    onClick = { showSavedOnly = !showSavedOnly },
                    label = { Text("Saved (${savedWords.size})") }
                )
            }
        }

        // Compact Search Bar
        CompactSearchBar(
            value = query,
            onValueChange = { query = it },
            placeholder = if (dictMode == 0) "Search English, Burmese, or Chinese…" else "Search 49,537 English definitions…"
        )

        if (dictMode == 1) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(enEnMatches, key = { it.id }) { entry ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(entry.word, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    VoiceChip("🇺🇸 US", TealPrimary) { onSpeak(entry.word, Locale.US, 0.95f); onXp(1) }
                                    VoiceChip("🇬🇧 UK", IndigoAccent) { onSpeak(entry.word, Locale.UK, 0.95f); onXp(1) }
                                }
                            }
                            HorizontalDivider()
                            entry.definitions.forEachIndexed { idx, d ->
                                Text("${idx + 1}. $d", fontSize = 13.sp, lineHeight = 19.sp)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(items, key = { it.id }) { item ->
                    val isSaved = item.word in savedWords
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(item.word, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                        BadgePill(item.level, IndigoAccent)
                                    }
                                    if (item.ipa.isNotEmpty()) {
                                        Text("/${item.ipa}/", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                    }
                                }
                                IconButton(onClick = { onToggleSave(item.word) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Save",
                                        tint = if (isSaved) CoralOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // US & UK Voice Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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

                            Text("🇲🇲 ${item.myDef}", color = TealPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp)
                            if (item.zhDef.isNotEmpty()) {
                                Text("🇨🇳 ${item.zhDef}", fontSize = 13.sp)
                            }
                            if (item.enDef.isNotEmpty()) {
                                Text("🇬🇧 ${item.enDef}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (item.tag.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
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
}

// MARK: - 2. Speed Vocabulary (Compact Layout)
@Composable
fun SpeedVocabTab(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onStudied: (Int) -> Unit
) {
    val filters = listOf("ALL", "A1", "A2", "B1", "B2", "C1", "C2", "COCA", "CET4", "CET6", "TOEFL", "IELTS", "GRE")
    var selectedFilter by remember { mutableStateOf("ALL") }
    var speedMode by remember { mutableStateOf(true) }
    var useUkVoice by remember { mutableStateOf(false) }
    var autoSpeak by remember { mutableStateOf(true) }
    var index by remember { mutableIntStateOf(0) }
    var flipped by remember { mutableStateOf(false) }

    val cards = remember(selectedFilter) { db.searchVocab(tagFilter = selectedFilter, limit = 250) }
    val current = cards.getOrNull(index % cards.size.coerceAtLeast(1))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Slim Header Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Speed Vocabulary", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            BadgePill("${cards.size} Cards", CoralOrange)
        }

        // Filter Chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = speedMode, onClick = { speedMode = !speedMode }, label = { Text(if (speedMode) "⚡ Speed" else "🃏 Flashcard") })
            FilterChip(selected = autoSpeak, onClick = { autoSpeak = !autoSpeak }, label = { Text(if (autoSpeak) "🔊 Auto-Speak" else "🔇 Silent") })
            FilterChip(selected = useUkVoice, onClick = { useUkVoice = !useUkVoice }, label = { Text(if (useUkVoice) "🇬🇧 UK" else "🇺🇸 US") })
        }

        if (current != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().clickable { flipped = !flipped },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgePill(current.level, IndigoAccent)
                        Text("${(index % cards.size) + 1} / ${cards.size}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = {
                            val loc = if (useUkVoice) Locale.UK else Locale.US
                            onSpeak(current.word, loc, 0.95f)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = TealPrimary)
                        }
                    }

                    Text(
                        text = current.word,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (current.ipa.isNotEmpty()) {
                        Text("/${current.ipa}/", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (!speedMode && !flipped) {
                        Text("Tap to reveal meaning", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp)
                    } else {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            text = "🇲🇲 ${current.myDef}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary,
                            lineHeight = 24.sp
                        )
                        if (current.zhDef.isNotEmpty()) {
                            Text("🇨🇳 ${current.zhDef}", fontSize = 14.sp)
                        }
                        if (current.enDef.isNotEmpty()) {
                            Text("🇬🇧 ${current.enDef}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    onClick = {
                        if (index > 0) index--
                        flipped = false
                        if (autoSpeak) onSpeak(cards[index].word, if (useUkVoice) Locale.UK else Locale.US, 0.95f)
                    }
                ) { Text("Previous") }

                Button(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    onClick = {
                        index++
                        flipped = false
                        onStudied(2)
                        if (autoSpeak) onSpeak(cards[index % cards.size].word, if (useUkVoice) Locale.UK else Locale.US, 0.95f)
                    }
                ) { Text("Next (+2 XP)") }

                FilledTonalButton(
                    shape = RoundedCornerShape(12.dp),
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

// MARK: - 3. Grammar Tab with Author's 49 Lessons + yukiar/CEFR-SP (10k) + HSK 1-6 + EN Errors
@Composable
fun GrammarTab(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    var subTab by remember { mutableIntStateOf(0) }
    // 0 = 🌸 Foundation A1 (ထက်)
    // 1 = 📖 စာအုပ် သဒ္ဒါ (Author's 49 Lessons A1–C2)
    // 2 = 🌐 CEFR-SP (10,004 Sentences from yukiar/CEFR-SP)
    // 3 = 🇨🇳 HSK 1–6 (472 Patterns)
    // 4 = ⚠️ EN Errors (1,553 Mistakes)
    // 5 = 📚 Reference Chapters

    var bookLevelFilter by remember { mutableStateOf("ALL") }
    var cefrSpFilter by remember { mutableStateOf("ALL") }
    var hskLevel by remember { mutableStateOf("ALL") }
    var search by remember { mutableStateOf("") }
    var expandedLessonNum by remember { mutableIntStateOf(1) }
    var expandedFoundationId by remember { mutableStateOf("foundation-a1-ch01") }
    var showAnswersFor by remember { mutableStateOf(setOf<Int>()) }

    val foundationChapters = remember { db.fetchFoundationChapters() }
    val bookLessons = remember(search, bookLevelFilter) {
        val all = db.fetchBookGrammarLessons(search)
        if (bookLevelFilter == "ALL") all
        else all.filter { it.level.contains(bookLevelFilter, ignoreCase = true) }
    }
    val cefrSpItems = remember(cefrSpFilter, search) {
        db.searchCefrSp(cefrSpFilter, search, 40)
    }
    val hskItems = remember(hskLevel, search) { db.searchHskGrammar(hskLevel, search, 40) }
    val errItems = remember(search) { db.searchEnglishErrors(search, 50) }
    val bookChapters = remember { db.fetchGrammarChapters() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Slim Header Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Grammar Studio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            BadgePill(
                when (subTab) {
                    0 -> "🌸 Foundation A1 (ထက်) · ၁၄ ခန်း"
                    1 -> "49 Lessons · A1–C2"
                    2 -> "10k CEFR-SP Sentences"
                    3 -> "472 HSK 1–6"
                    4 -> "1,553 Pitfalls"
                    else -> "Reference Chapters"
                },
                if (subTab == 0) SakuraPink else IndigoAccent
            )
        }

        // SubTab Chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = subTab == 0, onClick = { subTab = 0; search = "" }, label = { Text("🌸 Foundation A1 (ထက်)") })
            FilterChip(selected = subTab == 1, onClick = { subTab = 1; search = "" }, label = { Text("📖 Mastery A1–C2 (49)") })
            FilterChip(selected = subTab == 2, onClick = { subTab = 2; search = "" }, label = { Text("🌐 CEFR-SP (10k)") })
            FilterChip(selected = subTab == 3, onClick = { subTab = 3; search = "" }, label = { Text("🇨🇳 HSK 1–6") })
            FilterChip(selected = subTab == 4, onClick = { subTab = 4; search = "" }, label = { Text("⚠️ EN Errors") })
            FilterChip(selected = subTab == 5, onClick = { subTab = 5; search = "" }, label = { Text("📚 Reference") })
        }

        // Compact Search Bar
        if (subTab != 0 && subTab != 5) {
            CompactSearchBar(
                value = search,
                onValueChange = { search = it },
                placeholder = when (subTab) {
                    1 -> "Search 49 book lessons (Passive, Inversion, Conditionals)…"
                    2 -> "Search 10k CEFR-SP sentences…"
                    3 -> "Search Chinese grammar patterns…"
                    else -> "Search misused English words…"
                }
            )
        }

        // Secondary Filters
        if (subTab == 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("ALL", "A1", "A2", "B1", "B2", "C1", "C2").forEach { lvl ->
                    FilterChip(selected = bookLevelFilter == lvl, onClick = { bookLevelFilter = lvl }, label = { Text(lvl) })
                }
            }
        } else if (subTab == 2) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("ALL", "A1", "A2", "B1", "B2", "C1", "C2").forEach { lvl ->
                    FilterChip(selected = cefrSpFilter == lvl, onClick = { cefrSpFilter = lvl }, label = { Text(lvl) })
                }
            }
        } else if (subTab == 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("ALL", "HSK 1", "HSK 2", "HSK 3", "HSK 4", "HSK 5", "HSK 6").forEach { lvl ->
                    FilterChip(selected = hskLevel == lvl, onClick = { hskLevel = lvl }, label = { Text(lvl) })
                }
            }
        }

        // Content
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (subTab == 0) {
                // Foundation English (A1) by ထက်
                items(foundationChapters, key = { it.id }) { ch ->
                    val open = expandedFoundationId == ch.id
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            expandedFoundationId = if (open) "" else ch.id
                            if (!open) onXp(2)
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    BadgePill("Foundation A1", SakuraPink)
                                    Text(ch.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Icon(
                                    if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = SakuraPink
                                )
                            }

                            if (!open) {
                                Text(ch.content.take(160) + "...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                            }

                            if (open) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    VoiceChip("🇺🇸 Read Aloud", TealPrimary) {
                                        val englishWords = ch.content.lines().filter { it.contains(Regex("[a-zA-Z]{3,}")) }.joinToString(". ").take(200)
                                        if (englishWords.isNotBlank()) onSpeak(englishWords, Locale.US, 0.95f)
                                    }
                                }
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(10.dp)) {
                                    Text(ch.content, modifier = Modifier.padding(12.dp), fontSize = 13.sp, lineHeight = 20.sp)
                                }
                            }
                        }
                    }
                }
            } else if (subTab == 1) {
                // Author's 49 Grammar Lessons directly in the main Grammar Tab!
                items(bookLessons, key = { it.number }) { l ->
                    val open = expandedLessonNum == l.number
                    val ansVisible = l.number in showAnswersFor

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            expandedLessonNum = if (open) 0 else l.number
                        },
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BadgePill("Lesson ${l.number}", TealPrimary)
                                    BadgePill(l.level, IndigoAccent)
                                }
                                Icon(
                                    if (open) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(l.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)

                            if (open) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                if (l.explanation.isNotEmpty()) {
                                    Text("ရှင်းလင်းချက်:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TealPrimary)
                                    Text(l.explanation, fontSize = 13.sp, lineHeight = 19.sp)
                                }

                                if (l.patterns.isNotEmpty()) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            Text("ဝါကျတည်ဆောက်ပုံ / Formula:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = IndigoAccent)
                                            Text(l.patterns, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        }
                                    }
                                }

                                if (l.examples.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("နမူနာဝါကျများ:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WarmGold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            VoiceChip("🇺🇸 US", TealPrimary) {
                                                onSpeak(l.examples.take(180), Locale.US, 0.95f)
                                                onXp(1)
                                            }
                                            VoiceChip("🇬🇧 UK", IndigoAccent) {
                                                onSpeak(l.examples.take(180), Locale.UK, 0.95f)
                                                onXp(1)
                                            }
                                        }
                                    }
                                    Text(l.examples, fontSize = 13.sp, lineHeight = 19.sp)
                                }

                                if (l.practice.isNotEmpty()) {
                                    Text("လေ့ကျင့်ခန်း:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RosePink)
                                    Text(l.practice, fontSize = 12.sp, lineHeight = 18.sp)

                                    if (l.answers.isNotEmpty()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = {
                                                showAnswersFor = if (ansVisible) showAnswersFor - l.number else showAnswersFor + l.number
                                            }) {
                                                Text(if (ansVisible) "အဖြေဖျောက်မည်" else "အဖြေစစ်ဆေးမည်", fontSize = 12.sp)
                                            }
                                        }
                                        if (ansVisible) {
                                            Surface(color = EmeraldGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                                                Text(l.answers, modifier = Modifier.padding(8.dp), fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (subTab == 2) {
                // yukiar/CEFR-SP 10,004 Sentences
                items(cefrSpItems, key = { it.id }) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    BadgePill(item.cefrLevel, when (item.cefrLevel) {
                                        "A1" -> TealPrimary
                                        "A2" -> SkyBlue
                                        "B1" -> IndigoAccent
                                        "B2" -> DeepPurple
                                        "C1" -> RosePink
                                        else -> CrimsonRed
                                    })
                                    BadgePill(item.source, WarmGold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    VoiceChip("🇺🇸", TealPrimary) { onSpeak(item.sentence, Locale.US, 0.95f); onXp(1) }
                                    VoiceChip("🇬🇧", IndigoAccent) { onSpeak(item.sentence, Locale.UK, 0.95f); onXp(1) }
                                }
                            }
                            Text(item.sentence, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
                        }
                    }
                }
            } else if (subTab == 3) {
                // HSK 1-6 Chinese Grammar
                items(hskItems) { h ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                BadgePill(h.level, RosePink)
                                IconButton(onClick = { onSpeak(h.exampleZh, Locale.SIMPLIFIED_CHINESE, 0.9f) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = IndigoAccent, modifier = Modifier.size(18.dp))
                                }
                            }
                            Text(h.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (h.pattern.isNotEmpty()) {
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                    Text(h.pattern, modifier = Modifier.padding(6.dp), fontSize = 12.sp)
                                }
                            }
                            Text(h.exampleZh, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(h.examplePy, color = TealPrimary, fontSize = 12.sp)
                            Text(h.exampleEn, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        }
                    }
                }
            } else if (subTab == 4) {
                // English Common Errors
                items(errItems) { err ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(err.wrong, color = RosePink, textDecoration = TextDecoration.LineThrough, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("→ ${err.right}", color = EmeraldGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                // Codeyu English Book (19 Chapters)
                items(bookChapters) { ch ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(ch.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                BadgePill(ch.source.split("/").last(), IndigoAccent)
                            }
                            Text(ch.content.take(500), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - 4. Infinite Quiz & Authentic CEFR Exam Launcher
@Composable
fun InfiniteQuizTab(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit,
    onOpenCefrExam: () -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0=EN->MY, 1=EN->ZH, 2=Listen US, 3=Listen UK, 4=Grammar Exercises (770)
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
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // AUTHENTIC CEFR EXAM LAUNCHER (Full Screen)
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenCefrExam() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.5.dp, IndigoAccent.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Color(0xFF312E81), Color(0xFF4C1D95)))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.School, contentDescription = null, tint = WarmGold, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Authentic CEFR Exam", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        BadgePill("Full Screen", TealPrimary)
                    }
                    Text("30 Questions · 25 Min Timer · A1 to C2 Diagnostic Certificate", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onOpenCefrExam,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Start", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Mode Chips
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = mode == 0, onClick = { mode = 0; seed++ }, label = { Text("🇲🇲 EN→MY") })
            FilterChip(selected = mode == 1, onClick = { mode = 1; seed++ }, label = { Text("🇨🇳 EN→ZH") })
            FilterChip(selected = mode == 2, onClick = { mode = 2; seed++ }, label = { Text("🇺🇸 Listen US") })
            FilterChip(selected = mode == 3, onClick = { mode = 3; seed++ }, label = { Text("🇬🇧 Listen UK") })
            FilterChip(selected = mode == 4, onClick = { mode = 4 }, label = { Text("📝 Exercises (770)") })
        }

        if (mode == 4) {
            GrammarExercisesSection(db, onSpeak, onXp)
            return@Column
        }

        LinearProgressIndicator(
            progress = { (if (finished) total else step).toFloat() / total.toFloat() },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = TealPrimary
        )

        if (finished) {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmGold, modifier = Modifier.size(50.dp))
                    Text("Score: $score / $total", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Earned +${score * 15} XP!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { seed++ }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)) {
                        Text("Generate Fresh Quiz")
                    }
                }
            }
        } else if (current != null) {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Question ${step + 1} / $total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val prompt = when (mode) {
                        1 -> "Choose Chinese meaning: \"${current.word}\""
                        2 -> "Listen to 🇺🇸 US voice and select word:"
                        3 -> "Listen to 🇬🇧 UK voice and select word:"
                        else -> "Choose Burmese meaning: \"${current.word}\""
                    }
                    Text(prompt, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                    if (mode == 2 || mode == 3) {
                        Button(
                            onClick = { onSpeak(current.word, if (mode == 3) Locale.UK else Locale.US, 0.9f) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (mode == 3) IndigoAccent else TealPrimary)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (mode == 3) "Play 🇬🇧 UK Audio" else "Play 🇺🇸 US Audio")
                        }
                    }

                    options.forEach { opt ->
                        val isChosen = selected == opt
                        val isCorrect = selected != null && opt == correctAnswer
                        val isWrong = isChosen && opt != correctAnswer

                        val bg = when {
                            isCorrect -> EmeraldGreen.copy(alpha = 0.2f)
                            isWrong -> RosePink.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = selected == null) {
                                    selected = opt
                                    if (opt == correctAnswer) {
                                        score++
                                        onXp(15)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = bg
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(opt, fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal, fontSize = 14.sp)
                                if (isCorrect) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(18.dp))
                                if (isWrong) Icon(Icons.Default.Cancel, contentDescription = null, tint = RosePink, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (selected != null) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (step + 1 >= total) finished = true
                                else {
                                    step++
                                    selected = null
                                }
                            }
                        ) {
                            Text(if (step + 1 >= total) "Finish Quiz" else "Next Question")
                        }
                    }
                }
            }
        }
    }
}

// MARK: - 5. Studio & Hub Tab
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
    onXp: (Int) -> Unit,
    onOpenCefrExam: () -> Unit
) {
    var section by remember { mutableIntStateOf(0) }
    // 0=Htet Book, 1=CEFR, 2=SpellingBee, 3=DevInterview, 4=Voice, 5=Translate, 6=Myanmar Hub, 7=VIP Account

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = section == 0, onClick = { section = 0 }, label = { Text("📚 Books (Foundation A1 + Mastery)") })
            FilterChip(selected = section == 1, onClick = { section = 1 }, label = { Text("🎯 CEFR Test") })
            FilterChip(selected = section == 2, onClick = { section = 2 }, label = { Text("🐝 Spelling Bee") })
            FilterChip(selected = section == 3, onClick = { section = 3 }, label = { Text("💻 Dev Interview") })
            FilterChip(selected = section == 4, onClick = { section = 4 }, label = { Text("🎙️ US/UK Voice") })
            FilterChip(selected = section == 5, onClick = { section = 5 }, label = { Text("🌐 Translate") })
            FilterChip(selected = section == 6, onClick = { section = 6 }, label = { Text("🇲🇲 Myanmar Hub") })
            FilterChip(selected = section == 7, onClick = { section = 7 }, label = { Text("👑 VIP & Stats") })
        }

        when (section) {
            0 -> HtetBookSection(db, onSpeak, onXp)
            1 -> CefrStudioSection(db, onSpeak, onXp, onOpenCefrExam)
            2 -> SpellingBeeSection(db, onSpeak, onXp)
            3 -> DevInterviewSection(db, onSpeak)
            4 -> PronunciationStudioSection(onSpeak, onXp)
            5 -> TranslateSection(onSpeak, onXp)
            6 -> MyanmarHubSection(db)
            7 -> VipAccountSection(db, xp, studiedCount, savedCount, email, isVip, onEmailChange)
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

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pronunciation Studio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            BadgePill("🇺🇸 & 🇬🇧 Voices", TealPrimary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            voices.forEachIndexed { i, v ->
                FilterChip(
                    selected = selectedIdx == i,
                    onClick = { selectedIdx = i; text = v.third },
                    label = { Text(v.first) }
                )
            }
        }

        OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("Phrase to pronounce") })

        Text("Voice Speed: ${"%.2f".format(speed)}x", style = MaterialTheme.typography.labelMedium)
        Slider(value = speed, onValueChange = { speed = it }, valueRange = 0.6f..1.35f)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
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

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Multilingual Translator", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            BadgePill(provider, TealPrimary)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf("my" to "🇲🇲 MY", "ja" to "🇯🇵 JA", "zh" to "🇨🇳 ZH", "ko" to "🇰🇷 KO", "th" to "🇹🇭 TH", "fr" to "🇫🇷 FR", "de" to "🇩🇪 DE").forEach { (code, label) ->
                FilterChip(selected = target == code, onClick = { target = code }, label = { Text(label) })
            }
        }

        OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), minLines = 2, label = { Text("English text") })

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

        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(output, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
    var sub by remember { mutableIntStateOf(0) } // 0=Names, 1=Postal, 2=Calendar, 3=Phone
    val names = remember(query) { db.searchMyanmarNames(query, 30) }
    val codes = remember(query) { db.searchPostalCodes(query, 30) }
    val today = remember { MyanmarUtils.todayMyanmarDate() }
    var phone by remember { mutableStateOf("09420000000") }
    val phoneCheck = remember(phone) { MyanmarUtils.checkPhone(phone) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Myanmar Utilities", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            BadgePill("Names · Postal · Cal", DeepPurple)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = sub == 0, onClick = { sub = 0; query = "" }, label = { Text("Names (8,700)") })
            FilterChip(selected = sub == 1, onClick = { sub = 1; query = "" }, label = { Text("Postal Codes") })
            FilterChip(selected = sub == 2, onClick = { sub = 2 }, label = { Text("Calendar") })
            FilterChip(selected = sub == 3, onClick = { sub = 3 }, label = { Text("Phone Operator") })
        }

        if (sub == 0 || sub == 1) {
            CompactSearchBar(value = query, onValueChange = { query = it }, placeholder = if (sub == 0) "Search English or Myanmar name…" else "Search township or postal code…")
        }

        if (sub == 0) {
            names.forEach { n ->
                ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(n.en, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(n.mm, color = TealPrimary, fontSize = 14.sp)
                    }
                }
            }
        } else if (sub == 1) {
            codes.forEach { p ->
                ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${p.region} · ${p.township}", fontSize = 13.sp)
                        BadgePill(p.code, WarmGold)
                    }
                }
            }
        } else if (sub == 2) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Myanmar Calendar (mmcal)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("မြန်မာသက္ကရာဇ် ${today.first} ခုနှစ်", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DeepPurple)
                    Text("${today.second} · ${today.third}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
        } else {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Myanmar Phone Operator Checker", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(phoneCheck.operator, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TealPrimary)
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
    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (isVip) "👑 Founder VIP" else "HtetLingo Free Plan", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    BadgePill(if (isVip) "Lifetime Free" else "Standard", WarmGold)
                }
                Text("All features & Language Packs unlocked for htetmyanmar321@icloud.com", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Learning Progress", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                LinearProgressIndicator(progress = { (xp.coerceAtMost(100)) / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = WarmGold)
                Text("XP: $xp · Studied Cards: $studiedCount · Bookmarks: $savedCount", fontSize = 12.sp)
                OutlinedTextField(value = email, onValueChange = onEmailChange, modifier = Modifier.fillMaxWidth(), label = { Text("Account Email (Supabase)") }, singleLine = true)
            }
        }

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bundled 100% Offline SQLite Database", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("• Trilingual & EN-EN Vocab: ${db.countTable("vocab")} + ${db.countTable("en_en_entries")} words", fontSize = 12.sp)
                Text("• Author's A1–C2 Book: ${db.countTable("book_grammar_lessons")} lessons + ${db.countTable("book_vocab_units")} vocab units", fontSize = 12.sp)
                Text("• CEFR-SP Sentences: ${db.countTable("cefr_sp_sentences")} sentences", fontSize = 12.sp)
                Text("• Authentic CEFR Exam Bank: ${db.countTable("cefr_exam_questions")} questions", fontSize = 12.sp)
                Text("• Chinese HSK 1–6 Grammar: ${db.countTable("hsk_grammar")} patterns", fontSize = 12.sp)
                Text("• Common English Errors: ${db.countTable("english_errors")} pitfalls", fontSize = 12.sp)
                Text("• Myanmar Names & Postal Codes: ${db.countTable("myanmar_names")} pairs", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun GrammarCard(badge: String, title: String, formula: String, explanation: String, example: String, onListen: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                BadgePill(badge, IndigoAccent)
                IconButton(onClick = onListen, modifier = Modifier.size(28.dp)) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Listen", tint = TealPrimary, modifier = Modifier.size(18.dp)) }
            }
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Text(formula, modifier = Modifier.padding(8.dp), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text(explanation, fontSize = 13.sp)
            Text("Example: $example", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = tint, modifier = Modifier.size(13.dp))
            Text(label, color = tint, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@Composable
fun BadgePill(text: String, tint: Color) {
    Surface(color = tint.copy(alpha = 0.15f), shape = RoundedCornerShape(999.dp)) {
        Text(text, color = tint, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
    }
}
