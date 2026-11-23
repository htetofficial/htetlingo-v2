package com.htetlingo.app

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale
import kotlin.math.sin

// Glass border gradient
val GlassBorderGradient = Brush.linearGradient(
    listOf(Color.White.copy(alpha = 0.9f), Color(0xFFF472B6).copy(alpha = 0.4f), Color(0xFFA855F7).copy(alpha = 0.4f))
)

// Quest Unit and Node models
enum class NodeState { COMPLETED, CURRENT, LOCKED }

data class QuestLessonNode(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val stars: Int, // 0..3
    val state: NodeState,
    val xpReward: Int = 25,
    val gemReward: Int = 10
)

data class QuestUnit(
    val number: Int,
    val title: String,
    val subtitle: String,
    val badge: String,
    val color: Color,
    val nodes: List<QuestLessonNode>
)

// Interactive Exercise Types
enum class ExerciseKind { WORD_BUBBLE, MULTI_CHOICE, PAIR_MATCH, LISTENING }

data class GamifiedExercise(
    val kind: ExerciseKind,
    val prompt: String,
    val targetAudio: String,
    val correctSentence: List<String> = emptyList(), // For word bubbles
    val bubblePool: List<String> = emptyList(),      // Bubble choices
    val options: List<String> = emptyList(),         // For MCQ
    val correctIndex: Int = 0,
    val pairsEnMy: List<Pair<String, String>> = emptyList() // For Pair matching
)

