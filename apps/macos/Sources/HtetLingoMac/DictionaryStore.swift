import Foundation
import SQLite3

struct DictionaryEntry: Identifiable, Hashable, Sendable {
    let id: Int64
    let word: String
    let plainText: String
}

struct EnZhEntry: Identifiable, Hashable, Sendable {
    let id: Int64
    let word: String
    let phonetic: String
    let translation: String
    let tag: String
}

struct EnEnEntry: Identifiable, Hashable, Sendable {
    let id: Int64
    let word: String
    let definitions: [String]
}

struct UnifiedVocabItem: Identifiable, Hashable, Sendable {
    let id: Int64
    let word: String
    let ipa: String
    let myDef: String
    let zhDef: String
    let enDef: String
    let tag: String
    let level: String
}

struct MyanmarNamePair: Identifiable, Hashable, Sendable {
    let id: String
    let en: String
    let mm: String
}

struct GrammarExerciseItem: Identifiable, Hashable, Sendable {
    let id: Int64
    let topic: String
    let question: String
    let options: [String]
    let answer: String
    let explanation: String
}

struct DevInterviewItem: Identifiable, Hashable, Sendable {
    let id: String
    let category: String
    let title: String
    let content: String
}

struct SpellingBeePuzzle: Identifiable, Hashable, Sendable {
    let id: String
    let center: String
    let outer: [String]
    let answers: Set<String>
}

struct GrammarChapterItem: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let source: String
    let content: String
}

struct BookGrammarLesson: Identifiable, Hashable, Sendable {
    let id: Int
    let title: String
    let level: String
    let explanation: String
    let patterns: String
    let examples: String
    let practice: String
    let answers: String
}

struct BookVocabUnit: Identifiable, Hashable, Sendable {
    let id: Int
    let title: String
    let level: String
    let words: [(en: String, my: String)]
    let collocations: String
    let examples: String

    static func == (lhs: BookVocabUnit, rhs: BookVocabUnit) -> Bool { lhs.id == rhs.id }
    func hash(into hasher: inout Hasher) { hasher.combine(id) }
}

struct BookReadingItem: Identifiable, Hashable, Sendable {
    let id: Int
    let title: String
    let level: String
    let passage: String
    let grammarNotes: String
    let questions: String
    let answers: String
}

struct BookWritingItem: Identifiable, Hashable, Sendable {
    let id: Int
    let title: String
    let level: String
    let task: String
    let modelAnswer: String
    let whyItWorks: String
    let checklist: String
}

struct BookIrregularVerb: Identifiable, Hashable, Sendable {
    let id: String
    let v1: String
    let v2: String
    let v3: String
    let my: String
}

struct BookStudyWeek: Identifiable, Hashable, Sendable {
    let id: String
    let week: String
    let grammar: String
    let vocabulary: String
    let skills: String
}

final class DictionaryStore: @unchecked Sendable {
    private var db: OpaquePointer?
    let databasePath: String

    init(databasePath: String? = nil) {
        let bundleDb = Bundle.main.resourcePath.map { $0 + "/ornagai.sqlite" }
        let candidates = [
            databasePath,
            bundleDb,
            FileManager.default.currentDirectoryPath + "/packages/data-pipeline/out/ornagai.sqlite",
            FileManager.default.currentDirectoryPath + "/../../packages/data-pipeline/out/ornagai.sqlite",
            "/Users/htet/Downloads/HL/packages/data-pipeline/out/ornagai.sqlite"
        ].compactMap { $0 }

        self.databasePath = candidates.first(where: { FileManager.default.fileExists(atPath: $0) }) ?? candidates.last!
        openDatabase()
    }

    deinit {
        if let db { sqlite3_close(db) }
    }

    var isConnected: Bool { db != nil }

    private func openDatabase() {
        var ptr: OpaquePointer?
        if sqlite3_open_v2(databasePath, &ptr, SQLITE_OPEN_READONLY, nil) == SQLITE_OK {
            self.db = ptr
        }
    }

