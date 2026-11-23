#!/usr/bin/env python3
"""
HtetLingo — Author's Original Bilingual Book Parser (© 2026 ထက်)
Extracts & structures the complete A1–C2 Bilingual Self-Study Book into:
  - apps/web/public/datasets/htet-book.json
  - SQLite tables in ornagai.sqlite (Mac Native) & htetlingo_mobile.sqlite (Android Offline):
      * book_grammar_lessons (49 A1–C2 lessons)
      * book_vocab_units (24 themed units with words, collocations, examples)
      * book_readings (8 A1–C2 Reading Lab passages + Q&A)
      * book_writings (9 A1–C2 Writing Workshops + Model Answers)
      * book_irregular_verbs (V1/V2/V3/Burmese)
      * book_review_tests (60 Foundation / Intermediate / Advanced MCQ questions + also added to grammar_exercises!)
      * book_study_plan (16-week integrated curriculum)
"""

from __future__ import annotations
import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
UPLOADED_BOOK = Path(
    "/Users/htet/.gemini/antigravity/brain/12984283-454b-4d8f-a95a-dc0090f62cac/.user_uploaded/media_1795423416490_daf135ea.txt"
)
RAW_BOOK = ROOT / "packages" / "data-pipeline" / "raw" / "htet_book.txt"
WEB_OUT = ROOT / "apps" / "web" / "public" / "datasets" / "htet-book.json"
ORNAGAI_DB = ROOT / "packages" / "data-pipeline" / "out" / "ornagai.sqlite"
MOBILE_DB = ROOT / "apps" / "android" / "app" / "src" / "main" / "assets" / "htetlingo_mobile.sqlite"


def extract_book_text() -> str:
    src = UPLOADED_BOOK if UPLOADED_BOOK.exists() else RAW_BOOK
    book_text = src.read_text(encoding="utf-8-sig", errors="replace")
    book_text = book_text.replace("\r\n", "\n").replace("\r", "\n").replace("\x00", "o")
    RAW_BOOK.parent.mkdir(parents=True, exist_ok=True)
    RAW_BOOK.write_text(book_text, encoding="utf-8")
    return book_text


def slice_between(text: str, start_marker: str, end_marker: str | None = None) -> str:
    # Find the LAST occurrence of start_marker if it also appears in TOC
    idx = text.rfind(start_marker)
    if idx == -1:
        return ""
    sub = text[idx:]
    if end_marker:
        eidx = sub.find(end_marker)
        if eidx != -1:
            return sub[:eidx]
    return sub


def parse_grammar_lessons(part1: str):
    # Split on "သင်ခန်းစာ NN |"
    chunks = re.split(r"\n(?=သင်ခန်းစာ \d{2} \| )", "\n" + part1)
    lessons = []
    for ch in chunks:
        ch = ch.strip()
        m = re.match(r"သင်ခန်းစာ (\d{2}) \| ([^\n]+)\nLEVEL ([^\n]+)\n(.*)", ch, flags=re.S)
        if not m:
            continue
        num = int(m.group(1))
        title = m.group(2).strip()
        level = m.group(3).strip()
        body = m.group(4).strip()

        def grab(sec_start, sec_ends):
            si = body.find(sec_start)
            if si == -1:
                return ""
            s = body[si + len(sec_start):]
            end_pos = len(s)
            for e in sec_ends:
                ei = s.find(e)
                if ei != -1 and ei < end_pos:
                    end_pos = ei
            return s[:end_pos].strip()

        core = grab("Core Explanation | Grammar & Meaning", ["Grammar Patterns"])
        patterns = grab("Grammar Patterns", ["Examples with Myanmar Translation"])
        examples = grab("Examples with Myanmar Translation", ["Vocabulary in This Lesson"])
        practice = grab("Practice | Try Before Checking Answers", ["Answer Check"])
        answers = grab("Answer Check", [])

        lessons.append({
            "number": num,
            "title": title,
            "level": level,
            "explanation": core,
            "patterns": patterns,
            "examples": examples,
            "practice": practice,
            "answers": answers,
        })
    return lessons


