import { useEffect, useMemo, useState } from 'react';
import { Calendar, Phone, MapPin, Users, Scissors, BookOpen } from 'lucide-react';
import { checkMyanmarPhone, getMyanmarDate, segmentBurmeseSyllables } from '../lib/myanmarTools.ts';

interface NamesDataset {
  totalPairs: number;
  maleCount: number;
  femaleCount: number;
  pairs: [string, string][];
}

interface PostalDataset {
  count: number;
  entries: { region: string; township: string; code: string }[];
}

interface WordlistsDataset {
  burmeseTotal: number;
  karenTotal: number;
  nrcTotal: number;
  burmeseSample: string[];
  karenSample: string[];
  nrcCodes: string[];
}

export function MyanmarToolsView() {
  const [subTab, setSubTab] = useState<'names' | 'postal' | 'wordlists' | 'utilities'>('names');
  const [namesData, setNamesData] = useState<NamesDataset | null>(null);
  const [postalData, setPostalData] = useState<PostalDataset | null>(null);
  const [wordlists, setWordlists] = useState<WordlistsDataset | null>(null);

  const [nameQuery, setNameQuery] = useState('');
  const [postalQuery, setPostalQuery] = useState('');
  const [wordQuery, setWordQuery] = useState('');
  const [phoneInput, setPhoneInput] = useState('09791234567');
  const [dateInput, setDateInput] = useState(() => new Date().toISOString().slice(0, 10));
  const [segInput, setSegInput] = useState('မင်္ဂလာပါ ဘာသာစကားသင်ယူခြင်း');

  useEffect(() => {
    fetch('/datasets/myanmar-names.json').then((r) => r.json()).then(setNamesData).catch(() => {});
    fetch('/datasets/myanmar-postal.json').then((r) => r.json()).then(setPostalData).catch(() => {});
    fetch('/datasets/myanmar-wordlists.json').then((r) => r.json()).then(setWordlists).catch(() => {});
  }, []);

  const filteredNames = useMemo(() => {
    if (!namesData) return [];
    const q = nameQuery.trim().toLowerCase();
    if (!q) return namesData.pairs.slice(0, 40);
    return namesData.pairs
      .filter(([en, mm]) => en.toLowerCase().includes(q) || mm.includes(q))
      .slice(0, 50);
  }, [namesData, nameQuery]);

  const filteredPostal = useMemo(() => {
    if (!postalData) return [];
    const q = postalQuery.trim().toLowerCase();
    if (!q) return postalData.entries.slice(0, 36);
    return postalData.entries
      .filter(
        (e) =>
          e.township.toLowerCase().includes(q) ||
          e.region.toLowerCase().includes(q) ||
          e.code.includes(q)
      )
      .slice(0, 50);
  }, [postalData, postalQuery]);

  const filteredBurmese = useMemo(() => {
    if (!wordlists) return [];
    const q = wordQuery.trim();
    return (q ? wordlists.burmeseSample.filter((w) => w.includes(q)) : wordlists.burmeseSample).slice(0, 60);
  }, [wordlists, wordQuery]);

  const filteredKaren = useMemo(() => {
    if (!wordlists) return [];
    const q = wordQuery.trim();
    return (q ? wordlists.karenSample.filter((w) => w.includes(q)) : wordlists.karenSample).slice(0, 60);
  }, [wordlists, wordQuery]);

  const phoneResult = checkMyanmarPhone(phoneInput);
  const myDate = getMyanmarDate(new Date(dateInput + 'T12:00:00'));
  const syllables = segmentBurmeseSyllables(segInput);

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Myanmar Culture & Reference Hub</h2>
          <p>
            Powered by open-source Myanmar datasets: Names EN↔MY, Postal Codes, Burmese/Sgaw Karen Wordlists,
            Calendar (mmcal), Phone Checker & Syllable Segmenter.
          </p>
        </div>
      </div>

      <div className="row">
        <button type="button" className={`chip ${subTab === 'names' ? 'active' : ''}`} onClick={() => setSubTab('names')}>
          <Users size={14} /> Myanmar Names ({namesData?.totalPairs.toLocaleString() ?? '…'})
        </button>
        <button type="button" className={`chip ${subTab === 'postal' ? 'active' : ''}`} onClick={() => setSubTab('postal')}>
          <MapPin size={14} /> Postal Codes ({postalData?.count.toLocaleString() ?? '…'})
        </button>
        <button type="button" className={`chip ${subTab === 'wordlists' ? 'active' : ''}`} onClick={() => setSubTab('wordlists')}>
          <BookOpen size={14} /> Burmese & Karen Wordlists
        </button>
        <button type="button" className={`chip ${subTab === 'utilities' ? 'active' : ''}`} onClick={() => setSubTab('utilities')}>
          <Calendar size={14} /> Calendar, Phone & Segmenter
        </button>
      </div>

      {subTab === 'names' && (
        <div className="card stack">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <strong>Myanmar Name Transliteration (EN ↔ MY)</strong>
            <span className="badge">MgHla/Myanmar-Name_en-2-mm · L16H7/Myanmar_Names</span>
          </div>
          <input
            type="search"
            className="input"
            placeholder="Search English or Burmese name (e.g. Aung, Htet, Su, အောင်)..."
            value={nameQuery}
            onChange={(e) => setNameQuery(e.target.value)}
          />
          <div className="grid-3">
            {filteredNames.map(([en, mm], i) => (
              <div key={`${en}-${mm}-${i}`} className="card" style={{ padding: 12, background: 'var(--surface-2)' }}>
                <div style={{ fontWeight: 700 }}>{en}</div>
                <div style={{ color: 'var(--accent)', fontSize: '1.05rem' }}>{mm}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {subTab === 'postal' && (
        <div className="card stack">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <strong>Myanmar Township Postal Codes</strong>
            <span className="badge">kanaung/wordlists</span>
          </div>
          <input
            type="search"
            className="input"
            placeholder="Search township, state/region, or 5-digit postal code (e.g. Yangon, 11181)..."
            value={postalQuery}
            onChange={(e) => setPostalQuery(e.target.value)}
          />
          <div className="grid-3">
            {filteredPostal.map((p, i) => (
              <div key={`${p.code}-${i}`} className="card" style={{ padding: 12, background: 'var(--surface-2)' }}>
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <strong>{p.township}</strong>
                  <span className="badge">{p.code}</span>
                </div>
                <div style={{ fontSize: '0.82rem', color: 'var(--muted)', marginTop: 4 }}>{p.region}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      {subTab === 'wordlists' && (
        <div className="stack">
          <div className="card stack">
            <input
              type="search"
              className="input"
              placeholder="Filter Burmese or Sgaw Karen words..."
              value={wordQuery}
              onChange={(e) => setWordQuery(e.target.value)}
            />
            <div className="grid-2">
              <div className="stack">
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <strong>Burmese Wordlist</strong>
                  <span className="badge">{wordlists?.burmeseTotal.toLocaleString() ?? '…'} source words</span>
                </div>
                <div className="row" style={{ gap: 6 }}>
                  {filteredBurmese.map((w, i) => (
                    <span key={`${w}-${i}`} className="chip">{w}</span>
                  ))}
                </div>
              </div>

              <div className="stack">
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <strong>Sgaw Karen Wordlist</strong>
                  <span className="badge badge-amber">{wordlists?.karenTotal.toLocaleString() ?? '…'} source words</span>
                </div>
                <div className="row" style={{ gap: 6 }}>
                  {filteredKaren.map((w, i) => (
                    <span key={`${w}-${i}`} className="chip">{w}</span>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      )}

      {subTab === 'utilities' && (
        <div className="grid-2">
          <div className="card stack">
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <strong className="row"><Calendar size={16} /> Myanmar Calendar (mmcal)</strong>
              <span className="badge">yan9a/mmcal</span>
            </div>
            <input
              type="date"
              className="input"
              value={dateInput}
              onChange={(e) => setDateInput(e.target.value)}
            />
            <div className="card" style={{ background: 'var(--surface-2)', padding: 14 }}>
              <div style={{ fontSize: '1.25rem', fontWeight: 800 }}>မြန်မာသက္ကရာဇ် {myDate.myanmarYear} ခုနှစ်</div>
              <div style={{ color: 'var(--accent)', fontWeight: 600 }}>{myDate.approxMonth}</div>
              <div style={{ color: 'var(--muted)', fontSize: '0.88rem' }}>{myDate.weekday}</div>
            </div>
          </div>

          <div className="card stack">
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <strong className="row"><Phone size={16} /> Myanmar Phone Checker</strong>
              <span className="badge">kaungmyatlwin/myanmar-phonenumber</span>
            </div>
            <input
              type="tel"
              className="input"
              value={phoneInput}
              onChange={(e) => setPhoneInput(e.target.value)}
              placeholder="09791234567 or +959..."
            />
            <div className="card" style={{ background: 'var(--surface-2)', padding: 14 }}>
              <div className="row" style={{ justifyContent: 'space-between' }}>
                <strong>{phoneResult.operator}</strong>
                <span className="badge">{phoneResult.valid ? 'Valid' : 'Invalid'}</span>
              </div>
              <div style={{ color: 'var(--muted)', fontSize: '0.88rem', marginTop: 4 }}>
                Normalized: <code>{phoneResult.normalized}</code> · {phoneResult.networkType}
              </div>
            </div>
          </div>

          <div className="card stack" style={{ gridColumn: '1 / -1' }}>
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <strong className="row"><Scissors size={16} /> Burmese Syllable Segmenter</strong>
              <span className="badge">lwinmoe/segment</span>
            </div>
            <input
              type="text"
              className="input"
              value={segInput}
              onChange={(e) => setSegInput(e.target.value)}
            />
            <div className="row" style={{ gap: 6 }}>
              {syllables.map((s, i) => (
                <span key={i} className="badge">{s}</span>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
