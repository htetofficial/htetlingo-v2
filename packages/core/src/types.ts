export type LanguageCode = 'en' | 'ja' | 'zh' | 'my' | 'th' | 'fr' | 'de' | 'ko';

export interface LanguageInfo {
  code: LanguageCode;
  name: string;
  nativeName: string;
  bcp47: string;
  flag: string;
  script: string;
  tier: 'free' | 'premium' | 'pack';
}

export interface VocabItem {
  id: string;
  language: LanguageCode;
  word: string;
  reading: string;
  meaningEn: string;
  meaningMy: string;
  partOfSpeech: string;
  example: string;
  exampleTranslation: string;
  level: 'A1' | 'A2' | 'B1' | 'B2';
}

export interface GrammarLesson {
  id: string;
  language: LanguageCode;
  title: string;
  summary: string;
  level: 'Beginner' | 'Intermediate' | 'Advanced';
  tier: 'free' | 'premium';
  formula: string;
  explanationMy: string;
  examples: { sentence: string; translation: string; note?: string }[];
  commonMistake?: { wrong: string; right: string; why: string };
}

export interface QuizQuestion {
  id: string;
  language: LanguageCode;
  type: 'mcq' | 'fill' | 'listening';
  prompt: string;
  audioText?: string;
  options: string[];
  answer: string;
  explanation: string;
}

export interface SrsState {
  cardId: string;
  easeFactor: number;
  intervalDays: number;
  repetitions: number;
  nextReviewAt: number;
}

export interface LearningPack {
  id: string;
  title: string;
  subtitle: string;
  price: string;
  category: 'language_pack' | 'learning_pack';
  entitlementId: string;
  highlights: string[];
}

export interface SubscriptionPlan {
  id: 'free' | 'premium_monthly' | 'premium_yearly' | 'lifetime';
  name: string;
  price: string;
  cadence: string;
  badge?: string;
  entitlement: 'free' | 'premium' | 'lifetime';
  features: string[];
}

export const LANGUAGES: LanguageInfo[] = [
  { code: 'en', name: 'English', nativeName: 'English', bcp47: 'en-US', flag: '🇬🇧', script: 'Latin', tier: 'free' },
  { code: 'my', name: 'Burmese', nativeName: 'မြန်မာ', bcp47: 'my-MM', flag: '🇲🇲', script: 'Myanmar', tier: 'free' },
  { code: 'ja', name: 'Japanese', nativeName: '日本語', bcp47: 'ja-JP', flag: '🇯🇵', script: 'Kana/Kanji', tier: 'free' },
  { code: 'zh', name: 'Chinese', nativeName: '中文', bcp47: 'zh-CN', flag: '🇨🇳', script: 'Hanzi', tier: 'free' },
  { code: 'ko', name: 'Korean', nativeName: '한국어', bcp47: 'ko-KR', flag: '🇰🇷', script: 'Hangul', tier: 'free' },
  { code: 'th', name: 'Thai', nativeName: 'ไทย', bcp47: 'th-TH', flag: '🇹🇭', script: 'Thai', tier: 'premium' },
  { code: 'fr', name: 'French', nativeName: 'Français', bcp47: 'fr-FR', flag: '🇫🇷', script: 'Latin', tier: 'premium' },
  { code: 'de', name: 'German', nativeName: 'Deutsch', bcp47: 'de-DE', flag: '🇩🇪', script: 'Latin', tier: 'premium' },
];

export const SUBSCRIPTION_PLANS: SubscriptionPlan[] = [
  {
    id: 'free',
    name: 'Free',
    price: '$0',
    cadence: 'Forever',
    entitlement: 'free',
    features: [
      'Core Vocabulary & Flashcards (EN, MY, JA, ZH, KO)',
      'Ornagai English–Myanmar Dictionary (111,232 words)',
      'Daily Quizzes & Pronunciation TTS',
      'Basic Offline Starter Pack',
    ],
  },
  {
    id: 'premium_monthly',
    name: 'Premium',
    price: '$4.99',
    cadence: '/ month',
    badge: 'Most Popular',
    entitlement: 'premium',
    features: [
      'All 8+ Languages & Unlimited Grammar Lessons',
      'Unlimited AI & Neural Translation + Offline Packs',
      'Full 111k Offline Dictionary Sync Across Mac, Android & iPad',
      'Advanced Pronunciation Scoring & Cloud Progress Sync',
    ],
  },
  {
    id: 'lifetime',
    name: 'Lifetime',
    price: '$79',
    cadence: 'One-time payment',
    badge: 'Best Value',
    entitlement: 'lifetime',
    features: [
      'Permanent Unlock of All Core Premium Features',
      'All 8 Core Languages + Future Grammar Updates',
      'Cross-Platform Sync (Web, iPad PWA, Mac Native, Android)',
      'No Recurring Subscription Fees',
    ],
  },
];

export const OPTIONAL_PACKS: LearningPack[] = [
  {
    id: 'pack_jlpt_n5_n3',
    title: 'JLPT N5–N3 Mastery Pack',
    subtitle: '2,500 Japanese Kanji, vocabulary & listening drills',
    price: '$9.99',
    category: 'language_pack',
    entitlementId: 'pack_jlpt',
    highlights: ['Kanji stroke & reading mnemonics', 'Pitch accent audio', 'Mock JLPT quizzes'],
  },
  {
    id: 'pack_ielts_my',
    title: 'IELTS & Academic English (MY)',
    subtitle: 'Burmese-explained Band 7.5+ vocabulary & grammar patterns',
    price: '$11.99',
    category: 'learning_pack',
    entitlementId: 'pack_ielts',
    highlights: ['Academic collocations', 'Common Myanmar learner errors', 'Essay templates'],
  },
  {
    id: 'pack_hsk_1_4',
    title: 'HSK 1–4 Mandarin Pack',
    subtitle: 'Tone trainer, character radicals & daily dialogues',
    price: '$8.99',
    category: 'language_pack',
    entitlementId: 'pack_hsk',
    highlights: ['Pinyin tone drills', '1,200 HSK words', 'Offline audio pack'],
  },
  {
    id: 'pack_topik_kr',
    title: 'TOPIK & K-Culture Pack',
    subtitle: 'Hangul mastery, honorifics & natural conversation',
    price: '$8.99',
    category: 'language_pack',
    entitlementId: 'pack_topik',
    highlights: ['Formal vs casual speech', 'Listening exercises', 'Offline deck'],
  },
];
