#!/usr/bin/env python3
"""
HtetLingo CEFR Integration Pipeline
Combines:
  - openlanguageprofiles/olp-en-cefrj (CEFR-J A1-B2 vocab + Octanove C1-C2 vocab + CEFR-J Grammar Profile)
  - Maximax67/Words-CEFR-Dataset (frequency-ranked words.csv)
Updates:
  1. ornagai.sqlite (for Mac Native App v2.2)
  2. apps/android/app/src/main/assets/htetlingo_mobile.sqlite (for Android APK v2.3)
  3. apps/web/public/datasets/vocab-core.json & cefr-grammar.json (for Web App & iPad PWA)
"""

import csv
import io
import json
import sqlite3
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ORNAGAI_DB = ROOT / "packages" / "data-pipeline" / "out" / "ornagai.sqlite"
MOBILE_DB = ROOT / "apps" / "android" / "app" / "src" / "main" / "assets" / "htetlingo_mobile.sqlite"
WEB_DATASETS = ROOT / "apps" / "web" / "public" / "datasets"

CEFR_ORDER = {"A1": 1, "A2": 2, "B1": 3, "B2": 4, "C1": 5, "C2": 6}


def fetch_text(url: str) -> str:
    req = urllib.request.Request(url, headers={"User-Agent": "HtetLingo-CEFR/2.3"})
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.read().decode("utf-8-sig", errors="replace")


def load_cefr_maps():
    cefr_map = {}

    # 1. CEFR-J A1-B2 Vocabulary Profile
    print("[1/4] Fetching CEFR-J A1-B2 & Octanove C1-C2 vocabulary profiles...")
    for url in [
        "https://raw.githubusercontent.com/openlanguageprofiles/olp-en-cefrj/master/cefrj-vocabulary-profile-1.5.csv",
        "https://raw.githubusercontent.com/openlanguageprofiles/olp-en-cefrj/master/octanove-vocabulary-profile-c1c2-1.0.csv",
    ]:
        raw = fetch_text(url)
        reader = csv.DictReader(io.StringIO(raw))
        for row in reader:
            hw = (row.get("headword") or "").strip()
            lvl = (row.get("CEFR") or "").strip().upper()[:2]
            if lvl not in CEFR_ORDER or not hw:
                continue
            for variant in hw.split("/"):
                w = variant.strip().lower()
                if not w:
                    continue
                if w not in cefr_map or CEFR_ORDER[lvl] < CEFR_ORDER[cefr_map[w]]:
                    cefr_map[w] = lvl
    print(f"    -> CEFR-J + Octanove mapped {len(cefr_map):,} exact headwords (A1–C2).")

    # 2. Maximax67/Words-CEFR-Dataset frequency ranks (fallback for words not in CEFR-J)
    print("[2/4] Fetching Maximax67/Words-CEFR-Dataset words.csv...")
    try:
        raw_m = fetch_text("https://raw.githubusercontent.com/Maximax67/Words-CEFR-Dataset/main/csv/words.csv")
        reader_m = csv.DictReader(io.StringIO(raw_m))
        added = 0
        for row in reader_m:
            w = (row.get("word") or "").strip().lower()
            try:
                wid = int(row.get("word_id") or 0)
            except ValueError:
                continue
            if not w or w in cefr_map:
                continue
            if wid <= 800:
                lvl = "A1"
            elif wid <= 2200:
                lvl = "A2"
            elif wid <= 5000:
                lvl = "B1"
            elif wid <= 10000:
                lvl = "B2"
            elif wid <= 20000:
                lvl = "C1"
            else:
                lvl = "C2"
            cefr_map[w] = lvl
            added += 1
        print(f"    -> Maximax67 added {added:,} ranked CEFR words (total CEFR map: {len(cefr_map):,}).")
    except Exception as e:
        print(f"    -> WARN Maximax67: {e}")

    return cefr_map


