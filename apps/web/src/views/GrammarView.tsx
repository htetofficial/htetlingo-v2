import { useEffect, useMemo, useState } from 'react';
import { Lock, Volume2, AlertTriangle, CheckCircle2, BookMarked, Search } from 'lucide-react';
import { GRAMMAR_LESSONS, LANGUAGES, type LanguageCode } from '@htetlingo/core';
import { speakText } from '../lib/speech.ts';
import type { Tier } from '../lib/revenuecat.ts';

interface GrammarViewProps {
  tier: Tier;
  onOpenPaywall: () => void;
  onGainXp: (xp: number) => void;
}

interface GrammarChapter {
  id: string;
  title: string;
  source: string;
  content: string;
}

interface ErrorsDataset {
  misusedPhrases: { wrong: string; right: string }[];
  misusedWords: { word: string; note: string }[];
}

interface HskGrammarLesson {
  id: string;
  level: string;
  title: string;
  pattern: string;
  category: string;
  examples: { zh: string; pinyin: string; en: string }[];
}

interface HskGrammarDataset {
  totalPoints: number;
  lessons: HskGrammarLesson[];
}

export function GrammarView({ tier, onOpenPaywall, onGainXp }: GrammarViewProps) {
  const [subTab, setSubTab] = useState<'lessons' | 'hsk' | 'book' | 'errors'>('lessons');
  const [lang, setLang] = useState<LanguageCode | 'all'>('all');
  const [completed, setCompleted] = useState<string[]>([]);
  const [chapters, setChapters] = useState<GrammarChapter[]>([]);
  const [activeChapter, setActiveChapter] = useState<GrammarChapter | null>(null);
  const [errorsData, setErrorsData] = useState<ErrorsDataset | null>(null);
  const [hskData, setHskData] = useState<HskGrammarDataset | null>(null);
  const [hskLevel, setHskLevel] = useState<string>('ALL');
  const [hskQuery, setHskQuery] = useState('');
  const [errQuery, setErrQuery] = useState('');

  useEffect(() => {
    fetch('/datasets/grammar-book.json')
      .then((r) => r.json())
      .then((data: GrammarChapter[]) => {
        setChapters(data);
        if (data[0]) setActiveChapter(data[0]);
      })
      .catch(() => {});
    fetch('/datasets/english-errors.json')
      .then((r) => r.json())
      .then(setErrorsData)
      .catch(() => {});
    fetch('/datasets/chinese-grammar-hsk.json')
      .then((r) => r.json())
      .then(setHskData)
      .catch(() => {});
  }, []);

  const lessons =
    lang === 'all' ? GRAMMAR_LESSONS : GRAMMAR_LESSONS.filter((l) => l.language === lang);

  const filteredHsk = useMemo(() => {
    if (!hskData) return [];
    const q = hskQuery.trim().toLowerCase();
    return hskData.lessons
      .filter((l) => (hskLevel === 'ALL' || l.level === hskLevel))
      .filter(
        (l) =>
          !q ||
          l.title.toLowerCase().includes(q) ||
          l.pattern.toLowerCase().includes(q) ||
          l.category.toLowerCase().includes(q)
      )
      .slice(0, 30);
  }, [hskData, hskLevel, hskQuery]);

  const filteredWords = useMemo(() => {
    if (!errorsData) return [];
    const q = errQuery.trim().toLowerCase();
    const list = errorsData.misusedWords;
    return (q ? list.filter((e) => e.word.toLowerCase().includes(q) || e.note.toLowerCase().includes(q)) : list).slice(0, 40);
  }, [errorsData, errQuery]);

  const filteredPhrases = useMemo(() => {
    if (!errorsData) return [];
    const q = errQuery.trim().toLowerCase();
    const list = errorsData.misusedPhrases;
    return (q ? list.filter((e) => e.wrong.toLowerCase().includes(q) || e.right.toLowerCase().includes(q)) : list).slice(0, 40);
  }, [errorsData, errQuery]);

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Multilingual Grammar Studio</h2>
          <p>Core multilingual lessons, 472 Chinese HSK 1–6 patterns, 19 English Grammar chapters, and 1,553 common errors.</p>
        </div>
        <div className="row">
          <button type="button" className={`chip ${subTab === 'lessons' ? 'active' : ''}`} onClick={() => setSubTab('lessons')}>
            Core Lessons
          </button>
          <button type="button" className={`chip ${subTab === 'hsk' ? 'active' : ''}`} onClick={() => setSubTab('hsk')}>
            🇨🇳 Chinese HSK 1–6 ({hskData?.totalPoints ?? 472})
          </button>
          <button type="button" className={`chip ${subTab === 'book' ? 'active' : ''}`} onClick={() => setSubTab('book')}>
            <BookMarked size={14} /> EN Grammar Book ({chapters.length})
          </button>
          <button type="button" className={`chip ${subTab === 'errors' ? 'active' : ''}`} onClick={() => setSubTab('errors')}>
            <AlertTriangle size={14} /> Common Errors (1,553)
          </button>
        </div>
      </div>

      {subTab === 'lessons' && (
        <>
          <div className="row">
            <button type="button" className={`chip ${lang === 'all' ? 'active' : ''}`} onClick={() => setLang('all')}>
              All Languages
            </button>
            {LANGUAGES.filter((l) => GRAMMAR_LESSONS.some((g) => g.language === l.code)).map((l) => (
              <button
                key={l.code}
                type="button"
                className={`chip ${lang === l.code ? 'active' : ''}`}
                onClick={() => setLang(l.code)}
              >
                {l.flag} {l.name}
              </button>
            ))}
          </div>

          <div className="stack">
            {lessons.map((lesson) => {
              const locked = lesson.tier === 'premium' && tier === 'free';
              const isDone = completed.includes(lesson.id);

              return (
                <article key={lesson.id} className="card stack">
                  <div className="row" style={{ justifyContent: 'space-between' }}>
                    <div className="row">
                      <span className="badge">{lesson.language.toUpperCase()}</span>
                      <span className="badge badge-amber">{lesson.level}</span>
                      {lesson.tier === 'premium' && <span className="badge">Premium</span>}
                    </div>
                    {!locked && (
                      <button
                        type="button"
                        className={`btn btn-sm ${isDone ? 'btn-primary' : ''}`}
                        onClick={() => {
                          if (!isDone) {
                            setCompleted((prev) => [...prev, lesson.id]);
                            onGainXp(15);
                          }
                        }}
                      >
                        <CheckCircle2 size={15} /> {isDone ? 'Completed' : 'Mark Complete (+15 XP)'}
                      </button>
                    )}
                  </div>

                  <div>
                    <h3 style={{ margin: '0 0 4px' }}>{lesson.title}</h3>
                    <p style={{ margin: 0, color: 'var(--muted)' }}>{lesson.summary}</p>
                  </div>

                  {locked ? (
                    <div className="card row" style={{ justifyContent: 'space-between', background: 'var(--surface-2)' }}>
                      <span className="row">
                        <Lock size={16} /> Unlock this lesson with HtetLingo Premium or Lifetime.
                      </span>
                      <button type="button" className="btn btn-primary btn-sm" onClick={onOpenPaywall}>
                        Upgrade
                      </button>
                    </div>
                  ) : (
                    <>
                      <div className="card" style={{ background: 'var(--surface-2)', padding: 12, fontFamily: 'ui-monospace, monospace', fontSize: '0.9rem' }}>
                        {lesson.formula}
                      </div>
                      <p style={{ margin: 0 }}>{lesson.explanationMy}</p>
                      <div className="stack" style={{ gap: 8 }}>
                        {lesson.examples.map((ex, idx) => (
                          <div key={idx} className="row" style={{ justifyContent: 'space-between', padding: '8px 12px', borderRadius: 10, background: 'var(--surface-2)' }}>
                            <div>
                              <div style={{ fontWeight: 600 }}>{ex.sentence}</div>
                              <div style={{ fontSize: '0.86rem', color: 'var(--muted)' }}>{ex.translation}</div>
                            </div>
                            <button
                              type="button"
                              className="btn btn-sm btn-ghost"
                              onClick={() => speakText(ex.sentence, lesson.language)}
                              aria-label="Listen to example"
                            >
                              <Volume2 size={16} />
                            </button>
                          </div>
                        ))}
                      </div>
                      {lesson.commonMistake && (
                        <div className="card" style={{ borderColor: 'var(--amber)', background: 'rgba(245, 158, 11, 0.08)', padding: 12 }}>
                          <div className="row" style={{ fontWeight: 700, color: 'var(--amber)', marginBottom: 4 }}>
                            <AlertTriangle size={16} /> Common Mistake
                          </div>
                          <div style={{ textDecoration: 'line-through', color: 'var(--danger)' }}>
                            {lesson.commonMistake.wrong}
                          </div>
                          <div style={{ fontWeight: 600, color: 'var(--success)' }}>
                            {lesson.commonMistake.right}
                          </div>
                          <div style={{ fontSize: '0.85rem', color: 'var(--muted)', marginTop: 4 }}>
                            {lesson.commonMistake.why}
                          </div>
                        </div>
                      )}
                    </>
                  )}
                </article>
              );
            })}
          </div>
        </>
      )}

      {subTab === 'hsk' && (
        <div className="stack">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <div className="row">
              {['ALL', 'HSK 1', 'HSK 2', 'HSK 3', 'HSK 4', 'HSK 5', 'HSK 6'].map((lvl) => (
                <button
                  key={lvl}
                  type="button"
                  className={`chip ${hskLevel === lvl ? 'active' : ''}`}
                  onClick={() => setHskLevel(lvl)}
                >
                  {lvl}
                </button>
              ))}
            </div>
            <span className="badge">krmanik/Chinese-Grammar</span>
          </div>

          <div className="card row">
            <Search size={18} style={{ color: 'var(--muted)' }} />
            <input
              type="search"
              className="input"
              style={{ border: 'none', padding: '4px 6px', flex: 1 }}
              placeholder="Search Mandarin grammar patterns (e.g., zai, ba, le, comparison, question)..."
              value={hskQuery}
              onChange={(e) => setHskQuery(e.target.value)}
            />
          </div>

          <div className="stack">
            {filteredHsk.map((item) => (
              <article key={item.id} className="card stack">
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <div className="row">
                    <span className="badge">{item.level}</span>
                    {item.category && <span className="badge badge-amber">{item.category.split(',')[0]}</span>}
                  </div>
                </div>
                <h3 style={{ margin: 0 }}>{item.title}</h3>
                {item.pattern && (
                  <div className="card" style={{ background: 'var(--surface-2)', padding: 10, fontFamily: 'ui-monospace, monospace', fontSize: '0.9rem' }}>
                    {item.pattern}
                  </div>
                )}
                <div className="stack" style={{ gap: 8 }}>
                  {item.examples.map((ex, i) => (
                    <div key={i} className="row" style={{ justifyContent: 'space-between', padding: '8px 12px', borderRadius: 10, background: 'var(--surface-2)' }}>
                      <div>
                        <div style={{ fontWeight: 700, fontSize: '1.05rem' }}>{ex.zh}</div>
                        <div style={{ color: 'var(--accent)', fontSize: '0.88rem' }}>{ex.pinyin}</div>
                        <div style={{ color: 'var(--muted)', fontSize: '0.86rem' }}>{ex.en}</div>
                      </div>
                      <button
                        type="button"
                        className="btn btn-sm btn-ghost"
                        onClick={() => speakText(ex.zh, 'zh')}
                        aria-label="Listen to Mandarin example"
                      >
                        <Volume2 size={16} />
                      </button>
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </div>
        </div>
      )}

      {subTab === 'book' && (
        <div className="grid-2" style={{ alignItems: 'start' }}>
          <div className="card stack" style={{ maxHeight: 520, overflowY: 'auto', gap: 6 }}>
            <span className="badge">codeyu/EnglishGrammarBook · mahavivo/logical-grammar</span>
            {chapters.map((ch) => (
              <button
                key={ch.id}
                type="button"
                className="nav-btn"
                aria-current={activeChapter?.id === ch.id ? 'page' : undefined}
                onClick={() => setActiveChapter(ch)}
              >
                {ch.title}
              </button>
            ))}
          </div>
          <div className="card stack">
            {activeChapter ? (
              <>
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <h3 style={{ margin: 0 }}>{activeChapter.title}</h3>
                  <span className="badge">{activeChapter.source}</span>
                </div>
                <pre style={{ whiteSpace: 'pre-wrap', fontFamily: 'inherit', margin: 0, lineHeight: 1.65 }}>
                  {activeChapter.content}
                </pre>
              </>
            ) : (
              <p style={{ color: 'var(--muted)' }}>Loading grammar chapters…</p>
            )}
          </div>
        </div>
      )}

      {subTab === 'errors' && (
        <div className="stack">
          <div className="card row">
            <Search size={18} style={{ color: 'var(--muted)' }} />
            <input
              type="search"
              className="input"
              style={{ border: 'none', padding: '4px 6px', flex: 1 }}
              placeholder="Search misused English words or phrases (e.g., affect, loose, irregardless)..."
              value={errQuery}
              onChange={(e) => setErrQuery(e.target.value)}
            />
            <span className="badge">max-mapper/common-english-errors</span>
          </div>

          <div className="grid-2">
            <div className="card stack">
              <h3 style={{ margin: 0, fontSize: '1rem' }}>Commonly Misused Words ({filteredWords.length})</h3>
              {filteredWords.map((item, idx) => (
                <div key={idx} style={{ padding: '8px 10px', borderRadius: 10, background: 'var(--surface-2)' }}>
                  <span style={{ color: 'var(--danger)', textDecoration: 'line-through', marginRight: 8 }}>{item.word}</span>
                  <span style={{ color: 'var(--success)', fontWeight: 600 }}>→ {item.note}</span>
                </div>
              ))}
            </div>

            <div className="card stack">
              <h3 style={{ margin: 0, fontSize: '1rem' }}>Misused Phrases ({filteredPhrases.length})</h3>
              {filteredPhrases.map((item, idx) => (
                <div key={idx} style={{ padding: '8px 10px', borderRadius: 10, background: 'var(--surface-2)' }}>
                  <div style={{ color: 'var(--danger)', textDecoration: 'line-through' }}>{item.wrong}</div>
                  <div style={{ color: 'var(--success)', fontWeight: 600 }}>→ {item.right}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
