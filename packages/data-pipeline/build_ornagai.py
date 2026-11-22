#!/usr/bin/env python3
"""
HtetLingo Data Pipeline — Ornagai StarDict Parser
Parses ornagai-en-my-dict.zip (111,232 headwords, 249,103 examples) into:
  1. SQLite3 + FTS5 database (for Mac Native SwiftUI & Android Kotlin/Compose)
  2. Sharded JSON prefix files (for Cloudflare Pages Web App & iPad PWA offline cache)
  3. Curated demo JSON (for GitHub Pages Static Demo)
"""

import html
import json
import os
import re
import sqlite3
import struct
import sys
import zipfile
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_ZIP = Path(
    "/Users/htet/.gemini/antigravity/brain/12984283-454b-4d8f-a95a-dc0090f62cac/.user_uploaded/media_1795366427948_5929b894.zip"
)
OUT_DIR = ROOT / "packages" / "data-pipeline" / "out"
WEB_DICT_DIR = ROOT / "apps" / "web" / "public" / "dict"
SITE_DICT_DIR = ROOT / "apps" / "site" / "public" / "dict"

TAG_RE = re.compile(r"<[^>]+>")

DEMO_SEED_WORDS = {
    "hello", "world", "language", "learn", "culture", "understand", "connect",
    "dictionary", "translate", "vocabulary", "grammar", "pronunciation", "listen",
    "speak", "read", "write", "study", "school", "teacher", "student", "book",
    "knowledge", "wisdom", "friend", "family", "love", "peace", "happy", "smile",
    "water", "food", "coffee", "tea", "morning", "evening", "night", "today",
    "tomorrow", "yesterday", "time", "year", "month", "week", "day", "sun",
    "moon", "star", "earth", "sky", "rain", "wind", "fire", "mountain", "river",
    "ocean", "tree", "flower", "music", "art", "science", "technology", "computer",
    "phone", "internet", "code", "design", "build", "create", "future", "dream",
    "hope", "courage", "kindness", "respect", "grateful", "thank", "please",
    "sorry", "welcome", "goodbye", "travel", "journey", "adventure", "explore",
    "discover", "achieve", "success", "practice", "memory", "focus", "question",
    "answer", "example", "sentence", "meaning", "word", "phrase", "accent",
    "fluent", "native", "bilingual", "multilingual", "myanmar", "english",
    "japanese", "chinese", "korean", "thai", "french", "german", "apple",
    "beautiful", "brilliant", "calm", "challenge", "clarity", "community",
    "confidence", "curious", "dedication", "effort", "energy", "essential",
    "freedom", "growth", "harmony", "inspire", "joy", "leader", "mindful",
    "nature", "opportunity", "passion", "patience", "progress", "quality",
    "reason", "resilience", "simple", "strength", "thought", "trust", "unity",
    "value", "vision", "wonder", "zen"
}


def strip_html(raw: str) -> str:
    return html.unescape(TAG_RE.sub(" ", raw)).strip()


def parse_stardict(zip_path: Path):
    with zipfile.ZipFile(zip_path) as zf:
        idx = zf.read("ornagai-en-my/ornagai-en-my.idx")
        dct = zf.read("ornagai-en-my/ornagai-en-my.dict")

    entries = []
    pos = 0
    idx_len = len(idx)
    while pos < idx_len:
        end = idx.index(b"\x00", pos)
        word = idx[pos:end].decode("utf-8", errors="replace").strip()
        offset, size = struct.unpack(">II", idx[end + 1 : end + 9])
        pos = end + 9
        body_html = dct[offset : offset + size].decode("utf-8", errors="replace").strip()
        if word:
            entries.append((word, body_html))
    return entries


