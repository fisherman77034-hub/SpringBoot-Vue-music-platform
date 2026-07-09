import { http } from './http'
import type { Music } from './types'

export type Playlist = { id: number; name: string; defaultLiked: boolean }

export function apiMyPlaylists() {
  return http.get<Playlist[]>('/api/playlist/my')
}

export function apiCreatePlaylist(name: string) {
  return http.post<number>('/api/playlist/create', { name })
}

export function apiPlaylistSongs(id: number) {
  return http.get<Music[]>(`/api/playlist/${id}/songs`)
}

export function apiPlaylistAdd(id: number, musicId: number) {
  return http.post<void>(`/api/playlist/${id}/add`, null, { params: { musicId } })
}

export function apiPlaylistRemove(id: number, musicId: number) {
  return http.post<void>(`/api/playlist/${id}/remove`, null, { params: { musicId } })
}

export function apiPlaylistDelete(id: number) {
  return http.delete<void>(`/api/playlist/${id}`)
}

