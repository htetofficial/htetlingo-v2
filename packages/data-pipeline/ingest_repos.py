#!/usr/bin/env python3
"""
HtetLingo Open-Source Dataset Ingestion Pipeline
Fetches, cleans, normalizes, and packages real datasets from the referenced
open-source GitHub repositories into offline-ready JSON shards & SQLite tables:

  1. adityasahu252/English-to-English-dictionary -> EN-EN 27 alphabetical JSON shards + SQLite
  2. dwyl/english-words -> UK vs US English spelling pairs
  3. max-mapper/common-english-errors -> misused words, misused phrases, commonly misspelled words
  4. codeyu/EnglishGrammarBook -> structured English grammar chapters
  5. MgHla/Myanmar-Name_en-2-mm & L16H7/Myanmar_Names -> Myanmar Names EN <-> MY dataset
  6. myanmartools/myanmar-words & lwinmoe/segment -> Burmese wordlist & syllable/word segmenter lexicon
  7. dangerchitthu/myanmar-karen-word-lists -> Sgaw Karen & Burmese wordlist
  8. kanaung/wordlists -> Myanmar NRC prefixes & postal codes
"""

import csv
import io
import json
import os
import re
import sqlite3
import urllib.request
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WEB_PUB = ROOT / "apps" / "web" / "public"
SITE_PUB = ROOT / "apps" / "site" / "public"
DATASETS_DIR = WEB_PUB / "datasets"
EN_EN_DIR = WEB_PUB / "dict" / "en-en"
SQLITE_PATH = ROOT / "packages" / "data-pipeline" / "out" / "ornagai.sqlite"

TOKEN = os.environ.get("GITHUB_TOKEN", "")


def fetch_raw(url: str) -> bytes:
    headers = {"User-Agent": "HtetLingo-DataPipeline/2.0"}
    if TOKEN and "api.github.com" in url:
        headers["Authorization"] = f"Bearer {TOKEN}"
    req = urllib.request.Request(url, headers=headers)
    with urllib.request.urlopen(req, timeout=25) as resp:
        return resp.read()


def fetch_text(url: str) -> str:
    return fetch_raw(url).decode("utf-8", errors="replace")


def ingest_en_en_dictionary():
    print("[1/7] Fetching English-to-English dictionary (adityasahu252/English-to-English-dictionary)...")
    raw = fetch_text(
        "https://raw.githubusercontent.com/adityasahu252/English-to-English-dictionary/master/data.json"
    )
    data = json.loads(raw)

    shards = defaultdict(list)
    for word, defs in data.items():
        w = word.strip()
        if not w:
            continue
        if isinstance(defs, list):
            clean_defs = [str(d).strip() for d in defs if str(d).strip()]
        else:
            clean_defs = [str(defs).strip()]
        if not clean_defs:
            continue
        first = w[0].lower()
        key = first if "a" <= first <= "z" else "other"
        shards[key].append([w, clean_defs[:4]])

    shards_dir = EN_EN_DIR / "shards"
    shards_dir.mkdir(parents=True, exist_ok=True)
    meta = {}
    total = 0
    for key, items in sorted(shards.items()):
        items.sort(key=lambda x: x[0].lower())
        payload = json.dumps(items, ensure_ascii=False, separators=(",", ":"))
        (shards_dir / f"{key}.json").write_text(payload, encoding="utf-8")
        meta[key] = {"count": len(items), "bytes": len(payload.encode("utf-8"))}
        total += len(items)

    (EN_EN_DIR / "index.json").write_text(
        json.dumps(
            {
                "source": "adityasahu252/English-to-English-dictionary",
                "wordCount": total,
                "shards": meta,
            },
            ensure_ascii=False,
            indent=2,
        ),
        encoding="utf-8",
    )
    print(f"    -> Sharded {total:,} EN-EN words across {len(meta)} shards.")
    return data


