@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.htetlingo.app

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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
    val unitNumber: Int,
    val title: String,
    val description: String,
    val icon: String,
    val stars: Int, // 0..3
    val state: NodeState,
    val targetSentence: String = "",
    val burmeseMeaning: String = "",
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

// MARK: - Auto Quest Generation & Offline Storage Engine
object AutoQuestManager {
    private const val UNITS_FILE = "quest_units_store_v34.json"
    private const val PROGRESS_FILE = "quest_progress_v34.json"

    fun loadOrAutoGenerateUnits(context: Context, db: DictionaryDbHelper): List<QuestUnit> {
        val file = File(context.filesDir, UNITS_FILE)
        val progressFile = File(context.filesDir, PROGRESS_FILE)

        val units = if (!file.exists()) {
            val gen = generateAll32Units()
            saveUnitsToFile(file, gen)
            val initProgress = JSONObject().apply {
                put("completed", JSONArray())
                put("current", "u1_n1")
                put("stars", JSONObject())
            }
            runCatching { progressFile.writeText(initProgress.toString()) }
            gen
        } else {
            val loaded = loadUnitsFromFile(file)
            if (loaded.size < 30) {
                val gen = generateAll32Units()
                saveUnitsToFile(file, gen)
                gen
            } else loaded
        }

        val progress = if (progressFile.exists()) {
            runCatching { JSONObject(progressFile.readText()) }.getOrDefault(JSONObject())
        } else JSONObject()

        return applyProgressToUnits(units, progress)
    }

    fun resetOrRegenerate(context: Context, db: DictionaryDbHelper): List<QuestUnit> {
        val file = File(context.filesDir, UNITS_FILE)
        val progressFile = File(context.filesDir, PROGRESS_FILE)
        val gen = generateAll32Units()
        saveUnitsToFile(file, gen)
        val initProgress = JSONObject().apply {
            put("completed", JSONArray())
            put("current", "u1_n1")
            put("stars", JSONObject())
        }
        runCatching { progressFile.writeText(initProgress.toString()) }
        return applyProgressToUnits(gen, initProgress)
    }

    fun markNodeComplete(context: Context, currentUnits: List<QuestUnit>, completedNodeId: String): List<QuestUnit> {
        val allNodes = currentUnits.flatMap { it.nodes }
        val currentIndex = allNodes.indexOfFirst { it.id == completedNodeId }
        val nextNodeId = if (currentIndex != -1 && currentIndex + 1 < allNodes.size) {
            allNodes[currentIndex + 1].id
        } else null

        val progressFile = File(context.filesDir, PROGRESS_FILE)
        val progress = if (progressFile.exists()) {
            runCatching { JSONObject(progressFile.readText()) }.getOrDefault(JSONObject())
        } else JSONObject()

        val completedArr = progress.optJSONArray("completed") ?: JSONArray()
        var exists = false
        for (i in 0 until completedArr.length()) {
            if (completedArr.optString(i) == completedNodeId) {
                exists = true
                break
            }
        }
        if (!exists) completedArr.put(completedNodeId)
        progress.put("completed", completedArr)

        val starsObj = progress.optJSONObject("stars") ?: JSONObject()
        starsObj.put(completedNodeId, 3)
        progress.put("stars", starsObj)

        if (nextNodeId != null) {
            progress.put("current", nextNodeId)
        }

        runCatching { progressFile.writeText(progress.toString()) }
        return applyProgressToUnits(currentUnits, progress)
    }

    private fun applyProgressToUnits(units: List<QuestUnit>, progress: JSONObject): List<QuestUnit> {
        val completedSet = mutableSetOf<String>()
        val completedArr = progress.optJSONArray("completed")
        if (completedArr != null) {
            for (i in 0 until completedArr.length()) {
                completedSet.add(completedArr.optString(i))
            }
        }
        val currentNodeId = progress.optString("current", "u1_n1")
        val starsObj = progress.optJSONObject("stars")

        return units.map { unit ->
            unit.copy(nodes = unit.nodes.map { node ->
                val isCompleted = node.id in completedSet
                val isCurrent = node.id == currentNodeId
                val stars = starsObj?.optInt(node.id, if (isCompleted) 3 else 0) ?: if (isCompleted) 3 else 0
                val state = when {
                    isCompleted -> NodeState.COMPLETED
                    isCurrent -> NodeState.CURRENT
                    else -> NodeState.LOCKED
                }
                node.copy(state = state, stars = stars)
            })
        }
    }

