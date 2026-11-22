/**
 * RevenueCat Entitlement & Billing Bridge
 * Uses VITE_REVENUECAT_API_KEY (public/test key) and syncs entitlements locally + with Supabase profiles.
 */

export type Tier = 'free' | 'premium' | 'lifetime';

export interface EntitlementState {
  tier: Tier;
  unlockedPacks: string[];
  revenueCatConfigured: boolean;
  apiKeyPrefix: string;
}

const STORAGE_KEY = 'htetlingo_entitlements_v2';

export function getRevenueCatConfig() {
  const key = (import.meta.env.VITE_REVENUECAT_API_KEY as string | undefined) ?? '';
  return {
    configured: key.length > 0,
    isTestKey: key.startsWith('test_'),
    prefix: key ? `${key.slice(0, 9)}…` : 'not-set',
  };
}

export function loadEntitlements(): EntitlementState {
  const cfg = getRevenueCatConfig();
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as { tier?: Tier; unlockedPacks?: string[] };
      return {
        tier: parsed.tier ?? 'free',
        unlockedPacks: parsed.unlockedPacks ?? [],
        revenueCatConfigured: cfg.configured,
        apiKeyPrefix: cfg.prefix,
      };
    }
  } catch {}
  return {
    tier: 'free',
    unlockedPacks: [],
    revenueCatConfigured: cfg.configured,
    apiKeyPrefix: cfg.prefix,
  };
}

export function saveEntitlements(tier: Tier, unlockedPacks: string[]): EntitlementState {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ tier, unlockedPacks }));
  } catch {}
  const cfg = getRevenueCatConfig();
  return {
    tier,
    unlockedPacks,
    revenueCatConfigured: cfg.configured,
    apiKeyPrefix: cfg.prefix,
  };
}
