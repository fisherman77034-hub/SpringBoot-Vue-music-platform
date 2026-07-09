import { defineStore } from 'pinia'
import type { Music } from '@/api/types'
import { apiMusicPlayed } from '@/api/music'
import { resolveMediaUrl } from '@/utils/media'
import { useAuthStore } from '@/stores/auth'
import {
  apiPlayQueueAdd,
  apiPlayQueueList,
  apiPlayQueueReplace,
  toPlayQueueEntries
} from '@/api/playQueue'

export type PlayMode = 'ORDER' | 'SINGLE' | 'SHUFFLE'

type State = {
  playlist: Music[]
  currentIndex: number
  playing: boolean
  volume: number
  mode: PlayMode
  /** 由 PlayerBar 绑定，用于在用户点击链路内同步调用 play()，避免被浏览器拦截 */
  _audioEl: HTMLAudioElement | null
  _lastPlaySrc: string
  /** 当前播放进度（秒），供详情页歌词高亮 */
  playbackTimeSec: number
}

export const usePlayerStore = defineStore('player', {
  state: (): State => ({
    playlist: [],
    currentIndex: 0,
    playing: false,
    volume: 0.8,
    mode: 'ORDER',
    _audioEl: null,
    _lastPlaySrc: '',
    playbackTimeSec: 0
  }),
  getters: {
    current(state): Music | null {
      return state.playlist[state.currentIndex] ?? null
    }
  },
  actions: {
    bindAudio(el: HTMLAudioElement | null) {
      this._audioEl = el
      if (el) el.volume = this.volume
      if (el && this.playing && this.current?.musicUrl) {
        this.attachAndPlay()
      }
    },

    reportPlaybackTime(sec: number) {
      this.playbackTimeSec = sec
    },

    /** 根据当前曲目设置 audio.src 并在 playing 为 true 时 play（load 异步，需 canplay 兜底） */
    attachAndPlay() {
      const el = this._audioEl
      const m = this.current
      if (!el || !m?.musicUrl) return
      const url = resolveMediaUrl(m.musicUrl)
      if (!url) return

      const tryPlay = () => {
        if (!this.playing || this._audioEl !== el) return
        if (resolveMediaUrl(this.current?.musicUrl) !== url) return
        void el.play().catch(() => {})
      }

      if (this._lastPlaySrc !== url) {
        this._lastPlaySrc = url
        el.src = url
        el.load()
        el.addEventListener('canplay', tryPlay, { once: true })
        el.addEventListener('loadeddata', tryPlay, { once: true })
        tryPlay()
      } else if (this.playing) {
        tryPlay()
      }

      if (!this.playing) {
        el.pause()
      }
    },

    setPlaylist(list: Music[], startIndex = 0) {
      this.playlist = list
      this.currentIndex = Math.min(Math.max(0, startIndex), Math.max(0, list.length - 1))
    },

    /** 从服务端拉取队列并写入当前播放列表，尽量保持当前曲目索引（失败时静默跳过，避免阻塞播放） */
    async syncQueueFromServer() {
      const auth = useAuthStore()
      if (!auth.token) return
      try {
        const rows = await apiPlayQueueList()
        const list = toPlayQueueEntries(rows)
        const prevId = this.current?.id
        this.playlist = list
        if (prevId != null) {
          const idx = list.findIndex((x) => x.id === prevId)
          this.currentIndex = idx >= 0 ? idx : Math.min(this.currentIndex, Math.max(0, list.length - 1))
        } else {
          this.currentIndex = Math.min(this.currentIndex, Math.max(0, list.length - 1))
        }
      } catch {
        /* 常见原因：未执行 db/migration_queue_and_liked_playlist.sql，表不存在导致 500 */
      }
    },

    /**
     * 设置待播列表；若已登录则同步替换服务端队列（用于首页/歌单「播放」等）。
     */
    async setPlaylistPersist(list: Music[], startIndex = 0) {
      const auth = useAuthStore()
      const plain = list.map(({ queueItemId: _q, ...m }) => m)
      if (!auth.token) {
        this.setPlaylist(plain, startIndex)
        return
      }
      try {
        await apiPlayQueueReplace(plain.map((m) => m.id))
        await this.syncQueueFromServer()
        this.currentIndex = Math.min(Math.max(0, startIndex), Math.max(0, this.playlist.length - 1))
      } catch {
        this.setPlaylist(plain, startIndex)
      }
    },

    /** 将歌曲追加到服务端播放队列并刷新本地列表（需已登录） */
    async appendToQueuePersist(musicId: number) {
      const auth = useAuthStore()
      if (!auth.token) return false
      try {
        await apiPlayQueueAdd(musicId)
        await this.syncQueueFromServer()
        return true
      } catch {
        return false
      }
    },

    playIndex(i: number) {
      if (!this.playlist.length) return
      this.currentIndex = Math.min(Math.max(0, i), this.playlist.length - 1)
      this.playing = true
      const cur = this.current
      if (cur) apiMusicPlayed(cur.id).catch(() => {})
      this.attachAndPlay()
      // 模板 ref 偶发晚于 playIndex，下一帧再试一次
      queueMicrotask(() => this.attachAndPlay())
      requestAnimationFrame(() => this.attachAndPlay())
    },

    togglePlay() {
      if (!this.playlist.length || !this.current) return
      if (this.playing) {
        this.playing = false
        this._audioEl?.pause()
      } else {
        this.playing = true
        this.attachAndPlay()
      }
    },

    next() {
      if (!this.playlist.length) return
      if (this.mode === 'SINGLE') {
        this.playing = true
        const el = this._audioEl
        if (el) {
          el.currentTime = 0
          void el.play().catch(() => {})
        } else {
          this.attachAndPlay()
        }
        return
      }
      if (this.mode === 'SHUFFLE') {
        const n = Math.floor(Math.random() * this.playlist.length)
        this.playIndex(n)
        return
      }
      const n = (this.currentIndex + 1) % this.playlist.length
      this.playIndex(n)
    },

    prev() {
      if (!this.playlist.length) return
      const n = (this.currentIndex - 1 + this.playlist.length) % this.playlist.length
      this.playIndex(n)
    },

    toggleMode() {
      this.mode = this.mode === 'ORDER' ? 'SHUFFLE' : this.mode === 'SHUFFLE' ? 'SINGLE' : 'ORDER'
    }
  }
})