    private fun saveUnitsToFile(file: File, units: List<QuestUnit>) {
        runCatching {
            val root = JSONArray()
            units.forEach { u ->
                val uObj = JSONObject().apply {
                    put("number", u.number)
                    put("title", u.title)
                    put("subtitle", u.subtitle)
                    put("badge", u.badge)
                    put("colorHex", when (u.color) {
                        SakuraPink -> "#F472B6"
                        LilacPurple -> "#A855F7"
                        PeachPastel -> "#FDBA74"
                        MintPastel -> "#34D399"
                        SkyBlue -> "#0284C7"
                        WarmGold -> "#D97706"
                        else -> "#4F46E5"
                    })
                    val nodesArr = JSONArray()
                    u.nodes.forEach { n ->
                        nodesArr.put(JSONObject().apply {
                            put("id", n.id)
                            put("unitNumber", n.unitNumber)
                            put("title", n.title)
                            put("description", n.description)
                            put("icon", n.icon)
                            put("targetSentence", n.targetSentence)
                            put("burmeseMeaning", n.burmeseMeaning)
                            put("xpReward", n.xpReward)
                            put("gemReward", n.gemReward)
                        })
                    }
                    put("nodes", nodesArr)
                }
                root.put(uObj)
            }
            file.writeText(root.toString())
        }
    }

    private fun loadUnitsFromFile(file: File): List<QuestUnit> {
        return runCatching {
            val text = file.readText()
            val root = JSONArray(text)
            val list = mutableListOf<QuestUnit>()
            for (i in 0 until root.length()) {
                val uObj = root.getJSONObject(i)
                val num = uObj.getInt("number")
                val title = uObj.getString("title")
                val subtitle = uObj.getString("subtitle")
                val badge = uObj.getString("badge")
                val colorHex = uObj.optString("colorHex", "#F472B6")
                val color = when (colorHex) {
                    "#F472B6" -> SakuraPink
                    "#A855F7" -> LilacPurple
                    "#FDBA74" -> PeachPastel
                    "#34D399" -> MintPastel
                    "#0284C7" -> SkyBlue
                    "#D97706" -> WarmGold
                    else -> IndigoAccent
                }
                val nodesArr = uObj.getJSONArray("nodes")
                val nodes = mutableListOf<QuestLessonNode>()
                for (j in 0 until nodesArr.length()) {
                    val nObj = nodesArr.getJSONObject(j)
                    nodes.add(
                        QuestLessonNode(
                            id = nObj.getString("id"),
                            unitNumber = nObj.optInt("unitNumber", num),
                            title = nObj.getString("title"),
                            description = nObj.getString("description"),
                            icon = nObj.getString("icon"),
                            stars = 0,
                            state = NodeState.LOCKED,
                            targetSentence = nObj.optString("targetSentence", ""),
                            burmeseMeaning = nObj.optString("burmeseMeaning", ""),
                            xpReward = nObj.optInt("xpReward", 25),
                            gemReward = nObj.optInt("gemReward", 10)
                        )
                    )
                }
                list.add(QuestUnit(num, title, subtitle, badge, color, nodes))
            }
            list
        }.getOrElse { generateAll32Units() }
    }

