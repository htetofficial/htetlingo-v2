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

    private func colText(_ stmt: OpaquePointer?, _ idx: Int32) -> String {
        guard let cstr = sqlite3_column_text(stmt, idx) else { return "" }
        return String(cString: cstr)
    }
}
