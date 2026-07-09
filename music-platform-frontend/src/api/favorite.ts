import { http } from './http'

export function apiFavoriteStatus(musicId: number) {
  return http.get<boolean>('/api/favorite/status', { params: { musicId } })
}

export function apiFavoriteToggle(musicId: number) {
  return http.post<boolean>('/api/favorite/toggle', null, { params: { musicId } })
}

export function apiFavoriteAddToPlaylist(musicId: number, playlistId: number) {
  return http.post<void>('/api/favorite/add-to-playlist', null, { params: { musicId, playlistId } })
}

export function apiFavoriteRemoveFromPlaylist(musicId: number, playlistId: number) {
  return http.post<void>('/api/favorite/remove-from-playlist', null, { params: { musicId, playlistId } })
}

export function apiFavoriteMovePlaylist(musicId: number, fromPlaylistId: number, toPlaylistId: number) {
  return http.post<void>('/api/favorite/move-playlist', null, {
    params: { musicId, fromPlaylistId, toPlaylistId }
  })
}