    fun generateAll32Units(): List<QuestUnit> {
        val meta = listOf(
            // STAGE 1: Foundation English (A1) by ထက် (Units 1 - 12)
            Triple(1, "The English Alphabet & Phonics", "အင်္ဂလိပ်အက္ခရာ ၂၆ လုံး၊ သရနှင့် Capitalization စည်းမျဉ်းများ"),
            Triple(2, "Sounds & Word Stress", "အင်္ဂလိပ်အသံထွက် ၄၄ သံ၊ Voiced/Voiceless နှင့် Word Stress"),
            Triple(3, "Parts of Speech Overview", "စကားလုံးအမျိုးအစား ၈ မျိုးနှင့် ဝါကျထဲက အဓိကတာဝန်များ"),
            Triple(4, "Nouns: Types & Plurals", "ရေတွက်ရ/မရနာမ်နှင့် အများကိန်းပြောင်းခြင်း စည်းမျဉ်း ၆ ချက်"),
            Triple(5, "Articles & Determiners", "a, an, the, this, that နှင့် ညွှန်ပြစကားလုံးများ"),
            Triple(6, "Be Verbs (am, is, are)", "am, is, are အခြေခံဖွဲ့စည်းပုံ၊ အငြင်းနှင့် မေးခွန်းများ"),
            Triple(7, "Basic English Sentences", "အင်္ဂလိပ်ဝါကျတည်ဆောက်ပုံ SVO Golden Rule နှင့် PT စည်းမျဉ်း"),
            Triple(8, "Present Simple: Statements", "ပုံမှန်လုပ်ရပ်၊ အမှန်တရားနှင့် -s/-es ပေါင်းထည့်နည်းများ"),
            Triple(9, "Present Simple: Negatives & Questions", "do/does အကူကြိယာဖြင့် အငြင်းဝါကျနှင့် မေးခွန်းများ"),
            Triple(10, "Everyday Vocabulary & Life", "နေ့စဉ်လုပ်ရိုးလုပ်စဉ်၊ မိသားစုနှင့် အစားအသောက် ဝေါဟာရများ"),
            Triple(11, "Everyday Reading & Writing", "သီရိ၏ နေ့စဉ်ဘဝ၊ Connectors (and, but, so, because)"),
            Triple(12, "Imperatives & Suggestions", "အမိန့်ပေး၊ တောင်းဆို၊ သတိပေးဝါကျများနှင့် Let's / How about"),

            // STAGE 2: Elementary (A2) & Practical Daily Life (Units 13 - 20)
            Triple(13, "Past Simple Verbs & Stories", "အတိတ်ကာလ ကြိယာပုံစံများ (went, saw, ate) နှင့် Did မေးခွန်း"),
            Triple(14, "Future Plans: Will vs Going to", "အနာဂတ်ကာလ ဆုံးဖြတ်ချက်နှင့် ကြိုတင်စီစဉ်မှုများ"),
            Triple(15, "Modals of Ability & Permission", "Can, Could, May ဖြင့် စွမ်းဆောင်ရည်နှင့် ယဉ်ကျေးစွာ ခွင့်တောင်းခြင်း"),
            Triple(16, "Comparatives & Superlatives", "နှိုင်းယှဉ်ပြ နာမဝိသေသနများ (-er than, more than, the most)"),
            Triple(17, "Prepositions of Place & Movement", "In, On, At, into, through ဖြင့် နေရာနှင့် လမ်းညွှန်ချက်များ"),
            Triple(18, "Quantifiers & Partitives", "Much, Many, A lot of, A cup of, A bottle of ပမာဏပြများ"),
            Triple(19, "Present Continuous Actions", "လက်ရှိလုပ်ဆောင်ဆဲ လုပ်ရပ်များ (-ing) နှင့် အစီအစဉ်များ"),
            Triple(20, "Travel, Airport & Hotel English", "လေဆိပ်၊ ဟိုတယ်၊ စားသောက်ဆိုင် လက်တွေ့သုံး စကားပြောများ"),

            // STAGE 3: Intermediate (B1) Fluency (Units 21 - 26)
            Triple(21, "Present Perfect: Have/Has + V3", "အတွေ့အကြုံ၊ For vs Since ချိတ်ဆက်ပုံနှင့် မကြာသေးမီက ပြီးစီးမှု"),
            Triple(22, "First & Second Conditionals", "အခြေအနေပြ ဝါကျများ (If it rains..., If I were you...)"),
            Triple(23, "Passive Voice Formations", "Subject အာရုံပြောင်း ဝါကျများ (is done, was built, has been made)"),
            Triple(24, "Modals of Obligation & Advice", "Must, Have to, Should ဖြင့် တာဝန်နှင့် အကြံပြုချက်များ"),
            Triple(25, "Relative Clauses (Who, Which, That)", "လူနှင့် ပစ္စည်းများကို စာကြောင်းဆက်သွယ် အထူးပြုပုံ"),
            Triple(26, "Past Habits: Used to & Would", "အရင်က အလေ့အထများနှင့် အတိတ်အမှတ်တရများ ဖော်ပြခြင်း"),

            // STAGE 4: Upper-Intermediate & Advanced (B2 - C2) (Units 27 - 32)
            Triple(27, "Third Conditional & Past Regrets", "အတိတ်နောင်တများ (If I had known, I would have helped)"),
            Triple(28, "Reported Speech & Indirect Qs", "ပြန်လည်ပြောကြားချက်များနှင့် ယဉ်ကျေးသော သွယ်ဝိုက်မေးခွန်းများ"),
            Triple(29, "Phrasal Verbs & Real Collocations", "သဘာဝကျသော စကားစုအတွဲများနှင့် မရှိမဖြစ် Phrasal Verbs"),
            Triple(30, "Inversion & Emphatic Structures", "စကားအလေးပေးပြ ဝါကျပြောင်းထုံးများ (Never have I ever...)"),
            Triple(31, "Subjunctive Mood & Formal Linkers", "တရားဝင် ဆွေးနွေးမှု၊ အကြံပြုချက်နှင့် Academic Linkers"),
            Triple(32, "Grand Master Certification Chest", "ထက် ၏ ဂုဏ်ထူးဆောင် အောင်လက်မှတ် စိန်ခေါ်မှု")
        )

        val colors = listOf(SakuraPink, LilacPurple, PeachPastel, MintPastel, SkyBlue, WarmGold)

        return meta.map { (num, title, subtitle) ->
            val color = colors[(num - 1) % colors.size]
            val badge = when {
                num <= 12 -> "🌸 Foundation A1 (ထက်)"
                num <= 20 -> "⭐ Elementary A2"
                num <= 26 -> "💎 Intermediate B1"
                else -> "👑 Master B2–C2"
            }

            val n1 = QuestLessonNode(
                id = "u${num}_n1",
                unitNumber = num,
                title = "$title: Basics",
                description = "အခြေခံ သဘောတရားနှင့် စည်းမျဉ်းများ",
                icon = if (num <= 12) "🌸" else "✨",
                stars = 0,
                state = NodeState.LOCKED,
                targetSentence = when (num) {
                    1 -> "She is an university student."
                    6 -> "I am happy to meet you today."
                    7 -> "Su Su drinks coffee at home every morning."
                    8 -> "He goes to work every day."
                    9 -> "She does not like coffee."
                    11 -> "My name is Thiri and I live in Yangon."
                    12 -> "Please open the window."
                    21 -> "I have known her for five years."
                    22 -> "If it rains tomorrow, we will stay at home."
                    else -> "Language learning cultivates cognitive resilience."
                },
                burmeseMeaning = when (num) {
                    6 -> "ဒီကနေ့ သင့်ကို တွေ့ဆုံရတဲ့အတွက် ကျွန်မ ဝမ်းသာပါတယ်။"
                    7 -> "စုစုသည် နံနက်တိုင်း အိမ်တွင် ကော်ဖီသောက်သည်။"
                    8 -> "သူသည် နေ့စဉ် အလုပ်သို့ သွားသည်။"
                    9 -> "သူမသည် ကော်ဖီ မကြိုက်ပါ။"
                    11 -> "ကျွန်မနာမည် သီရိ ဖြစ်ပြီး ရန်ကုန်တွင် နေထိုင်ပါသည်။"
                    12 -> "ကျေးဇူးပြုပြီး ပြတင်းပေါက်ကို ဖွင့်ပေးပါ။"
                    21 -> "ကျွန်တော် သူမကို သိကျွမ်းခဲ့သည်မှာ ငါးနှစ်ရှိပါပြီ။"
                    else -> "ဘာသာစကားသင်ယူခြင်းသည် စဉ်းစားတွေးခေါ်နိုင်စွမ်းကို တိုးတက်စေသည်။"
                }
            )

            val n2 = QuestLessonNode(
                id = "u${num}_n2",
                unitNumber = num,
                title = "$title: Rules & Practice",
                description = "စည်းမျဉ်း လေ့ကျင့်ခန်းနှင့် ဝါကျတည်ဆောက်ပုံ",
                icon = "⭐",
                stars = 0,
                state = NodeState.LOCKED,
                targetSentence = when (num) {
                    1 -> "Capitalize names and the first letter."
                    6 -> "They are not at home right now."
                    7 -> "I study English at school every day."
                    8 -> "The sun rises in the east."
                    9 -> "Does he speak English well?"
                    else -> "Every practice session brings permanent fluency."
                },
                burmeseMeaning = "လေ့ကျင့်မှုတိုင်းသည် ကျွမ်းကျင်မှုကို အမှန်တကယ် ပိုင်ဆိုင်စေသည်။"
            )

            val n3 = QuestLessonNode(
                id = "u${num}_n3",
                unitNumber = num,
                title = "$title: Real Conversation",
                description = "လက်တွေ့ စကားပြောနှင့် အသုံးအနှုန်းများ",
                icon = "💬",
                stars = 0,
                state = NodeState.LOCKED,
                targetSentence = when (num) {
                    12 -> "Let's watch a movie together this evening."
                    20 -> "Could you tell me how to get to the airport?"
                    else -> "Effective communication connects hearts across borders."
                },
                burmeseMeaning = "ထိရောက်သော ဆက်သွယ်ပြောဆိုမှုသည် လူအချင်းချင်း ဆက်သွယ်ပေးသည်။"
            )

            val n4 = QuestLessonNode(
                id = "u${num}_n4",
                unitNumber = num,
                title = "Unit $num Milestone Chest",
                description = "ရတနာသေတ္တာ စိန်ဆုလာဘ် 🎁",
                icon = if (num == 12 || num == 32) "👑" else "🎁",
                stars = 0,
                state = NodeState.LOCKED,
                targetSentence = "Congratulations on completing this milestone unit!",
                burmeseMeaning = "ဤယူနစ်၏ ရတနာသေတ္တာ စိန်ခေါ်မှုကို အောင်မြင်စွာ ပြီးမြောက်ပါပြီ!",
                xpReward = 50,
                gemReward = 25
            )

            QuestUnit(num, "Unit $num: $title", subtitle, badge, color, listOf(n1, n2, n3, n4))
        }
    }

