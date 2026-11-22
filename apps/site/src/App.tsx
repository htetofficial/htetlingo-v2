import { useEffect, useRef, useState } from 'react';
import {
  BookA,
  Laptop,
  Smartphone,
  Globe,
  ExternalLink,
  Search,
  Volume2,
  CheckCircle2,
  FileText,
  Sparkles,
} from 'lucide-react';
import { LANGUAGES, OPTIONAL_PACKS, SUBSCRIPTION_PLANS, VOCAB_DECK } from '@htetlingo/core';

type Section = 'landing' | 'demo' | 'docs' | 'intro';

interface DemoEntry {
  word: string;
  html: string;
}

let domPurifyPromise: Promise<typeof import('dompurify')['default']> | undefined;
function loadDOMPurify() {
  domPurifyPromise ??= import('dompurify').then((m) => m.default ?? (m as unknown as typeof import('dompurify')['default']));
  return domPurifyPromise;
}

function SafeDemoHtml({ html }: { html: string }) {
  const ref = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const elements = ['b', 'i', 'br', 'strong', 'em', 'p', 'span'];
    const attributes = ['class'];
    const maybe = el as HTMLElement & { setHTML?: (h: string, o?: unknown) => void };
    if ('setHTML' in Element.prototype && typeof maybe.setHTML === 'function') {
      maybe.setHTML(html, { sanitizer: { elements, attributes } });
    } else {
      loadDOMPurify().then((DOMPurify) => {
        el.innerHTML = DOMPurify.sanitize(html, {
          ALLOWED_TAGS: [...elements, '#text'],
          ALLOWED_ATTR: attributes,
          ALLOW_ARIA_ATTR: false,
          ALLOW_DATA_ATTR: false,
          KEEP_CONTENT: false,
        });
      });
    }
  }, [html]);
  return <div ref={ref} style={{ lineHeight: 1.7 }} />;
}

const FEATURES = [
  'Vocabulary (SM-2 Spaced Repetition)',
  'Grammar Lessons & Common Error Fixes',
  'Interactive Quizzes & Listening Drills',
  'Pronunciation & Speech Recognition',
  '111,232-Word Ornagai EN–MY Dictionary',
  'Multilingual Translation + Offline Phrasebook',
  'Offline Learning Packs (IndexedDB & SQLite FTS5)',
  'Learning Progress, Streaks & Daily XP',
  'Supabase User Accounts + RevenueCat Plans',
];

const OPEN_SOURCES = [
  'Ornagai EN–MY Dictionary (111,232 headwords, 249,103 examples)',
  'dwyl/english-words & LexiVerse / reader-dict monolingual dictionaries',
  'mcfnlp/Dictionary, myanmartools/myanmar-words, kanaung/wordlists',
  'codeyu/EnglishGrammarBook, max-mapper/common-english-errors, grammar-forge-english',
  'YeeNyunn/nara-mini-v1 & Ye-Bhone-Lin EN–MY translation datasets',
];

