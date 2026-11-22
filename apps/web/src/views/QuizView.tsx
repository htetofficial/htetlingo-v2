import { useEffect, useState } from 'react';
import { Volume2, CheckCircle2, XCircle, RotateCcw, Sparkles } from 'lucide-react';
import { QUIZ_QUESTIONS, type QuizQuestion } from '@htetlingo/core';
import { speakText } from '../lib/speech.ts';

type UnifiedVocabTuple = [string, string, string, string, string, string, string];

interface QuizViewProps {
  onCompleteQuiz: (score: number, total: number, xp: number) => void;
}

function shuffle<T>(arr: T[]): T[] {
  const copy = [...arr];
  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [copy[i], copy[j]] = [copy[j], copy[i]];
  }
  return copy;
}

function generateQuizFromDb(
  pool: UnifiedVocabTuple[],
  mode: 'en-my' | 'en-zh' | 'listening',
  count = 10
): QuizQuestion[] {
  if (pool.length < 10) return QUIZ_QUESTIONS;
  const picked = shuffle(pool).slice(0, count);
  return picked.map((entry, idx) => {
    const [word, ipa, myDef, zhDef] = entry;
    const distractors = shuffle(pool.filter((p) => p[0] !== word)).slice(0, 3);
    if (mode === 'en-zh') {
      const options = shuffle([zhDef, ...distractors.map((d) => d[3])]);
      return {
        id: `auto-zh-${idx}-${word}`,
        language: 'zh',
        type: 'mcq',
        prompt: `Select the Chinese meaning of "${word}" ${ipa ? `/${ipa}/` : ''}:`,
        options,
        answer: zhDef,
        explanation: `${word} → 🇨🇳 ${zhDef} | 🇲🇲 ${myDef}`,
      };
    }
    if (mode === 'listening') {
      const options = shuffle([word, ...distractors.map((d) => d[0])]);
      return {
        id: `auto-listen-${idx}-${word}`,
        language: 'en',
        type: 'listening',
        prompt: 'Listen carefully and choose the exact English word you heard:',
        audioText: word,
        options,
        answer: word,
        explanation: `${word} (${ipa || 'EN'}) → 🇲🇲 ${myDef}`,
      };
    }
    const options = shuffle([myDef, ...distractors.map((d) => d[2])]);
    return {
      id: `auto-my-${idx}-${word}`,
      language: 'en',
      type: 'mcq',
      prompt: `Choose the Burmese meaning of "${word}" ${ipa ? `/${ipa}/` : ''}:`,
      options,
      answer: myDef,
      explanation: `${word} → 🇲🇲 ${myDef} | 🇨🇳 ${zhDef}`,
    };
  });
}

