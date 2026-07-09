import { http } from './http'
import type { Music, PlayQueueEntry } from './types'

function stripQueueMeta(m: Music): Music {
  const { queueItemId: _q, ...rest } = m
  return rest
}

/** 列表接口返回的 music 不含 queueItemId，与队列项 id 组合为前端统一结构 */
export function toPlayQueueEntries(rows: PlayQueueEntry[]): Music[] {
  return rows.map((r) => ({
    ...stripQueueMeta(r.music),
    queueItemId: r.queueItemId
  }))
}

export function apiPlayQueueList() {
  return http.get<PlayQueueEntry[]>('/api/play-queue')
}
export function apiPlayQueueAdd(musicId: number) {
  return http.post<void>('/api/play-queue/add', null, { params: { musicId } })
}
export function apiPlayQueueRemoveItem(itemId: number) {
  return http.delete<void>(`/api/play-queue/item/${itemId}`)
}
export function apiPlayQueueClear() {
  return http.delete<void>('/api/play-queue/clear')
}
export function apiPlayQueueReorder(orderedIds: number[]) {
  return http.put<void>('/api/play-queue/reorder', { orderedIds })
}
export function apiPlayQueueReplace(musicIds: number[]) {
  return http.put<void>('/api/play-queue/replace', { musicIds })
}