    // Dynamic exercise generator based on the node and database
    fun generateExercisesForNode(node: QuestLessonNode, db: DictionaryDbHelper): List<GamifiedExercise> {
        val target = if (node.targetSentence.isNotBlank()) node.targetSentence else "I study English every single day."
        val words = target.replace(Regex("[.,!?]"), "").split(" ").filter { it.isNotBlank() }
        val pool = (words + listOf("coffee", "school", "water", "books", "always")).shuffled()

        // 1. Word Bubble exercise
        val ex1 = GamifiedExercise(
            kind = ExerciseKind.WORD_BUBBLE,
            prompt = "အောက်ပါ စကားလုံးများကို မှန်ကန်သော အင်္ဂလိပ်ဝါကျဖြစ်အောင် စီစဉ်ပါ:",
            targetAudio = target,
            correctSentence = words,
            bubblePool = pool
        )

        // 2. Multiple Choice Grammar Question
        val ex2 = when (node.unitNumber) {
            1 -> GamifiedExercise(
                kind = ExerciseKind.MULTI_CHOICE,
                prompt = "အောက်ပါတို့အနက် Capital letter (အက္ခရာအကြီး) ရေးရမည့် မှန်ကန်သော နေရာမှာ:",
                targetAudio = "Monday Yangon Myanmar Thiri",
                options = listOf("အမည်နာမ၊ နေ့ရက်၊ လ နှင့် နိုင်ငံအမည်များ", "ဝါကျအဆုံးသတ်တိုင်းတွင်", "နာမ်စား အားလုံးတွင်", "ကြိယာ အားလုံးတွင်"),
                correctIndex = 0
            )
            6 -> GamifiedExercise(
                kind = ExerciseKind.MULTI_CHOICE,
                prompt = "\"They ___ late yesterday.\" ကွက်လပ်ဖြည့်ရန် အမှန်မှာ:",
                targetAudio = "They were late yesterday.",
                options = listOf("were", "was", "is", "are"),
                correctIndex = 0
            )
            8 -> GamifiedExercise(
                kind = ExerciseKind.MULTI_CHOICE,
                prompt = "\"She ___ coffee every morning.\" ပုံမှန်လုပ်ရပ်အတွက် မှန်ကန်သော ကြိယာမှာ:",
                targetAudio = "She drinks coffee every morning.",
                options = listOf("drinks", "drink", "drinking", "drank"),
                correctIndex = 0
            )
            9 -> GamifiedExercise(
                kind = ExerciseKind.MULTI_CHOICE,
                prompt = "\"She does not ___ coffee.\" does not နောက်တွင် လိုက်ရမည့် ပုံစံမှာ:",
                targetAudio = "She does not like coffee.",
                options = listOf("like (Verb 1 Base Form)", "likes (with s)", "liked", "liking"),
                correctIndex = 0
            )
            else -> GamifiedExercise(
                kind = ExerciseKind.MULTI_CHOICE,
                prompt = "\"${node.burmeseMeaning.ifEmpty { "မှန်ကန်သော အင်္ဂလိပ်အသုံးအနှုန်းကို ရွေးချယ်ပါ:" }}\"",
                targetAudio = target,
                options = listOf(target, "She does not know the answer.", "They are waiting for the bus.", "He will arrive tomorrow morning."),
                correctIndex = 0
            )
        }

        // 3. Pair Matching exercise
        val ex3 = GamifiedExercise(
            kind = ExerciseKind.PAIR_MATCH,
            prompt = "ကိုက်ညီသော စကားလုံးအတွဲများကို နှိပ်ပါ (Tap matching pairs):",
            targetAudio = "daily routine habit practice fluency",
            pairsEnMy = when (node.unitNumber) {
                1 -> listOf("alphabet" to "အက္ခရာ", "vowel" to "သရ", "consonant" to "ဗျည်း", "spelling" to "စာလုံးပေါင်း")
                6 -> listOf("am" to "ဖြစ်သည် (I)", "is" to "ဖြစ်သည် (He/She)", "are" to "ဖြစ်ကြသည် (We/They)", "happy" to "ပျော်ရွှင်သော")
                7 -> listOf("subject" to "ကတ္တား", "verb" to "ကြိယာ", "object" to "ကံပုဒ်", "sentence" to "ဝါကျ")
                10 -> listOf("breakfast" to "နံနက်စာ", "family" to "မိသားစု", "routine" to "လုပ်ရိုးလုပ်စဉ်", "study" to "လေ့လာသည်")
                12 -> listOf("please" to "ကျေးဇူးပြု၍", "quiet" to "တိတ်ဆိတ်သော", "listen" to "နားထောင်ပါ", "careful" to "ဂရုစိုက်သော")
                else -> listOf("resilience" to "ကြံ့ကြံ့ခံနိုင်စွမ်း", "culture" to "ယဉ်ကျေးမှု", "language" to "ဘာသာစကား", "fluency" to "ကျွမ်းကျင်မှု")
            }
        )

        // 4. Listening exercise
        val ex4 = GamifiedExercise(
            kind = ExerciseKind.LISTENING,
            prompt = "အသံကို ဂရုတစိုက် နားထောင်ပြီး မှန်ကန်သော အဓိပ္ပာယ်ကို ရွေးချယ်ပါ:",
            targetAudio = target,
            options = listOf(
                node.burmeseMeaning.ifEmpty { "အင်္ဂလိပ်စာ လေ့လာမှုသည် ဘဝတိုးတက်စေသည်" },
                "မနက်ဖြန် မိုးရွာသွန်းနိုင်ခြေရှိသည်",
                "သူမသည် စာကြည့်တိုက်သို့ သွားရောက်ခဲ့သည်",
                "အလုပ်ခွင်တွင် ကွန်ပျူတာ အသုံးပြုရသည်"
            ),
            correctIndex = 0
        )

        return listOf(ex1, ex2, ex3, ex4)
    }
}

