import { useEffect, useState } from 'react';
import {
  BookA,
  BookOpen,
  GraduationCap,
  HelpCircle,
  Volume2,
  Languages,
  CloudOff,
  UserCircle,
  Crown,
  Compass,
  BookMarked,
  Sparkles,
} from 'lucide-react';
import { SUBSCRIPTION_PLANS, type SrsState } from '@htetlingo/core';
import { QuestPathView } from './views/QuestPathView.tsx';
import { DictionaryView } from './views/DictionaryView.tsx';
import { VocabularyView } from './views/VocabularyView.tsx';
import { GrammarView } from './views/GrammarView.tsx';
import { QuizView } from './views/QuizView.tsx';
import { PronunciationView } from './views/PronunciationView.tsx';
import { TranslateView } from './views/TranslateView.tsx';
import { OfflineView } from './views/OfflineView.tsx';
import { MyanmarToolsView } from './views/MyanmarToolsView.tsx';
import { HtetBookView } from './views/HtetBookView.tsx';
import { ProgressAccountView } from './views/ProgressAccountView.tsx';
import { ModalDialog } from './components/ModalDialog.tsx';
import {
  isVipEmail,
  loadEntitlements,
  saveEntitlements,
  type EntitlementState,
  type Tier,
} from './lib/revenuecat.ts';
import { supabase } from './lib/supabase.ts';
import { OPTIONAL_PACKS } from '@htetlingo/core';

type TabId =
  | 'quest'
  | 'dictionary'
  | 'book'
  | 'vocabulary'
  | 'grammar'
  | 'quiz'
  | 'pronunciation'
  | 'translate'
  | 'myanmar'
  | 'offline'
  | 'account';

const NAV_ITEMS: { id: TabId; label: string; icon: typeof BookA }[] = [
  { id: 'quest', label: '🌸 Quest', icon: Sparkles as unknown as typeof BookA },
  { id: 'dictionary', label: 'Dictionary', icon: BookA },
  { id: 'book', label: 'A1–C2 Book', icon: BookMarked },
  { id: 'vocabulary', label: 'Vocabulary', icon: BookOpen },
  { id: 'grammar', label: 'Grammar', icon: GraduationCap },
  { id: 'quiz', label: 'Quiz', icon: HelpCircle },
  { id: 'pronunciation', label: 'Pronunciation', icon: Volume2 },
  { id: 'translate', label: 'Translate', icon: Languages },
  { id: 'myanmar', label: 'Myanmar Hub', icon: Compass },
  { id: 'offline', label: 'Offline Packs', icon: CloudOff },
  { id: 'account', label: 'Progress & Account', icon: UserCircle },
];

function loadJson<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

