#!/usr/bin/env python3
"""
HtetLingo Pipeline — mahavivo/english-wordlists Ingestion & Offline Mobile SQLite Builder
1. Downloads COCA_20000, Highschool, CET4, CET6, TOEFL, GRE wordlists from mahavivo/english-wordlists
2. Enriches ornagai.sqlite (unified_vocab) & apps/web/public/datasets/vocab-core.json (15,000 ranked words)
3. Builds compact self-contained apps/android/app/src/main/assets/htetlingo_mobile.sqlite
   for 100% offline Android app operation.
"""

import json
import re
import sqlite3
import urllib.request
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SQLITE_PATH = ROOT / "packages" / "data-pipeline" / "out" / "ornagai.sqlite"
WEB_DATASETS = ROOT / "apps" / "web" / "public" / "datasets"
MOBILE_DB = ROOT / "apps" / "android" / "app" / "src" / "main" / "assets" / "htetlingo_mobile.sqlite"

BASE_URL = "https://raw.githubusercontent.com/mahavivo/english-wordlists/master"
WORDLIST_FILES = {
    "coca": "COCA_20000.txt",
    "highschool": "Highschool_edited.txt",
    "cet4": "CET4_edited.txt",
    "cet6": "CET6_edited.txt",
    "toefl": "TOEFL_abridged.txt",
    "gre": "GRE_abridged.txt",
}

WORD_RE = re.compile(r"^[A-Za-z][A-Za-z'-]{1,28}$")
IPA_RE = re.compile(r"/([^/]{1,40})/")
BULLET_RE = re.compile(r"•.*$")


