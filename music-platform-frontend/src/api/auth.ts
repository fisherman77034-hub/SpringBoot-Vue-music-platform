import { http } from './http'
import type { UserProfile, Mood } from './types'

export function apiRegister(username: string, password: string, nickname: string) {
  return http.post<void>('/api/auth/register', { username, password, nickname })
}

export function apiLogin(username: string, password: string) {
  return http.post<{ token: string }>('/api/auth/login', { username, password })
}

export function apiMe() {
  return http.get<UserProfile>('/api/user/me')
}

export function apiUpdateMood(mood: Mood) {
  return http.post<void>('/api/user/mood', { mood })
}

