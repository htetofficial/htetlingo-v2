package com.htetlingo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun HtetBookSection(
    db: DictionaryDbHelper,
    onSpeak: (String, Locale, Float) -> Unit,
    onXp: (Int) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) } // 0=49 Lessons, 1=24 Vocab, 2=8 Readings, 3=9 Writings, 4=97 Verbs, 5=16-Week Plan
    var query by remember { mutableStateOf("") }
    var expandedLesson by remember { mutableIntStateOf(1) }
    var showAnswerFor by remember { mutableStateOf(setOf<Int>()) }

    val lessons = remember(query) { db.fetchBookGrammarLessons(query) }
    val vocabUnits = remember { db.fetchBookVocabUnits() }
    val readings = remember { db.fetchBookReadings() }
    val writings = remember { db.fetchBookWritings() }
    val verbs = remember(query) { db.fetchIrregularVerbs(query) }
    val plan = remember { db.fetchStudyPlan() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Slim Header Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📚 Htet A1–C2 Book", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            BadgePill("© 2026 ထက် · 49 Lessons", IndigoAccent)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            FilterChip(selected = tab == 0, onClick = { tab = 0; query = "" }, label = { Text("49 Lessons") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("24 Vocab") })
            FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("8 Reading Lab") })
            FilterChip(selected = tab == 3, onClick = { tab = 3 }, label = { Text("9 Writing") })
            FilterChip(selected = tab == 4, onClick = { tab = 4; query = "" }, label = { Text("97 Verbs") })
            FilterChip(selected = tab == 5, onClick = { tab = 5 }, label = { Text("16-Week Plan") })
        }

        if (tab == 0 || tab == 4) {
            CompactSearchBar(
                value = query,
                onValueChange = { query = it },
                placeholder = if (tab == 0) "Search 49 A1–C2 lessons (Passive, Conditionals, C1...)" else "Search 97 irregular verbs (go, went, gone)..."
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 18.dp)) {
            when (tab) {
                0 -> items(lessons, key = { it.number }) { l ->
                    val open = expandedLesson == l.number
                    val ansVisible = l.number in showAnswerFor
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            expandedLesson = if (open) -1 else l.number
                            if (!open) onXp(2)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Lesson ${"%02d".format(l.number)} · ${l.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                                BadgePill(l.level, cefrColor(l.level.take(2)))
                            }
                            if (open) {
                                Text(l.explanation, fontSize = 14.sp, lineHeight = 21.sp)
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                                    Text(l.patterns, modifier = Modifier.padding(10.dp), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(l.examples, fontSize = 13.sp, color = TealPrimary, lineHeight = 20.sp)
                                HorizontalDivider()
                                Text("Practice:\n${l.practice}", fontSize = 13.sp)
                                OutlinedButton(onClick = {
                                    showAnswerFor = if (ansVisible) showAnswerFor - l.number else showAnswerFor + l.number
                                }) { Text(if (ansVisible) "Hide Answer Key" else "Show Answer Key") }
                                if (ansVisible) {
                                    Text(l.answers, color = EmeraldGreen, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                1 -> items(vocabUnits, key = { it.number }) { u ->
                    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Unit ${"%02d".format(u.number)} · ${u.title}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                BadgePill(u.level, IndigoAccent)
                            }
                            u.words.forEach { (en, my) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(en, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(my, color = TealPrimary, fontSize = 13.sp)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        VoiceChip("🇺🇸", TealPrimary) { onSpeak(en, Locale.US, 0.95f) }
                                        VoiceChip("🇬🇧", IndigoAccent) { onSpeak(en, Locale.UK, 0.95f) }
                                    }
                                }
                            }
                            HorizontalDivider()
                            Text("Collocations: ${u.collocations}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(u.examples, fontSize = 13.sp)
                        }
                    }
                }

                2 -> items(readings, key = { it.number }) { r ->
                    val ansVisible = (100 + r.number) in showAnswerFor
                    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    BadgePill("Reading ${"%02d".format(r.number)} · ${r.level}", SkyBlue)
                                    Text(r.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    VoiceChip("🇺🇸", TealPrimary) { onSpeak(r.passage, Locale.US, 0.92f) }
                                    VoiceChip("🇬🇧", IndigoAccent) { onSpeak(r.passage, Locale.UK, 0.92f) }
                                }
                            }
                            Text(r.passage, fontSize = 14.sp, lineHeight = 22.sp)
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                                Text("Grammar Notes: ${r.grammarNotes}", modifier = Modifier.padding(10.dp), fontSize = 13.sp)
                            }
                            Text("Questions:\n${r.questions}", fontSize = 13.sp)
                            OutlinedButton(onClick = {
                                val k = 100 + r.number
                                showAnswerFor = if (ansVisible) showAnswerFor - k else showAnswerFor + k
                            }) { Text(if (ansVisible) "Hide Answers" else "Show Suggested Answers") }
                            if (ansVisible) Text(r.answers, color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                3 -> items(writings, key = { it.number }) { w ->
                    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    BadgePill("Writing ${"%02d".format(w.number)} · ${w.level}", DeepPurple)
                                    Text(w.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                VoiceChip("🔊 Listen", IndigoAccent) { onSpeak(w.modelAnswer, Locale.UK, 0.92f) }
                            }
                            Text("Task: ${w.task}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                                Text(w.modelAnswer, modifier = Modifier.padding(12.dp), fontSize = 14.sp, lineHeight = 21.sp)
                            }
                            Text("Why It Works: ${w.whyItWorks}", fontSize = 13.sp, color = TealPrimary)
                            Text("Checklist: ${w.checklist}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                4 -> items(verbs) { v ->
                    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("${v.v1} → ${v.v2} → ${v.v3}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(v.my, color = TealPrimary, fontSize = 13.sp)
                            }
                            VoiceChip("🇺🇸", TealPrimary) { onSpeak("${v.v1}, ${v.v2}, ${v.v3}", Locale.US, 0.9f) }
                        }
                    }
                }

                5 -> items(plan) { wk ->
                    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(wk.week, fontWeight = FontWeight.ExtraBold, color = IndigoAccent)
                                BadgePill(wk.grammar, TealPrimary)
                            }
                            Text("Vocabulary: ${wk.vocabulary}", fontSize = 13.sp)
                            Text("Integrated Skills: ${wk.skills}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
