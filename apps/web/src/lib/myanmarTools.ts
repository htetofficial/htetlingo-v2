/**
 * Myanmar Language & Culture Utilities
 * Adapted from:
 *  - kaungmyatlwin/myanmar-phonenumber (telecom operator & network detection)
 *  - yan9a/mmcal (Myanmar Era & lunar calendar calculation)
 *  - lwinmoe/segment (Burmese Unicode syllable segmentation)
 */

export interface PhoneCheckResult {
  valid: boolean;
  normalized: string;
  operator: 'MPT' | 'ATOM (Telenor)' | 'Ooredoo' | 'Mytel' | 'MEC' | 'Unknown';
  networkType: string;
}

const MY_DIGITS: Record<string, string> = {
  '၀': '0', '၁': '1', '၂': '2', '၃': '3', '၄': '4',
  '၅': '5', '၆': '6', '၇': '7', '၈': '8', '၉': '9',
};

export function normalizeMyanmarPhone(raw: string): string {
  const ascii = raw.replace(/[၀-၉]/g, (ch) => MY_DIGITS[ch] ?? ch);
  let cleaned = ascii.replace(/[\s\-()]/g, '');
  if (cleaned.startsWith('+959')) cleaned = '09' + cleaned.slice(4);
  else if (cleaned.startsWith('959')) cleaned = '09' + cleaned.slice(3);
  else if (cleaned.startsWith('+95')) cleaned = '0' + cleaned.slice(3);
  return cleaned;
}

export function checkMyanmarPhone(raw: string): PhoneCheckResult {
  const n = normalizeMyanmarPhone(raw);
  const valid = /^09\d{7,9}$/.test(n);
  if (!valid) {
    return { valid: false, normalized: n, operator: 'Unknown', networkType: 'Invalid format' };
  }

  // Operator prefixes adapted from kaungmyatlwin/myanmar-phonenumber
  if (/^09(9[4-8])\d{7}$/.test(n)) {
    return { valid: true, normalized: n, operator: 'Ooredoo', networkType: 'GSM / 4G' };
  }
  if (/^09(7[4-9])\d{7}$/.test(n)) {
    return { valid: true, normalized: n, operator: 'ATOM (Telenor)', networkType: 'GSM / 4G' };
  }
  if (/^09(6[5-9])\d{7}$/.test(n)) {
    return { valid: true, normalized: n, operator: 'Mytel', networkType: 'GSM / 4G' };
  }
  if (/^09(3[0-6])\d{6,7}$/.test(n)) {
    return { valid: true, normalized: n, operator: 'MEC', networkType: 'CDMA / GSM' };
  }
  if (/^09(2[0-6]|4[0-5]|4[7-9]|5[0-6]|8[8-9])\d{5,7}$/.test(n)) {
    return { valid: true, normalized: n, operator: 'MPT', networkType: 'GSM / 4G' };
  }
  return { valid: true, normalized: n, operator: 'MPT', networkType: 'GSM' };
}

const MY_WEEKDAYS = [
  'တနင်္ဂနွေ (Sunday)',
  'တနင်္လာ (Monday)',
  'အင်္ဂါ (Tuesday)',
  'ဗုဒ္ဓဟူး (Wednesday)',
  'ကြာသပတေး (Thursday)',
  'သောကြာ (Friday)',
  'စနေ (Saturday)',
];

const MY_MONTHS = [
  'တန်ခူး (Tagu)', 'ကဆုန် (Kason)', 'နယုန် (Nayon)', 'ဝါဆို (Waso)',
  'ဝါခေါင် (Wagaung)', 'တော်သလင်း (Tawthalin)', 'သီတင်းကျွတ် (Thadingyut)',
  'တန်ဆောင်မုန်း (Tazaungmon)', 'နတ်တော် (Nadaw)', 'ပြာသို (Pyatho)',
  'တပို့တွဲ (Tabodwe)', 'တပေါင်း (Tabaung)',
];

export interface MyanmarDateInfo {
  myanmarYear: number;
  weekday: string;
  approxMonth: string;
  gregorian: string;
}

/**
 * Lightweight Myanmar Era (ME) date summary inspired by yan9a/mmcal.
 * Thingyan (Myanmar New Year) falls around April 16–17.
 */
export function getMyanmarDate(date: Date = new Date()): MyanmarDateInfo {
  const y = date.getFullYear();
  const m = date.getMonth(); // 0..11
  const d = date.getDate();
  const beforeThingyan = m < 3 || (m === 3 && d < 17);
  const myanmarYear = y - (beforeThingyan ? 639 : 638);
  // Map Gregorian month offset from April (Tagu)
  const monthIdx = (m + 9) % 12;
  return {
    myanmarYear,
    weekday: MY_WEEKDAYS[date.getDay()],
    approxMonth: MY_MONTHS[monthIdx],
    gregorian: date.toISOString().slice(0, 10),
  };
}

/**
 * Burmese Unicode syllable segmenter inspired by lwinmoe/segment.
 */
const MY_SYLLABLE_RE =
  /([\u1000-\u102A\u103F\u104A-\u104F](?:[\u1039][\u1000-\u1021])?[\u102B-\u103E]*)/g;

export function segmentBurmeseSyllables(text: string): string[] {
  const matches = text.match(MY_SYLLABLE_RE);
  return matches ? matches.filter((s) => s.trim().length > 0) : [];
}