// MARK: - Interactive Quest Path UI Tab
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
    val context = LocalContext.current
    var units by remember { mutableStateOf(AutoQuestManager.loadOrAutoGenerateUnits(context, db)) }
    var activeLesson by remember { mutableStateOf<QuestLessonNode?>(null) }
    var showHeartRefillDialog by remember { mutableStateOf(false) }

    // Active Interactive Lesson Dialog
    if (activeLesson != null) {
        val exercises = remember(activeLesson!!.id) {
            AutoQuestManager.generateExercisesForNode(activeLesson!!, db)
        }
        InteractiveLessonDialog(
            lesson = activeLesson!!,
            exercises = exercises,
            currentHearts = hearts,
            onSpeak = onSpeak,
            onHeartLost = onHeartLost,
            onLessonComplete = {
                onRewardEarned(activeLesson!!.xpReward, activeLesson!!.gemReward)
                units = AutoQuestManager.markNodeComplete(context, units, activeLesson!!.id)
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

            // Auto Quests Offline Banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(12.dp),
                color = SakuraPink.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("✨", fontSize = 14.sp)
                        Text("Auto Quest Offline: ၃၂ ယူနစ် (၁၂၈ အဆင့်)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SakuraDark)
                    }
                    TextButton(
                        onClick = {
                            units = AutoQuestManager.resetOrRegenerate(context, db)
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("🔄 Reset", fontSize = 11.sp, color = SakuraPink)
                    }
                }
            }

            // S-CURVED GAMIFIED QUEST PATH
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 32.dp, top = 6.dp),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    BadgePill(unit.badge, unit.color)
                                    Spacer(Modifier.height(4.dp))
                                    Text(unit.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                    Text(
                                        unit.subtitle,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = unit.color.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("U${unit.number}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = unit.color)
                                    }
                                }
                            }
                        }
                    }

                    // Stepping stones nodes in S-Curve
                    itemsIndexed(unit.nodes, key = { _, node -> node.id }) { index, node ->
                        val curveX = sin(index * 1.5).toFloat() * 75f
                        val offset = curveX.dp

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
                    fontSize = if (node.state == NodeState.LOCKED) 22.sp else 28.sp
                )
            }
        }

        // Stars row under node
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { starIndex ->
                Text(
                    text = if (node.stars > starIndex) "⭐" else "☆",
                    fontSize = 12.sp,
                    color = if (node.stars > starIndex) WarmGold else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                )
            }
        }

        // Node Title label
        Text(
            text = node.title,
            fontWeight = if (node.state == NodeState.CURRENT) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = if (node.state == NodeState.LOCKED) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = 120.dp),
            maxLines = 1
        )
    }
}