export function App() {
  const [tab, setTab] = useState<TabId>('quest');
  const [online, setOnline] = useState(() => (typeof navigator !== 'undefined' ? navigator.onLine : true));
  const [xp, setXp] = useState<number>(() => loadJson('htetlingo_xp', 25));
  const [streak] = useState<number>(() => loadJson('htetlingo_streak', 3));
  const [quizCount, setQuizCount] = useState<number>(() => loadJson('htetlingo_quizzes', 1));
  const [savedWords, setSavedWords] = useState<string[]>(() => loadJson('htetlingo_saved', ['resilience', 'language']));
  const [srsMap, setSrsMap] = useState<Record<string, SrsState>>(() => loadJson('htetlingo_srs', {}));
  const [entitlements, setEntitlements] = useState<EntitlementState>(() => loadEntitlements());
  const [paywallOpen, setPaywallOpen] = useState(false);

  useEffect(() => {
    const onOnline = () => setOnline(true);
    const onOffline = () => setOnline(false);
    window.addEventListener('online', onOnline);
    window.addEventListener('offline', onOffline);

    const checkVip = (email?: string | null) => {
      if (isVipEmail(email)) {
        setEntitlements(saveEntitlements('lifetime', OPTIONAL_PACKS.map((p) => p.id), true));
      }
    };
    if (supabase) {
      supabase.auth.getUser().then(({ data }) => checkVip(data.user?.email));
      const { data: sub } = supabase.auth.onAuthStateChange((_ev, session) => {
        checkVip(session?.user?.email);
      });
      return () => {
        window.removeEventListener('online', onOnline);
        window.removeEventListener('offline', onOffline);
        sub.subscription.unsubscribe();
      };
    }

    return () => {
      window.removeEventListener('online', onOnline);
      window.removeEventListener('offline', onOffline);
    };
  }, []);

  const gainXp = (amount: number) => {
    setXp((prev) => {
      const next = prev + amount;
      try {
        localStorage.setItem('htetlingo_xp', JSON.stringify(next));
      } catch {}
      return next;
    });
  };

  const toggleSaveWord = (word: string) => {
    setSavedWords((prev) => {
      const next = prev.includes(word) ? prev.filter((w) => w !== word) : [...prev, word];
      try {
        localStorage.setItem('htetlingo_saved', JSON.stringify(next));
      } catch {}
      return next;
    });
  };

  const handleReview = (cardId: string, nextSrs: SrsState) => {
    setSrsMap((prev) => {
      const updated = { ...prev, [cardId]: nextSrs };
      try {
        localStorage.setItem('htetlingo_srs', JSON.stringify(updated));
      } catch {}
      return updated;
    });
  };

  const handleCompleteQuiz = (_score: number, _total: number, earnedXp: number) => {
    gainXp(earnedXp);
    setQuizCount((prev) => {
      const next = prev + 1;
      try {
        localStorage.setItem('htetlingo_quizzes', JSON.stringify(next));
      } catch {}
      return next;
    });
  };

  const selectTier = (tier: Tier) => {
    setEntitlements(saveEntitlements(tier, entitlements.unlockedPacks));
    setPaywallOpen(false);
  };

  const unlockPack = (packId: string) => {
    if (entitlements.unlockedPacks.includes(packId)) return;
    setEntitlements(saveEntitlements(entitlements.tier, [...entitlements.unlockedPacks, packId]));
  };

  return (
    <div className="app-shell">
      <aside className="sidebar" aria-label="Main navigation">
        <div className="brand">
          <div className="brand-mark">HL</div>
          <div>
            <h1>HtetLingo</h1>
            <span>Learn · Understand · Connect</span>
          </div>
        </div>

        <nav className="nav-list">
          {NAV_ITEMS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              type="button"
              className="nav-btn"
              aria-current={tab === id ? 'page' : undefined}
              onClick={() => setTab(id)}
            >
              <Icon size={18} />
              <span>{label}</span>
            </button>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="status-pill">
            <span className={`status-dot ${online ? '' : 'offline'}`} />
            <span>{online ? 'Online · PWA Ready' : 'Offline Mode'}</span>
          </div>
          <div className="status-pill">
            <span>Daily XP: <strong>{xp}</strong></span>
          </div>
          {entitlements.tier === 'free' && (
            <button type="button" className="btn btn-primary btn-sm" onClick={() => setPaywallOpen(true)}>
              <Crown size={15} /> Upgrade to Premium
            </button>
          )}
        </div>
      </aside>

      <main className="main-area">
        {tab === 'quest' && <QuestPathView onGainXp={gainXp} />}
        {tab === 'dictionary' && (
          <DictionaryView savedWords={savedWords} onToggleSave={toggleSaveWord} onGainXp={gainXp} />
        )}
        {tab === 'book' && <HtetBookView onGainXp={gainXp} />}
        {tab === 'vocabulary' && (
          <VocabularyView srsMap={srsMap} onReview={handleReview} onGainXp={gainXp} />
        )}
        {tab === 'grammar' && (
          <GrammarView
            tier={entitlements.tier}
            onOpenPaywall={() => setPaywallOpen(true)}
            onGainXp={gainXp}
          />
        )}
        {tab === 'quiz' && <QuizView onCompleteQuiz={handleCompleteQuiz} />}
        {tab === 'pronunciation' && <PronunciationView onGainXp={gainXp} />}
        {tab === 'translate' && <TranslateView onGainXp={gainXp} />}
        {tab === 'myanmar' && <MyanmarToolsView />}
        {tab === 'offline' && <OfflineView online={online} />}
        {tab === 'account' && (
          <ProgressAccountView
            xp={xp}
            streak={streak}
            savedCount={savedWords.length}
            studiedCards={Object.keys(srsMap).length}
            quizCount={quizCount}
            entitlements={entitlements}
            onSelectTier={selectTier}
            onUnlockPack={unlockPack}
          />
        )}
      </main>

      <nav className="mobile-nav" aria-label="Mobile navigation">
        {NAV_ITEMS.map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            type="button"
            aria-current={tab === id ? 'page' : undefined}
            onClick={() => setTab(id)}
          >
            <Icon size={18} />
            <span>{label.split(' ')[0]}</span>
          </button>
        ))}
      </nav>

      <ModalDialog open={paywallOpen} title="Upgrade HtetLingo" onClose={() => setPaywallOpen(false)}>
        <div className="stack">
          <p style={{ margin: 0, color: 'var(--muted)' }}>
            Unlock all 8+ languages, unlimited grammar lessons, offline packs, and cross-device sync via RevenueCat.
          </p>
          <div className="grid-3">
            {SUBSCRIPTION_PLANS.map((plan) => (
              <div key={plan.id} className="card stack">
                <strong>{plan.name}</strong>
                <div style={{ fontSize: '1.4rem', fontWeight: 800 }}>
                  {plan.price} <span style={{ fontSize: '0.8rem', color: 'var(--muted)' }}>{plan.cadence}</span>
                </div>
                <button
                  type="button"
                  className={`btn btn-sm ${plan.entitlement === entitlements.tier ? '' : 'btn-primary'}`}
                  onClick={() => selectTier(plan.entitlement)}
                >
                  {plan.entitlement === entitlements.tier ? 'Active' : `Choose ${plan.name}`}
                </button>
              </div>
            ))}
          </div>
        </div>
      </ModalDialog>
    </div>
  );
}
