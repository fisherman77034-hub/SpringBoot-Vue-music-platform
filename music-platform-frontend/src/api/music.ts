import { http } from './http'
import type { Music, Mood, MusicAdminPage } from './types'

export function apiMusicDetail(id: number) {
  return http.get<Music>(`/api/music/${id}`)
}

export function apiMusicList(page = 0, size = 20) {
  return http.get<Music[]>('/api/music/list', { params: { page, size } })
}

export function apiMusicSearch(kw: string, page = 0, size = 20) {
  return http.get<Music[]>('/api/music/search', { params: { kw, page, size } })
}

export function apiMusicRecommend(mood: Mood, limit = 6) {
  return http.get<Music[]>('/api/music/recommend', { params: { mood, limit } })
}

export function apiMusicPlayed(id: number) {
  return http.post<void>(`/api/music/${id}/play`)
}

export function apiMusicAdminUpload(form: {
  title: string
  artist: string
  album?: string
  moodTag: Mood
  durationMs?: number
  musicFile: File
  coverFile?: File | null
  lyricFile?: File | null
}) {
  const fd = new FormData()
  fd.append('title', form.title)
  fd.append('artist', form.artist)
  if (form.album) fd.append('album', form.album)
  fd.append('moodTag', form.moodTag)
  fd.append('durationMs', String(form.durationMs ?? 0))
  fd.append('musicFile', form.musicFile)
  if (form.coverFile) fd.append('coverFile', form.coverFile)
  if (form.lyricFile) fd.append('lyricFile', form.lyricFile)
  return http.post<number>('/api/music/admin/upload', fd)
}

export function apiMusicAdminPage(kw: string, page = 0, size = 20) {
  return http.get<MusicAdminPage>('/api/music/admin/page', {
    params: { kw: kw || undefined, page, size }
  })
}

export function apiMusicAdminUpdate(
  id: number,
  form: {
    title: string
    artist: string
    album?: string
    moodTag: Mood
    durationMs: number
    musicFile?: File | null
    coverFile?: File | null
    lyricFile?: File | null
    removeCover: boolean
    removeLyric: boolean
  }
) {
  const fd = new FormData()
  fd.append('title', form.title)
  fd.append('artist', form.artist)
  fd.append('album', form.album ?? '')
  fd.append('moodTag', form.moodTag)
  fd.append('durationMs', String(form.durationMs))
  if (form.musicFile) fd.append('musicFile', form.musicFile)
  if (form.coverFile) fd.append('coverFile', form.coverFile)
  if (form.lyricFile) fd.append('lyricFile', form.lyricFile)
  fd.append('removeCover', String(form.removeCover))
  fd.append('removeLyric', String(form.removeLyric))
  return http.post<void>(`/api/music/admin/${id}/update`, fd)
}

export function apiMusicAdminDelete(id: number) {
  return http.delete<void>(`/api/music/admin/${id}`)
}

