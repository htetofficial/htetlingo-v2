#!/usr/bin/env python3
"""
Ingest yukiar/CEFR-SP dataset (10,004 sentences across A1-C2) and construct
the Authentic Full-Screen CEFR Level Exam question bank.
"""
from __future__ import annotations
import glob
import json
import os
import random
import re
import sqlite3

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
CEFR_SP_DIR = os.path.join(BASE_DIR, "raw", "cefr_sp")
MOBILE_DB_PATH = os.path.join(BASE_DIR, "../../apps/android/app/src/main/assets/htetlingo_mobile.sqlite")
ORNAGAI_DB_PATH = os.path.join(BASE_DIR, "out", "ornagai.sqlite")

CEFR_LEVELS = {1: "A1", 2: "A2", 3: "B1", 4: "B2", 5: "C1", 6: "C2"}

def ingest_cefr_sp_sentences(cursor: sqlite3.Cursor):
    print("Creating cefr_sp_sentences table...")
    cursor.execute("DROP TABLE IF EXISTS cefr_sp_sentences")
    cursor.execute("""
        CREATE TABLE cefr_sp_sentences (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sentence TEXT NOT NULL,
            level_num INTEGER NOT NULL,
            cefr_level TEXT NOT NULL,
            annotator_a INTEGER,
            annotator_b INTEGER,
            source TEXT NOT NULL
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_cefr_sp_lvl ON cefr_sp_sentences(cefr_level)")

    total = 0
    files = glob.glob(os.path.join(CEFR_SP_DIR, "*.txt"))
    for f in sorted(files):
        source = "SCoRE" if "SCoRE" in f else "Wiki-Auto"
        with open(f, encoding="utf-8", errors="ignore") as fp:
            for line in fp:
                line = line.strip()
                if not line:
                    continue
                parts = line.split("\t")
                if len(parts) >= 2:
                    sent = parts[0].strip()
                    try:
                        lvl_a = int(parts[1].strip())
                        lvl_b = int(parts[2].strip()) if len(parts) > 2 and parts[2].strip().isdigit() else lvl_a
                        avg_lvl = round((lvl_a + lvl_b) / 2)
                        cefr = CEFR_LEVELS.get(avg_lvl, "B1")
                        cursor.execute(
                            "INSERT INTO cefr_sp_sentences (sentence, level_num, cefr_level, annotator_a, annotator_b, source) VALUES (?, ?, ?, ?, ?, ?)",
                            (sent, avg_lvl, cefr, lvl_a, lvl_b, source)
                        )
                        total += 1
                    except Exception:
                        pass
    print(f"Inserted {total} CEFR-SP sentences.")
    return total

def build_cefr_exam_pool(cursor: sqlite3.Cursor):
    print("Building authentic CEFR Exam question bank (cefr_exam_questions)...")
    cursor.execute("DROP TABLE IF EXISTS cefr_exam_questions")
    cursor.execute("""
        CREATE TABLE cefr_exam_questions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            cefr_level TEXT NOT NULL,
            category TEXT NOT NULL,
            question TEXT NOT NULL,
            option_a TEXT NOT NULL,
            option_b TEXT NOT NULL,
            option_c TEXT NOT NULL,
            option_d TEXT NOT NULL,
            correct_index INTEGER NOT NULL,
            explanation TEXT NOT NULL,
            audio_text TEXT
        )
    """)
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_exam_level ON cefr_exam_questions(cefr_level)")
    cursor.execute("CREATE INDEX IF NOT EXISTS idx_exam_cat ON cefr_exam_questions(category)")

    questions = []

    # 1. Author's Official Book Review Tests (60 questions across A1–C2)
    # Check if grammar_exercises table has them or read from parsed book json
    book_json_path = os.path.join(BASE_DIR, "out", "htet-book.json")
    if os.path.exists(book_json_path):
        with open(book_json_path, encoding="utf-8") as bf:
            book_data = json.load(bf)
            for rt in book_data.get("reviewTests", []):
                sec = rt.get("section", "")
                if "A1" in sec:
                    lvl = "A1" if len(questions) % 2 == 0 else "A2"
                elif "B1" in sec:
                    lvl = "B1" if len(questions) % 2 == 0 else "B2"
                else:
                    lvl = "C1" if len(questions) % 2 == 0 else "C2"

                opts = rt.get("options", [])
                while len(opts) < 4:
                    opts.append("None of the above")
                opts = opts[:4]

                corr_ans = rt.get("answer", "").strip()
                corr_idx = 0
                for idx, o in enumerate(opts):
                    if o.strip() == corr_ans or o.strip().startswith(corr_ans):
                        corr_idx = idx
                        break

                questions.append({
                    "cefr_level": lvl,
                    "category": "Grammar & Structure",
                    "question": rt.get("question", ""),
                    "options": opts,
                    "correct_index": corr_idx,
                    "explanation": rt.get("explanation", "Reference: A Complete Bilingual Self-Study Book: English Grammar & Vocabulary (A1–C2)"),
                    "audio_text": rt.get("question", "").replace("_____", "blank")
                })
        print(f"Loaded {len(questions)} book review questions.")

    # 2. Authentic A1 to C2 Curated Exam Questions
    # Let's add curated CEFR test questions specifically designed for each CEFR band
    curated_pool = [
        # --- A1 LEVEL (Beginner) ---
        {
            "cefr_level": "A1",
            "category": "Grammar & Structure",
            "question": "Sarah _____ from Canada. She is a student.",
            "options": ["is", "am", "are", "be"],
            "correct_index": 0,
            "explanation": "Subject 'Sarah' is third person singular (she), so use 'is'.",
            "audio_text": "Sarah is from Canada. She is a student."
        },
        {
            "cefr_level": "A1",
            "category": "Vocabulary in Context",
            "question": "I need to buy some milk and bread from the _____.",
            "options": ["supermarket", "library", "cinema", "airport"],
            "correct_index": 0,
            "explanation": "Milk and bread are groceries bought at a supermarket.",
            "audio_text": "I need to buy some milk and bread from the supermarket."
        },
        {
            "cefr_level": "A1",
            "category": "Listening Comprehension",
            "question": "Listen carefully: What time does the train arrive?",
            "options": ["At half past eight (8:30)", "At eight o'clock (8:00)", "At nine o'clock (9:00)", "At ten thirty (10:30)"],
            "correct_index": 0,
            "explanation": "The speaker clearly states the arrival time is 8:30 (half past eight).",
            "audio_text": "Attention passengers: The morning express train from Manchester will arrive on platform 3 at half past eight."
        },
        {
            "cefr_level": "A1",
            "category": "Grammar & Structure",
            "question": "They _____ have a car, so they take the bus.",
            "options": ["don't", "doesn't", "aren't", "isn't"],
            "correct_index": 0,
            "explanation": "With plural pronoun 'they', the present simple negative auxiliary is 'don't'.",
            "audio_text": "They don't have a car, so they take the bus."
        },
        {
            "cefr_level": "A1",
            "category": "Error Identification",
            "question": "Identify the incorrect word: 'She like apples and oranges very much.'",
            "options": ["like", "apples", "and", "much"],
            "correct_index": 0,
            "explanation": "Third person singular requires 'likes' instead of 'like'.",
            "audio_text": "She like apples and oranges very much."
        },
        {
            "cefr_level": "A1",
            "category": "Vocabulary in Context",
            "question": "My father's sister is my _____.",
            "options": ["aunt", "cousin", "niece", "grandmother"],
            "correct_index": 0,
            "explanation": "Your father's sister is your aunt (အဒေါ်).",
            "audio_text": "My father's sister is my aunt."
        },
        {
            "cefr_level": "A1",
            "category": "Listening Comprehension",
            "question": "Listen carefully: Where is David going today?",
            "options": ["To the hospital", "To the university library", "To the city center", "To the gym"],
            "correct_index": 1,
            "explanation": "David states he must study at the university library today.",
            "audio_text": "Hi Jane, I cannot come to lunch today because I must spend the afternoon at the university library preparing for my exam."
        },

        # --- A2 LEVEL (Elementary) ---
        {
            "cefr_level": "A2",
            "category": "Grammar & Structure",
            "question": "Yesterday, we _____ to the beach because the weather was sunny.",
            "options": ["went", "go", "gone", "going"],
            "correct_index": 0,
            "explanation": "Past time marker 'yesterday' requires past simple 'went'.",
            "audio_text": "Yesterday, we went to the beach because the weather was sunny."
        },
        {
            "cefr_level": "A2",
            "category": "Vocabulary in Context",
            "question": "Can you _____ me your pencil for a moment? I will give it back.",
            "options": ["lend", "borrow", "rent", "keep"],
            "correct_index": 0,
            "explanation": "'Lend' means to give temporarily; 'borrow' means to receive temporarily.",
            "audio_text": "Can you lend me your pencil for a moment?"
        },
        {
            "cefr_level": "A2",
            "category": "Listening Comprehension",
            "question": "Listen carefully: How much did the jacket cost?",
            "options": ["Forty-five pounds (£45)", "Fifty-five pounds (£55)", "Thirty-five pounds (£35)", "Twenty pounds (£20)"],
            "correct_index": 0,
            "explanation": "The speaker states the jacket was on discount for forty-five pounds.",
            "audio_text": "I bought this warm winter jacket yesterday on sale. Originally it was sixty pounds, but I only paid forty-five pounds."
        },
        {
            "cefr_level": "A2",
            "category": "Grammar & Structure",
            "question": "Mount Everest is _____ mountain in the world.",
            "options": ["the highest", "higher", "highest", "more high"],
            "correct_index": 0,
            "explanation": "Superlative of one-syllable adjective 'high' takes 'the highest'.",
            "audio_text": "Mount Everest is the highest mountain in the world."
        },
        {
            "cefr_level": "A2",
            "category": "Error Identification",
            "question": "Identify the incorrect word: 'I have seen him two days ago in Yangon.'",
            "options": ["have seen", "him", "two days ago", "in"],
            "correct_index": 0,
            "explanation": "'Two days ago' is a specific finished past time marker; use past simple 'saw' instead of 'have seen'.",
            "audio_text": "I have seen him two days ago in Yangon."
        },
        {
            "cefr_level": "A2",
            "category": "Vocabulary in Context",
            "question": "Please turn off the lights to _____ electricity.",
            "options": ["save", "spend", "waste", "lose"],
            "correct_index": 0,
            "explanation": "Turning off unnecessary lights helps to 'save' electricity.",
            "audio_text": "Please turn off the lights to save electricity."
        },

        # --- B1 LEVEL (Intermediate) ---
        {
            "cefr_level": "B1",
            "category": "Grammar & Structure",
            "question": "If I had enough money, I _____ around the world.",
            "options": ["would travel", "will travel", "traveled", "have traveled"],
            "correct_index": 0,
            "explanation": "Second conditional (hypothetical present/future): If + past simple, would + base verb.",
            "audio_text": "If I had enough money, I would travel around the world."
        },
        {
            "cefr_level": "B1",
            "category": "Vocabulary in Context",
            "question": "The meeting was _____ until next Thursday due to the heavy monsoon rain.",
            "options": ["postponed", "cancelled", "refused", "prevented"],
            "correct_index": 0,
            "explanation": "'Postponed' means delayed or rescheduled to a later time.",
            "audio_text": "The meeting was postponed until next Thursday due to the heavy monsoon rain."
        },
        {
            "cefr_level": "B1",
            "category": "Listening Comprehension",
            "question": "Listen carefully: What is the main purpose of Dr. Lynn's message?",
            "options": ["To reschedule an appointment", "To confirm lab test results", "To ask for payment", "To cancel all visits"],
            "correct_index": 0,
            "explanation": "The audio states Dr. Lynn needs to move Tuesday's appointment to Friday morning.",
            "audio_text": "Good morning Mr. Smith, this is Dr. Lynn from the community clinic calling to see if we can reschedule your routine checkup from Tuesday afternoon to Friday at 10 AM."
        },
        {
            "cefr_level": "B1",
            "category": "Grammar & Structure",
            "question": "The new bridge _____ by the engineering team since last summer.",
            "options": ["has been being constructed", "has been built", "is constructing", "was built"],
            "correct_index": 1,
            "explanation": "Present perfect passive indicates completion or continuation connected to the present.",
            "audio_text": "The new bridge has been built across the river."
        },
        {
            "cefr_level": "B1",
            "category": "Error Identification",
            "question": "Identify the error: 'Although it rained heavily, but we still enjoyed our hiking trip.'",
            "options": ["Although", "rained", "but", "hiking"],
            "correct_index": 2,
            "explanation": "Do not combine 'Although' with 'but' in the same complex sentence. Remove 'but'.",
            "audio_text": "Although it rained heavily, but we still enjoyed our hiking trip."
        },
        {
            "cefr_level": "B1",
            "category": "Vocabulary in Context",
            "question": "Her explanation was so _____ that everyone understood immediately.",
            "options": ["clear", "vague", "complicated", "boring"],
            "correct_index": 0,
            "explanation": "'Clear' fits logically because everyone understood immediately.",
            "audio_text": "Her explanation was so clear that everyone understood immediately."
        },

        # --- B2 LEVEL (Upper-Intermediate) ---
        {
            "cefr_level": "B2",
            "category": "Grammar & Structure",
            "question": "Had I known about the flight delay earlier, I _____ at the airport so early.",
            "options": ["would not have arrived", "will not arrive", "had not arrived", "would not arrive"],
            "correct_index": 0,
            "explanation": "Inverted third conditional (Had I known = If I had known): requires 'would not have arrived'.",
            "audio_text": "Had I known about the flight delay earlier, I would not have arrived at the airport so early."
        },
        {
            "cefr_level": "B2",
            "category": "Vocabulary in Context",
            "question": "The government launched an ambitious initiative to promote _____ energy sources.",
            "options": ["renewable", "exhaustible", "temporary", "fossilized"],
            "correct_index": 0,
            "explanation": "'Renewable energy' (solar, wind) is sustainable and clean.",
            "audio_text": "The government launched an ambitious initiative to promote renewable energy sources."
        },
        {
            "cefr_level": "B2",
            "category": "Listening Comprehension",
            "question": "Listen carefully: What factor contributed most to the project's turnaround?",
            "options": ["Cross-functional team alignment", "Increased financial budget", "Outsourcing overseas", "Extending the deadline by a year"],
            "correct_index": 0,
            "explanation": "The speaker highlights cross-functional team alignment as the pivotal element.",
            "audio_text": "While securing additional funding was helpful, what truly turned the project around was establishing tight cross-functional alignment between engineering and product management."
        },
        {
            "cefr_level": "B2",
            "category": "Grammar & Structure",
            "question": "It is essential that every candidate _____ on time for the examination.",
            "options": ["be", "is", "was", "are"],
            "correct_index": 0,
            "explanation": "The mandative subjunctive requires base form 'be' after 'It is essential that...'.",
            "audio_text": "It is essential that every candidate be on time for the examination."
        },
        {
            "cefr_level": "B2",
            "category": "Error Identification",
            "question": "Identify the incorrect part: 'Neither the manager nor the employees was aware of the policy update.'",
            "options": ["Neither", "nor", "was", "aware"],
            "correct_index": 2,
            "explanation": "In 'Neither... nor...', the verb agrees with the closer subject ('employees' is plural, so use 'were').",
            "audio_text": "Neither the manager nor the employees was aware of the policy update."
        },
        {
            "cefr_level": "B2",
            "category": "Vocabulary in Context",
            "question": "The newly published study sheds _____ on the correlation between sleep patterns and cognitive performance.",
            "options": ["light", "dark", "fire", "sound"],
            "correct_index": 0,
            "explanation": "The idiom 'shed light on' means to clarify or provide new insight into a subject.",
            "audio_text": "The newly published study sheds light on the correlation between sleep patterns and cognitive performance."
        },

        # --- C1 LEVEL (Advanced) ---
        {
            "cefr_level": "C1",
            "category": "Grammar & Structure",
            "question": "Seldom _____ such an articulate defense of civil liberties in contemporary jurisprudence.",
            "options": ["have we witnessed", "we have witnessed", "we witnessed", "did we witnessed"],
            "correct_index": 0,
            "explanation": "Negative adverbial 'Seldom' at the beginning of a clause triggers subject-auxiliary inversion ('have we witnessed').",
            "audio_text": "Seldom have we witnessed such an articulate defense of civil liberties in contemporary jurisprudence."
        },
        {
            "cefr_level": "C1",
            "category": "Vocabulary in Context",
            "question": "His argument, though superficially persuasive, was fundamentally _____ upon flawed statistical premises.",
            "options": ["predicated", "precluded", "procured", "profaned"],
            "correct_index": 0,
            "explanation": "'Predicated on' means based, founded, or dependent upon.",
            "audio_text": "His argument was fundamentally predicated upon flawed statistical premises."
        },
        {
            "cefr_level": "C1",
            "category": "Listening Comprehension",
            "question": "Listen carefully: What is the speaker's overarching stance on AI governance?",
            "options": ["Pragmatic and adaptive multilateral oversight", "Complete deregulation to spur innovation", "Immediate global moratorium on all deployments", "Exclusive corporate self-policing"],
            "correct_index": 0,
            "explanation": "The speaker advocates for dynamic, multilateral institutional oversight rather than extremes.",
            "audio_text": "Rigid unilateral prohibitions are doomed to obsolescence; what the international community desperately requires is a pragmatic, iterative multilateral framework that evolves alongside generative capabilities."
        },
        {
            "cefr_level": "C1",
            "category": "Grammar & Structure",
            "question": "Under no circumstances _____ confidential client credentials without prior written authorization.",
            "options": ["are associates to disclose", "associates should disclose", "associates are disclosing", "must associates disclosing"],
            "correct_index": 0,
            "explanation": "Inversion after 'Under no circumstances': 'are associates to disclose'.",
            "audio_text": "Under no circumstances are associates to disclose confidential client credentials without prior written authorization."
        },
        {
            "cefr_level": "C1",
            "category": "Error Identification",
            "question": "Identify the erroneous usage: 'The board recommended that the CEO steps down immediately.'",
            "options": ["board", "recommended", "steps", "immediately"],
            "correct_index": 2,
            "explanation": "Subjunctive mood after 'recommended that' demands the base form 'step down', not 'steps down'.",
            "audio_text": "The board recommended that the CEO steps down immediately."
        },
        {
            "cefr_level": "C1",
            "category": "Vocabulary in Context",
            "question": "The diplomatic envoy acted as a _____ between the conflicting factions to broker the ceasefire.",
            "options": ["liaison", "pariah", "spectator", "sovereign"],
            "correct_index": 0,
            "explanation": "A 'liaison' facilitates close collaboration or communication between opposing entities.",
            "audio_text": "The diplomatic envoy acted as a liaison between the conflicting factions to broker the ceasefire."
        },

        # --- C2 LEVEL (Proficiency / Mastery) ---
        {
            "cefr_level": "C2",
            "category": "Grammar & Structure",
            "question": "No sooner _____ the rostrum than a rapturous applause erupted throughout the auditorium.",
            "options": ["had the laureates approached", "did the laureates approached", "the laureates had approached", "the laureates approached"],
            "correct_index": 0,
            "explanation": "Inverted construction 'No sooner had [Subject] [V3] than...'.",
            "audio_text": "No sooner had the laureates approached the rostrum than a rapturous applause erupted throughout the auditorium."
        },
        {
            "cefr_level": "C2",
            "category": "Vocabulary in Context",
            "question": "Her thesis offered a remarkably _____ synthesis of post-structuralist critique and empirical sociology.",
            "options": ["lucid", "obfuscated", "pedestrian", "specious"],
            "correct_index": 0,
            "explanation": "'Lucid' means extraordinarily clear, articulate, and easily understood despite deep complexity.",
            "audio_text": "Her thesis offered a remarkably lucid synthesis of post-structuralist critique and empirical sociology."
        },
        {
            "cefr_level": "C2",
            "category": "Listening Comprehension",
            "question": "Listen carefully: How does the keynote critique contemporary epistemic discourse?",
            "options": ["It decries the echo chamber of algorithmic epistemic insularity", "It praises unfiltered social media democratisation", "It advocates abandoning empirical methodologies", "It predicts the complete demise of academic journals"],
            "correct_index": 0,
            "explanation": "The speaker censures how algorithmic curation leads to epistemic insularity and polarising rhetoric.",
            "audio_text": "The inexorable atomisation of public discourse into algorithmic echo chambers fosters an epistemic insularity that fundamentally threatens democratic deliberation."
        },
        {
            "cefr_level": "C2",
            "category": "Grammar & Structure",
            "question": "Be that _____ it may, the governing board must abide by the statutory covenant.",
            "options": ["as", "so", "like", "how"],
            "correct_index": 0,
            "explanation": "Fixed archaic subjunctive idiom: 'Be that as it may' (သို့သော်လည်းကောင်း).",
            "audio_text": "Be that as it may, the governing board must abide by the statutory covenant."
        },
        {
            "cefr_level": "C2",
            "category": "Error Identification",
            "question": "Identify the flawed syntax: 'Scarcely had she begun her disquisition when did the proctor interrupt her.'",
            "options": ["Scarcely had", "begun", "when did", "interrupt"],
            "correct_index": 2,
            "explanation": "In 'Scarcely... when...', the clause following 'when' takes normal non-inverted word order ('when the proctor interrupted her').",
            "audio_text": "Scarcely had she begun her disquisition when did the proctor interrupt her."
        },
        {
            "cefr_level": "C2",
            "category": "Vocabulary in Context",
            "question": "The candidate's rhetorical eloquence was mere _____, concealing a profound absence of substantive policy.",
            "options": ["sophistry", "integrity", "erudition", "veracity"],
            "correct_index": 0,
            "explanation": "'Sophistry' denotes the clever use of specious, deceptive arguments.",
            "audio_text": "The candidate's rhetorical eloquence was mere sophistry, concealing a profound absence of substantive policy."
        }
    ]

    for q in curated_pool:
        questions.append(q)

    # 3. Add more questions generated from CEFR-SP sentences for high variety!
    # Fetch random sentences from CEFR-SP for each level and create Cloze / Level classification tests
    print("Generating additional Cloze questions from CEFR-SP sentences...")
    cursor.execute("SELECT sentence, cefr_level FROM cefr_sp_sentences ORDER BY RANDOM() LIMIT 200")
    sp_rows = cursor.fetchall()
    
    # Common grammar distractor words
    grammar_distractors = {
        "is": ["are", "am", "be"],
        "are": ["is", "am", "was"],
        "was": ["were", "is", "been"],
        "were": ["was", "are", "been"],
        "have": ["has", "had", "having"],
        "has": ["have", "had", "having"],
        "had": ["have", "has", "having"],
        "would": ["will", "could", "should"],
        "could": ["can", "would", "might"],
        "should": ["must", "would", "shall"],
        "been": ["being", "be", "was"],
        "since": ["for", "during", "while"],
        "for": ["since", "during", "ago"],
        "although": ["despite", "however", "because"],
        "because": ["although", "despite", "so"],
        "which": ["who", "whom", "whose"],
        "who": ["which", "whom", "what"]
    }

    sp_added = 0
    for sent, lvl in sp_rows:
        words = sent.split()
        if len(words) < 6:
            continue
        # Find if a target word exists in sentence
        target_idx = None
        target_word = None
        for i, w in enumerate(words):
            clean_w = re.sub(r'[^a-zA-Z]', '', w).lower()
            if clean_w in grammar_distractors:
                target_idx = i
                target_word = clean_w
                break
        
        if target_idx is not None and target_word:
            distractors = grammar_distractors[target_word]
            blanked_sent = " ".join([words[j] if j != target_idx else "_____" for j in range(len(words))])
            
            opts = [target_word] + distractors[:3]
            random.shuffle(opts)
            c_idx = opts.index(target_word)
            
            questions.append({
                "cefr_level": lvl,
                "category": "CEFR Structure & Usage",
                "question": f"Complete the {lvl} level sentence:\n{blanked_sent}",
                "options": opts,
                "correct_index": c_idx,
                "explanation": f"In this {lvl} sentence, the correct grammatical word is '{target_word}'. Full sentence: '{sent}'.",
                "audio_text": sent
            })
            sp_added += 1

    print(f"Added {sp_added} questions derived from CEFR-SP.")
    print(f"Total CEFR Exam Question Pool: {len(questions)} items.")

    for q in questions:
        opts = q["options"]
        cursor.execute("""
            INSERT INTO cefr_exam_questions (
                cefr_level, category, question, option_a, option_b, option_c, option_d,
                correct_index, explanation, audio_text
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, (
            q["cefr_level"],
            q["category"],
            q["question"],
            opts[0],
            opts[1],
            opts[2],
            opts[3],
            q["correct_index"],
            q["explanation"],
            q.get("audio_text", q["question"])
        ))

    return len(questions)

def main():
    print("=== Processing CEFR-SP and Building Authentic CEFR Level Exam Bank ===")
    
    # 1. Update Android Assets Mobile DB
    if os.path.exists(MOBILE_DB_PATH):
        print(f"Updating Mobile DB: {MOBILE_DB_PATH}")
        conn = sqlite3.connect(MOBILE_DB_PATH)
        cur = conn.cursor()
        ingest_cefr_sp_sentences(cur)
        build_cefr_exam_pool(cur)
        conn.commit()
        conn.close()
        print("Mobile DB updated successfully.")

    # 2. Update Out Ornagai DB
    if os.path.exists(ORNAGAI_DB_PATH):
        print(f"Updating Ornagai DB: {ORNAGAI_DB_PATH}")
        conn = sqlite3.connect(ORNAGAI_DB_PATH)
        cur = conn.cursor()
        ingest_cefr_sp_sentences(cur)
        build_cefr_exam_pool(cur)
        conn.commit()
        conn.close()
        print("Ornagai DB updated successfully.")

    print("=== Done CEFR-SP Pipeline ===")

if __name__ == "__main__":
    main()
