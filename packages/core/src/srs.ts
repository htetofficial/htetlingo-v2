import type { SrsState } from './types.ts';

/**
 * SM-2 Spaced Repetition Algorithm
 * quality: 0 (Again), 3 (Hard), 4 (Good), 5 (Easy)
 */
export function reviewCard(prev: SrsState | undefined, cardId: string, quality: 0 | 3 | 4 | 5): SrsState {
  const now = Date.now();
  const base: SrsState = prev ?? {
    cardId,
    easeFactor: 2.5,
    intervalDays: 0,
    repetitions: 0,
    nextReviewAt: now,
  };

  let { easeFactor, intervalDays, repetitions } = base;

  if (quality < 3) {
    repetitions = 0;
    intervalDays = 1;
  } else {
    if (repetitions === 0) {
      intervalDays = 1;
    } else if (repetitions === 1) {
      intervalDays = 6;
    } else {
      intervalDays = Math.round(intervalDays * easeFactor);
    }
    repetitions += 1;
  }

  easeFactor = Math.max(1.3, easeFactor + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)));

  return {
    cardId,
    easeFactor: Number(easeFactor.toFixed(2)),
    intervalDays,
    repetitions,
    nextReviewAt: now + intervalDays * 24 * 60 * 60 * 1000,
  };
}