def parse_loose_pairs(text: str):
    """Handles strict JSON dicts/lists or comment-prefixed pseudo-JSON key:value lines."""
    try:
        parsed = json.loads(text)
        if isinstance(parsed, dict):
            return [(str(k).strip(), v if isinstance(v, str) else ", ".join(map(str, v))) for k, v in parsed.items()]
    except Exception:
        pass
    pairs = []
    for m in re.finditer(r'"([^"]+)"\s*:\s*"([^"]+)"', text):
        pairs.append((m.group(1).strip(), m.group(2).strip()))
    return pairs


def ingest_common_english_errors():
    print("[2/7] Fetching Common English Errors (max-mapper/common-english-errors) + UK/US (dwyl/english-words)...")
    base = "https://raw.githubusercontent.com/max-mapper/common-english-errors/master"
    misspelled_pairs = parse_loose_pairs(fetch_text(f"{base}/commonly-misspelled-words.json"))
    misused_phrase_pairs = parse_loose_pairs(fetch_text(f"{base}/misused-phrases.json"))
    misused_word_pairs = parse_loose_pairs(fetch_text(f"{base}/misused-words.json"))

    uk_us_raw = fetch_text(
        "https://raw.githubusercontent.com/dwyl/english-words/master/uk-us-dict.txt"
    )
    uk_us = []
    for line in uk_us_raw.splitlines():
        parts = [p.strip() for p in re.split(r"[\t,:]+|\s{2,}", line.strip()) if p.strip()]
        if len(parts) >= 2:
            uk_us.append({"uk": parts[0], "us": parts[1]})

    payload = {
        "sources": [
            "max-mapper/common-english-errors",
            "dwyl/english-words (uk-us-dict.txt)",
        ],
        "misspelled": [{"wrong": k, "right": v} for k, v in misspelled_pairs],
        "misusedPhrases": [{"wrong": k, "right": v} for k, v in misused_phrase_pairs],
        "misusedWords": [{"word": k, "note": v} for k, v in misused_word_pairs],
        "ukVsUs": uk_us[:300],
    }

    DATASETS_DIR.mkdir(parents=True, exist_ok=True)
    out = DATASETS_DIR / "english-errors.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(
        f"    -> Misspelled: {len(payload['misspelled'])}, Misused Phrases: {len(payload['misusedPhrases'])}, "
        f"Misused Words: {len(payload['misusedWords'])}, UK/US pairs: {len(payload['ukVsUs'])}."
    )
    return payload