def fetch_lines(url: str):
    req = urllib.request.Request(url, headers={"User-Agent": "HtetLingo-DataPipeline/2.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        text = r.read().decode("utf-8-sig", errors="replace")
    words = []
    for line in text.splitlines():
        token = line.strip().split()[0] if line.strip() else ""
        token = token.strip(",.;:()[]\"'")
        if WORD_RE.match(token):
            words.append(token.lower())
    return words


def clean_my_summary(plain: str) -> str:
    s = re.sub(
        r"^(noun|verb|adjective|adverb|preposition|conjunction|pronoun|interjection|contraction)\s+",
        "",
        plain,
        flags=re.I,
    )
    s = re.sub(r"/[^/]+/\s*", "", s)
    s = re.sub(r"\[[^\]]+\]\s*", "", s)
    s = BULLET_RE.sub("", s).strip()
    return s[:135].strip(" ;,.")


def main():
    print("[1/3] Downloading mahavivo/english-wordlists (COCA 20k, Highschool, CET4/6, TOEFL, GRE)...")
    tag_map = defaultdict(set)
    coca_rank = {}

    for tag, fname in WORDLIST_FILES.items():
        try:
            words = fetch_lines(f"{BASE_URL}/{fname}")
            for idx, w in enumerate(words):
                tag_map[w].add(tag)
                if tag == "coca" and w not in coca_rank:
                    coca_rank[w] = idx + 1
            print(f"    -> {fname}: {len(words):,} words")
        except Exception as e:
            print(f"    -> WARN {fname}: {e}")

    print("[2/3] Rebuilding 15,000-word COCA & Exam-tagged unified_vocab...")
    conn = sqlite3.connect(str(SQLITE_PATH))
    cur = conn.cursor()

    rows = cur.execute(
        """
        SELECT
            o.word,
            o.word_lower,
            COALESCE(e.phonetic, ''),
            o.plain_text,
            COALESCE(e.translation, ''),
            COALESCE(en.definitions, ''),
            COALESCE(e.tag, '')
        FROM entries o
        LEFT JOIN ecdict_en_zh e ON e.word_lower = o.word_lower
        LEFT JOIN en_en_entries en ON en.word_lower = o.word_lower
        WHERE length(o.word) >= 2
          AND o.word NOT LIKE '% %'
        GROUP BY o.word_lower;
        """
    ).fetchall()

    enriched = []
    for word, wl, phonetic, my_plain, zh_trans, en_defs, e_tag in rows:
        extra_tags = tag_map.get(wl, set())
        rank = coca_rank.get(wl, 999999)
        if not extra_tags and not e_tag and rank == 999999:
            continue
        my_clean = clean_my_summary(my_plain or "")
        if not my_clean:
            continue

        all_tags = set(t.lower() for t in (e_tag or "").split() if t)
        all_tags.update(extra_tags)
        tag_str = " ".join(sorted(all_tags)[:4])

        if "highschool" in all_tags or rank <= 2000:
            lvl = "A2"
        elif "cet4" in all_tags or rank <= 5000:
            lvl = "B1"
        elif "cet6" in all_tags or "toefl" in all_tags or "ielts" in all_tags or rank <= 10000:
            lvl = "B2"
        else:
            lvl = "C1"

        ipa = phonetic
        if not ipa:
            m = IPA_RE.search(my_plain or "")
            if m:
                ipa = m.group(1)

        zh_short = (zh_trans or "").split("; ")[0][:90]
        en_short = (en_defs or "").split(" | ")[0][:120]
        enriched.append((rank, word, ipa, my_clean, zh_short, en_short, tag_str, lvl))

    enriched.sort(key=lambda x: (x[0], len(x[1])))
    top_vocab = enriched[:15000]

    # Update unified_vocab in ornagai.sqlite
    cur.execute("DROP TABLE IF EXISTS unified_vocab;")
    cur.execute(
        """
        CREATE TABLE unified_vocab (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            word TEXT NOT NULL,
            ipa TEXT,
            my_def TEXT NOT NULL,
            zh_def TEXT,
            en_def TEXT,
            tag TEXT,
            level TEXT
        );
        """
    )
    cur.executemany(
        "INSERT INTO unified_vocab(word, ipa, my_def, zh_def, en_def, tag, level) VALUES (?, ?, ?, ?, ?, ?, ?)",
        [(w, ipa, my_d, zh_d, en_d, tg, lv) for (_, w, ipa, my_d, zh_d, en_d, tg, lv) in top_vocab],
    )
    conn.commit()
    conn.close()

    # Export to web vocab-core.json
    web_items = [[w, ipa, my_d, zh_d, en_d, tg, lv] for (_, w, ipa, my_d, zh_d, en_d, tg, lv) in top_vocab]
    (WEB_DATASETS / "vocab-core.json").write_text(
        json.dumps(
            {
                "source": "Ornagai EN-MY + ECDICT EN-ZH + EN-EN + mahavivo/english-wordlists (COCA 20k/CET/TOEFL/GRE)",
                "count": len(web_items),
                "items": web_items,
            },
            ensure_ascii=False,
            separators=(",", ":"),
        ),
        encoding="utf-8",
    )
    print(f"    -> Updated unified_vocab & vocab-core.json with {len(web_items):,} COCA/exam-ranked words.")

    # [3/3] Build compact offline Android SQLite database in assets/
    print("[3/3] Building compact offline Android SQLite database (htetlingo_mobile.sqlite)...")
    MOBILE_DB.parent.mkdir(parents=True, exist_ok=True)
    if MOBILE_DB.exists():
        MOBILE_DB.unlink()

    mconn = sqlite3.connect(str(MOBILE_DB))
    mcur = mconn.cursor()
    mcur.execute("PRAGMA journal_mode = OFF;")
    mcur.execute("PRAGMA synchronous = OFF;")

    mcur.execute(
        """
        CREATE TABLE vocab (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            word TEXT NOT NULL,
            word_lower TEXT NOT NULL,
            ipa TEXT,
            my_def TEXT NOT NULL,
            zh_def TEXT,
            en_def TEXT,
            tag TEXT,
            level TEXT
        );
        """
    )
    mcur.execute("CREATE INDEX idx_vocab_lower ON vocab(word_lower);")
    mcur.executemany(
        "INSERT INTO vocab(word, word_lower, ipa, my_def, zh_def, en_def, tag, level) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        [(w, w.lower(), ipa, my_d, zh_d, en_d, tg, lv) for (_, w, ipa, my_d, zh_d, en_d, tg, lv) in top_vocab],
    )

    # Myanmar names
    names_file = WEB_DATASETS / "myanmar-names.json"
    if names_file.exists():
        ndata = json.loads(names_file.read_text(encoding="utf-8"))
        mcur.execute("CREATE TABLE myanmar_names (en TEXT, mm TEXT);")
        mcur.executemany("INSERT INTO myanmar_names(en, mm) VALUES (?, ?)", ndata.get("pairs", []))

    # Postal codes
    postal_file = WEB_DATASETS / "myanmar-postal.json"
    if postal_file.exists():
        pdata = json.loads(postal_file.read_text(encoding="utf-8"))
        mcur.execute("CREATE TABLE postal_codes (region TEXT, township TEXT, code TEXT);")
        mcur.executemany(
            "INSERT INTO postal_codes(region, township, code) VALUES (?, ?, ?)",
            [(e["region"], e["township"], e["code"]) for e in pdata.get("entries", [])],
        )

    # HSK Chinese grammar
    hsk_file = WEB_DATASETS / "chinese-grammar-hsk.json"
    if hsk_file.exists():
        hdata = json.loads(hsk_file.read_text(encoding="utf-8"))
        mcur.execute("CREATE TABLE hsk_grammar (level TEXT, title TEXT, pattern TEXT, example_zh TEXT, example_py TEXT, example_en TEXT);")
        hrows = []
        for l in hdata.get("lessons", []):
            ex = l["examples"][0] if l.get("examples") else {"zh": "", "pinyin": "", "en": ""}
            hrows.append((l["level"], l["title"], l.get("pattern", ""), ex["zh"], ex["pinyin"], ex["en"]))
        mcur.executemany("INSERT INTO hsk_grammar VALUES (?, ?, ?, ?, ?, ?)", hrows)

    # Common English errors
    err_file = WEB_DATASETS / "english-errors.json"
    if err_file.exists():
        edata = json.loads(err_file.read_text(encoding="utf-8"))
        mcur.execute("CREATE TABLE english_errors (wrong TEXT, right TEXT, kind TEXT);")
        erows = [(e["word"], e["note"], "word") for e in edata.get("misusedWords", [])] + [
            (e["wrong"], e["right"], "phrase") for e in edata.get("misusedPhrases", [])
        ]
        mcur.executemany("INSERT INTO english_errors VALUES (?, ?, ?)", erows)

    mconn.commit()
    mcur.execute("VACUUM;")
    mconn.close()
    print(f"    -> Created {MOBILE_DB.name} ({MOBILE_DB.stat().st_size / (1024*1024):.2f} MB) — 100% offline ready!")


if __name__ == "__main__":
    main()