def build_sqlite(entries, db_path: Path):
    db_path.parent.mkdir(parents=True, exist_ok=True)
    if db_path.exists():
        db_path.unlink()

    conn = sqlite3.connect(str(db_path))
    cur = conn.cursor()
    cur.execute("PRAGMA journal_mode = OFF;")
    cur.execute("PRAGMA synchronous = OFF;")
    cur.execute(
        """
        CREATE TABLE entries (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            word TEXT NOT NULL,
            word_lower TEXT NOT NULL,
            body_html TEXT NOT NULL,
            plain_text TEXT NOT NULL
        );
        """
    )
    cur.execute("CREATE INDEX idx_entries_word_lower ON entries(word_lower);")
    cur.execute(
        """
        CREATE VIRTUAL TABLE entries_fts USING fts5(
            word,
            plain_text,
            content='entries',
            content_rowid='id'
        );
        """
    )

    batch = []
    for word, body_html in entries:
        batch.append((word, word.lower(), body_html, strip_html(body_html)))
        if len(batch) >= 5000:
            cur.executemany(
                "INSERT INTO entries(word, word_lower, body_html, plain_text) VALUES (?, ?, ?, ?)",
                batch,
            )
            batch.clear()
    if batch:
        cur.executemany(
            "INSERT INTO entries(word, word_lower, body_html, plain_text) VALUES (?, ?, ?, ?)",
            batch,
        )

    cur.execute("INSERT INTO entries_fts(entries_fts) VALUES('rebuild');")
    conn.commit()
    cur.execute("VACUUM;")
    conn.close()


def build_web_shards(entries, web_dir: Path, site_dir: Path):
    shards_dir = web_dir / "shards"
    shards_dir.mkdir(parents=True, exist_ok=True)
    site_dir.mkdir(parents=True, exist_ok=True)

    shards = defaultdict(list)
    demo_entries = []

    for word, body_html in entries:
        first = word[0].lower()
        key = first if "a" <= first <= "z" else "other"
        shards[key].append([word, body_html])
        if word.lower() in DEMO_SEED_WORDS:
            demo_entries.append({"word": word, "html": body_html})

    shard_meta = {}
    for key, items in sorted(shards.items()):
        out_file = shards_dir / f"{key}.json"
        payload = json.dumps(items, ensure_ascii=False, separators=(",", ":"))
        out_file.write_text(payload, encoding="utf-8")
        shard_meta[key] = {"count": len(items), "bytes": len(payload.encode("utf-8"))}

    index_meta = {
        "source": "Ornagai English-to-Myanmar Dictionary (v3)",
        "wordCount": len(entries),
        "shards": shard_meta,
    }
    (web_dir / "index.json").write_text(
        json.dumps(index_meta, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    # Sort demo entries alphabetically
    demo_entries.sort(key=lambda x: x["word"].lower())
    (site_dir / "demo.json").write_text(
        json.dumps(demo_entries, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    # Also copy demo.json into web as instant starter pack before shards load
    (web_dir / "starter.json").write_text(
        json.dumps(demo_entries, ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )
    return shard_meta, len(demo_entries)


def main():
    zip_path = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_ZIP
    if not zip_path.exists():
        print(f"ERROR: Ornagai zip not found at {zip_path}", file=sys.stderr)
        sys.exit(1)

    print(f"Parsing {zip_path}...")
    entries = parse_stardict(zip_path)
    print(f"Parsed {len(entries):,} headwords.")

    sqlite_path = OUT_DIR / "ornagai.sqlite"
    print(f"Building SQLite FTS5 database at {sqlite_path}...")
    build_sqlite(entries, sqlite_path)
    print(f"SQLite size: {sqlite_path.stat().st_size / (1024*1024):.2f} MB")

    print(f"Building web shards in {WEB_DICT_DIR} and site demo in {SITE_DICT_DIR}...")
    shard_meta, demo_count = build_web_shards(entries, WEB_DICT_DIR, SITE_DICT_DIR)
    max_shard = max(shard_meta.items(), key=lambda kv: kv[1]["bytes"])
    print(
        f"Created {len(shard_meta)} shards (largest: '{max_shard[0]}' = "
        f"{max_shard[1]['bytes'] / (1024*1024):.2f} MB, {max_shard[1]['count']:,} words). "
        f"Demo/starter words: {demo_count}."
    )


if __name__ == "__main__":
    main()
