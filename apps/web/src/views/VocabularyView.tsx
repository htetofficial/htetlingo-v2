import { useEffect, useMemo, useState } from 'react';
import { Volume2, RotateCcw, Sparkles, Zap, Shuffle, Search, CheckCircle2 } from 'lucide-react';
import { LANGUAGES, VOCAB_DECK, reviewCard, type LanguageCode, type SrsState } from '@htetlingo/core';
import { speakText } from '../lib/speech.ts';

// [word, ipa, myDef, zhDef, enDef, tag, level]
type UnifiedVocabTuple = [string, string, string, string, string, string, string];

interface UnifiedVocabPayload {
  count: number;
  items: UnifiedVocabTuple[];
}

interface VocabularyViewProps {
  srsMap: Record<string, SrsState>;
  onReview: (cardId: string, next: SrsState) => void;
  onGainXp: (xp: number) => void;
}

export function VocabularyView({ srsMap, onReview, onGainXp }: VocabularyViewProps) {
  const [studyMode, setStudyMode] = useState<'speed' | 'flashcard' | 'multilingual'>('speed');
  const [dbWords, setDbWords] = useState<UnifiedVocabTuple[]>([]);
  const [tagFilter, setTagFilter] = useState<string>('ALL');
  const [search, setSearch] = useState('');
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);
  const [autoSpeak, setAutoSpeak] = useState(true);
  const [sprintCount, setSprintCount] = useState(0);
  const [lang, setLang] = useState<LanguageCode | 'all'>('all');

  useEffect(() => {
    fetch('/datasets/vocab-core.json')
      .then((r) => r.json())
      .then((data: UnifiedVocabPayload) => setDbWords(data.items ?? []))
      .catch(() => {});
  }, []);

  const filteredDb = useMemo(() => {
    const q = search.trim().toLowerCase();
    return dbWords.filter(([word, , myDef, zhDef, , tag, level]) => {
      if (tagFilter !== 'ALL') {
        const tf = tagFilter.toLowerCase();
        if (!tag.toLowerCase().includes(tf) && level.toLowerCase() !== tf) return false;
      }
      if (!q) return true;
      return word.toLowerCase().includes(q) || myDef.includes(q) || zhDef.includes(q);
    });
  }, [dbWords, tagFilter, search]);

  const activeDbCard = filteredDb[index % Math.max(filteredDb.length, 1)];

  const multiCards = useMemo(
    () => (lang === 'all' ? VOCAB_DECK : VOCAB_DECK.filter((c) => c.language === lang)),
    [lang]
  );
  const multiCard = multiCards[index % Math.max(multiCards.length, 1)];

  const jumpRandom = () => {
    if (filteredDb.length === 0) return;
    const nextIdx = Math.floor(Math.random() * filteredDb.length);
    setIndex(nextIdx);
    setFlipped(false);
    if (autoSpeak && filteredDb[nextIdx]) {
      speakText(filteredDb[nextIdx][0], 'en', 1.0);
    }
  };

  const advanceDb = (quality: 0 | 3 | 4 | 5) => {
    if (!activeDbCard) return;
    const cardId = `db-${activeDbCard[0].toLowerCase()}`;
    const next = reviewCard(srsMap[cardId], cardId, quality);
    onReview(cardId, next);
    onGainXp(quality >= 4 ? 8 : 4);
    setSprintCount((c) => c + 1);
    const nextIdx = (index + 1) % Math.max(filteredDb.length, 1);
    setIndex(nextIdx);
    setFlipped(false);
    if (autoSpeak && filteredDb[nextIdx]) {
      speakText(filteredDb[nextIdx][0], 'en', 1.0);
    }
  };

  useEffect(() => {
    if (studyMode === 'multilingual') return;
    const onKey = (e: KeyboardEvent) => {
      if (e.target instanceof HTMLInputElement || e.target instanceof HTMLTextAreaElement) return;
      if (e.code === 'Space') {
        e.preventDefault();
        setFlipped((f) => !f);
      } else if (e.key === '1') {
        advanceDb(0);
      } else if (e.key === '2' || e.key === 'ArrowRight') {
        advanceDb(4);
      } else if (e.key === '3') {
        advanceDb(5);
      }
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  });

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Auto-Generated Vocabulary ({dbWords.length.toLocaleString()}+ Words)</h2>
          <p>
            Automatically built by joining Ornagai EN–MY, ECDICT EN–ZH & EN–EN databases — study fast with zero small-deck limits.
          </p>
        </div>
        <div className="row">
          <span className="badge">
            <Sparkles size={13} style={{ marginRight: 4 }} />
            Session: {sprintCount} words · Studied: {Object.keys(srsMap).length}
          </span>
        </div>
      </div>

      <div className="row" style={{ justifyContent: 'space-between' }}>
        <div className="row">
          <button type="button" className={`chip ${studyMode === 'speed' ? 'active' : ''}`} onClick={() => setStudyMode('speed')}>
            <Zap size={14} /> Speed Learn (Fast)
          </button>
          <button type="button" className={`chip ${studyMode === 'flashcard' ? 'active' : ''}`} onClick={() => setStudyMode('flashcard')}>
            SM-2 Flashcards ({filteredDb.length.toLocaleString()})
          </button>
          <button type="button" className={`chip ${studyMode === 'multilingual' ? 'active' : ''}`} onClick={() => setStudyMode('multilingual')}>
            8-Language Core Deck
          </button>
        </div>
        {studyMode !== 'multilingual' && (
          <div className="row">
            <button type="button" className={`chip ${autoSpeak ? 'active' : ''}`} onClick={() => setAutoSpeak((v) => !v)}>
              <Volume2 size={14} /> Auto-Speak: {autoSpeak ? 'ON' : 'OFF'}
            </button>
            <button type="button" className="btn btn-sm" onClick={jumpRandom}>
              <Shuffle size={14} /> Random Word
            </button>
          </div>
        )}
      </div>

      {studyMode !== 'multilingual' && (
        <>
          <div className="row">
            {['ALL', 'IELTS', 'TOEFL', 'CET4', 'CET6', 'GRE', 'A2', 'B1', 'B2', 'C1'].map((t) => (
              <button
                key={t}
                type="button"
                className={`chip ${tagFilter === t ? 'active' : ''}`}
                onClick={() => {
                  setTagFilter(t);
                  setIndex(0);
                  setFlipped(false);
                }}
              >
                {t}
              </button>
            ))}
          </div>

          <div className="card row">
            <Search size={18} style={{ color: 'var(--muted)' }} />
            <input
              type="search"
              className="input"
              style={{ border: 'none', padding: '4px 6px', flex: 1 }}
              placeholder="Filter 12,000+ words by English, Burmese, or Chinese..."
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setIndex(0);
              }}
            />
            <span className="badge">{filteredDb.length.toLocaleString()} words</span>
          </div>

          {activeDbCard ? (
            <div className="stack">
              <div
                className="flashcard"
                role="button"
                tabIndex={0}
                onClick={() => setFlipped((f) => !f)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') setFlipped((f) => !f);
                }}
              >
                <div className="row" style={{ marginBottom: 8 }}>
                  <span className="badge">{activeDbCard[6]}</span>
                  {activeDbCard[5] && <span className="badge badge-amber">{activeDbCard[5].toUpperCase()}</span>}
                  <span className="badge">
                    #{(index % filteredDb.length) + 1} / {filteredDb.length.toLocaleString()}
                  </span>
                </div>

                <div className="flashcard-word">{activeDbCard[0]}</div>
                {activeDbCard[1] && <div style={{ color: 'var(--muted)', fontSize: '1rem' }}>/{activeDbCard[1]}/</div>}

                <button
                  type="button"
                  className="btn btn-sm"
                  style={{ marginTop: 12 }}
                  onClick={(e) => {
                    e.stopPropagation();
                    speakText(activeDbCard[0], 'en');
                  }}
                >
                  <Volume2 size={16} /> Pronounce
                </button>

                {studyMode === 'speed' || flipped ? (
                  <div style={{ marginTop: 18, maxWidth: 600, width: '100%' }}>
                    <div className="card" style={{ textAlign: 'left', padding: 14, display: 'flex', flexDirection: 'column', gap: 6 }}>
                      <div>
                        <span className="badge" style={{ marginRight: 8 }}>🇲🇲 MY</span>
                        <strong style={{ color: 'var(--accent)' }}>{activeDbCard[2]}</strong>
                      </div>
                      <div>
                        <span className="badge" style={{ marginRight: 8 }}>🇨🇳 ZH</span>
                        <span>{activeDbCard[3]}</span>
                      </div>
                      {activeDbCard[4] && (
                        <div>
                          <span className="badge" style={{ marginRight: 8 }}>🇬🇧 EN</span>
                          <span style={{ color: 'var(--muted)', fontSize: '0.9rem' }}>{activeDbCard[4]}</span>
                        </div>
                      )}
                    </div>
                  </div>
                ) : (
                  <p style={{ marginTop: 18, color: 'var(--muted)', fontSize: '0.85rem' }}>
                    Tap card or press Space to reveal MY · ZH · EN meanings
                  </p>
                )}
              </div>

              <div className="row" style={{ justifyContent: 'center' }}>
                <button type="button" className="btn" onClick={() => advanceDb(0)}>
                  <RotateCcw size={15} /> [1] Again
                </button>
                <button type="button" className="btn btn-primary" onClick={() => advanceDb(4)}>
                  <CheckCircle2 size={15} /> [2] Got It (+8 XP)
                </button>
                <button type="button" className="btn" onClick={() => advanceDb(5)}>
                  <Zap size={15} /> [3] Easy / Next
                </button>
              </div>
              <p style={{ textAlign: 'center', fontSize: '0.8rem', color: 'var(--muted)', margin: 0 }}>
                Speed shortcuts: <code>Space</code> flip · <code>1</code> Again · <code>2</code> / <code>→</code> Got It · <code>3</code> Easy
              </p>
            </div>
          ) : (
            <div className="card">Loading 12,000+ auto-generated vocabulary words…</div>
          )}
        </>
      )}

      {studyMode === 'multilingual' && (
        <div className="stack">
          <div className="row">
            <button type="button" className={`chip ${lang === 'all' ? 'active' : ''}`} onClick={() => { setLang('all'); setIndex(0); }}>
              All ({VOCAB_DECK.length})
            </button>
            {LANGUAGES.map((l) => (
              <button key={l.code} type="button" className={`chip ${lang === l.code ? 'active' : ''}`} onClick={() => { setLang(l.code); setIndex(0); }}>
                {l.flag} {l.name}
              </button>
            ))}
          </div>
          {multiCard && (
            <div className="flashcard" onClick={() => setFlipped((f) => !f)}>
              <span className="badge">{multiCard.language.toUpperCase()} · {multiCard.level}</span>
              <div className="flashcard-word">{multiCard.word}</div>
              <div style={{ color: 'var(--muted)' }}>{multiCard.reading}</div>
              <button
                type="button"
                className="btn btn-sm"
                style={{ marginTop: 12 }}
                onClick={(e) => {
                  e.stopPropagation();
                  speakText(multiCard.word, multiCard.language);
                }}
              >
                <Volume2 size={16} /> Pronounce
              </button>
              <div style={{ marginTop: 16 }}>
                <div style={{ fontWeight: 700 }}>{multiCard.meaningEn}</div>
                <div style={{ color: 'var(--accent)', fontWeight: 600 }}>{multiCard.meaningMy}</div>
              </div>
              <button
                type="button"
                className="btn btn-primary btn-sm"
                style={{ marginTop: 16 }}
                onClick={(e) => {
                  e.stopPropagation();
                  onGainXp(10);
                  setIndex((i) => i + 1);
                }}
              >
                Next Card (+10 XP)
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
