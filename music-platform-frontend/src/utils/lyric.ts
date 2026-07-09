/** 解析 LRC，返回按时间排序的歌词行（秒） */
export type LrcLine = { time: number; text: string }

/** 常见元信息行（整行），非歌词；offset 单独处理 */
const META_LINE =
  /^\[(ti|ar|al|by|length|sign|re|tool|la|index|hash|offset|ve|total):/i

function fracFromGroup(g: string | undefined): number {
  if (!g) return 0
  return parseInt(g.padEnd(3, '0').slice(0, 3), 10) / 1000
}

type TagSpan = { start: number; end: number; time: number }

/**
 * 提取一行内所有时间标签（左→右）。
 * - 支持 [mm:ss.xx]、[m:ss]、标签内任意空格：[ 01:23.45 ]
 * - 支持 [hh:mm:ss.xx]（常见于导出 LRC），且避免与两段式标签重叠误判
 */
function collectTimeTags(line: string, offsetSec: number): TagSpan[] {
  const threePart: TagSpan[] = []
  const re3 = /\[\s*(\d{1,3})\s*:\s*(\d{1,2})\s*:\s*(\d{2})(?:\.(\d{1,3}))?\s*\]/g
  let m: RegExpExecArray | null
  while ((m = re3.exec(line)) !== null) {
    const h = parseInt(m[1], 10)
    const min = parseInt(m[2], 10)
    const s = parseInt(m[3], 10)
    const frac = fracFromGroup(m[4])
    threePart.push({
      start: m.index,
      end: m.index + m[0].length,
      time: Math.max(0, h * 3600 + min * 60 + s + frac + offsetSec)
    })
  }

  const twoPart: TagSpan[] = []
  const re2 = /\[\s*(\d{1,3})\s*:\s*(\d{2})(?:\.(\d{1,3}))?\s*\]/g
  while ((m = re2.exec(line)) !== null) {
    const start = m.index
    const end = start + m[0].length
    const overlaps3 = threePart.some((t) => start < t.end && end > t.start)
    if (overlaps3) continue
    const min = parseInt(m[1], 10)
    const sec = parseInt(m[2], 10)
    const frac = fracFromGroup(m[3])
    twoPart.push({
      start,
      end,
      time: Math.max(0, min * 60 + sec + frac + offsetSec)
    })
  }

  return [...threePart, ...twoPart].sort((a, b) => a.start - b.start)
}

export function parseLrc(text: string): LrcLine[] {
  let normalized = text
  if (normalized.charCodeAt(0) === 0xfeff) normalized = normalized.slice(1)
  let offsetSec = 0
  const lines: LrcLine[] = []

  for (const raw of normalized.split(/\r?\n/)) {
    const line = raw.trim()
    if (!line) continue

    const offsetMatch = line.match(/^\[\s*offset:\s*(-?\d+)\s*]\s*$/i)
    if (offsetMatch) {
      const ms = Number(offsetMatch[1])
      if (!Number.isNaN(ms)) offsetSec = ms / 1000
      continue
    }

    if (META_LINE.test(line)) continue

    const tags = collectTimeTags(line, offsetSec)
    if (!tags.length) continue

    for (let i = 0; i < tags.length; i++) {
      const tagEnd = tags[i].end
      const nextStart = i + 1 < tags.length ? tags[i + 1].start : line.length
      const lyricText = line.slice(tagEnd, nextStart).trim()
      if (!lyricText) continue
      lines.push({ time: tags[i].time, text: lyricText })
    }
  }
  lines.sort((a, b) => a.time - b.time)
  return lines
}

/** 当前时间对应的歌词行索引，无则 -1 */
export function currentLrcIndex(lines: LrcLine[], currentSec: number): number {
  if (!lines.length) return -1
  let lo = 0
  let hi = lines.length - 1
  let ans = -1
  while (lo <= hi) {
    const mid = (lo + hi) >> 1
    if (lines[mid].time <= currentSec) {
      ans = mid
      lo = mid + 1
    } else {
      hi = mid - 1
    }
  }
  return ans
}
