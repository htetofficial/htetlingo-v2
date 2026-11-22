import { useEffect, useState } from 'react';
import { Award, Crown, LogIn, LogOut, RefreshCw, Flame, BookOpen, ShieldCheck } from 'lucide-react';
import { OPTIONAL_PACKS, SUBSCRIPTION_PLANS } from '@htetlingo/core';
import { isSupabaseConfigured, supabase } from '../lib/supabase.ts';
import type { EntitlementState, Tier } from '../lib/revenuecat.ts';
import type { User } from '@supabase/supabase-js';

type ColorSchemePref = 'system' | 'light' | 'dark';

interface ProgressAccountViewProps {
  xp: number;
  streak: number;
  savedCount: number;
  studiedCards: number;
  quizCount: number;
  entitlements: EntitlementState;
  onSelectTier: (tier: Tier) => void;
  onUnlockPack: (packId: string) => void;
}

function getStoredScheme(): ColorSchemePref {
  try {
    const val = localStorage.getItem('color-scheme');
    if (val === 'light' || val === 'dark') return val;
  } catch {}
  return 'system';
}

export function ProgressAccountView({
  xp,
  streak,
  savedCount,
  studiedCards,
  quizCount,
  entitlements,
  onSelectTier,
  onUnlockPack,
}: ProgressAccountViewProps) {
  const [user, setUser] = useState<User | null>(null);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [authMsg, setAuthMsg] = useState('');
  const [scheme, setScheme] = useState<ColorSchemePref>(getStoredScheme);
  const [systemDark, setSystemDark] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia('(prefers-color-scheme: dark)').matches : false
  );

  useEffect(() => {
    const mq = window.matchMedia('(prefers-color-scheme: dark)');
    const onChange = (e: MediaQueryListEvent) => setSystemDark(e.matches);
    mq.addEventListener('change', onChange);

    const onStorage = (e: StorageEvent) => {
      if (e.key === 'color-scheme') {
        const next = e.newValue === 'light' || e.newValue === 'dark' ? e.newValue : 'system';
        setScheme(next);
        const meta = document.querySelector<HTMLMetaElement>('meta[name="color-scheme"]');
        if (meta) meta.content = next === 'system' ? 'light dark' : next;
      }
    };
    window.addEventListener('storage', onStorage);

    return () => {
      mq.removeEventListener('change', onChange);
      window.removeEventListener('storage', onStorage);
    };
  }, []);

  useEffect(() => {
    if (!supabase) return;
    supabase.auth.getUser().then(({ data }) => setUser(data.user ?? null));
    const { data: sub } = supabase.auth.onAuthStateChange((_ev, session) => {
      setUser(session?.user ?? null);
    });
    return () => sub.subscription.unsubscribe();
  }, []);

  const applyScheme = (next: ColorSchemePref) => {
    setScheme(next);
    const meta = document.querySelector<HTMLMetaElement>('meta[name="color-scheme"]');
    if (meta) meta.content = next === 'system' ? 'light dark' : next;
    try {
      if (next === 'system') localStorage.removeItem('color-scheme');
      else localStorage.setItem('color-scheme', next);
    } catch {}
  };

  const handleAuth = async (mode: 'signin' | 'signup') => {
    if (!supabase) {
      setAuthMsg('Supabase is not configured in this build.');
      return;
    }
    setAuthMsg('Connecting to Supabase…');
    const res =
      mode === 'signup'
        ? await supabase.auth.signUp({ email, password })
        : await supabase.auth.signInWithPassword({ email, password });

    if (res.error) setAuthMsg(res.error.message);
    else setAuthMsg(mode === 'signup' ? 'Account created! Check your email if confirmation is enabled.' : 'Signed in successfully!');
  };

  const syncProgress = async () => {
    if (!supabase || !user) return;
    setAuthMsg('Syncing progress to Supabase…');
    const { error } = await supabase.from('profiles').upsert({
      id: user.id,
      total_xp: xp,
      streak_days: streak,
      subscription_tier: entitlements.tier,
      unlocked_packs: entitlements.unlockedPacks,
      updated_at: new Date().toISOString(),
    });
    setAuthMsg(error ? `Sync note: ${error.message}` : 'Progress synced to Supabase cloud!');
  };

  const dailyPct = Math.min(100, Math.round((xp / 50) * 100));

  return (
    <div className="stack">
      <div className="page-header">
        <div>
          <h2>Progress, Account & Plans</h2>
          <p>Track daily XP, sync across Mac / Android / iPad via Supabase, and manage RevenueCat plans.</p>
        </div>
        <span className="badge">
          <Crown size={13} style={{ marginRight: 4 }} /> Tier: {entitlements.tier.toUpperCase()}
        </span>
      </div>

      {/* Progress KPIs */}
      <div className="grid-3">
        <div className="card">
          <div className="row" style={{ color: 'var(--muted)', fontSize: '0.84rem' }}>
            <Award size={16} /> Total XP
          </div>
          <div style={{ fontSize: '1.9rem', fontWeight: 800, marginTop: 4 }}>{xp} XP</div>
          <div className="progress-bar" style={{ marginTop: 8 }}>
            <div className="progress-fill" style={{ width: `${dailyPct}%` }} />
          </div>
          <div style={{ fontSize: '0.78rem', color: 'var(--muted)', marginTop: 4 }}>
            Daily goal: {Math.min(xp, 50)} / 50 XP
          </div>
        </div>

        <div className="card">
          <div className="row" style={{ color: 'var(--muted)', fontSize: '0.84rem' }}>
            <Flame size={16} /> Streak & Quizzes
          </div>
          <div style={{ fontSize: '1.9rem', fontWeight: 800, marginTop: 4 }}>{streak} Days</div>
          <div style={{ fontSize: '0.82rem', color: 'var(--muted)', marginTop: 6 }}>
            Quizzes completed: {quizCount}
          </div>
        </div>

        <div className="card">
          <div className="row" style={{ color: 'var(--muted)', fontSize: '0.84rem' }}>
            <BookOpen size={16} /> Vocabulary & Saved
          </div>
          <div style={{ fontSize: '1.9rem', fontWeight: 800, marginTop: 4 }}>{studiedCards} Cards</div>
          <div style={{ fontSize: '0.82rem', color: 'var(--muted)', marginTop: 6 }}>
            Bookmarked dictionary words: {savedCount}
          </div>
        </div>
      </div>

      {/* Supabase Account + Theme Settings */}
      <div className="grid-2">
        <div className="card stack">
          <div className="row" style={{ justifyContent: 'space-between' }}>
            <h3 style={{ margin: 0 }}>User Account (Supabase)</h3>
            <span className="badge">
              <ShieldCheck size={13} style={{ marginRight: 4 }} />
              {isSupabaseConfigured ? 'RLS Connected' : 'Local Only'}
            </span>
          </div>

          {user ? (
            <div className="stack">
              <p style={{ margin: 0 }}>
                Signed in as <strong>{user.email}</strong>
              </p>
              <div className="row">
                <button type="button" className="btn btn-primary btn-sm" onClick={syncProgress}>
                  <RefreshCw size={15} /> Sync Progress Now
                </button>
                <button
                  type="button"
                  className="btn btn-sm"
                  onClick={() => supabase?.auth.signOut()}
                >
                  <LogOut size={15} /> Sign Out
                </button>
              </div>
            </div>
          ) : (
            <div className="stack" style={{ gap: 10 }}>
              <input
                type="email"
                className="input"
                placeholder="Email address"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="email"
              />
              <input
                type="password"
                className="input"
                placeholder="Password (min 6 chars)"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
              />
              <div className="row">
                <button type="button" className="btn btn-primary btn-sm" onClick={() => handleAuth('signin')}>
                  <LogIn size={15} /> Sign In
                </button>
                <button type="button" className="btn btn-sm" onClick={() => handleAuth('signup')}>
                  Create Account
                </button>
              </div>
            </div>
          )}
          {authMsg && <p style={{ margin: 0, fontSize: '0.84rem', color: 'var(--muted)' }}>{authMsg}</p>}
        </div>

        {/* Appearance Settings — 3-state color-scheme control */}
        <div className="card stack">
          <h3 style={{ margin: 0 }}>Appearance & Preferences</h3>
          <p style={{ margin: 0, fontSize: '0.88rem', color: 'var(--muted)' }}>
            Choose your preferred color scheme. Defaults to your OS setting.
          </p>
          <div className="row">
            <button
              type="button"
              className={`chip ${scheme === 'system' ? 'active' : ''}`}
              onClick={() => applyScheme('system')}
            >
              System (Currently {systemDark ? 'dark' : 'light'})
            </button>
            <button
              type="button"
              className={`chip ${scheme === 'light' ? 'active' : ''}`}
              onClick={() => applyScheme('light')}
            >
              Light
            </button>
            <button
              type="button"
              className={`chip ${scheme === 'dark' ? 'active' : ''}`}
              onClick={() => applyScheme('dark')}
            >
              Dark
            </button>
          </div>
          <div style={{ fontSize: '0.82rem', color: 'var(--muted)' }}>
            RevenueCat SDK Key: <code>{entitlements.apiKeyPrefix}</code>
          </div>
        </div>
      </div>

      {/* Monetization: Free / Premium / Lifetime */}
      <div className="card stack">
        <h3 style={{ margin: 0 }}>HtetLingo Plans (RevenueCat)</h3>
        <div className="grid-3">
          {SUBSCRIPTION_PLANS.map((plan) => {
            const active = entitlements.tier === plan.entitlement;
            return (
              <div
                key={plan.id}
                className="card stack"
                style={{
                  borderColor: active ? 'var(--accent)' : 'var(--border)',
                  borderWidth: active ? 2 : 1,
                }}
              >
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <strong>{plan.name}</strong>
                  {plan.badge && <span className="badge">{plan.badge}</span>}
                </div>
                <div>
                  <span style={{ fontSize: '1.6rem', fontWeight: 800 }}>{plan.price}</span>{' '}
                  <span style={{ color: 'var(--muted)', fontSize: '0.85rem' }}>{plan.cadence}</span>
                </div>
                <ul style={{ margin: 0, paddingLeft: 18, fontSize: '0.86rem', color: 'var(--muted)' }}>
                  {plan.features.map((f) => (
                    <li key={f}>{f}</li>
                  ))}
                </ul>
                <button
                  type="button"
                  className={`btn btn-sm ${active ? '' : 'btn-primary'}`}
                  style={{ marginTop: 'auto' }}
                  onClick={() => onSelectTier(plan.entitlement)}
                >
                  {active ? 'Current Plan' : `Activate ${plan.name}`}
                </button>
              </div>
            );
          })}
        </div>
      </div>

      {/* Optional Purchases: Language Packs & Learning Packs */}
      <div className="card stack">
        <h3 style={{ margin: 0 }}>Optional Language & Learning Packs</h3>
        <div className="grid-2">
          {OPTIONAL_PACKS.map((pack) => {
            const owned = entitlements.unlockedPacks.includes(pack.id);
            return (
              <div key={pack.id} className="card stack" style={{ background: 'var(--surface-2)' }}>
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <strong>{pack.title}</strong>
                  <span className="badge badge-amber">{pack.price}</span>
                </div>
                <p style={{ margin: 0, fontSize: '0.86rem', color: 'var(--muted)' }}>{pack.subtitle}</p>
                <div className="row" style={{ justifyContent: 'space-between' }}>
                  <span style={{ fontSize: '0.78rem', color: 'var(--muted)' }}>
                    {pack.highlights.join(' · ')}
                  </span>
                  <button
                    type="button"
                    className={`btn btn-sm ${owned ? '' : 'btn-primary'}`}
                    onClick={() => onUnlockPack(pack.id)}
                  >
                    {owned ? 'Unlocked' : 'Purchase Pack'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