@Composable
fun InteractiveQuestPathTab(
    db: DictionaryDbHelper,
    hearts: Int,
    gems: Int,
    streak: Int,
    onSpeak: (String, Locale, Float) -> Unit,
    onHeartLost: () -> Unit,
    onRefillHearts: () -> Unit,
    onRewardEarned: (xp: Int, gems: Int) -> Unit
) {
    var activeLesson by remember { mutableStateOf<QuestLessonNode?>(null) }
    var showHeartRefillDialog by remember { mutableStateOf(false) }

    // Pre-built engaging Units mirroring the author's rich curriculum
    val units = remember {
        listOf(
            QuestUnit(
                number = 1,
                title = "Unit 1: First Words & Warm Greetings",
                subtitle = "စတင်မိတ်ဆက်ခြင်းနှင့် နေ့စဉ် နှုတ်ခွန်းဆက် စကားများ",
                badge = "🌸 Basics",
                color = SakuraPink,
                nodes = listOf(
                    QuestLessonNode("u1_n1", "Hello & Welcome", "မင်္ဂလာပါနှင့် နှုတ်ခွန်းဆက်", "👋", 3, NodeState.COMPLETED),
                    QuestLessonNode("u1_n2", "Personal Pronouns", "I, You, We, They ဝါကျဖွဲ့စည်းပုံ", "✨", 3, NodeState.COMPLETED),
                    QuestLessonNode("u1_n3", "Verb to Be (am/is/are)", "Be ကြိယာ ရှင်းလင်းချက်", "⭐", 2, NodeState.CURRENT),
                    QuestLessonNode("u1_n4", "Unit 1 Milestone Chest", "ရတနာသေတ္တာ စိန်ဆုလာဘ်", "🎁", 0, NodeState.LOCKED)
                )
            ),
            QuestUnit(
                number = 2,
                title = "Unit 2: Cafe, Food & Daily Talks",
                subtitle = "ကော်ဖီဆိုင်၊ စားသောက်ဆိုင်နှင့် နေ့စဉ်လူနေမှု",
                badge = "☕ Daily Life",
                color = LilacPurple,
                nodes = listOf(
                    QuestLessonNode("u2_n1", "Ordering Coffee", "ကော်ဖီနှင့် အစားအသောက် မှာယူခြင်း", "☕", 0, NodeState.LOCKED),
                    QuestLessonNode("u2_n2", "Present Continuous", "လုပ်ဆောင်ဆဲ လုပ်ငန်းများ (-ing)", "⏳", 0, NodeState.LOCKED),
                    QuestLessonNode("u2_n3", "Countable & Uncountable", "Much vs. Many ပမာဏပြများ", "🍰", 0, NodeState.LOCKED),
                    QuestLessonNode("u2_n4", "Unit 2 Milestone Chest", "စိန်ပွင့်ဆုလာဘ်", "🎁", 0, NodeState.LOCKED)
                )
            ),
            QuestUnit(
                number = 3,
                title = "Unit 3: Travel, Directions & Flights",
                subtitle = "လေဆိပ်၊ ခရီးသွားလုပ်ငန်းနှင့် လမ်းညွှန်စကား",
                badge = "✈️ Adventure",
                color = PeachPastel,
                nodes = listOf(
                    QuestLessonNode("u3_n1", "At the Airport", "လေဆိပ် Check-in နှင့် စကားပြော", "🛫", 0, NodeState.LOCKED),
                    QuestLessonNode("u3_n2", "Past Simple Verbs", "အတိတ်ကာလ ကြိယာပုံစံများ (V2)", "🕰️", 0, NodeState.LOCKED),
                    QuestLessonNode("u3_n3", "Asking Directions", "ဘယ်ဘက်၊ ညာဘက် လမ်းညွှန်စကား", "🧭", 0, NodeState.LOCKED),
                    QuestLessonNode("u3_n4", "Unit 3 Milestone Chest", "ရတနာသေတ္တာ", "🎁", 0, NodeState.LOCKED)
                )
            ),
            QuestUnit(
                number = 4,
                title = "Unit 4: Author's Grammar & Fluency",
                subtitle = "ထက် ၏ A1–C2 သဒ္ဒါစည်းမျဉ်းများနှင့် ဝါကျဖွဲ့ထုံး",
                badge = "📖 Master Grammar",
                color = MintPastel,
                nodes = listOf(
                    QuestLessonNode("u4_n1", "Present Perfect", "Have/Has + V3 ချိတ်ဆက်ပုံ", "💎", 0, NodeState.LOCKED),
                    QuestLessonNode("u4_n2", "Conditionals (If Clause)", "အခြေအနေပြ ဝါကျများ (If-rules)", "🔮", 0, NodeState.LOCKED),
                    QuestLessonNode("u4_n3", "Passive Voice Mastery", "Subject အာရုံပြောင်း ဝါကျများ", "⚡", 0, NodeState.LOCKED),
                    QuestLessonNode("u4_n4", "Crown Champion Node", "ဂုဏ်ထူးဆောင် သရဖူစိန်", "👑", 0, NodeState.LOCKED)
                )
            )
        )
    }

    // Active Interactive Lesson Dialog
    if (activeLesson != null) {
        InteractiveLessonDialog(
            lesson = activeLesson!!,
            currentHearts = hearts,
            onSpeak = onSpeak,
            onHeartLost = onHeartLost,
            onLessonComplete = {
                onRewardEarned(activeLesson!!.xpReward, activeLesson!!.gemReward)
                activeLesson = null
            },
            onDismiss = { activeLesson = null }
        )
    }

    // Heart Refill Dialog
    if (showHeartRefillDialog) {
        AlertDialog(
            onDismissRequest = { showHeartRefillDialog = false },
            icon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = SakuraPink, modifier = Modifier.size(36.dp)) },
            title = { Text("အသည်းနှလုံး ပြန်လည်ဖြည့်မည်လား?", fontWeight = FontWeight.Bold) },
            text = { Text("သင့်တွင် အသည်းနှလုံး $hearts ခုသာ ကျန်ရှိပါတော့သည်။ Gems 30 💎 ဖြင့် အသည်း ၅ ခု အပြည့် ပြန်လည်ဖြည့်နိုင်ပါသည်။") },
            confirmButton = {
                Button(
                    onClick = {
                        onRefillHearts()
                        showHeartRefillDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SakuraPink)
                ) { Text("ဖြည့်မည် (Use Gems)") }
            },
            dismissButton = {
                TextButton(onClick = { showHeartRefillDialog = false }) { Text("ပိတ်မည်") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // FLOATING TOP STATS BAR (Duolingo Style: Streak, Gems, Hearts)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .shadow(12.dp, RoundedCornerShape(999.dp), ambientColor = SakuraPink.copy(alpha = 0.2f), spotColor = LilacPurple.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, GlassBorderGradient)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Streak Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { }
                    ) {
                        Text("🔥", fontSize = 18.sp)
                        Text("$streak Days", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = PeachPastel)
                    }

                    // Gems Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { }
                    ) {
                        Text("💎", fontSize = 18.sp)
                        Text("$gems", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SkyBlue)
                    }

                    // Hearts Pill
                    Surface(
                        color = SakuraPink.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.clickable {
                            if (hearts < 5) showHeartRefillDialog = true
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("❤️", fontSize = 16.sp)
                            Text("$hearts/5", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = SakuraPink)
                            if (hearts < 5) {
                                Text("+", fontWeight = FontWeight.Bold, color = SakuraPink, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // S-CURVED GAMIFIED QUEST PATH
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                units.forEach { unit ->
                    item(key = "unit_header_${unit.number}") {
                        // Unit Header Card (Liquid Glass with soft pastel gradient)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = unit.color.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(20.dp),
                            color = unit.color.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, unit.color.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    BadgePill(unit.badge, unit.color)
                                    Text(unit.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Text(unit.subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(unit.color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }

                    // Stepping-Stone Circular Nodes (S-Curved Path like Duolingo!)
                    itemsIndexed(unit.nodes, key = { _, node -> node.id }) { index, node ->
                        // Calculate horizontal S-curve offset using sine wave
                        val waveOffsets = listOf(0.dp, 48.dp, 80.dp, 48.dp, 0.dp, (-48).dp, (-80).dp, (-48).dp)
                        val offset = waveOffsets[index % waveOffsets.size]

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.offset(x = offset),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QuestNodeItem(
                                    node = node,
                                    themeColor = unit.color,
                                    onClick = {
                                        if (node.state != NodeState.LOCKED) {
                                            activeLesson = node
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Circular Stepping-Stone Node with 3D button effect and stars
@Composable
fun QuestNodeItem(
    node: QuestLessonNode,
    themeColor: Color,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (node.state == NodeState.CURRENT) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Node Circle
        Surface(
            modifier = Modifier
                .size(68.dp)
                .scale(pulseScale)
                .shadow(
                    elevation = if (node.state == NodeState.CURRENT) 14.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = themeColor.copy(alpha = 0.4f),
                    spotColor = themeColor.copy(alpha = 0.6f)
                )
                .clickable(enabled = node.state != NodeState.LOCKED, onClick = onClick),
            shape = CircleShape,
            color = when (node.state) {
                NodeState.COMPLETED -> WarmGold
                NodeState.CURRENT -> themeColor
                NodeState.LOCKED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            },
            border = BorderStroke(
                width = if (node.state == NodeState.CURRENT) 3.5.dp else 2.dp,
                color = when (node.state) {
                    NodeState.COMPLETED -> Color(0xFFFDE047)
                    NodeState.CURRENT -> Color.White
                    NodeState.LOCKED -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                }
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (node.state == NodeState.LOCKED) "🔒" else node.icon,
                    fontSize = 26.sp
                )
            }
        }

        // Stars earned (⭐⭐⭐)
        if (node.state == NodeState.COMPLETED) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { i ->
                    Text(
                        text = if (i < node.stars) "⭐" else "☆",
                        fontSize = 11.sp
                    )
                }
            }
        } else if (node.state == NodeState.CURRENT) {
            Surface(
                color = themeColor,
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    "START",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// FULL SCREEN INTERACTIVE LESSON DIALOG (Word Bubbles + Chime + Feedback)
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveLessonDialog(
    lesson: QuestLessonNode,
    currentHearts: Int,
    onSpeak: (String, Locale, Float) -> Unit,
    onHeartLost: () -> Unit,
    onLessonComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    // Interactive Exercises Bank for the lesson
    val exercises = remember {
        listOf(
            GamifiedExercise(
                kind = ExerciseKind.WORD_BUBBLE,
                prompt = "အောက်ပါ စကားလုံးများကို စီစဉ်ပြီး ဝါကျတည်ဆောက်ပါ:",
                targetAudio = "I have a cup of coffee every morning.",
                correctSentence = listOf("I", "have", "a", "cup", "of", "coffee", "every", "morning"),
                bubblePool = listOf("I", "morning", "have", "a", "tea", "cup", "of", "water", "coffee", "every")
            ),
            GamifiedExercise(
                kind = ExerciseKind.MULTI_CHOICE,
                prompt = "\"ကျေးဇူးပြု၍ ကူညီပေးပါလား?\" ၏ အင်္ဂလိပ်စကားပြောမှာ:",
                targetAudio = "Could you please help me?",
                options = listOf("Could you please help me?", "Where is the library?", "I don't know the answer", "Have a great day"),
                correctIndex = 0
            ),
            GamifiedExercise(
                kind = ExerciseKind.PAIR_MATCH,
                prompt = "ကိုက်ညီသော စကားလုံးအတွဲများကို နှိပ်ပါ (Tap matching pairs):",
                targetAudio = "resilience language culture connect",
                pairsEnMy = listOf(
                    "resilience" to "ကြံ့ကြံ့ခံနိုင်စွမ်း",
                    "culture" to "ယဉ်ကျေးမှု",
                    "language" to "ဘာသာစကား",
                    "connect" to "ချိတ်ဆက်သည်"
                )
            ),
            GamifiedExercise(
                kind = ExerciseKind.LISTENING,
                prompt = "အသံကို ဂရုတစိုက် နားထောင်ပြီး မှန်ကန်သော အဓိပ္ပာယ်ကို ရွေးချယ်ပါ:",
                targetAudio = "Language learning cultivates cross-cultural empathy.",
                options = listOf(
                    "ဘာသာစကားသင်ယူခြင်းသည် စာနာနားလည်မှုကို တိုးပွားစေသည်",
                    "မနက်ဖြန် မိုးရွာသွန်းနိုင်ခြေရှိသည်",
                    "သူမသည် စာကြည့်တိုက်သို့ သွားရောက်ခဲ့သည်",
                    "အလုပ်ခွင်တွင် ကွန်ပျူတာ အသုံးပြုရသည်"
                ),
                correctIndex = 0
            )
        )
    }

    var currentStep by remember { mutableIntStateOf(0) }
    var livesRemaining by remember { mutableIntStateOf(currentHearts) }
    val selectedBubbles = remember { mutableStateListOf<String>() }
    var selectedMcq by remember { mutableStateOf<Int?>(null) }
    val matchedPairs = remember { mutableStateListOf<String>() }
    var selectedEnWord by remember { mutableStateOf<String?>(null) }

    var checkStatus by remember { mutableStateOf<Boolean?>(null) } // true=Correct, false=Wrong
    var showCelebration by remember { mutableStateOf(false) }

    val currentExercise = exercises.getOrNull(currentStep) ?: exercises[0]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showCelebration) {
                // CELEBRATION SCREEN (Confetti / Joyful mascot burst)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🌸✨🎉", fontSize = 54.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "သင်ခန်းစာ အောင်မြင်ပါပြီ!",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = SakuraPink
                    )
                    Text(
                        "Lesson Completed Successfully!",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(24.dp))

                    // Rewards Card
                    Surface(
                        modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, GlassBorderGradient)
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚡", fontSize = 28.sp)
                                Text("+25 XP", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = WarmGold)
                                Text("Exp Gained", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💎", fontSize = 28.sp)
                                Text("+10 Gems", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SkyBlue)
                                Text("Crystals", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔥", fontSize = 28.sp)
                                Text("Streak +1", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PeachPastel)
                                Text("Continuous", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = onLessonComplete,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SakuraPink)
                    ) {
                        Text("ဆက်လက်ဆောင်ရွက်မည် (Continue)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                return@Surface
            }

            Scaffold(
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Exit")
                        }

                        // Cute Progress Bar
                        LinearProgressIndicator(
                            progress = { (currentStep + 1).toFloat() / exercises.size },
                            modifier = Modifier
                                .weight(1f)
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp)),
                            color = SakuraPink,
                            trackColor = SakuraPink.copy(alpha = 0.2f)
                        )

                        // Heart Lives Indicator
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("❤️", fontSize = 16.sp)
                            Text("$livesRemaining", fontWeight = FontWeight.ExtraBold, color = SakuraPink, fontSize = 14.sp)
                        }
                    }
                },
                bottomBar = {
                    // Bottom Action & Feedback Bar (Joyful Green or Supportive Coral)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = when (checkStatus) {
                            true -> MintLight
                            false -> Color(0xFFFFF1F2)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (checkStatus != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        if (checkStatus == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (checkStatus == true) MintPastel else SakuraDark
                                    )
                                    Column {
                                        Text(
                                            if (checkStatus == true) "အံ့ဩဖွယ် မှန်ကန်ပါတယ်! (Excellent!)" else "အဖြေမှားယွင်းသွားပါသည် (Not quite!)",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = if (checkStatus == true) MintPastel else SakuraDark
                                        )
                                        if (checkStatus == false) {
                                            Text(
                                                "Target: ${currentExercise.targetAudio}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (checkStatus == null) {
                                        // CHECK ANSWER
                                        var isRight = false
                                        when (currentExercise.kind) {
                                            ExerciseKind.WORD_BUBBLE -> {
                                                isRight = selectedBubbles.toList() == currentExercise.correctSentence
                                            }
                                            ExerciseKind.MULTI_CHOICE, ExerciseKind.LISTENING -> {
                                                isRight = selectedMcq == currentExercise.correctIndex
                                            }
                                            ExerciseKind.PAIR_MATCH -> {
                                                isRight = matchedPairs.size >= currentExercise.pairsEnMy.size * 2
                                            }
                                        }

                                        checkStatus = isRight
                                        if (!isRight) {
                                            livesRemaining = (livesRemaining - 1).coerceAtLeast(0)
                                            onHeartLost()
                                        }
                                    } else {
                                        // NEXT EXERCISE OR COMPLETE
                                        if (currentStep + 1 >= exercises.size) {
                                            showCelebration = true
                                        } else {
                                            currentStep++
                                            selectedBubbles.clear()
                                            selectedMcq = null
                                            selectedEnWord = null
                                            matchedPairs.clear()
                                            checkStatus = null
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (checkStatus == true) MintPastel else if (checkStatus == false) SakuraDark else SakuraPink
                                )
                            ) {
                                Text(
                                    if (checkStatus == null) "စစ်ဆေးမည် (Check)" else "ဆက်သွားမည် (Continue)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        currentExercise.prompt,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Audio speaker button
                    if (currentExercise.targetAudio.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilledIconButton(
                                onClick = { onSpeak(currentExercise.targetAudio, Locale.US, 0.95f) },
                                modifier = Modifier.size(46.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SakuraPink)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play", tint = Color.White)
                            }
                            Text(
                                "အသံကို နားထောင်ရန် နှိပ်ပါ",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 1. WORD BUBBLE EXERCISE (Tappable Candy Bubbles!)
                    if (currentExercise.kind == ExerciseKind.WORD_BUBBLE) {
                        // Sentence Builder Line (Drop Area)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 68.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.5.dp, SakuraPink.copy(alpha = 0.35f))
                        ) {
                            FlowRow(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedBubbles.isEmpty()) {
                                    Text(
                                        "စကားလုံးများကို အစဉ်လိုက် နှိပ်ပါ…",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 10.dp, start = 6.dp)
                                    )
                                } else {
                                    selectedBubbles.forEachIndexed { idx, word ->
                                        Surface(
                                            modifier = Modifier.clickable {
                                                selectedBubbles.removeAt(idx)
                                            },
                                            shape = RoundedCornerShape(999.dp),
                                            color = SakuraPink,
                                            shadowElevation = 4.dp
                                        ) {
                                            Text(
                                                word,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Word Bubble Pool Area (Tappable Choices)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentExercise.bubblePool.forEach { word ->
                                val isUsed = selectedBubbles.contains(word)
                                Surface(
                                    modifier = Modifier
                                        .clickable(enabled = !isUsed) {
                                            selectedBubbles.add(word)
                                            onSpeak(word, Locale.US, 1f)
                                        },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isUsed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.5.dp, if (isUsed) Color.Transparent else LilacPurple.copy(alpha = 0.4f)),
                                    shadowElevation = if (isUsed) 0.dp else 4.dp
                                ) {
                                    Text(
                                        word,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = if (isUsed) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. MULTIPLE CHOICE OR LISTENING EXERCISE
                    if (currentExercise.kind == ExerciseKind.MULTI_CHOICE || currentExercise.kind == ExerciseKind.LISTENING) {
                        currentExercise.options.forEachIndexed { idx, opt ->
                            val isSelected = selectedMcq == idx
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMcq = idx },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) SakuraPink.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) SakuraPink else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                shadowElevation = if (isSelected) 6.dp else 2.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) SakuraPink else MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${idx + 1}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        opt,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // 3. PAIR MATCHING EXERCISE (Tap pairs)
                    if (currentExercise.kind == ExerciseKind.PAIR_MATCH) {
                        Text("English Words:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LilacPurple)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentExercise.pairsEnMy.forEach { pair ->
                                val isDone = matchedPairs.contains(pair.first)
                                val isSelected = selectedEnWord == pair.first

                                Surface(
                                    modifier = Modifier.clickable(enabled = !isDone) {
                                        selectedEnWord = pair.first
                                        onSpeak(pair.first, Locale.US, 1f)
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = when {
                                        isDone -> MintPastel.copy(alpha = 0.2f)
                                        isSelected -> LilacPurple
                                        else -> MaterialTheme.colorScheme.surface
                                    },
                                    border = BorderStroke(1.5.dp, if (isSelected) LilacPurple else LilacPurple.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        pair.first,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else if (isDone) MintPastel else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Text("Burmese Meanings (မြန်မာအဓိပ္ပာယ်):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SakuraPink)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currentExercise.pairsEnMy.forEach { pair ->
                                val isDone = matchedPairs.contains(pair.second)

                                Surface(
                                    modifier = Modifier.clickable(enabled = !isDone && selectedEnWord != null) {
                                        if (selectedEnWord == pair.first) {
                                            matchedPairs.add(pair.first)
                                            matchedPairs.add(pair.second)
                                            selectedEnWord = null
                                        }
                                    },
                                    shape = RoundedCornerShape(999.dp),
                                    color = if (isDone) MintPastel.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.5.dp, SakuraPink.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        pair.second,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDone) MintPastel else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