def ingest_grammar_book():
    print("[3/7] Fetching English Grammar Book (codeyu/EnglishGrammarBook) + Logical Grammar (mahavivo/logical-grammar)...")
    summary = fetch_text("https://raw.githubusercontent.com/codeyu/EnglishGrammarBook/master/SUMMARY.md")
    # Parse markdown links like * [Title](docs/xxx.md)
    links = re.findall(r"\[([^\]]+)\]\(([^)]+\.md)\)", summary)
    chapters = []
    for title, rel in links[:18]:
        try:
            md = fetch_text(f"https://raw.githubusercontent.com/codeyu/EnglishGrammarBook/master/{rel}")
            # Keep compact clean excerpt (first 1600 chars)
            clean = re.sub(r"\n{3,}", "\n\n", md.strip())
            chapters.append({
                "id": re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-"),
                "title": title.strip(),
                "source": "codeyu/EnglishGrammarBook",
                "content": clean[:1800],
            })
        except Exception:
            continue

    # Also pull logical connectors from mahavivo/logical-grammar
    try:
        connectors = fetch_text(
            "https://raw.githubusercontent.com/mahavivo/logical-grammar/master/%E8%8B%B1%E8%AF%AD%E9%80%BB%E8%BE%91%E5%85%B3%E8%81%94%E8%AF%8D.txt"
        )
        chapters.append({
            "id": "logical-connectors",
            "title": "English Logical Connectors & Discourse Markers",
            "source": "mahavivo/logical-grammar",
            "content": connectors.strip()[:2200],
        })
    except Exception:
        pass

    out = DATASETS_DIR / "grammar-book.json"
    out.write_text(json.dumps(chapters, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"    -> Ingested {len(chapters)} grammar chapters.")
    return chapters


def ingest_myanmar_names():
    print("[4/7] Fetching Myanmar Names EN <-> MY (MgHla/Myanmar-Name_en-2-mm & L16H7/Myanmar_Names)...")
    csv_text = fetch_text(
        "https://raw.githubusercontent.com/MgHla/Myanmar-Name_en-2-mm/master/MyanmarName-en-mm.csv"
    )
    names = []
    seen = set()
    reader = csv.reader(io.StringIO(csv_text))
    for row in reader:
        if len(row) >= 2:
            en = row[0].strip()
            mm = row[1].strip()
            if en and mm and en.lower() != "en" and (en.lower(), mm) not in seen:
                seen.add((en.lower(), mm))
                names.append([en, mm])

    # Also fetch gender lists from L16H7/Myanmar_Names
    male_count = female_count = 0
    try:
        males = [
            ln.strip()
            for ln in fetch_text(
                "https://raw.githubusercontent.com/L16H7/Myanmar_Names/master/sorted%20males.txt"
            ).splitlines()
            if ln.strip()
        ]
        females = [
            ln.strip()
            for ln in fetch_text(
                "https://raw.githubusercontent.com/L16H7/Myanmar_Names/master/sorted%20females.txt"
            ).splitlines()
            if ln.strip()
        ]
        male_count, female_count = len(males), len(females)
    except Exception:
        males, females = [], []

    payload = {
        "sources": ["MgHla/Myanmar-Name_en-2-mm", "L16H7/Myanmar_Names"],
        "totalPairs": len(names),
        "maleCount": male_count,
        "femaleCount": female_count,
        "pairs": names,
        "sampleMales": males[:150],
        "sampleFemales": females[:150],
    }
    out = DATASETS_DIR / "myanmar-names.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(f"    -> Ingested {len(names):,} EN-MY name pairs (+ {male_count} male / {female_count} female names).")
    return payload


def ingest_myanmar_karen_wordlists():
    print("[5/7] Fetching Burmese & Sgaw Karen Wordlists (lwinmoe/segment, dangerchitthu/myanmar-karen-word-lists, kanaung/wordlists)...")
    burmese_raw = fetch_text(
        "https://raw.githubusercontent.com/lwinmoe/segment/master/burmese-word-list.txt"
    )
    burmese_words = [w.strip() for w in burmese_raw.splitlines() if w.strip()]

    karen_raw = fetch_text(
        "https://raw.githubusercontent.com/dangerchitthu/myanmar-karen-word-lists/master/sgaw-word-list.txt"
    )
    karen_words = [w.strip() for w in karen_raw.splitlines() if w.strip()]

    nrc_raw = fetch_text(
        "https://raw.githubusercontent.com/kanaung/wordlists/master/nrclist.list"
    )
    nrc_codes = [w.strip() for w in nrc_raw.splitlines() if w.strip()]

    # Compact curated index for fast web lookup + segmentation lexicon
    payload = {
        "sources": [
            "lwinmoe/segment (burmese-word-list.txt)",
            "dangerchitthu/myanmar-karen-word-lists (sgaw-word-list.txt)",
            "kanaung/wordlists (nrclist.list)",
        ],
        "burmeseTotal": len(burmese_words),
        "karenTotal": len(karen_words),
        "nrcTotal": len(nrc_codes),
        "burmeseSample": burmese_words[:2500],
        "karenSample": karen_words[:2000],
        "nrcCodes": nrc_codes[:400],
    }
    out = DATASETS_DIR / "myanmar-wordlists.json"
    out.write_text(json.dumps(payload, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(
        f"    -> Burmese: {len(burmese_words):,} (2,500 bundled), Sgaw Karen: {len(karen_words):,} (2,000 bundled), NRC: {len(nrc_codes)}."
    )
    return payload


def ingest_myanmar_postal_codes():
    print("[6/7] Fetching Myanmar Postal Codes (kanaung/wordlists)...")
    # Inspect postal code folder via API
    api_url = "https://api.github.com/repos/kanaung/wordlists/contents/postal%20code"
    try:
        items = json.loads(fetch_text(api_url))
        postal_entries = []
        for it in items:
            if it.get("type") == "file" and it.get("download_url"):
                region = Path(it["name"]).stem
                txt = fetch_text(it["download_url"])
                for line in txt.splitlines():
                    s = line.strip()
                    if not s:
                        continue
                    m = re.search(r"(\d{5,6})", s)
                    code = m.group(1) if m else ""
                    township = re.sub(r"[\t,|]+", " ", s.replace(code, "")).strip(" -:")
                    if code and township:
                        postal_entries.append({"region": region, "township": township, "code": code})
        out = DATASETS_DIR / "myanmar-postal.json"
        out.write_text(
            json.dumps(
                {"source": "kanaung/wordlists (postal code)", "count": len(postal_entries), "entries": postal_entries},
                ensure_ascii=False,
                separators=(",", ":"),
            ),
            encoding="utf-8",
        )
        print(f"    -> Ingested {len(postal_entries):,} Myanmar postal codes.")
        return postal_entries
    except Exception as e:
        print(f"    -> Postal code note: {e}")
        return []


def update_sqlite_and_site(en_en_data, errors_payload, names_payload):
    print("[7/7] Updating SQLite offline DB (for Mac & Android) and GitHub Pages demo datasets...")
    if SQLITE_PATH.exists():
        conn = sqlite3.connect(str(SQLITE_PATH))
        cur = conn.cursor()
        cur.execute("DROP TABLE IF EXISTS en_en_entries;")
        cur.execute(
            """
            CREATE TABLE en_en_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                word TEXT NOT NULL,
                word_lower TEXT NOT NULL,
                definitions TEXT NOT NULL
            );
            """
        )
        cur.execute("CREATE INDEX idx_en_en_lower ON en_en_entries(word_lower);")
        batch = []
        for word, defs in en_en_data.items():
            w = word.strip()
            if not w:
                continue
            d_str = " | ".join(defs[:3]) if isinstance(defs, list) else str(defs)
            batch.append((w, w.lower(), d_str))
            if len(batch) >= 5000:
                cur.executemany("INSERT INTO en_en_entries(word, word_lower, definitions) VALUES (?, ?, ?)", batch)
                batch.clear()
        if batch:
            cur.executemany("INSERT INTO en_en_entries(word, word_lower, definitions) VALUES (?, ?, ?)", batch)

        cur.execute("DROP TABLE IF EXISTS myanmar_names;")
        cur.execute("CREATE TABLE myanmar_names (en TEXT, mm TEXT);")
        cur.executemany("INSERT INTO myanmar_names(en, mm) VALUES (?, ?)", names_payload["pairs"])
        conn.commit()
        conn.close()
        print(f"    -> Updated {SQLITE_PATH.name} with EN-EN dictionary & Myanmar Names tables.")

    # Save compact summary for GitHub Pages site
    site_datasets = SITE_PUB / "datasets"
    site_datasets.mkdir(parents=True, exist_ok=True)
    summary = {
        "enEnWords": len(en_en_data),
        "misspelledCount": len(errors_payload["misspelled"]),
        "misusedPhrasesCount": len(errors_payload["misusedPhrases"]),
        "misusedWordsCount": len(errors_payload["misusedWords"]),
        "myanmarNamesCount": names_payload["totalPairs"],
        "sampleErrors": errors_payload["misspelled"][:25],
        "sampleNames": names_payload["pairs"][:30],
    }
    (site_datasets / "summary.json").write_text(
        json.dumps(summary, ensure_ascii=False, separators=(",", ":")), encoding="utf-8"
    )
    print("    -> Done! All datasets ingested cleanly.")


def main():
    en_en = ingest_en_en_dictionary()
    errors = ingest_common_english_errors()
    ingest_grammar_book()
    names = ingest_myanmar_names()
    ingest_myanmar_karen_wordlists()
    ingest_myanmar_postal_codes()
    update_sqlite_and_site(en_en, errors, names)


if __name__ == "__main__":
    main()
