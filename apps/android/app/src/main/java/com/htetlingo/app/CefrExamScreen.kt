package com.htetlingo.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CefrExamScreen(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onFinish: (score: Int, total: Int, cefrBand: String) -> Unit,
    onDismiss: () -> Unit
) {
    val questions = remember { db.fetchCefrExamQuestions(count = 30) }
    var currentIndex by remember { mutableIntStateOf(0) }
    val userAnswers = remember { mutableStateMapOf<Int, Int>() } // questionIndex -> optionIndex
    val flagged = remember { mutableStateListOf<Int>() }

    var timeRemainingSeconds by remember { mutableIntStateOf(1500) } // 25 minutes
    var isSubmitted by remember { mutableStateOf(false) }
    var showGridSheet by remember { mutableStateOf(false) }
    var showConfirmSubmit by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }
    var showReviewDetail by remember { mutableStateOf(false) }

    var useUkVoice by remember { mutableStateOf(false) }

    // Live countdown timer
    LaunchedEffect(isSubmitted) {
        if (!isSubmitted) {
            while (timeRemainingSeconds > 0) {
                delay(1000L)
                timeRemainingSeconds--
            }
            if (timeRemainingSeconds == 0) {
                isSubmitted = true
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (isSubmitted) onDismiss() else showExitConfirm = true
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (questions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = TealPrimary)
                        Text("စာမေးပွဲ မေးခွန်းများ ပြင်ဆင်နေပါသည်…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                return@Surface
            }

            if (!isSubmitted) {
                // ACTIVE EXAM MODE
                val currentQ = questions.getOrNull(currentIndex) ?: questions[0]
                val minutes = timeRemainingSeconds / 60
                val seconds = timeRemainingSeconds % 60
                val timerString = String.format("%02d:%02d", minutes, seconds)
                val timerColor = when {
                    timeRemainingSeconds < 60 -> CrimsonRed
                    timeRemainingSeconds < 300 -> WarmGold
                    else -> MaterialTheme.colorScheme.primary
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Column {
                                    Text("CEFR Level Examination", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        "Question ${currentIndex + 1} of ${questions.size} · Band ${currentQ.cefrLevel}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(onClick = { showExitConfirm = true }) {
                                    Icon(Icons.Default.Close, contentDescription = "Exit Exam")
                                }
                            },
                            actions = {
                                // Timer chip
                                Surface(
                                    color = timerColor.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, timerColor.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(16.dp))
                                        Text(timerString, fontWeight = FontWeight.ExtraBold, color = timerColor, fontSize = 13.sp)
                                    }
                                }

                                // Question Grid button
                                IconButton(onClick = { showGridSheet = true }) {
                                    Icon(Icons.Default.GridView, contentDescription = "Question Map")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        Surface(
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp,
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous
                                OutlinedButton(
                                    onClick = { if (currentIndex > 0) currentIndex-- },
                                    enabled = currentIndex > 0,
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Prev")
                                }

                                // Flag button
                                IconButton(
                                    onClick = {
                                        if (currentIndex in flagged) flagged.remove(currentIndex)
                                        else flagged.add(currentIndex)
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Flag,
                                        contentDescription = "Flag",
                                        tint = if (currentIndex in flagged) WarmGold else MaterialTheme.colorScheme.outline
                                    )
                                }

                                // Next or Submit
                                if (currentIndex < questions.size - 1) {
                                    Button(
                                        onClick = { currentIndex++ },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Text("Next")
                                        Spacer(Modifier.width(4.dp))
                                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                } else {
                                    Button(
                                        onClick = { showConfirmSubmit = true },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Submit")
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Progress line
                        LinearProgressIndicator(
                            progress = { (currentIndex + 1).toFloat() / questions.size },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = TealPrimary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )

                        // Meta Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                BadgePill(currentQ.cefrLevel, when (currentQ.cefrLevel) {
                                    "A1" -> TealPrimary
                                    "A2" -> SkyBlue
                                    "B1" -> IndigoAccent
                                    "B2" -> DeepPurple
                                    "C1" -> RosePink
                                    else -> CrimsonRed
                                })
                                BadgePill(currentQ.category, WarmGold)
                            }

                            if (currentIndex in flagged) {
                                Surface(color = WarmGold.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)) {
                                    Row(Modifier.padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Flag, contentDescription = null, tint = WarmGold, modifier = Modifier.size(12.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("Flagged", color = WarmGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Audio Player for Listening or TTS
                        if (currentQ.category.contains("Listening", ignoreCase = true) || currentQ.audioText.isNotEmpty()) {
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledIconButton(
                                            onClick = {
                                                val loc = if (useUkVoice) Locale.UK else Locale.US
                                                onSpeak(currentQ.audioText, loc, 0.95f)
                                            },
                                            modifier = Modifier.size(40.dp),
                                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = IndigoAccent)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play Audio", tint = Color.White)
                                        }
                                        Column {
                                            Text("Audio Prompt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(if (useUkVoice) "🇬🇧 UK Voice" else "🇺🇸 US Voice", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }

                                    // Voice accent switcher
                                    FilterChip(
                                        selected = useUkVoice,
                                        onClick = { useUkVoice = !useUkVoice },
                                        label = { Text(if (useUkVoice) "UK" else "US", fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        // Question Text Card
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = currentQ.question,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 24.sp
                                    )
                                )
                            }
                        }

                        // Options A, B, C, D
                        Text("Select your answer:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        val chosenOpt = userAnswers[currentIndex]
                        val labels = listOf("A", "B", "C", "D")

                        currentQ.options.forEachIndexed { optIdx, optText ->
                            val isSelected = chosenOpt == optIdx
                            val borderColor = if (isSelected) IndigoAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            val bgColor = if (isSelected) IndigoAccent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        userAnswers[currentIndex] = optIdx
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = bgColor,
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Option badge (A, B, C, D)
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) IndigoAccent else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            labels.getOrElse(optIdx) { "?" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = optText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            lineHeight = 20.sp
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IndigoAccent, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Quick summary pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Answered: ${userAnswers.size} / ${questions.size}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = { showConfirmSubmit = true }) {
                                Text("End & Submit Exam", color = CrimsonRed, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                // EXAM RESULT & OFFICIAL CEFR CERTIFICATE
                val totalQ = questions.size
                var correctCount = 0
                val categoryStats = mutableMapOf<String, Pair<Int, Int>>() // Cat -> (correct, total)

                questions.forEachIndexed { i, q ->
                    val isCorrect = userAnswers[i] == q.correctIndex
                    if (isCorrect) correctCount++
                    val cur = categoryStats[q.category] ?: (0 to 0)
                    categoryStats[q.category] = (cur.first + (if (isCorrect) 1 else 0)) to (cur.second + 1)
                }

                val scorePercent = (correctCount.toFloat() / totalQ * 100f).toInt()
                val cefrBand = when {
                    scorePercent >= 90 -> "C2"
                    scorePercent >= 80 -> "C1"
                    scorePercent >= 65 -> "B2"
                    scorePercent >= 50 -> "B1"
                    scorePercent >= 35 -> "A2"
                    else -> "A1"
                }

                val bandTitle = when (cefrBand) {
                    "C2" -> "Mastery / Proficiency"
                    "C1" -> "Effective Operational Proficiency"
                    "B2" -> "Vantage (Upper-Intermediate)"
                    "B1" -> "Threshold (Intermediate)"
                    "A2" -> "Waystage (Elementary)"
                    else -> "Breakthrough (Beginner)"
                }

                val bandDescriptionMm = when (cefrBand) {
                    "C2" -> "အဆင့်အမြင့်ဆုံး အင်္ဂလိပ်စာ အရည်အချင်း။ မည်သည့်ရှုပ်ထွေးသော သဒ္ဒါ၊ စာပေနှင့် အသံကိုမဆို မူရင်းနိုင်ငံသားကဲ့သို့ တိကျစွာ သုံးစွဲနားလည်နိုင်ပါသည်။"
                    "C1" -> "အဆင့်မြင့် အင်္ဂလိပ်စာ ကျွမ်းကျင်မှု။ အလုပ်ခွင်၊ တက္ကသိုလ်စာတမ်းများနှင့် ပညာရပ်ဆိုင်ရာ စကားပြောဆိုမှုများကို လွယ်ကူလျင်မြန်စွာ ဆောင်ရွက်နိုင်ပါသည်။"
                    "B2" -> "အလယ်အလတ်အထက် ကျွမ်းကျင်မှု။ အကြောင်းအရာမျိုးစုံကို အဆင်ပြေပြေ ရေးသားပြောဆိုနိုင်ပြီး မူရင်းစကားပြောသူများနှင့် သွက်လက်စွာ ဆက်သွယ်နိုင်ပါသည်။"
                    "B1" -> "အခြေခံအလယ်အလတ် အဆင့်။ နေ့စဉ်ဘဝ၊ ခရီးသွားလုပ်ငန်းနှင့် မိမိစိတ်ဝင်စားသော အကြောင်းအရာများကို အဆင်ပြေစွာ ဆွေးနွေးနိုင်ပါသည်။"
                    "A2" -> "အခြေခံအဆင့်။ နေ့စဉ်သုံး ရိုးရှင်းသော ဝါကျများနှင့် မေးခွန်းများကို နားလည်ပြီး အခြေခံလိုအပ်ချက်များကို ဖော်ပြနိုင်ပါသည်။"
                    else -> "စတင်လေ့လာသူအဆင့်။ ရိုးရှင်းသော စကားလုံးများနှင့် အခြေခံနှုတ်ခွန်းဆက် စကားများကို လေ့လာသင်ယူနေဆဲ အဆင့်ဖြစ်ပါသည်။"
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("CEFR Assessment Result", fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = onDismiss) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Official Certificate Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF312E81), Color(0xFF4C1D95))))
                                .padding(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = WarmGold, modifier = Modifier.size(44.dp))
                                Text(
                                    "OFFICIAL CEFR LEVEL CERTIFICATE",
                                    color = WarmGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Awarded to Learner: ထက်",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp
                                )

                                Spacer(Modifier.height(4.dp))

                                // Band Display
                                Surface(
                                    color = Color.White.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.5.dp, WarmGold)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(cefrBand, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                        Text(bandTitle, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarmGold)
                                    }
                                }

                                Text(
                                    "Score: $scorePercent% ($correctCount of $totalQ correct)",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    bandDescriptionMm,
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        // Sectional Breakdown Card
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Sectional Diagnostics (ကဏ္ဍအလိုက် အမှတ်)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Divider()

                                categoryStats.forEach { (cat, pair) ->
                                    val catCorrect = pair.first
                                    val catTotal = pair.second
                                    val catPct = if (catTotal > 0) (catCorrect.toFloat() / catTotal * 100).toInt() else 0

                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(cat, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                            Text("$catPct% ($catCorrect/$catTotal)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TealPrimary)
                                        }
                                        LinearProgressIndicator(
                                            progress = { catPct / 100f },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = if (catPct >= 70) EmeraldGreen else if (catPct >= 50) WarmGold else CrimsonRed
                                        )
                                    }
                                }
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showReviewDetail = !showReviewDetail },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent)
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (showReviewDetail) "Hide Review" else "Review Answers")
                            }

                            Button(
                                onClick = {
                                    onFinish(correctCount, totalQ, cefrBand)
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                            ) {
                                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Complete")
                            }
                        }

                        // Detailed Question Review
                        if (showReviewDetail) {
                            Text("Full Question Review & Explanations", fontWeight = FontWeight.Bold, fontSize = 16.sp)

                            questions.forEachIndexed { idx, q ->
                                val userChoice = userAnswers[idx]
                                val isCorrect = userChoice == q.correctIndex

                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.elevatedCardColors(
                                        containerColor = if (isCorrect) EmeraldGreen.copy(alpha = 0.05f) else CrimsonRed.copy(alpha = 0.05f)
                                    )
                                ) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                BadgePill("Q${idx + 1}", IndigoAccent)
                                                BadgePill(q.cefrLevel, WarmGold)
                                            }
                                            Text(
                                                if (isCorrect) "✓ Correct" else "✗ Incorrect",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = if (isCorrect) EmeraldGreen else CrimsonRed
                                            )
                                        }

                                        Text(q.question, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                                        Text(
                                            "Your answer: ${userChoice?.let { q.options.getOrNull(it) } ?: "Not answered"}",
                                            fontSize = 12.sp,
                                            color = if (isCorrect) EmeraldGreen else CrimsonRed,
                                            fontWeight = FontWeight.Medium
                                        )

                                        if (!isCorrect) {
                                            Text(
                                                "Correct answer: ${q.options.getOrElse(q.correctIndex) { "" }}",
                                                fontSize = 12.sp,
                                                color = EmeraldGreen,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                q.explanation,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // QUESTION GRID SHEET DIALOG
            if (showGridSheet) {
                AlertDialog(
                    onDismissRequest = { showGridSheet = false },
                    title = { Text("Question Navigator (30 Questions)", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(IndigoAccent))
                                    Text("Answered", fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(WarmGold))
                                    Text("Flagged", fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
                                    Text("Unanswered", fontSize = 11.sp)
                                }
                            }

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(5),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.height(280.dp)
                            ) {
                                items(questions.size) { i ->
                                    val isAnswered = i in userAnswers
                                    val isFlagged = i in flagged
                                    val isCurrent = i == currentIndex

                                    val bg = when {
                                        isFlagged -> WarmGold
                                        isAnswered -> IndigoAccent
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                currentIndex = i
                                                showGridSheet = false
                                            },
                                        color = bg,
                                        border = if (isCurrent) BorderStroke(2.dp, TealPrimary) else null
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                "${i + 1}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isAnswered || isFlagged) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showGridSheet = false }) {
                            Text("Close")
                        }
                    }
                )
            }

            // CONFIRM SUBMIT DIALOG
            if (showConfirmSubmit) {
                val answeredCount = userAnswers.size
                val unansweredCount = questions.size - answeredCount

                AlertDialog(
                    onDismissRequest = { showConfirmSubmit = false },
                    icon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = EmeraldGreen) },
                    title = { Text("စာမေးပွဲ အပြီးသတ်တင်မည်လား?", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ဖြေဆိုပြီးသော မေးခွန်း: $answeredCount ခု")
                            if (unansweredCount > 0) {
                                Text("မဖြေဆိုရသေးသော မေးခွန်း: $unansweredCount ခု ကျန်ရှိနေပါသည်။", color = CrimsonRed, fontWeight = FontWeight.Bold)
                            }
                            Text("စာမေးပွဲကို တင်ပြီးပါက CEFR Level အဆင့်သတ်မှတ်ချက်နှင့် Certificate ကို ချက်ချင်းရရှိပါမည်။")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmSubmit = false
                                isSubmitted = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen)
                        ) {
                            Text("တင်မည် (Submit)")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmSubmit = false }) {
                            Text("ဆက်လက်ဖြေဆိုမည်")
                        }
                    }
                )
            }

            // EXIT CONFIRM DIALOG
            if (showExitConfirm) {
                AlertDialog(
                    onDismissRequest = { showExitConfirm = false },
                    icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = CrimsonRed) },
                    title = { Text("စာမေးပွဲမှ ထွက်မည်လား?", fontWeight = FontWeight.Bold) },
                    text = { Text("ယခုထွက်လိုက်ပါက လက်ရှိဖြေဆိုနေသော စာမေးပွဲ အချက်အလက်များ ဆုံးရှုံးသွားပါမည်။") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showExitConfirm = false
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed)
                        ) {
                            Text("ထွက်မည် (Exit)")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitConfirm = false }) {
                            Text("ဆက်လက်ဖြေမည်")
                        }
                    }
                )
            }
        }
    }
}