export function App() {
  const [section, setSection] = useState<Section>('landing');
  const [demoWords, setDemoWords] = useState<DemoEntry[]>([]);
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<DemoEntry | null>(null);
  const [cardIdx, setCardIdx] = useState(0);
  const [flipped, setFlipped] = useState(false);

  useEffect(() => {
    fetch(`${import.meta.env.BASE_URL}dict/demo.json`)
      .then((r) => r.json())
      .then((data: DemoEntry[]) => {
        setDemoWords(data);
        if (data[0]) setSelected(data[0]);
      })
      .catch(() => {});
  }, []);

  const filtered = query.trim()
    ? demoWords.filter((d) => d.word.toLowerCase().includes(query.trim().toLowerCase())).slice(0, 20)
    : demoWords.slice(0, 18);

  const speak = (text: string, lang = 'en-US') => {
    if (!('speechSynthesis' in window)) return;
    window.speechSynthesis.cancel();
    const u = new SpeechSynthesisUtterance(text);
    u.lang = lang;
    window.speechSynthesis.speak(u);
  };

  const sampleCard = VOCAB_DECK[cardIdx % VOCAB_DECK.length];

  return (
    <>
      <header className="site-header">
        <div className="container header-inner">
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, fontWeight: 800 }}>
            <span
              style={{
                width: 34,
                height: 34,
                borderRadius: 10,
                background: 'linear-gradient(135deg,#0f766e,#1e3a8a)',
                color: '#fff',
                display: 'grid',
                placeItems: 'center',
              }}
            >
              HL
            </span>
            <span>HtetLingo</span>
          </div>

          <nav className="nav-links" aria-label="Site sections">
            <button type="button" className="tab-btn" aria-current={section === 'landing' ? 'page' : undefined} onClick={() => setSection('landing')}>
              Home
            </button>
            <button type="button" className="tab-btn" aria-current={section === 'demo' ? 'page' : undefined} onClick={() => setSection('demo')}>
              Static Demo
            </button>
            <button type="button" className="tab-btn" aria-current={section === 'docs' ? 'page' : undefined} onClick={() => setSection('docs')}>
              Documentation
            </button>
            <button type="button" className="tab-btn" aria-current={section === 'intro' ? 'page' : undefined} onClick={() => setSection('intro')}>
              Project Intro
            </button>
            <a className="btn btn-primary" href="https://htetlingo.pages.dev" target="_blank" rel="noreferrer">
              Launch Web App <ExternalLink size={15} />
            </a>
          </nav>
        </div>
      </header>

      <main className="container" style={{ paddingTop: 28 }}>
        {section === 'landing' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 36 }}>
            <section className="hero">
              <span className="badge">Multilingual Learning · Dictionary · Translation</span>
              <h1>Learn Languages. Understand Words. Connect Cultures.</h1>
              <p>
                HtetLingo is a unified, offline-first language platform for English, Burmese, Japanese, Chinese,
                Korean, Thai, French, and German — built natively for Mac, Android (Kotlin/Compose), Web, and iPad PWA.
              </p>
              <div style={{ display: 'flex', justifyContent: 'center', gap: 12, flexWrap: 'wrap' }}>
                <a className="btn btn-primary" href="https://htetlingo.pages.dev" target="_blank" rel="noreferrer">
                  Open HtetLingo Web App <ExternalLink size={16} />
                </a>
                <button type="button" className="btn" onClick={() => setSection('demo')}>
                  <Sparkles size={16} /> Try Static Demo
                </button>
                <button type="button" className="btn" onClick={() => setSection('docs')}>
                  <FileText size={16} /> Read Docs
                </button>
              </div>
              <div style={{ display: 'flex', justifyContent: 'center', gap: 8, flexWrap: 'wrap', marginTop: 22 }}>
                {LANGUAGES.map((l) => (
                  <span key={l.code} className="badge">
                    {l.flag} {l.name} ({l.nativeName})
                  </span>
                ))}
              </div>
            </section>

            <section className="grid-3">
              <div className="card">
                <Laptop size={24} style={{ color: 'var(--accent)' }} />
                <h3>Mac Native (SwiftUI)</h3>
                <p style={{ color: 'var(--muted)' }}>
                  Native macOS SwiftUI experience with SQLite FTS5 Ornagai dictionary search and AVSpeechSynthesizer pronunciation.
                </p>
              </div>
              <div className="card">
                <Smartphone size={24} style={{ color: 'var(--accent)' }} />
                <h3>Android (Kotlin + Compose)</h3>
                <p style={{ color: 'var(--muted)' }}>
                  Material 3 Jetpack Compose Android app with local SQLite FTS5 dictionary, native TextToSpeech, and RevenueCat billing.
                </p>
              </div>
              <div className="card">
                <Globe size={24} style={{ color: 'var(--accent)' }} />
                <h3>Web App & iPad PWA</h3>
                <p style={{ color: 'var(--muted)' }}>
                  Hosted on Cloudflare Pages Free plan with Service Worker, IndexedDB offline dictionary shards, and Supabase RLS sync.
                </p>
              </div>
            </section>

            <section className="card">
              <h2 style={{ marginTop: 0 }}>Everything Included in HtetLingo</h2>
              <div className="grid-3">
                {FEATURES.map((f) => (
                  <div key={f} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <CheckCircle2 size={18} style={{ color: 'var(--accent)', flexShrink: 0 }} />
                    <span>{f}</span>
                  </div>
                ))}
              </div>
            </section>

            <section>
              <h2>Flexible Business Model</h2>
              <div className="grid-3">
                {SUBSCRIPTION_PLANS.map((p) => (
                  <div key={p.id} className="card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <strong>{p.name}</strong>
                      {p.badge && <span className="badge">{p.badge}</span>}
                    </div>
                    <div style={{ fontSize: '1.8rem', fontWeight: 800, margin: '8px 0' }}>
                      {p.price} <span style={{ fontSize: '0.85rem', color: 'var(--muted)' }}>{p.cadence}</span>
                    </div>
                    <ul style={{ paddingLeft: 18, color: 'var(--muted)', fontSize: '0.9rem' }}>
                      {p.features.map((feat) => (
                        <li key={feat}>{feat}</li>
                      ))}
                    </ul>
                  </div>
                ))}
              </div>
              <div className="grid-2" style={{ marginTop: 16 }}>
                {OPTIONAL_PACKS.map((pack) => (
                  <div key={pack.id} className="card">
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <strong>{pack.title}</strong>
                      <span className="badge">{pack.price}</span>
                    </div>
                    <p style={{ color: 'var(--muted)', marginBottom: 0 }}>{pack.subtitle}</p>
                  </div>
                ))}
              </div>
            </section>
          </div>
        )}

        {section === 'demo' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 22 }}>
            <div>
              <span className="badge">Interactive Static Demo (GitHub Pages)</span>
              <h2 style={{ marginBottom: 4 }}>Try Ornagai Dictionary & Flashcards Right Here</h2>
              <p style={{ color: 'var(--muted)', marginTop: 0 }}>
                Zero backend required — running directly on GitHub Pages. Launch the Cloudflare Pages Web App for all 111,232 words!
              </p>
            </div>

            <div className="grid-2">
              <div className="card">
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
                  <Search size={18} style={{ color: 'var(--muted)' }} />
                  <input
                    type="search"
                    className="input"
                    placeholder="Search demo dictionary (e.g., language, culture, resilience)..."
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                  />
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginBottom: 14 }}>
                  {filtered.map((item) => (
                    <button
                      key={item.word}
                      type="button"
                      className="tab-btn"
                      aria-current={selected?.word === item.word ? 'page' : undefined}
                      onClick={() => setSelected(item)}
                    >
                      {item.word}
                    </button>
                  ))}
                </div>
                {selected && (
                  <div className="card" style={{ background: 'var(--surface-2)' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <h3 style={{ margin: 0 }}>{selected.word}</h3>
                      <button type="button" className="btn" onClick={() => speak(selected.word)}>
                        <Volume2 size={16} /> Listen
                      </button>
                    </div>
                    <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: '10px 0' }} />
                    <SafeDemoHtml html={selected.html} />
                  </div>
                )}
              </div>

              <div className="card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: 14 }}>
                <div>
                  <span className="badge">
                    <BookA size={14} style={{ marginRight: 4 }} /> Multilingual Flashcard Preview
                  </span>
                  <div
                    role="button"
                    tabIndex={0}
                    onClick={() => setFlipped((f) => !f)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') setFlipped((f) => !f);
                    }}
                    style={{
                      marginTop: 14,
                      padding: 28,
                      borderRadius: 14,
                      background: 'var(--surface-2)',
                      textAlign: 'center',
                      cursor: 'pointer',
                    }}
                  >
                    <div className="badge">{sampleCard.language.toUpperCase()} · {sampleCard.level}</div>
                    <h3 style={{ fontSize: '2rem', margin: '10px 0 4px' }}>{sampleCard.word}</h3>
                    <div style={{ color: 'var(--muted)' }}>{sampleCard.reading}</div>
                    {flipped ? (
                      <div style={{ marginTop: 14 }}>
                        <div style={{ fontWeight: 700 }}>{sampleCard.meaningEn}</div>
                        <div style={{ color: 'var(--accent)', fontWeight: 600 }}>{sampleCard.meaningMy}</div>
                      </div>
                    ) : (
                      <p style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>Click card to flip</p>
                    )}
                  </div>
                </div>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <button
                    type="button"
                    className="btn"
                    onClick={() => {
                      setFlipped(false);
                      setCardIdx((i) => (i + 1) % VOCAB_DECK.length);
                    }}
                  >
                    Next Word
                  </button>
                  <a className="btn btn-primary" href="https://htetlingo.pages.dev" target="_blank" rel="noreferrer">
                    Open Full App <ExternalLink size={15} />
                  </a>
                </div>
              </div>
            </div>
          </div>
        )}

        {section === 'docs' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <h2>HtetLingo Technical Documentation</h2>
            <div className="card">
              <h3>1. Monorepo Architecture</h3>
              <pre>{`htetlingo-v2/
├── apps/
│   ├── web/         # React + TypeScript + iPad PWA → Cloudflare Pages
│   ├── site/        # Landing Page, Docs & Static Demo → GitHub Pages
│   ├── macos/       # SwiftUI Native macOS App + SQLite FTS5
│   └── android/     # Kotlin + Jetpack Compose Native Android App
├── packages/
│   ├── core/        # Shared types, SM-2 SRS engine, multilingual decks
│   ├── data-pipeline/ # Ornagai StarDict → SQLite FTS5 & 27 JSON shards
│   └── supabase/    # Postgres schema + strict Row Level Security (RLS)`}</pre>
            </div>
            <div className="card">
              <h3>2. Data Pipeline (Ornagai 111,232 Headwords)</h3>
              <pre>{`python3 packages/data-pipeline/build_ornagai.py /path/to/ornagai-en-my-dict.zip`}</pre>
              <p style={{ color: 'var(--muted)' }}>
                Generates <code>ornagai.sqlite</code> (FTS5 full-text index for macOS & Android) and 27 alphabetical JSON shards
                (<code>a.json</code>..<code>z.json</code>, <code>other.json</code>) cached in IndexedDB for offline Web/iPad PWA lookup.
              </p>
            </div>
            <div className="card">
              <h3>3. Supabase & RevenueCat Configuration</h3>
              <p style={{ color: 'var(--muted)' }}>
                Apply <code>packages/supabase/schema.sql</code> in Supabase SQL Editor to enable RLS-protected tables
                (<code>profiles</code>, <code>saved_words</code>, <code>srs_cards</code>, <code>quiz_attempts</code>).
                Never expose secret keys in client bundles — only publishable keys belong in <code>.env.local</code>.
              </p>
            </div>
          </div>
        )}

        {section === 'intro' && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 20 }}>
            <h2>Project Introduction — HtetLingo</h2>
            <div className="card">
              <p>
                <strong>HtetLingo</strong> (<em>Learn Languages. Understand Words. Connect Cultures.</em>) bridges Myanmar
                learners and global languages (English, Japanese, Chinese, Korean, Thai, French, German) in one fast,
                offline-capable platform across macOS, Android, iPad, and the Web.
              </p>
              <p style={{ color: 'var(--muted)' }}>
                မြန်မာဘာသာစကားဖြင့် ရှင်းလင်းတိကျသော အဘိဓာန် (Ornagai 111,232 words)၊ Grammar သင်ခန်းစာများ၊
                Spaced Repetition Vocabulary၊ Pronunciation & Listening လေ့ကျင့်ခန်းများနှင့် Offline Learning တို့ကို
                တစ်နေရာတည်းတွင် အသုံးပြုနိုင်ရန် ရည်ရွယ်တည်ဆောက်ထားပါသည်။
              </p>
            </div>
            <div className="card">
              <h3>Open-Source Datasets & Ecosystem</h3>
              <ul>
                {OPEN_SOURCES.map((src) => (
                  <li key={src}>{src}</li>
                ))}
              </ul>
            </div>
          </div>
        )}
      </main>

      <footer>
        <div className="container">
          HtetLingo v2 · Built for Mac Native, Android (Kotlin/Compose), Web & iPad PWA ·{' '}
          <a href="https://github.com/htetofficial/htetlingo-v2" target="_blank" rel="noreferrer">
            GitHub Repository
          </a>
        </div>
      </footer>
    </>
  );
}
