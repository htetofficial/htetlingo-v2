import { LANGUAGES, type LanguageCode } from '@htetlingo/core';

export function speakText(text: string, lang: LanguageCode = 'en', rate = 0.95): void {
  if (typeof window === 'undefined' || !('speechSynthesis' in window)) return;
  window.speechSynthesis.cancel();
  const utterance = new SpeechSynthesisUtterance(text);
  const info = LANGUAGES.find((l) => l.code === lang);
  utterance.lang = info?.bcp47 ?? 'en-US';
  utterance.rate = rate;
  window.speechSynthesis.speak(utterance);
}

interface SpeechRecognitionAlternativeLike {
  transcript: string;
  confidence: number;
}

interface SpeechRecognitionEventLike {
  results: ArrayLike<ArrayLike<SpeechRecognitionAlternativeLike>>;
}

interface SpeechRecognitionLike {
  lang: string;
  interimResults: boolean;
  maxAlternatives: number;
  onresult: ((e: SpeechRecognitionEventLike) => void) | null;
  onerror: ((e: { error: string }) => void) | null;
  onend: (() => void) | null;
  start: () => void;
  stop: () => void;
}

export function isSpeechRecognitionSupported(): boolean {
  if (typeof window === 'undefined') return false;
  const w = window as unknown as Record<string, unknown>;
  return Boolean(w.SpeechRecognition || w.webkitSpeechRecognition);
}

export function listenOnce(
  lang: LanguageCode,
  onResult: (transcript: string, confidence: number) => void,
  onError: (msg: string) => void
): () => void {
  const w = window as unknown as Record<string, new () => SpeechRecognitionLike>;
  const Ctor = w.SpeechRecognition ?? w.webkitSpeechRecognition;
  if (!Ctor) {
    onError('Speech recognition is not supported in this browser.');
    return () => {};
  }
  const rec = new Ctor();
  const info = LANGUAGES.find((l) => l.code === lang);
  rec.lang = info?.bcp47 ?? 'en-US';
  rec.interimResults = false;
  rec.maxAlternatives = 1;

  rec.onresult = (e) => {
    const first = e.results?.[0]?.[0];
    if (first) onResult(first.transcript, first.confidence ?? 0.85);
  };
  rec.onerror = (e) => onError(e.error || 'Recognition failed');
  rec.start();
  return () => {
    try {
      rec.stop();
    } catch {}
  };
}
