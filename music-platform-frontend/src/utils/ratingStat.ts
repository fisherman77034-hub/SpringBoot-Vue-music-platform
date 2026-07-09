import type { RatingStat } from '@/api/interaction'

export function ratingNumField(v: unknown): number {
  if (typeof v === 'number' && !Number.isNaN(v)) return v
  if (typeof v === 'string' && v.trim() !== '') {
    const n = Number(v)
    return Number.isNaN(n) ? 0 : n
  }
  return 0
}

export function normalizeRatingStat(raw: RatingStat): RatingStat {
  return {
    star1Count: ratingNumField(raw.star1Count),
    star2Count: ratingNumField(raw.star2Count),
    star3Count: ratingNumField(raw.star3Count),
    star4Count: ratingNumField(raw.star4Count),
    star5Count: ratingNumField(raw.star5Count),
    ratingCount: ratingNumField(raw.ratingCount),
    ratingSum: ratingNumField(raw.ratingSum),
    avgScore: ratingNumField(raw.avgScore),
    myStar: raw.myStar != null ? ratingNumField(raw.myStar) : undefined
  }
}
