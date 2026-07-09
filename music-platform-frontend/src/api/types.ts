export type ApiOk<T> = {
  code: 0
  message: 'OK'
  data: T
}

export type Mood = 'HAPPY' | 'SAD' | 'CALM' | 'ENERGETIC' | 'FOCUS'

export type Gender = 'MALE' | 'FEMALE' | 'OTHER'

export type UserProfile = {
  id: number
  username: string
  nickname: string
  avatarUrl: string | null
  bio: string | null
  gender: Gender | null
  age: number | null
  mood: Mood
  role: 'USER' | 'ADMIN'
}

export type Music = {
  id: number
  title: string
  artist: string
  album: string | null
  moodTag: Mood
  durationMs: number
  playCount: number
  musicUrl: string
  coverUrl: string | null
  lyricUrl: string | null
  /** 服务端播放队列项 id，仅队列同步时使用 */
  queueItemId?: number
}

export type PlayQueueEntry = {
  queueItemId: number
  music: Music
}

export type MusicAdminPage = {
  records: Music[]
  total: number
}

