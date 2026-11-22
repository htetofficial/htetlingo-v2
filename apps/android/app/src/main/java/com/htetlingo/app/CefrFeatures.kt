package com.htetlingo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val CEFR_LEVELS = listOf("A1", "A2", "B1", "B2", "C1", "C2")

fun cefrColor(level: String): Color = when (level) {
    "A1" -> EmeraldGreen
    "A2" -> TealPrimary
    "B1" -> SkyBlue
    "B2" -> IndigoAccent
    "C1" -> CoralOrange
    else -> RosePink
}

data class CefrPlacementItem(
    val level: String,
    val entry: VocabEntry,
    val options: List<String>
)

@Composable
fun CefrStudioSection(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = Level Test, 1 = Text Grader

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(DeepPurple, IndigoAccent, SkyBlue)))
                .padding(16.dp)
        ) {
            Column {
                Text("CEFR English Level Test & Text Grader", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Powered by CEFR-J · Octanove C1–C2 · Maximax67 (173k CEFR map)", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = mode == 0,
                onClick = { mode = 0 },
                leadingIcon = { Icon(Icons.Default.School, null, modifier = Modifier.size(16.dp)) },
                label = { Text("A1–C2 Placement Test") }
            )
            FilterChip(
                selected = mode == 1,
                onClick = { mode = 1 },
                leadingIcon = { Icon(Icons.Default.Analytics, null, modifier = Modifier.size(16.dp)) },
                label = { Text("Text CEFR Grader") }
            )
        }

        if (mode == 0) {
            CefrPlacementTestView(db, onSpeak, onXp)
        } else {
            CefrTextGraderView(db, onSpeak)
        }
    }
}

