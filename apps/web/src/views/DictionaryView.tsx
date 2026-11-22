import { useEffect, useState } from 'react';
import { Bookmark, BookmarkCheck, Search, Volume2, Database } from 'lucide-react';
import { loadShard, type DictTuple } from '../lib/offlineDb.ts';
import { speakText } from '../lib/speech.ts';
import { SafeHtml } from '../components/SafeHtml.tsx';

interface StarterEntry {
  word: string;
  html: string;
}

interface DictionaryViewProps {
  savedWords: string[];
  onToggleSave: (word: string) => void;
  onGainXp: (xp: number) => void;
}

export function DictionaryView({ savedWords, onToggleSave, onGainXp }: DictionaryViewProps) {
  const [query, setQuery] = useState('');
  const [starter, setStarter] = useState<StarterEntry[]>([]);
  const [results, setResults] = useState<StarterEntry[]>([]);
  const [selected, setSelected] = useState<StarterEntry | null>(null);
  const [loadingShard, setLoadingShard] = useState(false);
  const [showSavedOnly, setShowSavedOnly] = useState(false);

  useEffect(() => {
    fetch('/dict/starter.json')
      .then((r) => r.json())
      .then((data: StarterEntry[]) => {
        setStarter(data);
        setResults(data.slice(0, 24));
        if (data[0]) setSelected(data[0]);
      })
      .catch(() => {});
  }, []);

  useEffect(() => {
    const q = query.trim().toLowerCase();
    if (!q) {
      if (showSavedOnly) {
        setResults(starter.filter((s) => savedWords.includes(s.word)));
      } else {
        setResults(starter.slice(0, 30));
      }
      return;
    }

    let cancelled = false;
    const first = q[0];
    const shardKey = first >= 'a' && first <= 'z' ? first : 'other';
    setLoadingShard(true);

    loadShard(shardKey)
      .then((tuples: DictTuple[]) => {
        if (cancelled) return;
        const prefixMatches: StarterEntry[] = [];
        const containsMatches: StarterEntry[] = [];
        for (const [w, html] of tuples) {
          const wl = w.toLowerCase();
          if (wl.startsWith(q)) {
            prefixMatches.push({ word: w, html });
            if (prefixMatches.length >= 40) break;
          } else if (containsMatches.length < 15 && wl.includes(q)) {
            containsMatches.push({ word: w, html });
          }
        }
        const merged = [...prefixMatches, ...containsMatches].slice(0, 40);
        setResults(merged);
        if (merged[0]) setSelected(merged[0]);
      })
      .catch(() => {
        if (cancelled) return;
        const fallback = starter.filter((e) => e.word.toLowerCase().includes(q));
        setResults(fallback);
      })
      .finally(() => {
        if (!cancelled) setLoadingShard(false);
      });

    return () => {
      cancelled = true;
    };
  }, [query, starter, showSavedOnly, savedWords]);

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Dictionary (Ornagai EN–MY)</h2>
          <p>Search all 111,232 headwords & 249,103 examples — cached automatically for offline use.</p>
        </div>
        <button
          type="button"
          className={`chip ${showSavedOnly ? 'active' : ''}`}
          onClick={() => setShowSavedOnly((v) => !v)}
        >
          <Bookmark size={14} /> Saved ({savedWords.length})
        </button>
      </div>

      <div className="card row">
        <Search size={18} style={{ color: 'var(--muted)' }} />
        <input
          type="search"
          className="input"
          style={{ border: 'none', padding: '4px 6px', flex: 1 }}
          placeholder="Search English word (e.g., resilience, philosophy, serendipity, water)..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          aria-label="Search dictionary"
        />
        {loadingShard && <span className="badge">Loading shard…</span>}
      </div>

      <div className="grid-2" style={{ alignItems: 'start' }}>
        <div className="card stack" style={{ maxHeight: 520, overflowY: 'auto', gap: 6 }}>
          <div className="row" style={{ justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--muted)' }}>
            <span>Matches ({results.length})</span>
            <span className="row" style={{ gap: 4 }}>
              <Database size={13} /> IndexedDB Ready
            </span>
          </div>
          {results.length === 0 && (
            <p style={{ color: 'var(--muted)', margin: '12px 0' }}>No matching headwords found.</p>
          )}
          {results.map((item) => {
            const active = selected?.word === item.word;
            return (
              <button
                key={item.word}
                type="button"
                className="nav-btn"
                aria-current={active ? 'page' : undefined}
                onClick={() => {
                  setSelected(item);
                  onGainXp(1);
                }}
              >
                <span style={{ flex: 1 }}>{item.word}</span>
                {savedWords.includes(item.word) && <BookmarkCheck size={15} />}
              </button>
            );
          })}
        </div>

        <div className="card stack">
          {selected ? (
            <>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.6rem' }}>{selected.word}</h3>
                  <span className="badge">English → Burmese</span>
                </div>
                <div className="row">
                  <button
                    type="button"
                    className="btn btn-sm"
                    onClick={() => speakText(selected.word, 'en')}
                    aria-label={`Pronounce ${selected.word}`}
                  >
                    <Volume2 size={16} /> Listen
                  </button>
                  <button
                    type="button"
                    className={`btn btn-sm ${savedWords.includes(selected.word) ? 'btn-primary' : ''}`}
                    onClick={() => onToggleSave(selected.word)}
                  >
                    {savedWords.includes(selected.word) ? <BookmarkCheck size={16} /> : <Bookmark size={16} />}
                    {savedWords.includes(selected.word) ? 'Saved' : 'Save'}
                  </button>
                </div>
              </div>
              <hr style={{ width: '100%', border: 'none', borderTop: '1px solid var(--border)', margin: '4px 0' }} />
              <SafeHtml html={selected.html} className="dict-body" />
            </>
          ) : (
            <p style={{ color: 'var(--muted)' }}>Select a word to inspect its definition, IPA, Burmese phonetics & examples.</p>
          )}
        </div>
      </div>
    </div>
  );
}