// MARK: - Interactive Lesson Playing Dialog
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveLessonDialog(
    lesson: QuestLessonNode,
    exercises: List<GamifiedExercise>,
    currentHearts: Int,
    onSpeak: (String, Locale, Float) -> Unit,
    onHeartLost: () -> Unit,
    onLessonComplete: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    var livesRemaining by remember { mutableIntStateOf(currentHearts) }
    val selectedBubbles = remember { mutableStateListOf<String>() }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    val matchedPairs = remember { mutableStateMapOf<String, String>() }
    var selectedPairEn by remember { mutableStateOf<String?>(null) }
    var selectedPairMy by remember { mutableStateOf<String?>(null) }
    var checkStatus by remember { mutableStateOf<Boolean?>(null) } // null=waiting, true=correct, false=wrong
    var showCelebration by remember { mutableStateOf(false) }

    val currentExercise = exercises.getOrNull(currentStep) ?: exercises.first()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showCelebration) {
                // CELEBRATION SCREEN
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
                                Text("+${lesson.xpReward} XP", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = WarmGold)
                                Text("Exp Gained", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("💎", fontSize = 28.sp)
                                Text("+${lesson.gemReward} Gems", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = SkyBlue)
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

                        // Progress Bar
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
                    // Bottom Action & Feedback Bar
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
                                        val isCorrect = when (currentExercise.kind) {
                                            ExerciseKind.WORD_BUBBLE -> {
                                                selectedBubbles.toList() == currentExercise.correctSentence
                                            }
                                            ExerciseKind.MULTI_CHOICE, ExerciseKind.LISTENING -> {
                                                selectedOptionIndex == currentExercise.correctIndex
                                            }
                                            ExerciseKind.PAIR_MATCH -> {
                                                matchedPairs.size == currentExercise.pairsEnMy.size
                                            }
                                        }
                                        checkStatus = isCorrect
                                        if (isCorrect) {
                                            onSpeak("Correct!", Locale.US, 1.0f)
                                        } else {
                                            if (livesRemaining > 1) {
                                                livesRemaining--
                                                onHeartLost()
                                            }
                                            onSpeak("Try again", Locale.US, 1.0f)
                                        }
                                    } else {
                                        // NEXT QUESTION OR FINISH
                                        if (currentStep + 1 < exercises.size) {
                                            currentStep++
                                            checkStatus = null
                                            selectedBubbles.clear()
                                            selectedOptionIndex = null
                                            matchedPairs.clear()
                                            selectedPairEn = null
                                            selectedPairMy = null
                                        } else {
                                            showCelebration = true
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (checkStatus) {
                                        true -> MintPastel
                                        false -> SakuraDark
                                        else -> SakuraPink
                                    }
                                )
                            ) {
                                Text(
                                    if (checkStatus == null) "စစ်ဆေးမည် (CHECK)" else "ရှေ့သို့ (CONTINUE)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(18.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Exercise Prompt
                    Text(
                        currentExercise.prompt,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Listen Audio Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(SakuraPink.copy(alpha = 0.12f))
                            .clickable { onSpeak(currentExercise.targetAudio, Locale.US, 0.95f) }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Play", tint = SakuraPink)
                        Text("အသံထွက် နားထောင်မည် (Tap to Listen)", color = SakuraPink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(8.dp))

                    // Exercise Content
                    when (currentExercise.kind) {
                        ExerciseKind.WORD_BUBBLE -> {
                            // Target Sentence Build Zone
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 80.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.5.dp, SakuraPink.copy(alpha = 0.3f))
                            ) {
                                FlowRow(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (selectedBubbles.isEmpty()) {
                                        Text(
                                            "အောက်ပါ စကားလုံးပူဖောင်းများကို နှိပ်ပြီး နေရာချပါ...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    selectedBubbles.forEachIndexed { idx, word ->
                                        Surface(
                                            modifier = Modifier.clickable { selectedBubbles.removeAt(idx) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = SakuraPink,
                                            shadowElevation = 4.dp
                                        ) {
                                            Text(
                                                word,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Bubble Choices Pool
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                currentExercise.bubblePool.forEach { word ->
                                    val countInPool = currentExercise.bubblePool.count { it == word }
                                    val countInSelected = selectedBubbles.count { it == word }
                                    val isAvailable = countInSelected < countInPool

                                    Surface(
                                        modifier = Modifier.clickable(enabled = isAvailable) {
                                            if (isAvailable) selectedBubbles.add(word)
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isAvailable) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        border = BorderStroke(1.dp, if (isAvailable) SakuraPink.copy(alpha = 0.5f) else Color.Transparent),
                                        shadowElevation = if (isAvailable) 2.dp else 0.dp
                                    ) {
                                        Text(
                                            word,
                                            color = if (isAvailable) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        ExerciseKind.MULTI_CHOICE, ExerciseKind.LISTENING -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                currentExercise.options.forEachIndexed { idx, opt ->
                                    val isSelected = selectedOptionIndex == idx
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedOptionIndex = idx },
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) SakuraPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) SakuraPink else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        shadowElevation = if (isSelected) 4.dp else 1.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (isSelected) SakuraPink else MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        "${('A' + idx)}",
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                            Text(
                                                opt,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ExerciseKind.PAIR_MATCH -> {
                            // Match 4 Pairs of En <-> My
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                val enList = remember(currentStep) { currentExercise.pairsEnMy.map { it.first }.shuffled() }
                                val myList = remember(currentStep) { currentExercise.pairsEnMy.map { it.second }.shuffled() }

                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    // English column
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        enList.forEach { enWord ->
                                            val isMatched = enWord in matchedPairs
                                            val isSelected = selectedPairEn == enWord
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(enabled = !isMatched) {
                                                        selectedPairEn = enWord
                                                        if (selectedPairMy != null) {
                                                            // Check match
                                                            val correctMy = currentExercise.pairsEnMy.find { it.first == enWord }?.second
                                                            if (correctMy == selectedPairMy) {
                                                                matchedPairs[enWord] = selectedPairMy!!
                                                            }
                                                            selectedPairEn = null
                                                            selectedPairMy = null
                                                        }
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = when {
                                                    isMatched -> MintLight
                                                    isSelected -> SakuraPink.copy(alpha = 0.2f)
                                                    else -> MaterialTheme.colorScheme.surface
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    when {
                                                        isMatched -> MintPastel
                                                        isSelected -> SakuraPink
                                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                    }
                                                )
                                            ) {
                                                Text(
                                                    enWord,
                                                    modifier = Modifier.padding(12.dp),
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    color = if (isMatched) MintPastel else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    // Myanmar column
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        myList.forEach { myWord ->
                                            val isMatched = myWord in matchedPairs.values
                                            val isSelected = selectedPairMy == myWord
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable(enabled = !isMatched) {
                                                        selectedPairMy = myWord
                                                        if (selectedPairEn != null) {
                                                            // Check match
                                                            val correctEn = currentExercise.pairsEnMy.find { it.second == myWord }?.first
                                                            if (correctEn == selectedPairEn) {
                                                                matchedPairs[selectedPairEn!!] = myWord
                                                            }
                                                            selectedPairEn = null
                                                            selectedPairMy = null
                                                        }
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                color = when {
                                                    isMatched -> MintLight
                                                    isSelected -> SakuraPink.copy(alpha = 0.2f)
                                                    else -> MaterialTheme.colorScheme.surface
                                                },
                                                border = BorderStroke(
                                                    1.dp,
                                                    when {
                                                        isMatched -> MintPastel
                                                        isSelected -> SakuraPink
                                                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                                    }
                                                )
                                            ) {
                                                Text(
                                                    myWord,
                                                    modifier = Modifier.padding(12.dp),
                                                    fontWeight = FontWeight.Medium,
                                                    textAlign = TextAlign.Center,
                                                    fontSize = 13.sp,
                                                    color = if (isMatched) MintPastel else MaterialTheme.colorScheme.onSurface
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
        }
    }
}