def parse_vocab_units(part2: str):
    chunks = re.split(r"\n(?=Vocabulary Unit \d{2} \| )", "\n" + part2)
    units = []
    for ch in chunks:
        ch = ch.strip()
        m = re.match(r"Vocabulary Unit (\d{2}) \| ([^\n]+)\nLEVEL ([^\n]+)\n(.*)", ch, flags=re.S)
        if not m:
            continue
        num = int(m.group(1))
        title = m.group(2).strip()
        level = m.group(3).strip()
        body = m.group(4).strip()

        # Words are alternating lines between "မြန်မာအဓိပ္ပာယ်" and "Learn as Chunks and Collocations"
        w_start = body.find("မြန်မာအဓိပ္ပာယ်")
        w_end = body.find("Learn as Chunks and Collocations")
        words = []
        if w_start != -1 and w_end != -1:
            lines = [ln.strip() for ln in body[w_start + len("မြန်မာအဓိပ္ပာယ်"):w_end].splitlines() if ln.strip()]
            for i in range(0, len(lines) - 1, 2):
                words.append([lines[i], lines[i + 1]])

        c_start = body.find("Learn as Chunks and Collocations")
        e_start = body.find("Example Sentences")
        a_start = body.find("Active Vocabulary Challenge")
        collocations = body[c_start + len("Learn as Chunks and Collocations"):e_start].strip() if c_start != -1 and e_start != -1 else ""
        examples = body[e_start + len("Example Sentences"):a_start].strip() if e_start != -1 and a_start != -1 else ""

        units.append({
            "number": num,
            "title": title,
            "level": level,
            "words": words,
            "collocations": collocations,
            "examples": examples,
        })
    return units


def parse_readings(part3: str):
    chunks = re.split(r"\n(?=Reading \d{2} \| )", "\n" + part3)
    readings = []
    for ch in chunks:
        ch = ch.strip()
        m = re.match(r"Reading (\d{2}) \| ([^\n]+)\nLEVEL ([^\n]+)\n(.*)", ch, flags=re.S)
        if not m:
            continue
        num = int(m.group(1))
        title = m.group(2).strip()
        level = m.group(3).strip()
        body = m.group(4).strip()

        def grab(s, ends):
            si = body.find(s)
            if si == -1:
                return ""
            sub = body[si + len(s):]
            ep = len(sub)
            for e in ends:
                ei = sub.find(e)
                if ei != -1 and ei < ep:
                    ep = ei
            return sub[:ep].strip()

        readings.append({
            "number": num,
            "title": title,
            "level": level,
            "passage": grab("Read the passage", ["Grammar Notes in Burmese"]),
            "grammarNotes": grab("Grammar Notes in Burmese", ["Comprehension Questions"]),
            "questions": grab("Comprehension Questions", ["Suggested Answers"]),
            "answers": grab("Suggested Answers", ["Language Focus"]),
            "languageFocus": grab("Language Focus", ["Writing from Reading"]),
        })
    return readings


def parse_writings(part4: str):
    chunks = re.split(r"\n(?=Writing \d{2} \| )", "\n" + part4)
    writings = []
    for ch in chunks:
        ch = ch.strip()
        m = re.match(r"Writing (\d{2}) \| ([^\n]+)\nLEVEL ([^\n]+)\n(.*)", ch, flags=re.S)
        if not m:
            continue
        num = int(m.group(1))
        title = m.group(2).strip()
        level = m.group(3).strip()
        body = m.group(4).strip()

        def grab(s, ends):
            si = body.find(s)
            if si == -1:
                return ""
            sub = body[si + len(s):]
            ep = len(sub)
            for e in ends:
                ei = sub.find(e)
                if ei != -1 and ei < ep:
                    ep = ei
            return sub[:ep].strip()

        writings.append({
            "number": num,
            "title": title,
            "level": level,
            "task": grab("Your Task", ["Model Answer"]),
            "modelAnswer": grab("Model Answer", ["Why It Works | Grammar Explanation"]),
            "whyItWorks": grab("Why It Works | Grammar Explanation", ["Checklist"]),
            "checklist": grab("Checklist", ["Rewrite Activity"]),
        })
    return writings


def parse_irregular_verbs(part5: str):
    s = part5.find("Appendix A | Irregular Verbs: V1 / V2 / V3")
    e = part5.find("Appendix B | Prefixes and Suffixes")
    if s == -1 or e == -1:
        return []
    sec = part5[s:e]
    hdr = sec.find("မြန်မာ\n")
    if hdr == -1:
        return []
    lines = [ln.strip() for ln in sec[hdr + len("မြန်မာ\n"):].splitlines() if ln.strip()]
    verbs = []
    for i in range(0, len(lines) - 3, 4):
        verbs.append([lines[i], lines[i + 1], lines[i + 2], lines[i + 3]])
    return verbs