def load_cefr_grammar():
    print("[3/4] Fetching CEFR-J English Grammar Profile...")
    raw = fetch_text(
        "https://raw.githubusercontent.com/openlanguageprofiles/olp-en-cefrj/master/cefrj-grammar-profile-20180315.csv"
    )
    reader = csv.DictReader(io.StringIO(raw))
    items = []
    for row in reader:
        item = (row.get("Grammatical Item") or "").strip()
        lvl = (row.get("CEFR-J Level") or row.get("Core Inventory") or "").strip()
        stype = (row.get("Sentence Type") or "").strip()
        code = (row.get("Shorthand Code") or "").strip()
        if not item:
            continue
        band = lvl[:2].upper() if lvl[:2].upper() in CEFR_ORDER else "B1"
        items.append({
            "code": code,
            "item": item,
            "type": stype,
            "level": band,
            "sublevel": lvl or band,
        })
    print(f"    -> Parsed {len(items):,} CEFR-J grammar items.")
    return items


def apply_to_databases(cefr_map, grammar_items):
    print("[4/4] Updating ornagai.sqlite, htetlingo_mobile.sqlite, and web datasets...")

    # Update web vocab-core.json with true A1-C2 CEFR levels
    vocab_json = WEB_DATASETS / "vocab-core.json"
    vdata = json.loads(vocab_json.read_text(encoding="utf-8"))
    counts = {k: 0 for k in CEFR_ORDER}
    updated_items = []
    for entry in vdata["items"]:
        word = entry[0]
        true_lvl = cefr_map.get(word.lower(), entry[6] if entry[6] in CEFR_ORDER else "B1")
        entry[6] = true_lvl
        counts[true_lvl] = counts.get(true_lvl, 0) + 1
        updated_items.append(entry)

    vdata["source"] = "Ornagai EN-MY + ECDICT EN-ZH + EN-EN + mahavivo + CEFR-J + Maximax67 (A1-C2)"
    vdata["items"] = updated_items
    vocab_json.write_text(json.dumps(vdata, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    # Write cefr-grammar.json for web
    (WEB_DATASETS / "cefr-grammar.json").write_text(
        json.dumps({"source": "openlanguageprofiles/olp-en-cefrj", "count": len(grammar_items), "items": grammar_items},
                   ensure_ascii=False, separators=(",", ":")),
        encoding="utf-8",
    )

    # Update ornagai.sqlite (Mac Native)
    if ORNAGAI_DB.exists():
        conn = sqlite3.connect(str(ORNAGAI_DB))
        cur = conn.cursor()
        cur.executemany(
            "UPDATE unified_vocab SET level = ? WHERE lower(word) = ?",
            [(e[6], e[0].lower()) for e in updated_items],
        )
        cur.execute("DROP TABLE IF EXISTS cefr_grammar;")
        cur.execute("CREATE TABLE cefr_grammar (code TEXT, item TEXT, stype TEXT, level TEXT, sublevel TEXT);")
        cur.executemany(
            "INSERT INTO cefr_grammar VALUES (?, ?, ?, ?, ?)",
            [(g["code"], g["item"], g["type"], g["level"], g["sublevel"]) for g in grammar_items],
        )
        conn.commit()
        conn.close()

    # Update htetlingo_mobile.sqlite (Android Offline)
    if MOBILE_DB.exists():
        mconn = sqlite3.connect(str(MOBILE_DB))
        mcur = mconn.cursor()
        mcur.executemany(
            "UPDATE vocab SET level = ? WHERE word_lower = ?",
            [(e[6], e[0].lower()) for e in updated_items],
        )
        mcur.execute("DROP TABLE IF EXISTS cefr_grammar;")
        mcur.execute("CREATE TABLE cefr_grammar (code TEXT, item TEXT, stype TEXT, level TEXT, sublevel TEXT);")
        mcur.executemany(
            "INSERT INTO cefr_grammar VALUES (?, ?, ?, ?, ?)",
            [(g["code"], g["item"], g["type"], g["level"], g["sublevel"]) for g in grammar_items],
        )
        mconn.commit()
        mcur.execute("VACUUM;")
        mconn.close()

    print(f"    -> True CEFR distribution across 15,000 vocab words: {counts}")
    print(f"    -> Mobile DB size: {MOBILE_DB.stat().st_size / (1024*1024):.2f} MB")


def main():
    cefr_map = load_cefr_maps()
    grammar_items = load_cefr_grammar()
    apply_to_databases(cefr_map, grammar_items)


if __name__ == "__main__":
    main()
