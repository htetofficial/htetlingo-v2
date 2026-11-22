import { useEffect, useState } from 'react';
import { CloudDownload, HardDrive, Trash2, Wifi, WifiOff, CheckCircle2 } from 'lucide-react';
import {
  clearCachedShards,
  listCachedShards,
  loadShard,
  type DictIndexMeta,
} from '../lib/offlineDb.ts';

interface OfflineViewProps {
  online: boolean;
}

export function OfflineView({ online }: OfflineViewProps) {
  const [meta, setMeta] = useState<DictIndexMeta | null>(null);
  const [cached, setCached] = useState<string[]>([]);
  const [downloading, setDownloading] = useState(false);
  const [progress, setProgress] = useState(0);

  const refresh = async () => {
    const keys = await listCachedShards();
    setCached(keys);
  };

  useEffect(() => {
    fetch('/dict/index.json')
      .then((r) => r.json())
      .then((m: DictIndexMeta) => setMeta(m))
      .catch(() => {});
    refresh();
  }, []);

  const downloadOne = async (key: string) => {
    setDownloading(true);
    try {
      await loadShard(key);
      await refresh();
    } finally {
      setDownloading(false);
    }
  };

  const downloadAll = async () => {
    if (!meta) return;
    const keys = Object.keys(meta.shards);
    setDownloading(true);
    setProgress(0);
    for (let i = 0; i < keys.length; i++) {
      await loadShard(keys[i]);
      setProgress(Math.round(((i + 1) / keys.length) * 100));
    }
    await refresh();
    setDownloading(false);
  };

  const handleClear = async () => {
    await clearCachedShards();
    await refresh();
  };

  const shardKeys = meta ? Object.keys(meta.shards) : [];

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Offline Learning & Packs</h2>
          <p>Download Ornagai dictionary shards into IndexedDB for 100% offline study on Web & iPad PWA.</p>
        </div>
        <span className="status-pill">
          {online ? <Wifi size={14} /> : <WifiOff size={14} />}
          {online ? 'Online' : 'Offline Mode Active'}
        </span>
      </div>

      <div className="card stack">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <div>
            <h3 style={{ margin: 0 }}>Ornagai EN–MY Full Dictionary</h3>
            <p style={{ margin: '4px 0 0', color: 'var(--muted)', fontSize: '0.9rem' }}>
              {cached.length} / {shardKeys.length || 27} shards stored offline ({meta?.wordCount.toLocaleString() ?? '111,232'} total headwords)
            </p>
          </div>
          <div className="row">
            <button type="button" className="btn btn-primary" disabled={downloading || !online} onClick={downloadAll}>
              <CloudDownload size={16} /> {downloading ? `Downloading ${progress}%…` : 'Download All 111k Words'}
            </button>
            <button type="button" className="btn" disabled={downloading || cached.length === 0} onClick={handleClear}>
              <Trash2 size={16} /> Clear
            </button>
          </div>
        </div>

        {downloading && (
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${progress}%` }} />
          </div>
        )}
      </div>

      <div className="card stack">
        <div className="row" style={{ justifyContent: 'space-between' }}>
          <h3 style={{ margin: 0, fontSize: '1rem' }}>A–Z Dictionary Shards</h3>
          <span className="row" style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>
            <HardDrive size={14} /> Tap any letter to cache offline
          </span>
        </div>
        <div className="row">
          {shardKeys.map((key) => {
            const isCached = cached.includes(key);
            const info = meta?.shards[key];
            return (
              <button
                key={key}
                type="button"
                className={`chip ${isCached ? 'active' : ''}`}
                disabled={downloading}
                onClick={() => downloadOne(key)}
              >
                {isCached && <CheckCircle2 size={13} />}
                {key.toUpperCase()} ({info?.count.toLocaleString()})
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
