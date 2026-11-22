import { OPTIONAL_PACKS } from '@htetlingo/core';

/**
 * RevenueCat Entitlement & Billing Bridge
 * Grants permanent Lifetime + All Packs free access to Founder/VIP accounts.
 */

export type Tier = 'free' | 'premium' | 'lifetime';

export const VIP_FREE_EMAILS = ['htetmyanmar321@icloud.com'];

export interface EntitlementState {
  tier: Tier;
  unlockedPacks: string[];
  revenueCatConfigured: boolean;
  apiKeyPrefix: string;
  isVipFree: boolean;
}

const STORAGE_KEY = 'htetlingo_entitlements_v2';

export function isVipEmail(email?: string | null): boolean {
  if (!email) return false;
  return VIP_FREE_EMAILS.includes(email.trim().toLowerCase());
}

export function getRevenueCatConfig() {
  const key = (import.meta.env.VITE_REVENUECAT_API_KEY as string | undefined) ?? '';
  return {
    configured: key.length > 0,
    isTestKey: key.startsWith('test_'),
    prefix: key ? `${key.slice(0, 9)}…` : 'not-set',
  };
}

export function loadEntitlements(userEmail?: string | null): EntitlementState {
  const cfg = getRevenueCatConfig();
  if (isVipEmail(userEmail)) {
    const allPacks = OPTIONAL_PACKS.map((p) => p.id);
    return {
      tier: 'lifetime',
      unlockedPacks: allPacks,
      revenueCatConfigured: cfg.configured,
      apiKeyPrefix: cfg.prefix,
      isVipFree: true,
    };
  }
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as { tier?: Tier; unlockedPacks?: string[]; isVipFree?: boolean };
      return {
        tier: parsed.tier ?? 'free',
        unlockedPacks: parsed.unlockedPacks ?? [],
        revenueCatConfigured: cfg.configured,
        apiKeyPrefix: cfg.prefix,
        isVipFree: Boolean(parsed.isVipFree),
      };
    }
  } catch {}
  return {
    tier: 'free',
    unlockedPacks: [],
    revenueCatConfigured: cfg.configured,
    apiKeyPrefix: cfg.prefix,
    isVipFree: false,
  };
}

export function saveEntitlements(
  tier: Tier,
  unlockedPacks: string[],
  isVipFree = false
): EntitlementState {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ tier, unlockedPacks, isVipFree }));
  } catch {}
  const cfg = getRevenueCatConfig();
  return {
    tier,
    unlockedPacks,
    revenueCatConfigured: cfg.configured,
    apiKeyPrefix: cfg.prefix,
    isVipFree,
  };
}
