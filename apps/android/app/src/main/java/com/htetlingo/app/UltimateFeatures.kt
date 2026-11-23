package com.htetlingo.app

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
import androidx.compose.material.icons.filled.*
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

// MARK: - 1. 710 Interactive Grammar & Exam Exercises (Areso + Vera-com)
@Composable
fun GrammarExercisesSection(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    val topics = listOf("ALL", "Htet Book", "Grammar Exam", "Articles", "Do and Make", "Prepositions", "Phrasal", "Irregular", "Negative")
    var selectedTopic by remember { mutableStateOf("ALL") }
    var seed by remember { mutableIntStateOf(0) }
    val exercises = remember(selectedTopic, seed) { db.fetchGrammarExercises(selectedTopic, 20) }
    var step by remember(selectedTopic, seed) { mutableIntStateOf(0) }
    var chosen by remember(selectedTopic, seed, step) { mutableStateOf<String?>(null) }
    var score by remember(selectedTopic, seed) { mutableIntStateOf(0) }

    val current = exercises.getOrNull(step)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            topics.forEach { t ->
                FilterChip(
                    selected = selectedTopic == t,
                    onClick = { selectedTopic = t; seed++ },
                    label = { Text(t) }
                )
            }
        }

        if (current != null) {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        BadgePill(current.topic.take(28), IndigoAccent)
                        Text("${step + 1} / ${exercises.size} · Score: $score", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(current.question, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceChip("🇺🇸 Read US", TealPrimary) {
                            onSpeak(current.question.replace("_____", current.answer), Locale.US, 0.95f)
                        }
                        VoiceChip("🇬🇧 Read UK", IndigoAccent) {
                            onSpeak(current.question.replace("_____", current.answer), Locale.UK, 0.95f)
                        }
                    }

                    current.options.forEach { opt ->
                        val isRight = opt.equals(current.answer, ignoreCase = true)
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
                                    score++
                                    onXp(15)
                                }
                            }
                        ) {
                            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(opt, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                if (chosen != null && isRight) Icon(Icons.Default.CheckCircle, null, tint = EmeraldGreen)
                                if (isPick && !isRight) Icon(Icons.Default.Cancel, null, tint = RosePink)
                            }
                        }
                    }

                    if (chosen != null) {
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                            Text(current.explanation, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                        }
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (step + 1 >= exercises.size) seed++ else step++
                            }
                        ) { Text(if (step + 1 >= exercises.size) "Load Next 20 Exercises" else "Next Exercise") }
                    }
                }
            }
        }
    }
}

