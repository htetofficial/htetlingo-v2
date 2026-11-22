#!/usr/bin/env python3
"""
HtetLingo Unified Vocabulary & Quiz Generator
Automatically joins Ornagai (EN-MY 111k) + ECDICT (EN-ZH 27.8k) + EN-EN (49.5k)
inside ornagai.sqlite to produce a 12,000+ word multilingual vocabulary & quiz database
with zero artificial small-deck limits.
"""

import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SQLITE_PATH = ROOT / "packages" / "data-pipeline" / "out" / "ornagai.sqlite"
OUT_JSON = ROOT / "apps" / "web" / "public" / "datasets" / "vocab-core.json"

IPA_RE = re.compile(r"/([^/]{1,40})/")
BULLET_RE = re.compile(r"•.*$")


def clean_my_summary(plain: str) -> str:
    # Remove leading POS & IPA & bracketed phonetics to get crisp Burmese meaning
    s = re.sub(r"^(noun|verb|adjective|adverb|preposition|conjunction|pronoun|interjection|contraction)\s+", "", plain, flags=re.I)
    s = re.sub(r"/[^/]+/\s*", "", s)
    s = re.sub(r"\[[^\]]+\]\s*", "", s)
    s = BULLET_RE.sub("", s).strip()
    return s[:140].strip(" ;,.")


def infer_level(tag: str) -> str:
    t = tag.lower()
    if "zk" in t or "gk" in t or "ox3000" in t:
        return "A2"
    if "cet4" in t:
        return "B1"
    if "cet6" in t or "ielts" in t or "toefl" in t:
        return "B2"
    if "gre" in t:
        return "C1"
    return "B1"


def main():
    if not SQLITE_PATH.exists():
        raise FileNotFoundError(f"Missing {SQLITE_PATH}")

    conn = sqlite3.connect(str(SQLITE_PATH))
    cur = conn.cursor()

    # Join ECDICT core words with Ornagai EN-MY and EN-EN
    rows = cur.execute(
        """
        SELECT
            e.word,
            e.phonetic,
            o.plain_text,
            e.translation,
            en.definitions,
            e.tag
        FROM ecdict_en_zh e
        INNER JOIN entries o ON o.word_lower = e.word_lower
        LEFT JOIN en_en_entries en ON en.word_lower = e.word_lower
        WHERE length(e.word) >= 2
          AND e.word NOT LIKE '% %'
          AND e.word NOT LIKE '%-%'
        GROUP BY e.word_lower
        ORDER BY
          CASE
            WHEN e.tag LIKE '%ielts%' OR e.tag LIKE '%toefl%' THEN 1
            WHEN e.tag LIKE '%cet4%' THEN 2
            WHEN e.tag LIKE '%cet6%' THEN 3
            WHEN e.tag != '' THEN 4
            ELSE 5
          END,
          length(e.word) ASC
        LIMIT 12000;
        """
    ).fetchall()

    cur.execute("DROP TABLE IF EXISTS unified_vocab;")
    cur.execute(
        """
        CREATE TABLE unified_vocab (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            word TEXT NOT NULL,
            ipa TEXT,
            my_def TEXT NOT NULL,
            zh_def TEXT NOT NULL,
            en_def TEXT,
            tag TEXT,
            level TEXT
        );
        """
    )

    items = []
    db_rows = []
    for word, phonetic, my_plain, zh_trans, en_defs, tag in rows:
        my_clean = clean_my_summary(my_plain or "")
        if not my_clean:
            continue
        ipa = phonetic or ""
        if not ipa:
            m = IPA_RE.search(my_plain or "")
            if m:
                ipa = m.group(1)
        zh_short = (zh_trans or "").split("; ")[0][:90]
        en_short = ((en_defs or "").split(" | ")[0])[:120]
        lvl = infer_level(tag or "")
        tag_short = " ".join((tag or "").split()[:3])

        # Compact tuple: [word, ipa, myDef, zhDef, enDef, tag, level]
        items.append([word, ipa, my_clean, zh_short, en_short, tag_short, lvl])
        db_rows.append((word, ipa, my_clean, zh_short, en_short, tag_short, lvl))

    cur.executemany(
        "INSERT INTO unified_vocab(word, ipa, my_def, zh_def, en_def, tag, level) VALUES (?, ?, ?, ?, ?, ?, ?)",
        db_rows,
    )
    conn.commit()
    conn.close()

    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps(
        {
            "source": "Auto-generated join: Ornagai EN-MY + ECDICT EN-ZH + EN-EN",
            "count": len(items),
            "items": items,
        },
        ensure_ascii=False,
        separators=(",", ":"),
    )
    OUT_JSON.write_text(payload, encoding="utf-8")
    print(f"Generated unified vocabulary database: {len(items):,} cross-linked words ({len(payload.encode('utf-8'))/(1024*1024):.2f} MB).")


if __name__ == "__main__":
    main()