def parse_review_tests(part5: str):
    s = part5.find("Appendix E | Three Grammar Review Tests")
    e = part5.find("Appendix F | 16-Week Integrated Study Plan")
    if s == -1 or e == -1:
        return []
    sec = part5[s:e]
    tests = []
    tiers = [
        ("Foundation Review (A1–A2)", "Intermediate Review (B1–B2)"),
        ("Intermediate Review (B1–B2)", "Advanced Review (C1–C2)"),
        ("Advanced Review (C1–C2)", None),
    ]
    for title, next_title in tiers:
        ti = sec.find(title)
        if ti == -1:
            continue
        sub = sec[ti + len(title):]
        if next_title:
            ni = sub.find(next_title)
            if ni != -1:
                sub = sub[:ni]
        ak = sub.find("Answer Key")
        if ak == -1:
            continue
        q_block = sub[:ak].strip()
        a_block = sub[ak + len("Answer Key"):].strip()

        q_map = {}
        for ln in q_block.splitlines():
            m = re.match(r"(\d+)\.\s+(.*)", ln.strip())
            if m:
                q_map[int(m.group(1))] = m.group(2).strip()
        a_map = {}
        for ln in a_block.splitlines():
            m = re.match(r"(\d+)\.\s+(.*)", ln.strip())
            if m:
                a_map[int(m.group(1))] = m.group(2).strip()

        for num, q_text in sorted(q_map.items()):
            ans = a_map.get(num, "")
            opt_m = re.search(r"\(([^)]+/[^)]+)\)", q_text)
            opts = [o.strip() for o in opt_m.group(1).split("/")] if opt_m else [ans]
            if ans and ans not in opts:
                opts.insert(0, ans)
            tests.append({
                "tier": title,
                "number": num,
                "question": q_text,
                "options": opts,
                "answer": ans,
            })
    return tests


def parse_study_plan(part5: str):
    s = part5.find("Appendix F | 16-Week Integrated Study Plan")
    e = part5.find("နေ့စဉ် ၆၀ မိနစ် Study Routine")
    if s == -1 or e == -1:
        return []
    sec = part5[s:e]
    hdr = sec.find("Integrated skills\n")
    if hdr == -1:
        return []
    lines = [ln.strip() for ln in sec[hdr + len("Integrated skills\n"):].splitlines() if ln.strip()]
    weeks = []
    for i in range(0, len(lines) - 3, 4):
        weeks.append({
            "week": lines[i],
            "grammar": lines[i + 1],
            "vocabulary": lines[i + 2],
            "skills": lines[i + 3],
        })
    return weeks


def write_to_sqlite(data: dict):
    for db_path, is_mobile in [(ORNAGAI_DB, False), (MOBILE_DB, True)]:
        if not db_path.exists():
            continue
        conn = sqlite3.connect(str(db_path))
        cur = conn.cursor()
        cur.execute("PRAGMA journal_mode = OFF;")
        cur.execute("PRAGMA synchronous = OFF;")

        # 1. book_grammar_lessons (49)
        cur.execute("DROP TABLE IF EXISTS book_grammar_lessons;")
        cur.execute(
            """
            CREATE TABLE book_grammar_lessons (
                number INTEGER PRIMARY KEY,
                title TEXT,
                level TEXT,
                explanation TEXT,
                patterns TEXT,
                examples TEXT,
                practice TEXT,
                answers TEXT
            );
            """
        )
        cur.executemany(
            "INSERT INTO book_grammar_lessons VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            [
                (l["number"], l["title"], l["level"], l["explanation"], l["patterns"], l["examples"], l["practice"], l["answers"])
                for l in data["grammarLessons"]
            ],
        )

        # 2. book_vocab_units (24)
        cur.execute("DROP TABLE IF EXISTS book_vocab_units;")
        cur.execute(
            """
            CREATE TABLE book_vocab_units (
                number INTEGER PRIMARY KEY,
                title TEXT,
                level TEXT,
                words_json TEXT,
                collocations TEXT,
                examples TEXT
            );
            """
        )
        cur.executemany(
            "INSERT INTO book_vocab_units VALUES (?, ?, ?, ?, ?, ?)",
            [
                (u["number"], u["title"], u["level"], json.dumps(u["words"], ensure_ascii=False), u["collocations"], u["examples"])
                for u in data["vocabUnits"]
            ],
        )

        # 3. book_readings (8)
        cur.execute("DROP TABLE IF EXISTS book_readings;")
        cur.execute(
            """
            CREATE TABLE book_readings (
                number INTEGER PRIMARY KEY,
                title TEXT,
                level TEXT,
                passage TEXT,
                grammar_notes TEXT,
                questions TEXT,
                answers TEXT,
                language_focus TEXT
            );
            """
        )
        cur.executemany(
            "INSERT INTO book_readings VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            [
                (r["number"], r["title"], r["level"], r["passage"], r["grammarNotes"], r["questions"], r["answers"], r["languageFocus"])
                for r in data["readings"]
            ],
        )

        # 4. book_writings (9)
        cur.execute("DROP TABLE IF EXISTS book_writings;")
        cur.execute(
            """
            CREATE TABLE book_writings (
                number INTEGER PRIMARY KEY,
                title TEXT,
                level TEXT,
                task TEXT,
                model_answer TEXT,
                why_it_works TEXT,
                checklist TEXT
            );
            """
        )
        cur.executemany(
            "INSERT INTO book_writings VALUES (?, ?, ?, ?, ?, ?, ?)",
            [
                (w["number"], w["title"], w["level"], w["task"], w["modelAnswer"], w["whyItWorks"], w["checklist"])
                for w in data["writings"]
            ],
        )

        # 5. book_irregular_verbs
        cur.execute("DROP TABLE IF EXISTS book_irregular_verbs;")
        cur.execute("CREATE TABLE book_irregular_verbs (v1 TEXT, v2 TEXT, v3 TEXT, my TEXT);")
        cur.executemany("INSERT INTO book_irregular_verbs VALUES (?, ?, ?, ?)", data["irregularVerbs"])

        # 6. book_study_plan (16 weeks)
        cur.execute("DROP TABLE IF EXISTS book_study_plan;")
        cur.execute("CREATE TABLE book_study_plan (week TEXT, grammar TEXT, vocabulary TEXT, skills TEXT);")
        cur.executemany(
            "INSERT INTO book_study_plan VALUES (?, ?, ?, ?)",
            [(w["week"], w["grammar"], w["vocabulary"], w["skills"]) for w in data["studyPlan"]],
        )

        # 7. Append the 60 official A1-C2 Review Test questions into grammar_exercises!
        cur.execute("DELETE FROM grammar_exercises WHERE topic LIKE 'Htet Book:%';")
        cur.executemany(
            "INSERT INTO grammar_exercises(topic, question, options_Pipe, answer, explanation) VALUES (?, ?, ?, ?, ?)",
            [
                (
                    f"Htet Book: {t['tier']}",
                    t["question"],
                    "|".join(t["options"]),
                    t["answer"],
                    f"From © 2026 ထက် Self-Study Book ({t['tier']}) — Answer: {t['answer']}",
                )
                for t in data["reviewTests"]
            ],
        )

        conn.commit()
        if is_mobile:
            cur.execute("VACUUM;")
        conn.close()
        print(f"    -> Updated {db_path.name} ({db_path.stat().st_size / (1024*1024):.2f} MB)")


