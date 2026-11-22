import { useState } from 'react';
import { Lock, Volume2, AlertTriangle, CheckCircle2 } from 'lucide-react';
import { GRAMMAR_LESSONS, LANGUAGES, type LanguageCode } from '@htetlingo/core';
import { speakText } from '../lib/speech.ts';
import type { Tier } from '../lib/revenuecat.ts';

interface GrammarViewProps {
  tier: Tier;
  onOpenPaywall: () => void;
  onGainXp: (xp: number) => void;
}

export function GrammarView({ tier, onOpenPaywall, onGainXp }: GrammarViewProps) {
  const [lang, setLang] = useState<LanguageCode | 'all'>('all');
  const [completed, setCompleted] = useState<string[]>([]);

  const lessons =
    lang === 'all' ? GRAMMAR_LESSONS : GRAMMAR_LESSONS.filter((l) => l.language === lang);

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Grammar Studio</h2>
          <p>Clear structure formulas, Burmese explanations, and common error fixes.</p>
        </div>
      </div>

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
    </div>
  );
}
