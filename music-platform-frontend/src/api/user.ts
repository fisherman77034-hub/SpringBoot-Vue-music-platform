import { http } from './http'
import type { UserProfile } from './types'

export function apiUpdateProfile(payload: {
  nickname: string
  bio: string
  gender: 'MALE' | 'FEMALE' | 'OTHER' | null
  age: number | null
}) {
  return http.put<void>('/api/user/profile', payload)
}

export function apiUploadAvatar(file: File) {
  const fd = new FormData()
  fd.append('file', file)
  return http.post<string>('/api/user/avatar', fd)
}
