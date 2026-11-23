import { useEffect, useMemo, useState } from 'react';
import { BookOpen, FileText, PenTool, Calendar, Volume2, Search, Eye, EyeOff, ListChecks } from 'lucide-react';
import { speakText } from '../lib/speech.ts';

interface BookGrammarLesson {
  number: number;
  title: string;
  level: string;
  explanation: string;
  patterns: string;
  examples: string;
  practice: string;
  answers: string;
}

interface BookVocabUnit {
  number: number;
  title: string;
  level: string;
  words: [string, string][];
  collocations: string;
  examples: string;
}

interface BookReading {
  number: number;
  title: string;
  level: string;
  passage: string;
  grammarNotes: string;
  questions: string;
  answers: string;
  languageFocus: string;
}

interface BookWriting {
  number: number;
  title: string;
  level: string;
  task: string;
  modelAnswer: string;
  whyItWorks: string;
  checklist: string;
}

interface BookStudyWeek {
  week: string;
  grammar: string;
  vocabulary: string;
  skills: string;
}

interface HtetBookPayload {
  title: string;
  author: string;
  grammarLessons: BookGrammarLesson[];
  vocabUnits: BookVocabUnit[];
  readings: BookReading[];
  writings: BookWriting[];
  irregularVerbs: [string, string, string, string][];
  studyPlan: BookStudyWeek[];
}

