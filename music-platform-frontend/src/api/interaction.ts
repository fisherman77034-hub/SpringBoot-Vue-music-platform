import { http } from './http'

export type MusicComment = {
  id: number
  musicId: number
  userId: number
  username: string
  nickname: string
  content: string
  createdAt: string
}

export type RatingStat = {
  star1Count: number
  star2Count: number
  star3Count: number
  star4Count: number
  star5Count: number
  ratingCount: number
  ratingSum: number
  avgScore: number
  /** 仅登录且曾评过时由服务端返回 */
  myStar?: number
}

export function apiComments(musicId: number) {
  return http.get<MusicComment[]>(`/api/interaction/${musicId}/comments`)
}

export function apiPostComment(musicId: number, content: string) {
  return http.post<void>(`/api/interaction/${musicId}/comment`, { content })
}

export function apiRatingStat(musicId: number) {
  return http.get<RatingStat>(`/api/interaction/${musicId}/rating-stat`)
}

export function apiPostRating(musicId: number, star: number) {
  return http.post<void>(`/api/interaction/${musicId}/rate`, { star })
}
