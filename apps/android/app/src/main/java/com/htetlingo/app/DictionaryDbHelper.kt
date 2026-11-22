package com.htetlingo.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream

data class VocabEntry(
    val id: Long,
    val word: String,
    val ipa: String,
    val myDef: String,
    val zhDef: String,
    val enDef: String,
    val tag: String,
    val level: String
)

data class HskGrammarEntry(
    val level: String,
    val title: String,
    val pattern: String,
    val exampleZh: String,
    val examplePy: String,
    val exampleEn: String
)

data class EnglishErrorEntry(
    val wrong: String,
    val right: String,
    val kind: String
)

data class NamePairEntry(
    val en: String,
    val mm: String
)

data class PostalEntry(
    val region: String,
    val township: String,
    val code: String
)

class DictionaryDbHelper(private val context: Context) {
    private var db: SQLiteDatabase? = null

    init {
        val dbFile = File(context.filesDir, "htetlingo_mobile_v2.sqlite")
        if (!dbFile.exists()) {
            runCatching {
                context.assets.open("htetlingo_mobile.sqlite").use { input ->
                    FileOutputStream(dbFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
        if (dbFile.exists()) {
            db = runCatching {
                SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            }.getOrNull()
        }
    }

    val isReady: Boolean get() = db != null

    fun countTable(table: String): Int {
        val sqlite = db ?: return 0
        return runCatching {
            sqlite.rawQuery("SELECT COUNT(*) FROM $table", null).use { c ->
                if (c.moveToFirst()) c.getInt(0) else 0
            }
        }.getOrDefault(0)
    }

    fun searchVocab(query: String = "", tagFilter: String = "ALL", limit: Int = 50): List<VocabEntry> {
        val sqlite = db ?: return emptyList()
        val q = query.trim().lowercase()
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (tagFilter != "ALL") {
            val tf = tagFilter.lowercase()
            clauses.add("(lower(tag) LIKE ? OR lower(level) = ?)")
            args.add("%$tf%")
            args.add(tf)
        }
        if (q.isNotEmpty()) {
            clauses.add("(word_lower LIKE ? OR my_def LIKE ? OR zh_def LIKE ?)")
            args.add("$q%")
            args.add("%$q%")
            args.add("%$q%")
        }

        val where = if (clauses.isEmpty()) "" else "WHERE " + clauses.joinToString(" AND ")
        val order = if (q.isNotEmpty()) "ORDER BY length(word_lower) ASC" else "ORDER BY id ASC"
        args.add(limit.toString())

        val out = mutableListOf<VocabEntry>()
        sqlite.rawQuery(
            "SELECT id, word, ipa, my_def, zh_def, en_def, tag, level FROM vocab $where $order LIMIT ?",
            args.toTypedArray()
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    VocabEntry(
                        id = c.getLong(0),
                        word = c.getString(1) ?: "",
                        ipa = c.getString(2) ?: "",
                        myDef = c.getString(3) ?: "",
                        zhDef = c.getString(4) ?: "",
                        enDef = c.getString(5) ?: "",
                        tag = c.getString(6) ?: "",
                        level = c.getString(7) ?: ""
                    )
                )
            }
        }
        return out
    }

    fun randomVocab(count: Int = 40): List<VocabEntry> {
        val sqlite = db ?: return emptyList()
        val out = mutableListOf<VocabEntry>()
        sqlite.rawQuery(
            "SELECT id, word, ipa, my_def, zh_def, en_def, tag, level FROM vocab ORDER BY RANDOM() LIMIT ?",
            arrayOf(count.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    VocabEntry(
                        c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "",
                        c.getString(3) ?: "", c.getString(4) ?: "", c.getString(5) ?: "",
                        c.getString(6) ?: "", c.getString(7) ?: ""
                    )
                )
            }
        }
        return out
    }

    fun searchHskGrammar(level: String = "ALL", query: String = "", limit: Int = 40): List<HskGrammarEntry> {
        val sqlite = db ?: return emptyList()
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (level != "ALL") {
            clauses.add("level = ?")
            args.add(level)
        }
        val q = query.trim().lowercase()
        if (q.isNotEmpty()) {
            clauses.add("(lower(title) LIKE ? OR lower(pattern) LIKE ? OR example_zh LIKE ?)")
            args.add("%$q%")
            args.add("%$q%")
            args.add("%$q%")
        }
        val where = if (clauses.isEmpty()) "" else "WHERE " + clauses.joinToString(" AND ")
        args.add(limit.toString())

        val out = mutableListOf<HskGrammarEntry>()
        sqlite.rawQuery(
            "SELECT level, title, pattern, example_zh, example_py, example_en FROM hsk_grammar $where LIMIT ?",
            args.toTypedArray()
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    HskGrammarEntry(
                        c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: "",
                        c.getString(3) ?: "", c.getString(4) ?: "", c.getString(5) ?: ""
                    )
                )
            }
        }
        return out
    }

    fun searchEnglishErrors(query: String = "", limit: Int = 50): List<EnglishErrorEntry> {
        val sqlite = db ?: return emptyList()
        val q = query.trim().lowercase()
        val out = mutableListOf<EnglishErrorEntry>()
        val cursor = if (q.isEmpty()) {
            sqlite.rawQuery("SELECT wrong, right, kind FROM english_errors LIMIT ?", arrayOf(limit.toString()))
        } else {
            sqlite.rawQuery(
                "SELECT wrong, right, kind FROM english_errors WHERE lower(wrong) LIKE ? OR lower(right) LIKE ? LIMIT ?",
                arrayOf("%$q%", "%$q%", limit.toString())
            )
        }
        cursor.use { c ->
            while (c.moveToNext()) {
                out.add(EnglishErrorEntry(c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: ""))
            }
        }
        return out
    }

    fun searchMyanmarNames(query: String = "", limit: Int = 50): List<NamePairEntry> {
        val sqlite = db ?: return emptyList()
        val q = query.trim()
        val out = mutableListOf<NamePairEntry>()
        val cursor = if (q.isEmpty()) {
            sqlite.rawQuery("SELECT en, mm FROM myanmar_names LIMIT ?", arrayOf(limit.toString()))
        } else {
            sqlite.rawQuery(
                "SELECT en, mm FROM myanmar_names WHERE lower(en) LIKE ? OR mm LIKE ? LIMIT ?",
                arrayOf("%${q.lowercase()}%", "%$q%", limit.toString())
            )
        }
        cursor.use { c ->
            while (c.moveToNext()) {
                out.add(NamePairEntry(c.getString(0) ?: "", c.getString(1) ?: ""))
            }
        }
        return out
    }

    fun searchPostalCodes(query: String = "", limit: Int = 50): List<PostalEntry> {
        val sqlite = db ?: return emptyList()
        val q = query.trim().lowercase()
        val out = mutableListOf<PostalEntry>()
        val cursor = if (q.isEmpty()) {
            sqlite.rawQuery("SELECT region, township, code FROM postal_codes LIMIT ?", arrayOf(limit.toString()))
        } else {
            sqlite.rawQuery(
                "SELECT region, township, code FROM postal_codes WHERE lower(township) LIKE ? OR lower(region) LIKE ? OR code LIKE ? LIMIT ?",
                arrayOf("%$q%", "%$q%", "%$q%", limit.toString())
            )
        }
        cursor.use { c ->
            while (c.moveToNext()) {
                out.add(PostalEntry(c.getString(0) ?: "", c.getString(1) ?: "", c.getString(2) ?: ""))
            }
        }
        return out
    }
}
