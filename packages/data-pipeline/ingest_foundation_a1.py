#!/usr/bin/env python3
"""
HtetLingo — Ingest Foundation English (A1) by ထက်
Parses the author's complete Foundation English (A1) textbook, workbooks, foreword,
and dedication into SQLite databases (ornagai.sqlite & htetlingo_mobile.sqlite)
and Web JSON dataset (foundation-a1.json).
"""

from __future__ import annotations
import json
import re
import shutil
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SOURCE_TXT = Path(
    "/Users/htet/.gemini/antigravity/brain/12984283-454b-4d8f-a95a-dc0090f62cac/.user_uploaded/media_1795429606287_2953bb0e.txt"
)
RAW_COPY = ROOT / "packages" / "data-pipeline" / "raw" / "Foundation_English_A1.txt"
WEB_OUT = ROOT / "apps" / "web" / "public" / "datasets" / "foundation-a1.json"
ORNAGAI_DB = ROOT / "packages" / "data-pipeline" / "out" / "ornagai.sqlite"
MOBILE_DB = ROOT / "apps" / "android" / "app" / "src" / "main" / "assets" / "htetlingo_mobile.sqlite"


def main():
    RAW_COPY.parent.mkdir(parents=True, exist_ok=True)
    if SOURCE_TXT.exists():
        shutil.copyfile(SOURCE_TXT, RAW_COPY)
        print(f"Copied uploaded text to {RAW_COPY}")

    with open(RAW_COPY, "r", encoding="utf-8") as f:
        text = f.read()

    lines = text.splitlines()

    # Define sections
    sections_meta = [
        ("foundation-a1-dedication", "မူရင်းပညာရှင်များအား ဂုဏ်ပြုဦးညွှတ်ခြင်းနှင့် စာရေးသူ၏ ခံယူချက်", "မူရင်းပညာရှင်များအား ဂုဏ်ပြုဦးညွှတ်ခြင်းနှင့် စာရေးသူ၏ ခံယူချက်"),
        ("foundation-a1-foreword", "စာရေးသူ၏ အမှာစာ (Author's Foreword)", "စာရေးသူ၏ အမှာစာ (Author's Foreword) — ထက်"),
        ("foundation-a1-ch01", "အခန်း 01 • The English Alphabet — အင်္ဂလိပ်အက္ခရာနှင့် စာလုံးပေါင်း", "အခန်း 01 • The English Alphabet — အင်္ဂလိပ်အက္ခရာနှင့် စာလုံးပေါင်း"),
        ("foundation-a1-ch02", "အခန်း 02 • Sounds and Word Stress — အသံထွက်၊ သရသံနှင့် အလေးပေးသံ", "အခန်း 02 • Sounds and Word Stress — အသံထွက်၊ သရသံနှင့် အလေးပေးသံ"),
        ("foundation-a1-ch03", "အခန်း 03 • Parts of Speech — စကားလုံးအမျိုးအစားများ", "အခန်း 03 • Parts of Speech — စကားလုံးအမျိုးအစားများ"),
        ("foundation-a1-ch04", "အခန်း 04 • Nouns: Types and Plurals — နာမ်၊ အနည်းကိန်း၊ အများကိန်း", "အခန်း 04 • Nouns: Types and Plurals — နာမ်၊ အနည်းကိန်း၊ အများကိန်း"),
        ("foundation-a1-ch06", "အခန်း 06 • Articles and Determiners — a, an, the နှင့် ညွှန်ပြစကားလုံးများ", "အခန်း 06 • Articles and Determiners — a, an, the နှင့် ညွှန်ပြစကားလုံးများ"),
        ("foundation-a1-ch07", "အခန်း 07 • Be Verbs — am, is, are နှင့် အခြေခံဖွဲ့စည်းပုံ", "အခန်း 07 • Be Verbs — am, is, are နှင့် အခြေခံဖွဲ့စည်းပုံ"),
        ("foundation-a1-ch08", "အခန်း 08 • Basic English Sentences — အခြေခံဝါကျတည်ဆောက်ပုံ", "အခန်း 08 • Basic English Sentences — အခြေခံဝါကျတည်ဆောက်ပုံ"),
        ("foundation-a1-ch09", "အခန်း 09 • Present Simple: Statements — ပုံမှန်လုပ်ရပ်နှင့် အမှန်တရား", "အခန်း 09 • Present Simple: Statements — ပုံမှန်လုပ်ရပ်နှင့် အမှန်တရား"),
        ("foundation-a1-ch10", "အခန်း 10 • Present Simple: Negatives and Questions — do/does ဖြင့် အငြင်းနှင့် မေးခွန်း", "အခန်း 10 • Present Simple: Negatives and Questions — do/does ဖြင့် အငြင်းနှင့် မေးခွန်း"),
        ("foundation-a1-ch11", "အခန်း 11 • Everyday Vocabulary in Context — နေ့စဉ်အသုံးအနှုန်းနှင့် Vocabulary", "အခန်း 11 • Everyday Vocabulary in Context — နေ့စဉ်အသုံးအနှုန်းနှင့် Vocabulary"),
        ("foundation-a1-ch12", "အခန်း 12 • Everyday Reading and Writing — အခြေခံစာဖတ်ခြင်းနှင့် စာရေးခြင်း", "အခန်း 12 • Everyday Reading and Writing — အခြေခံစာဖတ်ခြင်းနှင့် စာရေးခြင်း"),
        ("foundation-a1-bonus", "PART I ၏ အချပ်ပို | Imperatives and Suggestions — အမိန့်ပေး၊ တောင်းဆို၊ အကြံပြုဝါကျများ", "PART I ၏ အချပ်ပို | Imperatives and Suggestions — အမိန့်ပေး၊ တောင်းဆို၊ အကြံပြုဝါကျများ"),
    ]

    indices = []
    for sec_id, pattern, full_title in sections_meta:
        if sec_id == "foundation-a1-dedication":
            indices.append((0, sec_id, full_title))
        else:
            found_idx = -1
            for i in range(180, len(lines)):
                if pattern in lines[i]:
                    found_idx = i
                    break
            indices.append((found_idx, sec_id, full_title))

    chapters_data = []
    for i in range(len(indices)):
        start_line, sec_id, full_title = indices[i]
        end_line = indices[i + 1][0] if i + 1 < len(indices) else len(lines)
        chunk = "\n".join(lines[start_line:end_line]).strip()
        chapters_data.append({
            "id": sec_id,
            "title": full_title,
            "source": "Foundation English (A1) by ထက်",
            "content": chunk,
            "start_line": start_line + 1,
            "end_line": end_line,
            "char_count": len(chunk),
        })

    print(f"Extracted {len(chapters_data)} Foundation A1 chapters/sections:")
    for ch in chapters_data:
        print(f"  [{ch['id']}] {ch['title'][:50]} ({ch['char_count']} chars)")

    # Write Web JSON
    WEB_OUT.parent.mkdir(parents=True, exist_ok=True)
    with open(WEB_OUT, "w", encoding="utf-8") as f:
        json.dump(chapters_data, f, ensure_ascii=False, indent=2)
    print(f"Saved JSON dataset to {WEB_OUT}")

    # Insert into SQLite databases
    for db_path in [ORNAGAI_DB, MOBILE_DB]:
        if not db_path.exists():
            print(f"Warning: {db_path} does not exist, skipping.")
            continue
        print(f"\nUpdating {db_path}...")
        conn = sqlite3.connect(db_path)
        cur = conn.cursor()

        cur.execute("""
            CREATE TABLE IF NOT EXISTS grammar_chapters (
                id TEXT,
                title TEXT,
                source TEXT,
                content TEXT
            );
        """)

        # Remove previous foundation chapters if any to avoid duplication
        cur.execute("DELETE FROM grammar_chapters WHERE id LIKE 'foundation-a1-%';")

        # Insert new Foundation chapters
        for ch in chapters_data:
            cur.execute(
                "INSERT INTO grammar_chapters (id, title, source, content) VALUES (?, ?, ?, ?);",
                (ch["id"], ch["title"], ch["source"], ch["content"]),
            )

        # Also extract Reading passages from Chapter 12 into book_readings
        # Chapter 12 has Reading Practice 1: Meet Thiri & Reading Practice 2: A Busy Day for Nwe Nwe
        cur.execute("CREATE TABLE IF NOT EXISTS book_readings (number INTEGER, title TEXT, level TEXT, passage TEXT, grammar_notes TEXT, questions TEXT, answers TEXT, language_focus TEXT);")
        cur.execute("DELETE FROM book_readings WHERE number IN (101, 102);")

        passage_1 = """Meet Thiri (သီရိနှင့် မိတ်ဆက်ခြင်း)
Hello! My name is Thiri. I am 22 years old and I live in Yangon, Myanmar. I am a university student. I study English and Computer Science. I love reading books and listening to music.

Every morning, I wake up at 6:00 AM. I brush my teeth, wash my face, and take a shower. Then, I have breakfast with my family. I usually eat fried rice and drink a cup of hot tea. My mother is a teacher and my father is an engineer. I have one younger brother. His name is Aung Aung. He is 16 years old.

At 8:00 AM, I go to university by bus. In the afternoon, I study in the library with my friends. I return home at 5:00 PM. In the evening, I help my mother cook dinner. We eat dinner together at 7:00 PM. After dinner, I do my homework and study English for one hour. I usually go to bed at 10:30 PM. I am happy with my life."""
        
        cur.execute(
            "INSERT INTO book_readings VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
            (
                101,
                "Meet Thiri — နေ့စဉ်ဘဝနှင့် မိသားစု မိတ်ဆက်",
                "A1",
                passage_1,
                "Present Simple Statements, Daily Routine Collocations, Family & Occupation Nouns",
                "1. How old is Thiri? | 2. Where does she live? | 3. What does she study? | 4. How does she go to university? | 5. What time does she go to bed?",
                "1. She is 22 years old. | 2. She lives in Yangon, Myanmar. | 3. She studies English and Computer Science. | 4. She goes to university by bus. | 5. She goes to bed at 10:30 PM.",
                "Foundation English (A1) Chapter 12 by ထက်"
            )
        )

        passage_2 = """A Busy Day for Nwe Nwe (နွယ်နွယ်၏ အလုပ်များသောနေ့)
Nwe Nwe is an accountant at a private company in Mandalay. She works five days a week, from Monday to Friday. Her office is in downtown Mandalay, so she rides an electric motorbike to work every day.

Today is Wednesday and Nwe Nwe is very busy. She arrives at her office at 8:30 AM. First, she checks her emails and answers urgent messages from clients. At 10:00 AM, she has an important meeting with her manager. They discuss the company's monthly financial report.

At 12:30 PM, Nwe Nwe has lunch with her colleagues at a nearby noodle shop. She likes Shan noodles very much. After lunch, she continues her work. She prepares invoices and calculates staff salaries. She finishes work at 5:30 PM.

In the evening, Nwe Nwe does not go home immediately. She goes to a gym near her apartment and exercises for 45 minutes. Then, she buys fresh vegetables and fruits at the evening market. She cooks dinner and eats at home. Before going to sleep, she reads an English novel for 20 minutes because she wants to improve her vocabulary. She goes to bed early at 10:00 PM because she wants to be energetic tomorrow."""

        cur.execute(
            "INSERT INTO book_readings VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
            (
                102,
                "A Busy Day for Nwe Nwe — ရုံးလုပ်ငန်းနှင့် နေ့စဉ်ဘဝ",
                "A1+",
                passage_2,
                "Present Simple 3rd person singular (-s/-es), Time expressions, Sequence markers (First, Then, After lunch)",
                "1. What is Nwe Nwe's job? | 2. How does she commute to work? | 3. What does she eat for lunch? | 4. Where does she go before going home? | 5. Why does she read an English novel before bed?",
                "1. She is an accountant. | 2. She rides an electric motorbike. | 3. She eats Shan noodles at a noodle shop. | 4. She goes to a gym near her apartment. | 5. Because she wants to improve her vocabulary.",
                "Foundation English (A1) Chapter 12 by ထက်"
            )
        )

        # Also add Foundation A1 core lessons to book_grammar_lessons
        cur.execute("CREATE TABLE IF NOT EXISTS book_grammar_lessons (number INTEGER, title TEXT, level TEXT, explanation TEXT, patterns TEXT, examples TEXT, practice TEXT, answers TEXT);")
        cur.execute("DELETE FROM book_grammar_lessons WHERE number >= 101 AND number <= 113;")

        a1_lessons = [
            (101, "The English Alphabet & Sounds", "A1", "အင်္ဂလိပ်အက္ခရာ ၂၆ လုံး၊ သရ ၅ လုံး၊ ဗျည်း ၂၁ လုံးနှင့် Capitalization စည်းမျဉ်းများ", "Uppercase vs Lowercase, Vowels (A, E, I, O, U)", "Apple, Cat, Elephant, Umbrella", "Spelling names out loud", "NATO Phonetics"),
            (102, "Sounds and Word Stress", "A1", "အင်္ဂလိပ်အသံထွက်စနစ် ၄၄ သံ၊ Voiced vs Voiceless ဗျည်းသံများနှင့် Word Stress စည်းမျဉ်း ၄ ချက်", "Primary Stress ('), Syllables count", "TEA-cher, de-CIDE, PHO-to-graph", "Identify stressed syllables in 10 words", "Rule 1: Nouns stress 1st syllable; Rule 2: Verbs stress 2nd syllable"),
            (103, "Parts of Speech Overview", "A1", "စကားလုံးအမျိုးအစား ၈ မျိုး (Noun, Pronoun, Verb, Adjective, Adverb, Preposition, Conjunction, Interjection) ၏ အခြေခံတာဝန်များ", "Word Classes & Sentence Roles", "Su Su (Noun) walks (Verb) quickly (Adv) to (Prep) school (Noun).", "Identify parts of speech in 5 sentences", "All 8 categories classified"),
            (104, "Nouns: Types & Plurals", "A1", "Countable vs Uncountable Nouns၊ ပုံမှန်အများကိန်းစည်းမျဉ်း ၆ ချက်နှင့် ပုံမမှန်အများကိန်းများ", "Singular -> Plural (-s, -es, -ies, -ves, irregular)", "cat -> cats, box -> boxes, baby -> babies, child -> children", "Convert 10 nouns into plural", "children, women, teeth, mice, sheep"),
            (106, "Articles and Determiners", "A1", "a / an အသံထွက်စည်းမျဉ်း၊ The သုံးစွဲပုံ၊ Zero Article၊ Demonstratives (this/that/these/those) နှင့် Quantifiers", "a + consonant sound, an + vowel sound, the + specific", "a university, an hour, the sun, this book, those apples", "Fill in a / an / the / zero", "an umbrella, a European, the sky"),
            (107, "Be Verbs (am / is / are)", "A1", "am/is/are ၏ အဓိကအသုံး ၈ မျိုး၊ Subject-Verb Agreement၊ အငြင်းနှင့် Yes/No မေးခွန်းများ", "Subject + am/is/are + Complement", "I am a student. She is ready. They are not at home.", "Transform 5 statements into questions", "Is she happy? Are they here?"),
            (108, "Basic English Sentences", "A1", "အင်္ဂလိပ်ဝါကျတည်ဆောက်ပုံ SVO ရွှေစည်းမျဉ်း (မြန်မာ SOV နှင့် ကွာခြားချက်)၊ PT Rule (Place before Time)", "Subject + Verb + Object + Place + Time", "Su Su drinks coffee at home every morning.", "Arrange jumbled words into correct SVO order", "I study English at school every day."),
            (109, "Present Simple: Statements", "A1", "အလေ့အထ၊ ပုံမှန်လုပ်ရပ်နှင့် သဘာဝအမှန်တရားဖော်ပြခြင်း၊ s/es ပေါင်းထည့်နည်း စည်းမျဉ်းများ၊ Adverbs of Frequency", "He/She/It + V1-s/es, I/You/We/They + V1", "The sun rises in the east. She usually wakes up at 6 AM.", "Add s/es to 10 verbs", "watches, goes, studies, plays"),
            (110, "Present Simple: Negatives & Questions", "A1", "do / does အကူကြိယာသုံးစွဲပုံ၊ don't / doesn't + V1 အငြင်းဝါကျ၊ Yes/No နှင့် Wh- မေးခွန်းများ", "Does + Subject + V1 (Base Form)?", "Does she like coffee? No, she doesn't. What do you do?", "Make negative and question for 5 sentences", "He doesn't eat meat. Where do you work?"),
            (111, "Everyday Vocabulary in Context", "A1", "နေ့စဉ်လုပ်ရိုးလုပ်စဉ်၊ မိသားစု၊ အစားအစာ၊ အဝတ်အစားနှင့် မြို့တွင်းနေရာများ ဝေါဟာရနှင့် Collocations", "Collocations: brush teeth, take shower, have lunch", "I take a shower every morning. She has lunch at noon.", "Match verbs with nouns", "make breakfast, do homework"),
            (112, "Everyday Reading & Writing", "A1", "အခြေခံစာဖတ်ခြင်း၊ Punctuation (စကားရပ်အမှတ်အသားများ)၊ Connectors (and, but, so, because) နှင့် မက်ဆေ့ခ်ျ/အီးမေးလ်ရေးသားခြင်း", "Subject + Verb, Capitalization, Connectors", "I am tired, so I want to sleep. She likes tea, but I prefer coffee.", "Combine sentences with connectors", "Clean punctuation and coherent paragraph"),
            (113, "Imperatives & Suggestions (Bonus)", "A1+", "အမိန့်ပေး၊ တောင်းဆို၊ သတိပေးဝါကျများ (Base Verb ဖြင့်စတင်ခြင်း)၊ Please နှင့် Let's / How about အကြံပြုဝါကျများ", "Verb 1 (Base)! / Don't + Verb 1! / Let's + V1 / How about + V-ing?", "Open the door! Please help me. Don't be late! Let's watch a movie.", "Write commands and suggestions for 5 scenarios", "Be quiet! Don't worry. How about having coffee?"),
        ]

        for num, title, lvl, exp, pat, ex, pr, ans in a1_lessons:
            cur.execute(
                "INSERT INTO book_grammar_lessons VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
                (num, title, lvl, exp, pat, ex, pr, ans)
            )

        conn.commit()

        # Check total rows
        cur.execute("SELECT count(*) FROM grammar_chapters WHERE source LIKE '%Foundation English%';")
        ch_cnt = cur.fetchone()[0]
        cur.execute("SELECT count(*) FROM book_grammar_lessons;")
        lesson_cnt = cur.fetchone()[0]
        cur.execute("SELECT count(*) FROM book_readings;")
        reading_cnt = cur.fetchone()[0]
        print(f"  -> grammar_chapters (Foundation A1): {ch_cnt}")
        print(f"  -> book_grammar_lessons (Total): {lesson_cnt}")
        print(f"  -> book_readings (Total): {reading_cnt}")
        conn.close()

    print("\nAll Foundation English (A1) content successfully ingested!")


if __name__ == "__main__":
    main()
