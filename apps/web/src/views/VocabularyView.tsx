import { useMemo, useState } from 'react';
import { Volume2, RotateCcw, Sparkles } from 'lucide-react';
import { LANGUAGES, VOCAB_DECK, reviewCard, type LanguageCode, type SrsState } from '@htetlingo/core';
import { speakText } from '../lib/speech.ts';

interface VocabularyViewProps {
  srsMap: Record<string, SrsState>;
  onReview: (cardId: string, next: SrsState) => void;
  onGainXp: (xp: number) => void;
}

export function VocabularyView({ srsMap, onReview, onGainXp }: VocabularyViewProps) {
  const [lang, setLang] = useState<LanguageCode | 'all'>('all');
  const [index, setIndex] = useState(0);
  const [flipped, setFlipped] = useState(false);

  const cards = useMemo(
    () => (lang === 'all' ? VOCAB_DECK : VOCAB_DECK.filter((c) => c.language === lang)),
    [lang]
  );

  const card = cards[index % Math.max(cards.length, 1)];
  const srs = card ? srsMap[card.id] : undefined;

  const handleGrade = (quality: 0 | 3 | 4 | 5) => {
    if (!card) return;
    const next = reviewCard(srs, card.id, quality);
    onReview(card.id, next);
    onGainXp(quality >= 4 ? 10 : 5);
    setFlipped(false);
    setIndex((i) => (i + 1) % cards.length);
  };

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Vocabulary & Spaced Repetition (SM-2)</h2>
          <p>Tap a flashcard to reveal Burmese & English meanings, then rate your recall.</p>
        </div>
        <span className="badge">
          <Sparkles size={13} style={{ marginRight: 4 }} />
          {Object.keys(srsMap).length} / {VOCAB_DECK.length} studied
        </span>
      </div>

      <div className="row">
        <button
          type="button"
          className={`chip ${lang === 'all' ? 'active' : ''}`}
          onClick={() => {
            setLang('all');
            setIndex(0);
            setFlipped(false);
          }}
        >
          All ({VOCAB_DECK.length})
        </button>
        {LANGUAGES.map((l) => (
          <button
            key={l.code}
            type="button"
            className={`chip ${lang === l.code ? 'active' : ''}`}
            onClick={() => {
              setLang(l.code);
              setIndex(0);
              setFlipped(false);
            }}
          >
            {l.flag} {l.name}
          </button>
        ))}
      </div>

      {card && (
        <div className="stack">
          <div
            className="flashcard"
            role="button"
            tabIndex={0}
            onClick={() => setFlipped((f) => !f)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                setFlipped((f) => !f);
              }
            }}
          >
            <div className="row" style={{ marginBottom: 8 }}>
              <span className="badge">{card.language.toUpperCase()} · {card.level}</span>
              <span className="badge badge-amber">{card.partOfSpeech}</span>
              {srs && <span className="badge">Next: {srs.intervalDays}d</span>}
            </div>
            <div className="flashcard-word">{card.word}</div>
            <div style={{ color: 'var(--muted)', fontSize: '1rem' }}>{card.reading}</div>

            <button
              type="button"
              className="btn btn-sm"
              style={{ marginTop: 14 }}
              onClick={(e) => {
                e.stopPropagation();
                speakText(card.word, card.language);
              }}
            >
              <Volume2 size={16} /> Pronounce
            </button>

            {flipped ? (
              <div style={{ marginTop: 20, maxWidth: 520 }}>
                <p style={{ margin: '0 0 6px', fontWeight: 700, fontSize: '1.1rem' }}>{card.meaningEn}</p>
                <p style={{ margin: '0 0 12px', color: 'var(--accent)', fontWeight: 600 }}>{card.meaningMy}</p>
                <div className="card" style={{ textAlign: 'left', padding: 12 }}>
                  <div style={{ fontWeight: 600 }}>{card.example}</div>
                  <div style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>{card.exampleTranslation}</div>
                </div>
              </div>
            ) : (
              <p style={{ marginTop: 18, color: 'var(--muted)', fontSize: '0.85rem' }}>
                Click or press Space to flip card
              </p>
            )}
          </div>

          <div className="row" style={{ justifyContent: 'center' }}>
            <button type="button" className="btn" onClick={() => handleGrade(0)}>
              <RotateCcw size={15} /> Again (1d)
            </button>
            <button type="button" className="btn" onClick={() => handleGrade(3)}>
              Hard
            </button>
            <button type="button" className="btn btn-primary" onClick={() => handleGrade(4)}>
              Good (+10 XP)
            </button>
            <button type="button" className="btn" onClick={() => handleGrade(5)}>
              Easy
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
