import { useEffect, useState } from 'react';
import { Bookmark, BookmarkCheck, Search, Volume2, Database } from 'lucide-react';
import { loadShard, type DictTuple } from '../lib/offlineDb.ts';
import { speakText } from '../lib/speech.ts';
import { SafeHtml } from '../components/SafeHtml.tsx';

interface StarterEntry {
  word: string;
  html: string;
}

interface EnEnEntry {
  word: string;
  defs: string[];
}

interface DictionaryViewProps {
  savedWords: string[];
  onToggleSave: (word: string) => void;
  onGainXp: (xp: number) => void;
}

const enEnCache = new Map<string, [string, string[]][]>();

async function loadEnEnShard(key: string): Promise<[string, string[]][]> {
  const hit = enEnCache.get(key);
  if (hit) return hit;
  const res = await fetch(`/dict/en-en/shards/${key}.json`);
  if (!res.ok) throw new Error('EN-EN shard unavailable');
  const data = (await res.json()) as [string, string[]][];
  enEnCache.set(key, data);
  return data;
}

export function DictionaryView({ savedWords, onToggleSave, onGainXp }: DictionaryViewProps) {
  const [mode, setMode] = useState<'en-my' | 'en-en'>('en-my');
  const [query, setQuery] = useState('');
  const [starter, setStarter] = useState<StarterEntry[]>([]);
  const [results, setResults] = useState<StarterEntry[]>([]);
  const [enEnResults, setEnEnResults] = useState<EnEnEntry[]>([]);
  const [selected, setSelected] = useState<StarterEntry | null>(null);
  const [selectedEnEn, setSelectedEnEn] = useState<EnEnEntry | null>(null);
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

    if (mode === 'en-en') {
      let cancelled = false;
      const key = q && q[0] >= 'a' && q[0] <= 'z' ? q[0] : 'a';
      setLoadingShard(true);
      loadEnEnShard(key)
        .then((tuples) => {
          if (cancelled) return;
          const matches: EnEnEntry[] = [];
          for (const [w, defs] of tuples) {
            if (!q || w.toLowerCase().startsWith(q)) {
              matches.push({ word: w, defs });
              if (matches.length >= 40) break;
            }
          }
          setEnEnResults(matches);
          if (matches[0]) setSelectedEnEn(matches[0]);
        })
        .catch(() => {})
        .finally(() => {
          if (!cancelled) setLoadingShard(false);
        });
      return () => {
        cancelled = true;
      };
    }

    if (!q) {
      setResults(showSavedOnly ? starter.filter((s) => savedWords.includes(s.word)) : starter.slice(0, 30));
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
        setResults(starter.filter((e) => e.word.toLowerCase().includes(q)));
      })
      .finally(() => {
        if (!cancelled) setLoadingShard(false);
      });

    return () => {
      cancelled = true;
    };
  }, [query, starter, showSavedOnly, savedWords, mode]);

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Dictionary (EN–MY 111k + EN–EN 49k)</h2>
          <p>Ornagai English–Myanmar (111,232 words) & English–English (49,537 words) with offline shard caching.</p>
        </div>
        <div className="row">
          <button type="button" className={`chip ${mode === 'en-my' ? 'active' : ''}`} onClick={() => setMode('en-my')}>
            Ornagai EN–MY (111k)
          </button>
          <button type="button" className={`chip ${mode === 'en-en' ? 'active' : ''}`} onClick={() => setMode('en-en')}>
            English–English (49k)
          </button>
          {mode === 'en-my' && (
            <button
              type="button"
              className={`chip ${showSavedOnly ? 'active' : ''}`}
              onClick={() => setShowSavedOnly((v) => !v)}
            >
              <Bookmark size={14} /> Saved ({savedWords.length})
            </button>
          )}
        </div>
      </div>

      <div className="card row">
        <Search size={18} style={{ color: 'var(--muted)' }} />
        <input
          type="search"
          className="input"
          style={{ border: 'none', padding: '4px 6px', flex: 1 }}
          placeholder={
            mode === 'en-my'
              ? 'Search 111,232 Ornagai EN–MY words (e.g., resilience, philosophy, water)...'
              : 'Search 49,537 English–English definitions (e.g., serendipity, algorithm)...'
          }
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          aria-label="Search dictionary"
        />
        {loadingShard && <span className="badge">Loading shard…</span>}
      </div>

      <div className="grid-2" style={{ alignItems: 'start' }}>
        <div className="card stack" style={{ maxHeight: 520, overflowY: 'auto', gap: 6 }}>
          <div className="row" style={{ justifyContent: 'space-between', fontSize: '0.8rem', color: 'var(--muted)' }}>
            <span>Matches ({mode === 'en-my' ? results.length : enEnResults.length})</span>
            <span className="row" style={{ gap: 4 }}>
              <Database size={13} /> Offline Shards
            </span>
          </div>

          {mode === 'en-my'
            ? results.map((item) => {
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
              })
            : enEnResults.map((item) => {
                const active = selectedEnEn?.word === item.word;
                return (
                  <button
                    key={item.word}
                    type="button"
                    className="nav-btn"
                    aria-current={active ? 'page' : undefined}
                    onClick={() => {
                      setSelectedEnEn(item);
                      onGainXp(1);
                    }}
                  >
                    <span style={{ flex: 1 }}>{item.word}</span>
                  </button>
                );
              })}
        </div>

        <div className="card stack">
          {mode === 'en-my' && selected ? (
            <>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.6rem' }}>{selected.word}</h3>
                  <span className="badge">Ornagai English → Burmese</span>
                </div>
                <div className="row">
                  <button type="button" className="btn btn-sm" onClick={() => speakText(selected.word, 'en')}>
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
          ) : mode === 'en-en' && selectedEnEn ? (
            <>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <div>
                  <h3 style={{ margin: 0, fontSize: '1.6rem' }}>{selectedEnEn.word}</h3>
                  <span className="badge">English → English</span>
                </div>
                <button type="button" className="btn btn-sm" onClick={() => speakText(selectedEnEn.word, 'en')}>
                  <Volume2 size={16} /> Listen
                </button>
              </div>
              <hr style={{ width: '100%', border: 'none', borderTop: '1px solid var(--border)', margin: '4px 0' }} />
              <ol style={{ margin: 0, paddingLeft: 20, lineHeight: 1.7 }}>
                {selectedEnEn.defs.map((d, idx) => (
                  <li key={idx} style={{ marginBottom: 8 }}>{d}</li>
                ))}
              </ol>
            </>
          ) : (
            <p style={{ color: 'var(--muted)' }}>Select a headword to view its definitions and examples.</p>
          )}
        </div>
      </div>
    </div>
  );
}
