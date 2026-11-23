package com.htetlingo.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
    // 0 = Foundation English (A1) by ထက်, 1 = Comprehensive A1–C2 Book (49 Lessons)
    var selectedBook by remember { mutableIntStateOf(0) }
    var tab by remember { mutableIntStateOf(0) } // For Book 1: 0=49 Lessons, 1=24 Vocab, 2=Reading Lab, 3=Writing, 4=Verbs, 5=Plan
    var query by remember { mutableStateOf("") }
    var expandedLesson by remember { mutableIntStateOf(101) }
    var expandedFoundationChapter by remember { mutableStateOf("foundation-a1-ch01") }
    var showAnswerFor by remember { mutableStateOf(setOf<String>()) }

    val foundationChapters = remember { db.fetchFoundationChapters() }
    val lessons = remember(query) { db.fetchBookGrammarLessons(query) }
    val vocabUnits = remember { db.fetchBookVocabUnits() }
    val readings = remember { db.fetchBookReadings() }
    val writings = remember { db.fetchBookWritings() }
    val verbs = remember(query) { db.fetchIrregularVerbs(query) }
    val plan = remember { db.fetchStudyPlan() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
        // Top Book Selector (Foundation A1 vs Comprehensive A1-C2)
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { selectedBook = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedBook == 0) SakuraPink else Color.Transparent,
                        contentColor = if (selectedBook == 0) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🌸 Foundation A1 (ထက်)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Button(
                    onClick = { selectedBook = 1 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedBook == 1) IndigoAccent else Color.Transparent,
                        contentColor = if (selectedBook == 1) Color.White else MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("📘 Mastery A1–C2", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        if (selectedBook == 0) {
            // BOOK 0: Foundation English (A1) by ထက်
            Surface(
                color = SakuraPink.copy(alpha = 0.1f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Foundation English (A1)", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SakuraPink)
                        Text("စာရေးသူ: ထက် · မူရင်းပညာရှင်များအား ဂုဏ်ပြုဦးညွှတ်ချက်ပါဝင်", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    BadgePill("၁၄ ခန်း + Workbooks", SakuraPink)
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(foundationChapters, key = { it.id }) { ch ->
                    val isOpen = expandedFoundationChapter == ch.id
                    val isAnsOpen = ch.id in showAnswerFor

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().clickable {
                            expandedFoundationChapter = if (isOpen) "" else ch.id
                            if (!isOpen) onXp(3)
                        },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = SakuraPink,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        ch.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                                Icon(
                                    imageVector = if (isOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = SakuraPink
                                )
                            }

                            if (!isOpen) {
                                Text(
                                    ch.content.take(180) + "...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )
                            }

                            if (isOpen) {
                                Divider(Modifier.padding(vertical = 4.dp))

                                // TTS Audio Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BadgePill("© ထက် · Foundation A1", LilacPurple)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        VoiceChip("🇺🇸 Read Aloud", TealPrimary) {
                                            val englishText = ch.content.lines().filter { it.contains(Regex("[a-zA-Z]{3,}")) }.joinToString(". ").take(220)
                                            if (englishText.isNotBlank()) onSpeak(englishText, Locale.US, 0.95f)
                                        }
                                        VoiceChip("🇬🇧 UK", IndigoAccent) {
                                            val englishText = ch.content.lines().filter { it.contains(Regex("[a-zA-Z]{3,}")) }.joinToString(". ").take(220)
                                            if (englishText.isNotBlank()) onSpeak(englishText, Locale.UK, 0.95f)
                                        }
                                    }
                                }

                                // Full Chapter Content
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        ch.content,
                                        modifier = Modifier.padding(14.dp),
                                        fontSize = 13.sp,
                                        lineHeight = 21.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Toggle Workbook Answers if exercise chapter
                                if (ch.content.contains("WORKBOOK") || ch.content.contains("Exercise") || ch.content.contains("Part C")) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = {
                                            showAnswerFor = if (isAnsOpen) showAnswerFor - ch.id else showAnswerFor + ch.id
                                        }) {
                                            Text(if (isAnsOpen) "Hide Workbook Hints" else "Show Workbook Hints / Keys", fontSize = 12.sp, color = WarmGold)
                                        }
                                    }
                                    if (isAnsOpen) {
                                        Surface(
                                            color = WarmGold.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                "💡 လေ့ကျင့်ခန်း အကြံပြုချက်: မေးခွန်းများကို သဒ္ဒါစည်းမျဉ်းအတိုင်း ကိုယ်တိုင် အရင်ဆုံး ရေးသားဖြေဆိုပါ။ အခန်းအလိုက် အမှားပြင်ဆင်ရန် ဥပမာဝါကျများတွင် Golden Rule (SVO)၊ Be Verbs (am/is/are) နှင့် Present Simple (V1/V-s) တို့ကို အဓိက သတိပြုစစ်ဆေးပါ။",
                                                modifier = Modifier.padding(10.dp),
                                                fontSize = 12.sp,
                                                color = WarmGold,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // BOOK 1: Comprehensive A1–C2 Book (49 Lessons + Labs + Vocab)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                FilterChip(selected = tab == 0, onClick = { tab = 0; query = "" }, label = { Text("49 Lessons") })
                FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("24 Vocab") })
                FilterChip(selected = tab == 2, onClick = { tab = 2 }, label = { Text("10 Reading Lab") })
                FilterChip(selected = tab == 3, onClick = { tab = 3 }, label = { Text("9 Writing") })
                FilterChip(selected = tab == 4, onClick = { tab = 4; query = "" }, label = { Text("97 Verbs") })
                FilterChip(selected = tab == 5, onClick = { tab = 5 }, label = { Text("16-Week Plan") })
            }

            if (tab == 0 || tab == 4) {
                CompactSearchBar(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = if (tab == 0) "Search A1–C2 lessons (Passive, Conditionals, C1...)" else "Search 97 irregular verbs (go, went, gone)..."
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                when (tab) {
                    0 -> items(lessons, key = { it.number }) { l ->
                        val open = expandedLesson == l.number
                        val ansVisible = l.number.toString() in showAnswerFor
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().clickable {
                                expandedLesson = if (open) -1 else l.number
                                if (!open) onXp(2)
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        BadgePill("Lesson ${"%02d".format(l.number)} · ${l.level}", IndigoAccent)
                                        Text(l.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    VoiceChip("🇺🇸", TealPrimary) { onSpeak(l.examples.take(180), Locale.US, 0.95f) }
                                }
                                Text(l.explanation, fontSize = 13.sp, lineHeight = 20.sp)

                                if (open) {
                                    if (l.patterns.isNotEmpty()) {
                                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                                            Text(l.patterns, modifier = Modifier.padding(10.dp), fontSize = 13.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        }
                                    }
                                    if (l.examples.isNotEmpty()) {
                                        Text("Examples:\n" + l.examples, fontSize = 13.sp, color = TealPrimary, lineHeight = 19.sp)
                                    }
                                    if (l.practice.isNotEmpty()) {
                                        Text("Practice:\n" + l.practice, fontSize = 13.sp, lineHeight = 19.sp)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                            TextButton(onClick = {
                                                showAnswerFor = if (ansVisible) showAnswerFor - l.number.toString() else showAnswerFor + l.number.toString()
                                            }) { Text(if (ansVisible) "Hide Answers" else "Show Answers") }
                                        }
                                        if (ansVisible) {
                                            Text("Answers:\n" + l.answers, color = EmeraldGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> items(vocabUnits, key = { it.number }) { u ->
                        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        BadgePill("Unit ${"%02d".format(u.number)} · ${u.level}", RosePink)
                                        Text(u.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    VoiceChip("🔊 Speak", RosePink) {
                                        val sample = u.words.take(4).joinToString(", ") { it.first }
                                        onSpeak(sample, Locale.US, 0.95f)
                                    }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    u.words.take(6).forEach { (w, m) ->
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(w, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                            Text(m, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> items(readings, key = { it.number }) { r ->
                        val ansVisible = r.number.toString() in showAnswerFor
                        ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        BadgePill("Reading ${"%02d".format(r.number)} · ${r.level}", EmeraldGreen)
                                        Text(r.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    VoiceChip("🇺🇸 Read Aloud", EmeraldGreen) { onSpeak(r.passage.take(250), Locale.US, 0.95f) }
                                }
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                                    Text(r.passage, modifier = Modifier.padding(12.dp), fontSize = 14.sp, lineHeight = 21.sp)
                                }
                                Text("Grammar Notes: ${r.grammarNotes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Comprehension Questions:\n${r.questions}", fontSize = 13.sp, lineHeight = 19.sp)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = {
                                        showAnswerFor = if (ansVisible) showAnswerFor - r.number.toString() else showAnswerFor + r.number.toString()
                                    }) { Text(if (ansVisible) "Hide Answers" else "Show Suggested Answers") }
                                }
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
}
