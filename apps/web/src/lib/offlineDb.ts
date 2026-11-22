const DB_NAME = 'htetlingo_offline_v2';
const DB_VERSION = 1;
const STORE_SHARDS = 'dict_shards';

export type DictTuple = [string, string]; // [word, body_html]

export interface DictIndexMeta {
  source: string;
  wordCount: number;
  shards: Record<string, { count: number; bytes: number }>;
}

function openDB(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE_SHARDS)) {
        db.createObjectStore(STORE_SHARDS);
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

export async function getCachedShard(key: string): Promise<DictTuple[] | null> {
  try {
    const db = await openDB();
    return await new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_SHARDS, 'readonly');
      const req = tx.objectStore(STORE_SHARDS).get(key);
      req.onsuccess = () => resolve((req.result as DictTuple[] | undefined) ?? null);
      req.onerror = () => reject(req.error);
    });
  } catch {
    return null;
  }
}

export async function putCachedShard(key: string, data: DictTuple[]): Promise<void> {
  const db = await openDB();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE_SHARDS, 'readwrite');
    tx.objectStore(STORE_SHARDS).put(data, key);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

export async function listCachedShards(): Promise<string[]> {
  try {
    const db = await openDB();
    return await new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_SHARDS, 'readonly');
      const req = tx.objectStore(STORE_SHARDS).getAllKeys();
      req.onsuccess = () => resolve((req.result as string[]) ?? []);
      req.onerror = () => reject(req.error);
    });
  } catch {
    return [];
  }
}

export async function clearCachedShards(): Promise<void> {
  const db = await openDB();
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE_SHARDS, 'readwrite');
    tx.objectStore(STORE_SHARDS).clear();
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

const memoryCache = new Map<string, DictTuple[]>();

export async function loadShard(key: string): Promise<DictTuple[]> {
  const cachedMem = memoryCache.get(key);
  if (cachedMem) return cachedMem;

  const cachedDb = await getCachedShard(key);
  if (cachedDb) {
    memoryCache.set(key, cachedDb);
    return cachedDb;
  }

  const res = await fetch(`/dict/shards/${key}.json`);
  if (!res.ok) throw new Error(`Failed to fetch shard "${key}"`);
  const data = (await res.json()) as DictTuple[];
  memoryCache.set(key, data);
  // Save to IndexedDB in background for offline use
  putCachedShard(key, data).catch(() => {});
  return data;
}
