import { useState } from 'react';
import { ArrowLeftRight, Languages, Volume2 } from 'lucide-react';
import { LANGUAGES, OFFLINE_PHRASEBOOK, type LanguageCode } from '@htetlingo/core';
import { speakText } from '../lib/speech.ts';

interface TranslateViewProps {
  onGainXp: (xp: number) => void;
}

export function TranslateView({ onGainXp }: TranslateViewProps) {
  const [source, setSource] = useState<LanguageCode>('en');
  const [target, setTarget] = useState<LanguageCode>('my');
  const [input, setInput] = useState('Hello');
  const [output, setOutput] = useState('မင်္ဂလာပါ');
  const [provider, setProvider] = useState('offline-phrasebook');
  const [loading, setLoading] = useState(false);

  const handleTranslate = async () => {
    const cleaned = input.trim();
    if (!cleaned) return;

    // 1. Instant offline phrasebook check
    const key = cleaned.toLowerCase().replace(/[?.!]+$/, '');
    const phrase = OFFLINE_PHRASEBOOK[key];
    if (phrase && phrase[target]) {
      setOutput(phrase[target]);
      setProvider('offline-phrasebook');
      onGainXp(3);
      return;
    }

    // 2. Online translation via Cloudflare Pages Function (/api/translate) with direct fallback
    setLoading(true);
    try {
      let translated = '';
      const res = await fetch('/api/translate', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ text: cleaned, source, target }),
      });
      if (res.ok) {
        const data = (await res.json()) as { translatedText?: string; provider?: string };
        translated = data.translatedText ?? '';
        setProvider(data.provider ?? 'cloudflare-api');
      } else {
        throw new Error('api unavailable');
      }
      setOutput(translated || 'No translation returned.');
      onGainXp(5);
    } catch {
      try {
        const fallback = await fetch(
          `https://api.mymemory.translated.net/get?q=${encodeURIComponent(cleaned)}&langpair=${source}|${target}`
        );
        const data = (await fallback.json()) as { responseData?: { translatedText?: string } };
        setOutput(data.responseData?.translatedText ?? 'Offline: phrase not in local phrasebook.');
        setProvider('direct-fallback');
      } catch {
        setOutput('Offline mode: connect to the internet for full sentences, or try quick phrases below.');
        setProvider('offline');
      }
    } finally {
      setLoading(false);
    }
  };

  const swap = () => {
    setSource(target);
    setTarget(source);
    setInput(output);
    setOutput(input);
  };

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Multilingual Translation</h2>
          <p>Cloudflare Pages serverless translation with instant offline phrasebook fallback.</p>
        </div>
        <span className="badge">{provider}</span>
      </div>

      <div className="card stack">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <select
            className="select"
            style={{ flex: 1, minWidth: 140 }}
            value={source}
            onChange={(e) => setSource(e.target.value as LanguageCode)}
            aria-label="Source language"
          >
            {LANGUAGES.map((l) => (
              <option key={l.code} value={l.code}>
                {l.flag} {l.name} ({l.nativeName})
              </option>
            ))}
          </select>

          <button type="button" className="btn" onClick={swap} aria-label="Swap languages">
            <ArrowLeftRight size={16} />
          </button>

          <select
            className="select"
            style={{ flex: 1, minWidth: 140 }}
            value={target}
            onChange={(e) => setTarget(e.target.value as LanguageCode)}
            aria-label="Target language"
          >
            {LANGUAGES.map((l) => (
              <option key={l.code} value={l.code}>
                {l.flag} {l.name} ({l.nativeName})
              </option>
            ))}
          </select>
        </div>

        <div className="grid-2">
          <div className="stack" style={{ gap: 8 }}>
            <textarea
              className="textarea"
              rows={5}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Enter text to translate..."
            />
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <button type="button" className="btn btn-sm" onClick={() => speakText(input, source)}>
                <Volume2 size={15} /> Speak Source
              </button>
              <button type="button" className="btn btn-primary" onClick={handleTranslate} disabled={loading}>
                <Languages size={16} /> {loading ? 'Translating…' : 'Translate'}
              </button>
            </div>
          </div>

          <div className="stack" style={{ gap: 8 }}>
            <div
              className="card"
              style={{ minHeight: 132, background: 'var(--surface-2)', fontSize: '1.08rem', fontWeight: 500 }}
            >
              {output}
            </div>
            <div className="row">
              <button type="button" className="btn btn-sm" onClick={() => speakText(output, target)}>
                <Volume2 size={15} /> Speak Translation
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="card stack">
        <h3 style={{ margin: 0, fontSize: '1rem' }}>Offline Phrasebook Quick-Tap</h3>
        <div className="row">
          {Object.keys(OFFLINE_PHRASEBOOK).map((phrase) => (
            <button
              key={phrase}
              type="button"
              className="chip"
              onClick={() => {
                setSource('en');
                setInput(phrase);
                const match = OFFLINE_PHRASEBOOK[phrase]?.[target];
                if (match) {
                  setOutput(match);
                  setProvider('offline-phrasebook');
                }
              }}
            >
              {phrase}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
