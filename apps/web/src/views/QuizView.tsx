import { useState } from 'react';
import { Volume2, CheckCircle2, XCircle, RotateCcw } from 'lucide-react';
import { QUIZ_QUESTIONS } from '@htetlingo/core';
import { speakText } from '../lib/speech.ts';

interface QuizViewProps {
  onCompleteQuiz: (score: number, total: number, xp: number) => void;
}

export function QuizView({ onCompleteQuiz }: QuizViewProps) {
  const [step, setStep] = useState(0);
  const [selected, setSelected] = useState<string | null>(null);
  const [score, setScore] = useState(0);
  const [finished, setFinished] = useState(false);

  const q = QUIZ_QUESTIONS[step];

  const handleAnswer = (opt: string) => {
    if (selected) return;
    setSelected(opt);
    if (opt === q.answer) {
      setScore((s) => s + 1);
    }
  };

  const handleNext = () => {
    if (step + 1 >= QUIZ_QUESTIONS.length) {
      setFinished(true);
      const finalScore = score;
      onCompleteQuiz(finalScore, QUIZ_QUESTIONS.length, finalScore * 15);
    } else {
      setStep((s) => s + 1);
      setSelected(null);
    }
  };

  const restart = () => {
    setStep(0);
    setSelected(null);
    setScore(0);
    setFinished(false);
  };

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Quiz & Exercises</h2>
          <p>Multiple-choice, grammar accuracy, and listening comprehension drills.</p>
        </div>
        <span className="badge">
          Question {Math.min(step + 1, QUIZ_QUESTIONS.length)} / {QUIZ_QUESTIONS.length}
        </span>
      </div>

      <div className="progress-bar">
        <div
          className="progress-fill"
          style={{ width: `${((finished ? QUIZ_QUESTIONS.length : step) / QUIZ_QUESTIONS.length) * 100}%` }}
        />
      </div>

      {finished ? (
        <div className="card stack" style={{ alignItems: 'center', textAlign: 'center', padding: 32 }}>
          <CheckCircle2 size={44} style={{ color: 'var(--success)' }} />
          <h3 style={{ margin: 0, fontSize: '1.6rem' }}>
            Score: {score} / {QUIZ_QUESTIONS.length}
          </h3>
          <p style={{ margin: 0, color: 'var(--muted)' }}>
            You earned +{score * 15} XP toward your daily goal!
          </p>
          <button type="button" className="btn btn-primary" onClick={restart}>
            <RotateCcw size={16} /> Practice Again
          </button>
        </div>
      ) : (
        <div className="card stack">
          <div className="row">
            <span className="badge">{q.language.toUpperCase()}</span>
            <span className="badge badge-amber">{q.type.toUpperCase()}</span>
          </div>
          <h3 style={{ margin: 0 }}>{q.prompt}</h3>

          {q.audioText && (
            <div>
              <button
                type="button"
                className="btn btn-primary"
                onClick={() => speakText(q.audioText!, q.language, 0.85)}
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
                {step + 1 >= QUIZ_QUESTIONS.length ? 'Finish' : 'Next'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