// MARK: - 2. Spelling Bee Honeycomb & Audio Spelling Challenge (josephmawa/SpellingBee)
@Composable
fun SpellingBeeSection(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) } // 0 = Honeycomb Bee, 1 = Audio Dictation Bee
    val puzzles = remember { db.fetchSpellingBeePuzzles() }
    var puzzleIdx by remember { mutableIntStateOf(0) }
    var typed by remember(puzzleIdx) { mutableStateOf("") }
    var found by remember(puzzleIdx) { mutableStateOf(setOf<String>()) }
    var feedback by remember(puzzleIdx) { mutableStateOf("Tap letters to build words (4+ letters, must include center gold letter)!") }

    val audioWords = remember(puzzleIdx) { db.randomVocab(25) }
    var audioIdx by remember { mutableIntStateOf(0) }
    var audioInput by remember(audioIdx) { mutableStateOf("") }
    var audioChecked by remember(audioIdx) { mutableStateOf(false) }

    Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(WarmGold, CoralOrange)))
                .padding(16.dp)
        ) {
            Column {
                Text("🐝 Spelling Bee & Audio Dictation", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("7-letter honeycomb word builder + US/UK Audio Spelling Challenge", color = Color.White.copy(alpha = 0.92f), fontSize = 12.sp)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == 0, onClick = { mode = 0 }, label = { Text("🐝 Honeycomb Bee") })
            FilterChip(selected = mode == 1, onClick = { mode = 1 }, label = { Text("🎧 Audio Spelling") })
        }

        if (mode == 0 && puzzles.isNotEmpty()) {
            val p = puzzles[puzzleIdx % puzzles.size]
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BadgePill("Found: ${found.size} / ${p.answers.size}", WarmGold)
                        TextButton(onClick = { puzzleIdx++ }) { Text("New Honeycomb →") }
                    }

                    Text(typed.ifEmpty { "—" }, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                    Text(feedback, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Center Golden Letter + 6 Outer Letters
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        p.outer.take(3).forEach { ch ->
                            FilledTonalButton(onClick = { typed += ch }) { Text(ch, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Button(
                        onClick = { typed += p.center },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmGold),
                        shape = CircleShape
                    ) {
                        Text("★ ${p.center}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        p.outer.drop(3).forEach { ch ->
                            FilledTonalButton(onClick = { typed += ch }) { Text(ch, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { if (typed.isNotEmpty()) typed = typed.dropLast(1) }) { Text("Delete") }
                        OutlinedButton(onClick = { typed = "" }) { Text("Clear") }
                        Button(
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen),
                            onClick = {
                                val w = typed.lowercase()
                                when {
                                    w.length < 4 -> feedback = "Too short (need 4+ letters)"
                                    !w.contains(p.center.lowercase()) -> feedback = "Must include center letter ${p.center}!"
                                    w in found -> feedback = "Already found!"
                                    w in p.answers -> {
                                        found = found + w
                                        feedback = "Awesome! +12 XP for '$w'"
                                        onSpeak(w, Locale.US, 0.95f)
                                        onXp(12)
                                        typed = ""
                                    }
                                    else -> feedback = "Not in puzzle word list — try another!"
                                }
                            }
                        ) { Text("Submit") }
                    }

                    if (found.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Discovered Words: ${found.joinToString(", ")}", fontSize = 13.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else if (mode == 1 && audioWords.isNotEmpty()) {
            val target = audioWords[audioIdx % audioWords.size]
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BadgePill("CEFR ${target.level}", cefrColor(target.level))
                        Text("Meaning: 🇲🇲 ${target.myDef}", fontSize = 13.sp, color = TealPrimary, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            onClick = { onSpeak(target.word, Locale.US, 0.88f) }
                        ) { Text("🇺🇸 Hear US") }
                        Button(
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoAccent),
                            onClick = { onSpeak(target.word, Locale.UK, 0.88f) }
                        ) { Text("🇬🇧 Hear UK") }
                    }
                    OutlinedTextField(
                        value = audioInput,
                        onValueChange = { audioInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Type the exact English word you heard") },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            modifier = Modifier.weight(1f),
                            onClick = {
                                audioChecked = true
                                if (audioInput.trim().equals(target.word, ignoreCase = true)) {
                                    onXp(15)
                                }
                            }
                        ) { Text("Check Spelling") }
                        OutlinedButton(onClick = { audioIdx++ }) { Text("Next Word →") }
                    }
                    if (audioChecked) {
                        val ok = audioInput.trim().equals(target.word, ignoreCase = true)
                        Text(
                            if (ok) "✓ Correct! ${target.word} (+15 XP)" else "✗ Correct spelling: ${target.word}",
                            color = if (ok) EmeraldGreen else RosePink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// MARK: - 3. English for Developers & Tech Interview Studio (English-For-Devs/interview)
@Composable
fun DevInterviewSection(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val items = remember(query) { db.fetchDevInterview(query) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(IndigoAccent, TealPrimary)))
                .padding(16.dp)
        ) {
            Column {
                Text("💻 English for Developers & Tech Interviews", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text("Real software engineering interview Q&A & communication templates", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            placeholder = { Text("Search interview questions (e.g., tell me about yourself, salary, strengths)…") },
            singleLine = true
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items) { item ->
                ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            BadgePill(item.category, IndigoAccent)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                VoiceChip("🇺🇸", TealPrimary) { onSpeak(item.title, Locale.US, 0.95f) }
                                VoiceChip("🇬🇧", IndigoAccent) { onSpeak(item.title, Locale.UK, 0.95f) }
                            }
                        }
                        Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(item.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}