def main():
    text = extract_book_text()
    part1 = slice_between(text, "အပိုင်း (၁) | ENGLISH GRAMMAR\nအခြေခံ", "အပိုင်း (၂) | VOCABULARY IN CONTEXT\nTopic")
    part2 = slice_between(text, "အပိုင်း (၂) | VOCABULARY IN CONTEXT\nTopic", "အပိုင်း (၃) | READING LAB\nဖတ်သောအခါ")
    part3 = slice_between(text, "အပိုင်း (၃) | READING LAB\nဖတ်သောအခါ", "အပိုင်း (၄) | WRITING WORKSHOP\nWriting ကို")
    part4 = slice_between(text, "အပိုင်း (၄) | WRITING WORKSHOP\nWriting ကို", "အပိုင်း (၅) | APPENDICES & REVIEW\nရည်ညွှန်း")
    part5 = slice_between(text, "အပိုင်း (၅) | APPENDICES & REVIEW\nရည်ညွှန်း", None)

    data = {
        "title": "English Grammar & Vocabulary — A Complete Bilingual Self-Study Book (A1–C2)",
        "author": "ထက် (© 2026)",
        "grammarLessons": parse_grammar_lessons(part1),
        "vocabUnits": parse_vocab_units(part2),
        "readings": parse_readings(part3),
        "writings": parse_writings(part4),
        "irregularVerbs": parse_irregular_verbs(part5),
        "reviewTests": parse_review_tests(part5),
        "studyPlan": parse_study_plan(part5),
    }

    WEB_OUT.parent.mkdir(parents=True, exist_ok=True)
    WEB_OUT.write_text(json.dumps(data, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    total_vocab = sum(len(u["words"]) for u in data["vocabUnits"])
    print(
        f"Parsed Author's Book (© 2026 ထက်):\n"
        f"  - Grammar Lessons : {len(data['grammarLessons'])} (A1–C2)\n"
        f"  - Vocab Units     : {len(data['vocabUnits'])} ({total_vocab} words + collocations)\n"
        f"  - Reading Lab     : {len(data['readings'])} passages (A1–C2)\n"
        f"  - Writing Workshop: {len(data['writings'])} models (A1–C2)\n"
        f"  - Irregular Verbs : {len(data['irregularVerbs'])} verbs\n"
        f"  - Review Tests    : {len(data['reviewTests'])} questions (Foundation/Intermediate/Advanced)\n"
        f"  - Study Plan      : {len(data['studyPlan'])} weeks"
    )
    write_to_sqlite(data)


if __name__ == "__main__":
    main()
