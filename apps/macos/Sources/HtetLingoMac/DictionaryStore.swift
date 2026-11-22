import Foundation
import SQLite3

struct DictionaryEntry: Identifiable, Hashable, Sendable {
    let id: Int64
    let word: String
    let plainText: String
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

    func totalWordCount() -> Int {
        guard let db else { return 0 }
        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        if sqlite3_prepare_v2(db, "SELECT COUNT(*) FROM entries;", -1, &stmt, nil) == SQLITE_OK {
            if sqlite3_step(stmt) == SQLITE_ROW {
                return Int(sqlite3_column_int64(stmt, 0))
            }
        }
        return 0
    }

    func search(prefix: String, limit: Int = 40) -> [DictionaryEntry] {
        guard let db else { return [] }
        let q = prefix.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let sql: String
        if q.isEmpty {
            sql = "SELECT id, word, plain_text FROM entries WHERE word_lower IN ('hello','language','resilience','culture','dictionary','translate','learn','grammar') LIMIT ?;"
        } else {
            sql = "SELECT id, word, plain_text FROM entries WHERE word_lower LIKE ? ORDER BY length(word_lower) ASC LIMIT ?;"
        }

        var stmt: OpaquePointer?
        defer { sqlite3_finalize(stmt) }
        guard sqlite3_prepare_v2(db, sql, -1, &stmt, nil) == SQLITE_OK else { return [] }

        if q.isEmpty {
            sqlite3_bind_int(stmt, 1, Int32(limit))
        } else {
            let pattern = q + "%"
            sqlite3_bind_text(stmt, 1, (pattern as NSString).utf8String, -1, nil)
            sqlite3_bind_int(stmt, 2, Int32(limit))
        }

        var results: [DictionaryEntry] = []
        while sqlite3_step(stmt) == SQLITE_ROW {
            let id = sqlite3_column_int64(stmt, 0)
            let word = String(cString: sqlite3_column_text(stmt, 1))
            let plain = String(cString: sqlite3_column_text(stmt, 2))
            results.append(DictionaryEntry(id: id, word: word, plainText: plain))
        }
        return results
    }
}
