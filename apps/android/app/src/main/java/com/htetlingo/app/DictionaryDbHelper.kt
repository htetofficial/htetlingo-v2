package com.htetlingo.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

data class DictItem(
    val word: String,
    val definition: String
)

class DictionaryDbHelper(private val context: Context) {
    private var db: SQLiteDatabase? = null

    private val starterEntries = listOf(
        DictItem("resilience", "noun /rɪˈzɪl.jəns/ [ရီဇီးလျန့်(စ်)] — အခက်အခဲများမှ လျင်မြန်စွာ ပြန်လည်နာလန်ထူနိုင်စွမ်း"),
        DictItem("language", "noun /ˈlæŋ.ɡwɪdʒ/ [လန်း(ဂ်)ဝစ်(ဂျ်)] — ဘာသာစကား"),
        DictItem("culture", "noun /ˈkʌl.tʃər/ [ကား(လ်)ချာ] — ယဉ်ကျေးမှု"),
        DictItem("dictionary", "noun /ˈdɪk.ʃən.er.i/ [ဒစ်(ခ်)ရှင်နရီ] — အဘိဓာန်"),
        DictItem("understand", "verb /ˌʌn.dəˈstænd/ [အန်းဒါစတန်း(ဒ်)] — နားလည်သည်"),
        DictItem("fluent", "adj /ˈfluː.ənt/ [ဖလူးအန့်(တ်)] — သွက်လက်ချောမွေ့သော")
    )

    init {
        val dbFile = File(context.filesDir, "ornagai.sqlite")
        if (dbFile.exists()) {
            db = runCatching {
                SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            }.getOrNull()
        }
    }

    val isFullOfflineDbLoaded: Boolean
        get() = db != null

    fun search(query: String, limit: Int = 30): List<DictItem> {
        val q = query.trim().lowercase()
        val sqlite = db
        if (sqlite != null) {
            val results = mutableListOf<DictItem>()
            val cursor = if (q.isEmpty()) {
                sqlite.rawQuery("SELECT word, plain_text FROM entries LIMIT ?", arrayOf(limit.toString()))
            } else {
                sqlite.rawQuery(
                    "SELECT word, plain_text FROM entries WHERE word_lower LIKE ? ORDER BY length(word_lower) ASC LIMIT ?",
                    arrayOf("$q%", limit.toString())
                )
            }
            cursor.use {
                while (it.moveToNext()) {
                    results.add(DictItem(it.getString(0), it.getString(1)))
                }
            }
            return results
        }
        return if (q.isEmpty()) starterEntries
        else starterEntries.filter { it.word.lowercase().contains(q) }
    }
}