export function QuizView({ onCompleteQuiz }: QuizViewProps) {
  const [pool, setPool] = useState<UnifiedVocabTuple[]>([]);
  const [quizMode, setQuizMode] = useState<'en-my' | 'en-zh' | 'listening'>('en-my');
  const [questions, setQuestions] = useState<QuizQuestion[]>(QUIZ_QUESTIONS);
  const [step, setStep] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [score, setScore] = useState(0);
  const [finished, setFinished] = useState(false);

  useEffect(() => {
    fetch('/datasets/vocab-core.json')
      .then((r) => r.json())
      .then((d: { items?: UnifiedVocabTuple[] }) => {
        if (d.items && d.items.length > 20) {
          setPool(d.items);
          setQuestions(generateQuizFromDb(d.items, 'en-my', 10));
        }
      })
      .catch(() => {});
  }, []);

  const startNewQuiz = (mode: 'en-my' | 'en-zh' | 'listening') => {
    setQuizMode(mode);
    setQuestions(generateQuizFromDb(pool, mode, 10));
    setStep(0);
    setSelected(null);
    setScore(0);
    setFinished(false);
  };

  const q = questions[step];

  const handleAnswer = (opt: string) => {
    if (selected || !q) return;
    setSelected(opt);
    if (opt === q.answer) setScore((s) => s + 1);
  };

  const handleNext = () => {
    if (step + 1 >= questions.length) {
      setFinished(true);
      onCompleteQuiz(score, questions.length, score * 15);
    } else {
      setStep((s) => s + 1);
      setSelected(null);
    }
  };

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Auto-Generated Infinite Quiz</h2>
          <p>Fresh 10-question quizzes generated dynamically from {pool.length.toLocaleString() || '12,000'} dictionary words.</p>
        </div>
        <div className="row">
          <button type="button" className={`chip ${quizMode === 'en-my' ? 'active' : ''}`} onClick={() => startNewQuiz('en-my')}>
            🇲🇲 EN → MY
          </button>
          <button type="button" className={`chip ${quizMode === 'en-zh' ? 'active' : ''}`} onClick={() => startNewQuiz('en-zh')}>
            🇨🇳 EN → ZH
          </button>
          <button type="button" className={`chip ${quizMode === 'listening' ? 'active' : ''}`} onClick={() => startNewQuiz('listening')}>
            🎧 Listening
          </button>
        </div>
      </div>

      <div className="progress-bar">
        <div
          className="progress-fill"
          style={{ width: `${((finished ? questions.length : step) / Math.max(questions.length, 1)) * 100}%` }}
        />
      </div>

      {finished ? (
        <div className="card stack" style={{ alignItems: 'center', textAlign: 'center', padding: 32 }}>
          <CheckCircle2 size={44} style={{ color: 'var(--success)' }} />
          <h3 style={{ margin: 0, fontSize: '1.6rem' }}>
            Score: {score} / {questions.length}
          </h3>
          <p style={{ margin: 0, color: 'var(--muted)' }}>
            You earned +{score * 15} XP toward your daily goal!
          </p>
          <button type="button" className="btn btn-primary" onClick={() => startNewQuiz(quizMode)}>
            <Sparkles size={16} /> Generate New 10-Word Quiz
          </button>
        </div>
      ) : q ? (
        <div className="card stack">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <div className="row">
              <span className="badge">{q.language.toUpperCase()}</span>
              <span className="badge badge-amber">{q.type.toUpperCase()}</span>
            </div>
            <span className="badge">
              Question {step + 1} / {questions.length}
            </span>
          </div>
          <h3 style={{ margin: 0 }}>{q.prompt}</h3>

          {q.audioText && (
            <div>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => speakText(q.audioText!, q.language, 0.88)}
              >
                <Volume2 size={18} /> Play Audio Prompt
              </button>
            </div>
          )}

          <div className="stack" style={{ gap: 10 }}>
            {q.options.map((opt) => {
              const isChosen = selected === opt;
              const isCorrect = opt === q.answer;
              let border = 'var(--border)';
              if (selected) {
                if (isCorrect) border = 'var(--success)';
                else if (isChosen) border = 'var(--danger)';
              }

              return (
                <button
                  key={opt}
                  type="button"
                  className="btn"
                  style={{
                    justifyContent: 'space-between',
                    textAlign: 'left',
                    padding: '12px 14px',
                    borderColor: border,
                    borderWidth: selected && (isCorrect || isChosen) ? 2 : 1,
                  }}
                  onClick={() => handleAnswer(opt)}
                >
                  <span>{opt}</span>
                  {selected && isCorrect && <CheckCircle2 size={18} style={{ color: 'var(--success)' }} />}
                  {selected && isChosen && !isCorrect && <XCircle size={18} style={{ color: 'var(--danger)' }} />}
                </button>
              );
            })}
          </div>

          {selected && (
            <div className="card row" style={{ justifyContent: 'space-between', background: 'var(--surface-2)' }}>
              <span style={{ fontSize: '0.9rem' }}>{q.explanation}</span>
              <button type="button" className="btn btn-primary btn-sm" onClick={handleNext}>
                {step + 1 >= questions.length ? 'Finish' : 'Next'}
              </button>
            </div>
          )}

          <div className="row" style={{ justifyContent: 'flex-end' }}>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => startNewQuiz(quizMode)}>
              <RotateCcw size={14} /> New Random Set
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