@Composable
private fun CefrPlacementTestView(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    var seed by remember { mutableIntStateOf(0) }
    val questions = remember(seed) {
        val distractorsPool = db.randomVocab(40)
        CEFR_LEVELS.flatMap { lvl ->
            db.sampleByCefrLevel(lvl, 2).map { item ->
                val dists = distractorsPool.filter { it.id != item.id }.shuffled().take(3).map { it.myDef }
                CefrPlacementItem(lvl, item, (listOf(item.myDef) + dists).shuffled())
            }
        }
    }
    var step by remember(seed) { mutableIntStateOf(0) }
    var chosen by remember(seed, step) { mutableStateOf<String?>(null) }
    var correctByLevel by remember(seed) { mutableStateOf(mapOf<String, Int>()) }
    var finished by remember(seed) { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (questions.isEmpty()) {
            Text("Preparing CEFR questions…")
            return@Column
        }

        LinearProgressIndicator(
            progress = { (if (finished) questions.size else step).toFloat() / questions.size.toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
        )

        if (finished) {
            val totalCorrect = correctByLevel.values.sum()
            val estimated = when {
                totalCorrect >= 11 -> "C2 (Proficiency)"
                totalCorrect >= 9 -> "C1 (Advanced)"
                totalCorrect >= 7 -> "B2 (Upper Intermediate)"
                totalCorrect >= 5 -> "B1 (Intermediate)"
                totalCorrect >= 3 -> "A2 (Elementary)"
                else -> "A1 (Beginner)"
            }
            val bandCode = estimated.take(2)
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.EmojiEvents, null, tint = cefrColor(bandCode), modifier = Modifier.size(56.dp))
                    Text("Your Estimated English Level", style = MaterialTheme.typography.labelLarge)
                    Text(estimated, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = cefrColor(bandCode))
                    Text("Accuracy: $totalCorrect / ${questions.size} (+${totalCorrect * 20} XP)")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CEFR_LEVELS.forEach { lvl ->
                            val c = correctByLevel[lvl] ?: 0
                            BadgePill("$lvl: $c/2", cefrColor(lvl))
                        }
                    }
                    Button(onClick = { seed++ }) { Text("Retake Placement Test") }
                }
            }
        } else {
            val q = questions[step]
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        BadgePill("CEFR ${q.level}", cefrColor(q.level))
                        Text("Step ${step + 1} / ${questions.size}", style = MaterialTheme.typography.labelMedium)
                    }
                    Text("What does \"${q.entry.word}\" mean?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceChip("🇺🇸 US", TealPrimary) { onSpeak(q.entry.word, Locale.US, 0.95f) }
                        VoiceChip("🇬🇧 UK", IndigoAccent) { onSpeak(q.entry.word, Locale.UK, 0.95f) }
                    }
                    q.options.forEach { opt ->
                        val isRight = opt == q.entry.myDef
                        val isPick = chosen == opt
                        val bg = when {
                            chosen == null -> MaterialTheme.colorScheme.surfaceVariant
                            isRight -> EmeraldGreen.copy(alpha = 0.22f)
                            isPick -> RosePink.copy(alpha = 0.22f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Surface(
                            color = bg,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().clickable(enabled = chosen == null) {
                                chosen = opt
                                if (isRight) {
                                    correctByLevel = correctByLevel + (q.level to ((correctByLevel[q.level] ?: 0) + 1))
                                    onXp(20)
                                }
                            }
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(opt, modifier = Modifier.weight(1f))
                                if (chosen != null && isRight) Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen)
                            }
                        }
                    }
                    if (chosen != null) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (step + 1 >= questions.size) finished = true else step++
                            }
                        ) { Text(if (step + 1 >= questions.size) "View My CEFR Level" else "Next Question") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CefrTextGraderView(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit
) {
    var input by remember {
        mutableStateOf(
            "Language learning cultivates cognitive resilience and fosters cross-cultural empathy across diverse global communities."
        )
    }

    val tokens = remember(input) {
        Regex("[A-Za-z]{2,}").findAll(input).map { it.value.lowercase() }.toSet()
    }
    val matched = remember(tokens) { db.lookupWordsForGrader(tokens) }
    val counts = remember(matched) {
        val m = mutableMapOf("A1" to 0, "A2" to 0, "B1" to 0, "B2" to 0, "C1" to 0, "C2" to 0)
        matched.values.forEach { v -> m[v.level] = (m[v.level] ?: 0) + 1 }
        m
    }
    val totalMatched = matched.size.coerceAtLeast(1)
    val weightedScore = remember(counts, totalMatched) {
        val sum = (counts["A1"]!! * 1) + (counts["A2"]!! * 2) + (counts["B1"]!! * 3) +
            (counts["B2"]!! * 4) + (counts["C1"]!! * 5) + (counts["C2"]!! * 6)
        sum.toFloat() / totalMatched.toFloat()
    }
    val overallBand = when {
        weightedScore >= 5.1f -> "C2"
        weightedScore >= 4.2f -> "C1"
        weightedScore >= 3.3f -> "B2"
        weightedScore >= 2.4f -> "B1"
        weightedScore >= 1.6f -> "A2"
        else -> "A1"
    }
    val advancedWords = remember(matched) {
        matched.values.filter { it.level in setOf("B2", "C1", "C2") }.sortedByDescending { CEFR_LEVELS.indexOf(it.level) }
    }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            label = { Text("Paste any English paragraph to grade its CEFR difficulty") }
        )

        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Estimated Passage Level", style = MaterialTheme.typography.labelMedium)
                        Text("CEFR $overallBand", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = cefrColor(overallBand))
                    }
                    Text("${matched.size} / ${tokens.size} words profiled", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                CEFR_LEVELS.forEach { lvl ->
                    val cnt = counts[lvl] ?: 0
                    val ratio = cnt.toFloat() / totalMatched.toFloat()
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(lvl, fontWeight = FontWeight.Bold, color = cefrColor(lvl), modifier = Modifier.width(28.dp))
                        LinearProgressIndicator(
                            progress = { ratio },
                            color = cefrColor(lvl),
                            modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape)
                        )
                        Text("$cnt", fontSize = 12.sp, modifier = Modifier.width(26.dp))
                    }
                }
            }
        }

        if (advancedWords.isNotEmpty()) {
            Text("Key Advanced Words (B2–C2)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            advancedWords.forEach { w ->
                ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(w.word, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                BadgePill(w.level, cefrColor(w.level))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                VoiceChip("🇺🇸", TealPrimary) { onSpeak(w.word, Locale.US, 0.95f) }
                                VoiceChip("🇬🇧", IndigoAccent) { onSpeak(w.word, Locale.UK, 0.95f) }
                            }
                        }
                        Text("🇲🇲 ${w.myDef}", color = TealPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (w.zhDef.isNotEmpty()) Text("🇨🇳 ${w.zhDef}", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