    func tableCount(_ table: String) -> Int {
        guard let db else { return 0 }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        if sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM \(table);", -1, &stmt, nil) == SQLITE_OK {
            if sqlite3_step(stmt) == SQLITE_ROW {
                return Int(sqlite3_column_int64(stmt, 0))
            }
        }
        return 0
    }

    func searchEnMy(prefix: String, limit: Int = 50) -> [DictionaryEntry] {
        guard let db else { return [] }
        let q = prefix.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let sql = q.isEmpty
            ? "SELECT id, word, plain_text FROM entries WHERE word_lower IN ('hello','language','resilience','culture','dictionary','translate','learn','grammar','philosophy','serendipity','wisdom','harmony') LIMIT ?;"
            : "SELECT id, word, plain_text FROM entries WHERE word_lower LIKE ? ORDER BY length(word_lower) ASC LIMIT ?;"

        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }

        if q.isEmpty {
            sqlite3_bind_int(stmt, 1, Int32(limit))
        } else {
            sqlite3_bind_text(stmt, 1, ((q + "%") as NSString).utf8String, -1, nil)
            sqlite3_bind_int(stmt, 2, Int32(limit))
        }

        var results: [DictionaryEntry] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            results.append(DictionaryEntry(
                id: sqlite3_column_int64(stmt, 0),
                word: colText(stmt, 1),
                plainText: colText(stmt, 2)
            ))
        }
        return results
    }

    func searchEnZh(prefix: String, limit: Int = 50) -> [EnZhEntry] {
        guard let db else { return [] }
        let q = prefix.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let sql = q.isEmpty
            ? "SELECT id, word, phonetic, translation, tag FROM ecdict_en_zh WHERE tag LIKE '%ielts%' LIMIT ?;"
            : "SELECT id, word, phonetic, translation, tag FROM ecdict_en_zh WHERE word_lower LIKE ? ORDER BY length(word_lower) ASC LIMIT ?;"

        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }

        if q.isEmpty {
            sqlite3_bind_int(stmt, 1, Int32(limit))
        } else {
            sqlite3_bind_text(stmt, 1, ((q + "%") as NSString).utf8String, -1, nil)
            sqlite3_bind_int(stmt, 2, Int32(limit))
        }

        var results: [EnZhEntry] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            results.append(EnZhEntry(
                id: sqlite3_column_int64(stmt, 0),
                word: colText(stmt, 1),
                phonetic: colText(stmt, 2),
                translation: colText(stmt, 3),
                tag: colText(stmt, 4)
            ))
        }
        return results
    }

    func searchEnEn(prefix: String, limit: Int = 50) -> [EnEnEntry] {
        guard let db else { return [] }
        let q = prefix.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let sql = q.isEmpty
            ? "SELECT id, word, definitions FROM en_en_entries WHERE word_lower IN ('language','culture','resilience','fluent','wisdom','harmony','knowledge','dictionary') LIMIT ?;"
            : "SELECT id, word, definitions FROM en_en_entries WHERE word_lower LIKE ? ORDER BY length(word_lower) ASC LIMIT ?;"

        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }

        if q.isEmpty {
            sqlite3_bind_int(stmt, 1, Int32(limit))
        } else {
            sqlite3_bind_text(stmt, 1, ((q + "%") as NSString).utf8String, -1, nil)
            sqlite3_bind_int(stmt, 2, Int32(limit))
        }

        var results: [EnEnEntry] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let rawDefs = colText(stmt, 2)
            results.append(EnEnEntry(
                id: sqlite3_column_int64(stmt, 0),
                word: colText(stmt, 1),
                definitions: rawDefs.components(separatedBy: " | ").filter { !$0.isEmpty }
            ))
        }
        return results
    }

    func fetchUnifiedVocab(filter: String = "ALL", search: String = "", limit: Int = 250) -> [UnifiedVocabItem] {
        guard let db else { return [] }
        var clauses: [String] = []
        var binds: [String] = []

        if filter != "ALL" {
            let f = filter.lowercased()
            clauses.append("(lower(tag) LIKE ? OR lower(level) = ?)")
            binds.append("%\(f)%")
            binds.append(f)
        }
        let q = search.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if !q.isEmpty {
            clauses.append("(lower(word) LIKE ? OR my_def LIKE ? OR zh_def LIKE ?)")
            binds.append("%\(q)%")
            binds.append("%\(q)%")
            binds.append("%\(q)%")
        }

        let whereSql = clauses.isEmpty ? "" : "WHERE " + clauses.joined(separator: " AND ")
        let sql = "SELECT id, word, ipa, my_def, zh_def, en_def, tag, level FROM unified_vocab \(whereSql) LIMIT ?;"

        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }

        for (i, val) in binds.enumerated() {
            sqlite3_bind_text(stmt, Int32(i + 1), (val as NSString).utf8String, -1, nil)
        }
        sqlite3_bind_int(stmt, Int32(binds.count + 1), Int32(limit))

        var items: [UnifiedVocabItem] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            items.append(UnifiedVocabItem(
                id: sqlite3_column_int64(stmt, 0),
                word: colText(stmt, 1),
                ipa: colText(stmt, 2),
                myDef: colText(stmt, 3),
                zhDef: colText(stmt, 4),
                enDef: colText(stmt, 5),
                tag: colText(stmt, 6),
                level: colText(stmt, 7)
            ))
        }
        return items
    }

    func randomVocabSample(count: Int = 40) -> [UnifiedVocabItem] {
        guard let db else { return [] }
        let sql = "SELECT id, word, ipa, my_def, zh_def, en_def, tag, level FROM unified_vocab ORDER BY RANDOM() LIMIT ?;"
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }
        sqlite3_bind_int(stmt, 1, Int32(count))

        var items: [UnifiedVocabItem] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            items.append(UnifiedVocabItem(
                id: sqlite3_column_int64(stmt, 0),
                word: colText(stmt, 1),
                ipa: colText(stmt, 2),
                myDef: colText(stmt, 3),
                zhDef: colText(stmt, 4),
                enDef: colText(stmt, 5),
                tag: colText(stmt, 6),
                level: colText(stmt, 7)
            ))
        }
        return items
    }

    func searchMyanmarNames(query: String, limit: Int = 60) -> [MyanmarNamePair] {
        guard let db else { return [] }
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let sql = q.isEmpty
            ? "SELECT en, mm FROM myanmar_names LIMIT ?;"
            : "SELECT en, mm FROM myanmar_names WHERE lower(en) LIKE ? OR mm LIKE ? LIMIT ?;"

        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }

        if q.isEmpty {
            sqlite3_bind_int(stmt, 1, Int32(limit))
        } else {
            let p = "%\(q.lowercased())%"
            sqlite3_bind_text(stmt, 1, (p as NSString).utf8String, -1, nil)
            sqlite3_bind_text(stmt, 2, ("%\(q)%" as NSString).utf8String, -1, nil)
            sqlite3_bind_int(stmt, 3, Int32(limit))
        }

        var items: [MyanmarNamePair] = []
        var idx = 0
        while sqlite3_step(stmt) == SQLITE_ROW {
            let en = colText(stmt, 0)
            let mm = colText(stmt, 1)
            items.append(MyanmarNamePair(id: "\(idx)-\(en)-\(mm)", en: en, mm: mm))
            idx += 1
        }
        return items
    }

    func sampleByCefrLevel(_ level: String, count: Int = 2) -> [UnifiedVocabItem] {
        guard let db else { return [] }
        let sql = "SELECT id, word, ipa, my_def, zh_def, en_def, tag, level FROM unified_vocab WHERE level = ? ORDER BY RANDOM() LIMIT ?;"
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }
        sqlite3_bind_text(stmt, 1, (level as NSString).utf8String, -1, nil)
        sqlite3_bind_int(stmt, 2, Int32(count))

        var items: [UnifiedVocabItem] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            items.append(UnifiedVocabItem(
                id: sqlite3_column_int64(stmt, 0),
                word: colText(stmt, 1),
                ipa: colText(stmt, 2),
                myDef: colText(stmt, 3),
                zhDef: colText(stmt, 4),
                enDef: colText(stmt, 5),
                tag: colText(stmt, 6),
                level: colText(stmt, 7)
            ))
        }
        return items
    }

    func lookupExactVocab(_ words: [String]) -> [String: UnifiedVocabItem] {
        guard let db, !words.isEmpty else { return [:] }
        let clean = Array(Set(words.map { $0.lowercased() })).prefix(160)
        let placeholders = clean.map { _ in "?" }.joined(separator: ",")
        let sql = "SELECT id, word, ipa, my_def, zh_def, en_def, tag, level FROM unified_vocab WHERE lower(word) IN (\(placeholders));"
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [:] }
        for (i, w) in clean.enumerated() {
            sqlite3_bind_text(stmt, Int32(i + 1), (w as NSString).utf8String, -1, nil)
        }
        var out: [String: UnifiedVocabItem] = [:]
        while sqlite3_step(stmt) == SQLITE_ROW {
            let item = UnifiedVocabItem(
                id: sqlite3_column_int64(stmt, 0),
                word: colText(stmt, 1),
                ipa: colText(stmt, 2),
                myDef: colText(stmt, 3),
                zhDef: colText(stmt, 4),
                enDef: colText(stmt, 5),
                tag: colText(stmt, 6),
                level: colText(stmt, 7)
            )
            out[item.word.lowercased()] = item
        }
        return out
    }

    func fetchGrammarExercises(topic: String = "ALL", limit: Int = 25) -> [GrammarExerciseItem] {
        guard let db else { return [] }
        let sql = topic == "ALL"
            ? "SELECT id, topic, question, options_Pipe, answer, explanation FROM grammar_exercises ORDER BY RANDOM() LIMIT ?;"
            : "SELECT id, topic, question, options_Pipe, answer, explanation FROM grammar_exercises WHERE topic LIKE ? ORDER BY RANDOM() LIMIT ?;"
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }
        if topic == "ALL" {
            sqlite3_bind_int(stmt, 1, Int32(limit))
        } else {
            let p = "%\(topic)%"
            sqlite3_bind_text(stmt, 1, (p as NSString).utf8String, -1, nil)
            sqlite3_bind_int(stmt, 2, Int32(limit))
        }
        var items: [GrammarExerciseItem] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let opts = colText(stmt, 3).components(separatedBy: "|").filter { !$0.isEmpty }.shuffled()
            items.append(GrammarExerciseItem(
                id: sqlite3_column_int64(stmt, 0),
                topic: colText(stmt, 1),
                question: colText(stmt, 2),
                options: opts,
                answer: colText(stmt, 4),
                explanation: colText(stmt, 5)
            ))
        }
        return items
    }

    func fetchDevInterview(query: String = "") -> [DevInterviewItem] {
        guard let db else { return [] }
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let sql = q.isEmpty
            ? "SELECT category, title, content FROM dev_interview LIMIT 40;"
            : "SELECT category, title, content FROM dev_interview WHERE lower(title) LIKE ? OR lower(content) LIKE ? LIMIT 40;"
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }
        if !q.isEmpty {
            let p = "%\(q)%"
            sqlite3_bind_text(stmt, 1, (p as NSString).utf8String, -1, nil)
            sqlite3_bind_text(stmt, 2, (p as NSString).utf8String, -1, nil)
        }
        var items: [DevInterviewItem] = []
        var idx = 0
        while sqlite3_step(stmt) == SQLITE_ROW {
            let t = colText(stmt, 1)
            items.append(DevInterviewItem(id: "\(idx)-\(t)", category: colText(stmt, 0), title: t, content: colText(stmt, 2)))
            idx += 1
        }
        return items
    }

    func fetchSpellingBeePuzzles() -> [SpellingBeePuzzle] {
        guard let db else { return [] }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, "SELECT id, center, outer_letters, answers_pipe FROM spelling_bee;", -1, &stmt, nil) == SQLITE_OK else { return [] }
        var items: [SpellingBeePuzzle] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let outer = colText(stmt, 2).map { String($0) }
            let answers = Set(colText(stmt, 3).components(separatedBy: "|").filter { !$0.isEmpty })
            items.append(SpellingBeePuzzle(id: colText(stmt, 0), center: colText(stmt, 1), outer: outer, answers: answers))
        }
        return items
    }

    func fetchGrammarChapters() -> [GrammarChapterItem] {
        guard let db else { return [] }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, "SELECT id, title, source, content FROM grammar_chapters;", -1, &stmt, nil) == SQLITE_OK else { return [] }
        var items: [GrammarChapterItem] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            items.append(GrammarChapterItem(id: colText(stmt, 0), title: colText(stmt, 1), source: colText(stmt, 2), content: colText(stmt, 3)))
        }
        return items
    }

    func fetchBookGrammarLessons() -> [BookGrammarLesson] {
        guard let db else { return [] }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, "SELECT number, title, level, explanation, patterns, examples, practice, answers FROM book_grammar_lessons ORDER BY number ASC;", -1, &stmt, nil) == SQLITE_OK else { return [] }
        var out: [BookGrammarLesson] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            out.append(BookGrammarLesson(
                id: Int(sqlite3_column_int(stmt, 0)),
                title: colText(stmt, 1), level: colText(stmt, 2), explanation: colText(stmt, 3),
                patterns: colText(stmt, 4), examples: colText(stmt, 5), practice: colText(stmt, 6), answers: colText(stmt, 7)
            ))
        }
        return out
    }

    func fetchBookVocabUnits() -> [BookVocabUnit] {
        guard let db else { return [] }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, "SELECT number, title, level, words_json, collocations, examples FROM book_vocab_units ORDER BY number ASC;", -1, &stmt, nil) == SQLITE_OK else { return [] }
        var out: [BookVocabUnit] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let rawJson = colText(stmt, 3)
            var pairs: [(String, String)] = []
            if let data = rawJson.data(using: .utf8),
               let arr = try? JSONSerialization.jsonObject(with: data) as? [[String]] {
                for p in arr where p.count >= 2 { pairs.append((p[0], p[1])) }
            }
            out.append(BookVocabUnit(
                id: Int(sqlite3_column_int(stmt, 0)),
                title: colText(stmt, 1), level: colText(stmt, 2), words: pairs,
                collocations: colText(stmt, 4), examples: colText(stmt, 5)
            ))
        }
        return out
    }

    func fetchBookReadings() -> [BookReadingItem] {
        guard let db else { return [] }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, "SELECT number, title, level, passage, grammar_notes, questions, answers FROM book_readings ORDER BY number ASC;", -1, &stmt, nil) == SQLITE_OK else { return [] }
        var out: [BookReadingItem] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            out.append(BookReadingItem(
                id: Int(sqlite3_column_int(stmt, 0)),
                title: colText(stmt, 1), level: colText(stmt, 2), passage: colText(stmt, 3),
                grammarNotes: colText(stmt, 4), questions: colText(stmt, 5), answers: colText(stmt, 6)
            ))
        }
        return out
    }

    func fetchBookWritings() -> [BookWritingItem] {
        guard let db else { return [] }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, "SELECT number, title, level, task, model_answer, why_it_works, checklist FROM book_writings ORDER BY number ASC;", -1, &stmt, nil) == SQLITE_OK else { return [] }
        var out: [BookWritingItem] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            out.append(BookWritingItem(
                id: Int(sqlite3_column_int(stmt, 0)),
                title: colText(stmt, 1), level: colText(stmt, 2), task: colText(stmt, 3),
                modelAnswer: colText(stmt, 4), whyItWorks: colText(stmt, 5), checklist: colText(stmt, 6)
            ))
        }
        return out
    }

    func fetchBookIrregularVerbs() -> [BookIrregularVerb] {
        guard let db else { return [] }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, "SELECT v1, v2, v3, my FROM book_irregular_verbs;", -1, &stmt, nil) == SQLITE_OK else { return [] }
        var out: [BookIrregularVerb] = []
        var i = 0
        while sqlite3_step(stmt) == SQLITE_ROW {
            let v1 = colText(stmt, 0)
            out.append(BookIrregularVerb(id: "\(i)-\(v1)", v1: v1, v2: colText(stmt, 1), v3: colText(stmt, 2), my: colText(stmt, 3)))
            i += 1
        }
        return out
    }

    func fetchBookStudyPlan() -> [BookStudyWeek] {
        guard let db else { return [] }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, "SELECT week, grammar, vocabulary, skills FROM book_study_plan;", -1, &stmt, nil) == SQLITE_OK else { return [] }
        var out: [BookStudyWeek] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let wk = colText(stmt, 0)
            out.append(BookStudyWeek(id: wk, week: wk, grammar: colText(stmt, 1), vocabulary: colText(stmt, 2), skills: colText(stmt, 3)))
        }
        return out
    }

    private func colText(_ stmt: OpaquePointer?, _ idx: Int32) -> String {
        guard let cstr = sqlite3_column_text(stmt, idx) else { return "" }
        return String(cString: cstr)
    }
}
