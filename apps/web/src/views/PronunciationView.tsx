import { useRef, useState } from 'react';
import { Mic, MicOff, Volume2 } from 'lucide-react';
import { LANGUAGES, type LanguageCode } from '@htetlingo/core';
import { isSpeechRecognitionSupported, listenOnce, speakText } from '../lib/speech.ts';

const PRACTICE_PHRASES: Record<LanguageCode, string[]> = {
  en: ['Learn languages, understand words, connect cultures.', 'Practice pronunciation every single day.'],
  my: ['မင်္ဂလာပါ နေကောင်းလား', 'ဘာသာစကားသင်ယူခြင်းသည် ပျော်စရာကောင်းသည်။'],
  ja: ['新しい言葉を毎日学びます。', 'おはようございます、お元気ですか。'],
  zh: ['我每天学习中文。', '学习新语言非常有趣。'],
  ko: ['안녕하세요, 만나서 반갑습니다.', '한국어 발음을 연습하고 있어요.'],
  th: ['สวัสดีครับ ยินดีที่ได้รู้จัก'],
  fr: ['Bonjour, je veux apprendre le français.'],
  de: ['Sprache verbindet Menschen weltweit.'],
};

interface PronunciationViewProps {
  onGainXp: (xp: number) => void;
}

export function PronunciationView({ onGainXp }: PronunciationViewProps) {
  const [lang, setLang] = useState<LanguageCode>('en');
  const [text, setText] = useState(PRACTICE_PHRASES.en[0]);
  const [rate, setRate] = useState(0.95);
  const [listening, setListening] = useState(false);
  const [heard, setHeard] = useState('');
  const [error, setError] = useState('');
  const stopRef = useRef<(() => void) | null>(null);

  const recSupported = isSpeechRecognitionSupported();

  const toggleMic = () => {
    if (listening) {
      stopRef.current?.();
      setListening(false);
      return;
    }
    setError('');
    setHeard('');
    setListening(true);
    stopRef.current = listenOnce(
      lang,
      (transcript) => {
        setHeard(transcript);
        setListening(false);
        onGainXp(10);
      },
      (err) => {
        setError(err);
        setListening(false);
      }
    );
  };

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Pronunciation & Listening Studio</h2>
          <p>Native voice synthesis with adjustable speed and live microphone speaking practice.</p>
        </div>
      </div>

      <div className="row">
        {LANGUAGES.map((l) => (
          <button
            key={l.code}
            type="button"
            className={`chip ${lang === l.code ? 'active' : ''}`}
            onClick={() => {
              setLang(l.code);
              setText(PRACTICE_PHRASES[l.code][0]);
              setHeard('');
              setError('');
            }}
          >
            {l.flag} {l.name}
          </button>
        ))}
      </div>

      <div className="card stack">
        <label htmlFor="pron-input" style={{ fontWeight: 600 }}>
          Practice Phrase
        </label>
        <textarea
          id="pron-input"
          className="textarea"
          rows={3}
          value={text}
          onChange={(e) => setText(e.target.value)}
        />

        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div className="row">
            <button type="button" className="btn btn-primary" onClick={() => speakText(text, lang, rate)}>
              <Volume2 size={18} /> Listen ({rate.toFixed(2)}x)
            </button>
            <button
              type="button"
              className="btn"
              onClick={toggleMic}
              disabled={!recSupported}
            >
              {listening ? <MicOff size={18} /> : <Mic size={18} />}
              {listening ? 'Stop Listening' : 'Speak & Check'}
            </button>
          </div>
          <label className="row" style={{ fontSize: '0.86rem', color: 'var(--muted)' }}>
            Speed
            <input
              type="range"
              min="0.65"
              max="1.25"
              step="0.05"
              value={rate}
              onChange={(e) => setRate(Number(e.target.value))}
            />
          </label>
        </div>

        {!recSupported && (
          <p style={{ margin: 0, fontSize: '0.85rem', color: 'var(--muted)' }}>
            Microphone recognition is not available in this browser; listening TTS works fully.
          </p>
        )}

        {heard && (
          <div className="card" style={{ background: 'var(--surface-2)', padding: 12 }}>
            <div style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>Recognized Speech (+10 XP)</div>
            <div style={{ fontWeight: 700, marginTop: 4 }}>{heard}</div>
          </div>
        )}
        {error && <p style={{ color: 'var(--danger)', margin: 0 }}>{error}</p>}
      </div>

      <div className="card stack">
        <h3 style={{ margin: 0, fontSize: '1rem' }}>Quick Phrases</h3>
        <div className="stack" style={{ gap: 8 }}>
          {PRACTICE_PHRASES[lang].map((phrase) => (
            <button
              key={phrase}
              type="button"
              className="btn"
              style={{ justifyContent: 'space-between' }}
              onClick={() => {
                setText(phrase);
                speakText(phrase, lang, rate);
              }}
            >
              <span>{phrase}</span>
              <Volume2 size={16} />
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
