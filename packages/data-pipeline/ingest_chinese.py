#!/usr/bin/env python3
"""
HtetLingo Multilingual Pipeline — Chinese Expansion
Ingests:
  1. skywind3000/ecdict (ecdict.csv) -> English-to-Chinese 27 A-Z JSON shards + SQLite table
  2. krmanik/Chinese-Grammar (HSK 1 - HSK 6 CSVs) -> Structured HSK1..HSK6 Chinese Grammar JSON
"""

import csv
import io
import json
import sqlite3
import urllib.request
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WEB_PUB = ROOT / "apps" / "web" / "public"
EN_ZH_DIR = WEB_PUB / "dict" / "en-zh"
DATASETS_DIR = WEB_PUB / "datasets"
SQLITE_PATH = ROOT / "packages" / "data-pipeline" / "out" / "ornagai.sqlite"


def fetch_stream(url: str):
    req = urllib.request.Request(url, headers={"User-Agent": "HtetLingo-DataPipeline/2.0"})
    return urllib.request.urlopen(req, timeout=60)


def ingest_hsk_grammar():
    print("[1/2] Fetching Chinese Grammar HSK 1–6 (krmanik/Chinese-Grammar)...")
    points = {}
    order = []

    for lvl in range(1, 7):
        url = f"https://raw.githubusercontent.com/krmanik/Chinese-Grammar/master/CSV%20Files%20HSK1%20-%20HSK6/hsk{lvl}.csv"
        raw = fetch_stream(url).read().decode("utf-8-sig", errors="replace")
        reader = csv.reader(io.StringIO(raw))
        for row in reader:
            if len(row) < 8:
                continue
            zh = row[1].strip()
            pinyin = row[2].strip()
            en = row[3].strip()
            pattern = row[5].strip().strip(":").strip()
            category = row[6].strip()
            title = row[7].strip()
            if not title or not zh:
                continue
            key = (lvl, title)
            if key not in points:
                points[key] = {
                    "id": f"hsk{lvl}-{len(order)+1}",
                    "level": f"HSK {lvl}",
                    "title": title,
                    "pattern": pattern,
                    "category": category,
                    "examples": [],
                }
                order.append(key)
            if len(points[key]["examples"]) < 4:
                points[key]["examples"].append({"zh": zh, "pinyin": pinyin, "en": en})

    lessons = [points[k] for k in order]
    DATASETS_DIR.mkdir(parents=True, exist_ok=True)
    out_file = DATASETS_DIR / "chinese-grammar-hsk.json"
    out_file.write_text(
        json.dumps(
            {
                "source": "krmanik/Chinese-Grammar (HSK 1–6)",
                "totalPoints": len(lessons),
                "lessons": lessons,
            },
            ensure_ascii=False,
            separators=(",", ":"),
        ),
        encoding="utf-8",
    )
    print(f"    -> Ingested {len(lessons)} Chinese grammar points across HSK 1–6.")
    return lessons


def ingest_ecdict():
    print("[2/2] Streaming ECDICT English–Chinese dictionary (skywind3000/ecdict)...")
    url = "https://raw.githubusercontent.com/skywind3000/ecdict/master/ecdict.csv"
    resp = fetch_stream(url)
    text_stream = io.TextIOWrapper(resp, encoding="utf-8", errors="replace", newline="")
    reader = csv.DictReader(text_stream)

    shards = defaultdict(list)
    sqlite_rows = []
    total_scanned = 0
    kept = 0

    for row in reader:
        total_scanned += 1
        word = (row.get("word") or "").strip()
        trans = (row.get("translation") or "").strip()
        if not word or not trans:
            continue

        # Keep high-value dictionary words (Oxford, Collins, exam tags, or top BNC/FRQ frequency <= 25000)
        tag = (row.get("tag") or "").strip()
        collins = (row.get("collins") or "").strip()
        oxford = (row.get("oxford") or "").strip()
        try:
            bnc = int(row.get("bnc") or 0)
        except ValueError:
            bnc = 0
        try:
            frq = int(row.get("frq") or 0)
        except ValueError:
            frq = 0

        is_core = (
            bool(tag)
            or (collins and collins != "0")
            or oxford == "1"
            or (0 < bnc <= 20000)
            or (0 < frq <= 20000)
        )
        if not is_core:
            continue

        phonetic = (row.get("phonetic") or "").strip()
        # Compact translation (replace literal \n with "; ")
        trans_clean = trans.replace("\\n", "; ").replace("\n", "; ")
        first = word[0].lower()
        key = first if "a" <= first <= "z" else "other"
        shards[key].append([word, phonetic, trans_clean, tag])
        sqlite_rows.append((word, word.lower(), phonetic, trans_clean, tag))
        kept += 1

    shards_dir = EN_ZH_DIR / "shards"
    shards_dir.mkdir(parents=True, exist_ok=True)
    meta = {}
    for key, items in sorted(shards.items()):
        items.sort(key=lambda x: x[0].lower())
        payload = json.dumps(items, ensure_ascii=False, separators=(",", ":"))
        (shards_dir / f"{key}.json").write_text(payload, encoding="utf-8")
        meta[key] = {"count": len(items), "bytes": len(payload.encode("utf-8"))}

    (EN_ZH_DIR / "index.json").write_text(
        json.dumps(
            {
                "source": "skywind3000/ecdict (Oxford/Collins/CET/IELTS/TOEFL/BNC Core)",
                "totalScanned": total_scanned,
                "wordCount": kept,
                "shards": meta,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    print(f"    -> Scanned {total_scanned:,} rows, sharded {kept:,} core EN–ZH entries across {len(meta)} shards.")

    if SQLITE_PATH.exists():
        conn = sqlite3.connect(str(SQLITE_PATH))
        cur = conn.cursor()
        cur.execute("DROP TABLE IF EXISTS ecdict_en_zh;")
        cur.execute(
            """
            CREATE TABLE ecdict_en_zh (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                word TEXT NOT NULL,
                word_lower TEXT NOT NULL,
                phonetic TEXT,
                translation TEXT NOT NULL,
                tag TEXT
            );
            """
        )
        cur.execute("CREATE INDEX idx_ecdict_lower ON ecdict_en_zh(word_lower);")
        cur.executemany(
            "INSERT INTO ecdict_en_zh(word, word_lower, phonetic, translation, tag) VALUES (?, ?, ?, ?, ?)",
            sqlite_rows,
        )
        conn.commit()
        conn.close()
        print(f"    -> Added {len(sqlite_rows):,} EN–ZH entries to {SQLITE_PATH.name}.")


def main():
    ingest_hsk_grammar()
    ingest_ecdict()


if __name__ == "__main__":
    main()