export function HtetBookView({ onGainXp }: { onGainXp: (xp: number) => void }) {
  const [book, setBook] = useState<HtetBookPayload | null>(null);
  const [tab, setTab] = useState<'grammar' | 'vocab' | 'reading' | 'writing' | 'verbs' | 'plan'>('grammar');
  const [query, setQuery] = useState('');
  const [activeLesson, setActiveLesson] = useState<number>(1);
  const [showAns, setShowAns] = useState(false);

  useEffect(() => {
    fetch('/datasets/htet-book.json')
      .then((r) => r.json())
      .then(setBook)
      .catch(() => {});
  }, []);

  const filteredLessons = useMemo(() => {
    if (!book) return [];
    const q = query.trim().toLowerCase();
    return q
      ? book.grammarLessons.filter(
          (l) => l.title.toLowerCase().includes(q) || l.level.toLowerCase().includes(q) || l.explanation.toLowerCase().includes(q)
        )
      : book.grammarLessons;
  }, [book, query]);

  const selectedLesson = book?.grammarLessons.find((l) => l.number === activeLesson) ?? filteredLessons[0];

  const filteredVerbs = useMemo(() => {
    if (!book) return [];
    const q = query.trim().toLowerCase();
    return q
      ? book.irregularVerbs.filter(([v1, v2, v3, my]) => `${v1} ${v2} ${v3} ${my}`.toLowerCase().includes(q))
      : book.irregularVerbs;
  }, [book, query]);

  if (!book) {
    return <div className="card">Loading A1–C2 Bilingual Self-Study Book (© 2026 ထက်)…</div>;
  }

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Htet A1–C2 Self-Study Book</h2>
          <p>{book.title} — {book.author}</p>
        </div>
        <span className="badge">49 Lessons · 24 Vocab · 8 Readings · 9 Writings · 16 Weeks</span>
      </div>

      <div className="row">
        <button type="button" className={`chip ${tab === 'grammar' ? 'active' : ''}`} onClick={() => setTab('grammar')}>
          <BookOpen size={14} /> 49 Grammar Lessons
        </button>
        <button type="button" className={`chip ${tab === 'vocab' ? 'active' : ''}`} onClick={() => setTab('vocab')}>
          24 Vocab Units (384)
        </button>
        <button type="button" className={`chip ${tab === 'reading' ? 'active' : ''}`} onClick={() => setTab('reading')}>
          <FileText size={14} /> 8 Reading Lab
        </button>
        <button type="button" className={`chip ${tab === 'writing' ? 'active' : ''}`} onClick={() => setTab('writing')}>
          <PenTool size={14} /> 9 Writing Workshop
        </button>
        <button type="button" className={`chip ${tab === 'verbs' ? 'active' : ''}`} onClick={() => setTab('verbs')}>
          <ListChecks size={14} /> 97 Irregular Verbs
        </button>
        <button type="button" className={`chip ${tab === 'plan' ? 'active' : ''}`} onClick={() => setTab('plan')}>
          <Calendar size={14} /> 16-Week Plan
        </button>
      </div>

      {tab === 'grammar' && (
        <div className="grid-2" style={{ alignItems: 'start' }}>
          <div className="card stack" style={{ maxHeight: 560, overflowY: 'auto', gap: 6 }}>
            <div className="row">
              <Search size={16} style={{ color: 'var(--muted)' }} />
              <input
                type="search"
                className="input"
                placeholder="Search 49 lessons (A1–C2, Passive, Inversion...)"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
            {filteredLessons.map((l) => (
              <button
                key={l.number}
                type="button"
                className="nav-btn"
                aria-current={selectedLesson?.number === l.number ? 'page' : undefined}
                onClick={() => {
                  setActiveLesson(l.number);
                  setShowAns(false);
                  onGainXp(2);
                }}
              >
                <span className="badge">{String(l.number).padStart(2, '0')}</span>
                <span style={{ flex: 1 }}>{l.title}</span>
                <span className="badge badge-amber">{l.level}</span>
              </button>
            ))}
          </div>

          {selectedLesson && (
            <div className="card stack">
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <div>
                  <span className="badge">Lesson {selectedLesson.number} · {selectedLesson.level}</span>
                  <h3 style={{ margin: '6px 0 0' }}>{selectedLesson.title}</h3>
                </div>
              </div>
              <p style={{ whiteSpace: 'pre-line', margin: 0 }}>{selectedLesson.explanation}</p>
              <div className="card" style={{ background: 'var(--surface-2)', padding: 12 }}>
                <strong>Grammar Patterns</strong>
                <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'ui-monospace, monospace', margin: '6px 0 0', fontSize: '0.88rem' }}>
                  {selectedLesson.patterns}
                </pre>
              </div>
              <div className="card" style={{ background: 'var(--surface-2)', padding: 12 }}>
                <strong>Examples with Myanmar Translation</strong>
                <p style={{ whiteSpace: 'pre-line', margin: '6px 0 0' }}>{selectedLesson.examples}</p>
              </div>
              <div className="card stack" style={{ padding: 12 }}>
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <strong>Practice Exercises</strong>
                  <button type="button" className="btn btn-sm" onClick={() => setShowAns((v) => !v)}>
                    {showAns ? <EyeOff size={14} /> : <Eye size={14} />}
                    {showAns ? 'Hide Answers' : 'Check Answers'}
                  </button>
                </div>
                <p style={{ whiteSpace: 'pre-line', margin: 0 }}>{selectedLesson.practice}</p>
                {showAns && (
                  <div style={{ color: 'var(--success)', fontWeight: 600, whiteSpace: 'pre-line' }}>
                    {selectedLesson.answers}
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}

      {tab === 'vocab' && (
        <div className="stack">
          {book.vocabUnits.map((u) => (
            <div key={u.number} className="card stack">
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <h3 style={{ margin: 0 }}>Unit {String(u.number).padStart(2, '0')} · {u.title}</h3>
                <span className="badge">{u.level}</span>
              </div>
              <div className="grid-3">
                {u.words.map(([en, my], idx) => (
                  <div key={idx} className="row" style={{ justifyContent: 'space-between', padding: '8px 10px', borderRadius: 10, background: 'var(--surface-2)' }}>
                    <div>
                      <div style={{ fontWeight: 700 }}>{en}</div>
                      <div style={{ fontSize: '0.85rem', color: 'var(--accent)' }}>{my}</div>
                    </div>
                    <button type="button" className="btn btn-ghost btn-sm" onClick={() => speakText(en, 'en')} aria-label={`Pronounce ${en}`}>
                      <Volume2 size={15} />
                    </button>
                  </div>
                ))}
              </div>
              <div style={{ fontSize: '0.88rem', color: 'var(--muted)' }}>
                <strong>Collocations:</strong> {u.collocations}
              </div>
              <div style={{ fontSize: '0.88rem', whiteSpace: 'pre-line' }}>{u.examples}</div>
            </div>
          ))}
        </div>
      )}

      {tab === 'reading' && (
        <div className="stack">
          {book.readings.map((r) => (
            <article key={r.number} className="card stack">
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <div>
                  <span className="badge">Reading {String(r.number).padStart(2, '0')} · {r.level}</span>
                  <h3 style={{ margin: '6px 0 0' }}>{r.title}</h3>
                </div>
                <button type="button" className="btn btn-sm btn-primary" onClick={() => speakText(r.passage, 'en', 0.9)}>
                  <Volume2 size={15} /> Read Aloud
                </button>
              </div>
              <p style={{ lineHeight: 1.75, margin: 0 }}>{r.passage}</p>
              <div className="card" style={{ background: 'var(--surface-2)', padding: 12 }}>
                <strong>Grammar Notes (Burmese):</strong> {r.grammarNotes}
              </div>
              <div className="grid-2">
                <div className="card" style={{ padding: 12 }}>
                  <strong>Comprehension Questions</strong>
                  <p style={{ whiteSpace: 'pre-line', margin: '6px 0 0', fontSize: '0.9rem' }}>{r.questions}</p>
                </div>
                <div className="card" style={{ padding: 12 }}>
                  <strong>Suggested Answers</strong>
                  <p style={{ whiteSpace: 'pre-line', margin: '6px 0 0', fontSize: '0.9rem', color: 'var(--success)' }}>{r.answers}</p>
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      {tab === 'writing' && (
        <div className="stack">
          {book.writings.map((w) => (
            <article key={w.number} className="card stack">
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <div>
                  <span className="badge">Writing {String(w.number).padStart(2, '0')} · {w.level}</span>
                  <h3 style={{ margin: '6px 0 0' }}>{w.title}</h3>
                </div>
                <button type="button" className="btn btn-sm" onClick={() => speakText(w.modelAnswer, 'en', 0.92)}>
                  <Volume2 size={15} /> Listen Model
                </button>
              </div>
              <p style={{ margin: 0 }}><strong>Task:</strong> {w.task}</p>
              <div className="card" style={{ background: 'var(--surface-2)', padding: 14, whiteSpace: 'pre-line', lineHeight: 1.7 }}>
                {w.modelAnswer}
              </div>
              <p style={{ margin: 0, fontSize: '0.9rem' }}><strong>Why It Works:</strong> {w.whyItWorks}</p>
              <p style={{ margin: 0, fontSize: '0.86rem', color: 'var(--muted)' }}><strong>Checklist:</strong> {w.checklist}</p>
            </article>
          ))}
        </div>
      )}

      {tab === 'verbs' && (
        <div className="card stack">
          <input
            type="search"
            className="input"
            placeholder="Search 97 irregular verbs (V1, V2, V3, or Burmese)..."
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
          <div className="grid-3">
            {filteredVerbs.map(([v1, v2, v3, my], idx) => (
              <div key={idx} className="card" style={{ padding: 12, background: 'var(--surface-2)' }}>
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <strong>{v1} → {v2} → {v3}</strong>
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => speakText(`${v1}, ${v2}, ${v3}`, 'en')}>
                    <Volume2 size={14} />
                  </button>
                </div>
                <div style={{ color: 'var(--accent)', fontSize: '0.88rem', marginTop: 4 }}>{my}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {tab === 'plan' && (
        <div className="grid-2">
          {book.studyPlan.map((w) => (
            <div key={w.week} className="card stack" style={{ gap: 6 }}>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <strong>{w.week}</strong>
                <span className="badge">{w.grammar}</span>
              </div>
              <div style={{ fontSize: '0.9rem' }}>Vocabulary: {w.vocabulary}</div>
              <div style={{ fontSize: '0.88rem', color: 'var(--muted)' }}>Integrated Skills: {w.skills}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
