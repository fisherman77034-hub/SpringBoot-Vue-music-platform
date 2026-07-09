<template>
  <div class="page" v-loading="loading">
    <el-page-header @back="goBack" class="back">
      <template #content>
        <span class="back-title">歌曲详情</span>
      </template>
    </el-page-header>

    <template v-if="music">
      <el-row :gutter="20" class="hero">
        <el-col :xs="24" :sm="10" :md="8">
          <div class="cover-wrap">
            <img v-if="music.coverUrl" :src="coverSrc" class="cover-big" alt="cover" />
            <div v-else class="cover-big placeholder"></div>
          </div>
        </el-col>
        <el-col :xs="24" :sm="14" :md="16">
          <h1 class="song-title">{{ music.title }}</h1>
          <p class="song-meta">
            <span>{{ music.artist }}</span>
            <span v-if="music.album" class="dot">·</span>
            <span v-if="music.album">{{ music.album }}</span>
          </p>
          <div class="tags">
            <el-tag effect="plain">{{ moodLabel(music.moodTag) }}</el-tag>
            <el-tag type="info" effect="plain">播放 {{ music.playCount }}</el-tag>
            <el-tag type="info" effect="plain">时长 {{ fmtDuration(music.durationMs) }}</el-tag>
          </div>
          <div class="actions">
            <el-button type="primary" size="large" @click="playThis">
              {{ isCurrentPlaying ? (player.playing ? '暂停' : '播放') : '播放' }}
            </el-button>
            <el-button v-if="auth.me" size="large" @click="enqueueThis">加入队列</el-button>
            <el-button
              v-if="auth.me"
              size="large"
              :type="favorited ? 'danger' : 'default'"
              @click="toggleFavorite"
            >
              {{ favorited ? '已喜欢' : '喜欢' }}
            </el-button>
            <el-dropdown v-if="auth.me && playlists.length" trigger="click" @command="onCollectCommand">
              <el-button size="large">
                收藏到歌单
                <span class="caret">▼</span>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-for="p in playlists" :key="p.id" :command="p.id">
                    {{ p.name }}{{ p.defaultLiked ? '（默认）' : '' }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-col>
      </el-row>

      <el-card class="section rating-card" shadow="never">
        <template #header>
          <span class="sec-title">评分</span>
          <span v-if="ratingStat && ratingStat.ratingCount > 0" class="count">
            （{{ ratingStat.ratingCount }} 人）
          </span>
        </template>

        <div class="rating-head">
          <div v-if="ratingStat && ratingStat.ratingCount > 0" class="avg-row">
            <el-rate :model-value="avgScoreDisplay" disabled allow-half />
            <span class="avg-num">{{ avgScoreDisplay.toFixed(1) }} 分</span>
          </div>
          <p v-else class="muted no-rating">暂无评分，欢迎登录后成为第一个评分的人</p>
        </div>

        <div v-if="ratingStat && ratingStat.ratingCount > 0" class="rating-bars">
          <div v-for="row in ratingBarRows" :key="row.star" class="bar-row">
            <span class="bar-label">{{ row.star }} 星</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: row.pct }" />
            </div>
            <span class="bar-num">{{ row.count }}</span>
          </div>
        </div>

        <div v-if="auth.me" class="rating-mine">
          <span class="mine-label">我的评分</span>
          <el-rate v-model="myStarEditable" :disabled="ratingSubmitting" @change="onMyStarChange" />
          <span class="mine-hint muted">{{ myStarHint }}</span>
        </div>
        <el-alert v-else type="info" :closable="false" show-icon title="登录后可参与评分" class="login-hint">
          <el-button type="primary" link @click="goLogin">去登录</el-button>
        </el-alert>
      </el-card>

      <el-card class="section" shadow="never">
        <template #header>
          <span class="sec-title">歌词</span>
        </template>
        <div v-if="lyricLoading" class="muted">加载歌词中…</div>
        <div v-else-if="lrcLines.length" class="lyric-panel" ref="lyricPanelRef">
          <div
            v-for="(line, i) in lrcLines"
            :key="i"
            :data-lrc-i="i"
            class="lyric-line"
            :class="{ active: i === activeLrcIndex }"
          >
            {{ line.text }}
          </div>
        </div>
        <pre v-else-if="plainLyric" class="lyric-plain">{{ plainLyric }}</pre>
        <el-empty v-else description="暂无歌词" />
      </el-card>

      <el-card class="section" shadow="never">
        <template #header>
          <span class="sec-title">评论</span>
          <span class="count">（{{ comments.length }}）</span>
        </template>

        <div v-if="auth.me" class="composer">
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="3"
            maxlength="800"
            show-word-limit
            placeholder="写下你的想法…"
          />
          <el-button type="primary" class="send" :loading="sending" @click="sendComment">发表评论</el-button>
        </div>
        <el-alert v-else type="info" :closable="false" show-icon title="登录后可发表评论" class="login-hint">
          <el-button type="primary" link @click="goLogin">去登录</el-button>
        </el-alert>

        <el-empty v-if="!comments.length" description="还没有评论" />
        <div v-else class="comment-list">
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="c-head">
              <span class="nick">{{ c.nickname || c.username }}</span>
              <span class="time">{{ formatTime(c.createdAt) }}</span>
            </div>
            <div class="c-body">{{ c.content }}</div>
          </div>
        </div>
      </el-card>
    </template>

    <el-empty v-else-if="!loading" description="歌曲不存在或已删除" />
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiMusicDetail } from '@/api/music'
import { apiFavoriteAddToPlaylist, apiFavoriteStatus, apiFavoriteToggle } from '@/api/favorite'
import { apiMyPlaylists, type Playlist } from '@/api/playlist'
import {
  apiComments,
  apiPostComment,
  apiPostRating,
  apiRatingStat,
  type MusicComment,
  type RatingStat
} from '@/api/interaction'
import type { Mood, Music } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { usePlayerStore } from '@/stores/player'
import { resolveMediaUrl } from '@/utils/media'
import { currentLrcIndex, parseLrc, type LrcLine } from '@/utils/lyric'
import { normalizeRatingStat } from '@/utils/ratingStat'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const player = usePlayerStore()

const musicId = computed(() => Number(route.params.id))

const loading = ref(true)
const music = ref<Music | null>(null)
const coverSrc = computed(() => (music.value?.coverUrl ? resolveMediaUrl(music.value.coverUrl) : ''))

const lyricLoading = ref(false)
const lrcLines = ref<LrcLine[]>([])
const plainLyric = ref('')
const lyricPanelRef = ref<HTMLElement | null>(null)

const comments = ref<MusicComment[]>([])
const commentText = ref('')
const sending = ref(false)

const favorited = ref(false)
const playlists = ref<Playlist[]>([])

const ratingStat = ref<RatingStat | null>(null)
const myStarEditable = ref(0)
const ratingSubmitting = ref(false)

const avgScoreDisplay = computed(() => {
  const s = ratingStat.value
  if (!s || s.ratingCount <= 0) return 0
  return Math.min(5, Math.max(0, s.avgScore))
})

const ratingBarMax = computed(() => {
  const s = ratingStat.value
  if (!s) return 0
  return Math.max(
    1,
    s.star1Count,
    s.star2Count,
    s.star3Count,
    s.star4Count,
    s.star5Count
  )
})

const ratingBarRows = computed(() => {
  const s = ratingStat.value
  if (!s) return []
  const pairs: { star: number; count: number }[] = [
    { star: 5, count: s.star5Count },
    { star: 4, count: s.star4Count },
    { star: 3, count: s.star3Count },
    { star: 2, count: s.star2Count },
    { star: 1, count: s.star1Count }
  ]
  const max = ratingBarMax.value
  return pairs.map((p) => ({
    ...p,
    pct: `${Math.round((100 * p.count) / max)}%`
  }))
})

const myStarHint = computed(() => {
  const s = ratingStat.value
  if (s?.myStar != null && s.myStar >= 1) return '可随时点击修改评分'
  return '点击星星提交评分'
})

const isCurrentPlaying = computed(() => player.current?.id === music.value?.id)

const lyricTimeSec = computed(() =>
  isCurrentPlaying.value ? player.playbackTimeSec : 0
)

const activeLrcIndex = computed(() => currentLrcIndex(lrcLines.value, lyricTimeSec.value))

/** 歌词面板平滑滚动（换行时取消上一段动画，避免连续 scrollIntoView 发飘） */
let lyricScrollRaf: number | null = null
const LYRIC_SCROLL_MS = 420

function scrollActiveLyricBuffered(idx: number) {
  const box = lyricPanelRef.value
  const el = box?.querySelector(`[data-lrc-i="${idx}"]`) as HTMLElement | null
  if (!box || !el) return

  const relTop = el.getBoundingClientRect().top - box.getBoundingClientRect().top + box.scrollTop
  const target =
    relTop + el.getBoundingClientRect().height / 2 - box.getBoundingClientRect().height / 2
  const maxScroll = Math.max(0, box.scrollHeight - box.clientHeight)
  const clamped = Math.max(0, Math.min(target, maxScroll))
  const start = box.scrollTop
  if (Math.abs(clamped - start) < 3) return

  if (lyricScrollRaf !== null) {
    cancelAnimationFrame(lyricScrollRaf)
    lyricScrollRaf = null
  }

  const t0 = performance.now()
  const easeOutCubic = (t: number) => 1 - Math.pow(1 - t, 3)

  const step = (now: number) => {
    const b = lyricPanelRef.value
    if (!b || !b.contains(el)) {
      lyricScrollRaf = null
      return
    }
    const u = Math.min(1, (now - t0) / LYRIC_SCROLL_MS)
    b.scrollTop = start + (clamped - start) * easeOutCubic(u)
    if (u < 1) lyricScrollRaf = requestAnimationFrame(step)
    else lyricScrollRaf = null
  }
  lyricScrollRaf = requestAnimationFrame(step)
}

const moods: { value: Mood; label: string }[] = [
  { value: 'HAPPY', label: '开心' },
  { value: 'SAD', label: '难过' },
  { value: 'CALM', label: '平静' },
  { value: 'ENERGETIC', label: '活力' },
  { value: 'FOCUS', label: '专注' }
]

function moodLabel(m: Mood) {
  return moods.find((x) => x.value === m)?.label ?? m
}

function fmtDuration(ms: number) {
  if (!ms) return '--:--'
  const s = Math.floor(ms / 1000)
  const m = Math.floor(s / 60)
  const r = s % 60
  return `${String(m).padStart(2, '0')}:${String(r).padStart(2, '0')}`
}

function formatTime(raw: unknown) {
  if (Array.isArray(raw) && raw.length >= 6) {
    const [y, mo, d, h, mi] = raw as number[]
    return `${y}-${String(mo).padStart(2, '0')}-${String(d).padStart(2, '0')} ${String(h).padStart(2, '0')}:${String(mi).padStart(2, '0')}`
  }
  if (typeof raw === 'string' && raw.includes('T')) {
    return raw.replace('T', ' ').slice(0, 16)
  }
  return raw == null ? '' : String(raw)
}

function goBack() {
  router.back()
}

function goLogin() {
  router.push({ name: 'login', query: { redirect: route.fullPath } })
}

async function loadLyrics(m: Music) {
  lrcLines.value = []
  plainLyric.value = ''
  if (!m.lyricUrl?.trim()) return
  lyricLoading.value = true
  try {
    const url = resolveMediaUrl(m.lyricUrl)
    const r = await fetch(url)
    if (!r.ok) {
      console.warn('[lyric] HTTP', r.status, url)
      return
    }
    const text = await r.text()
    const parsed = parseLrc(text)
    if (parsed.length) lrcLines.value = parsed
    else plainLyric.value = text.trim() || ''
  } catch (e) {
    console.warn('[lyric] fetch failed', m.lyricUrl, e)
  } finally {
    lyricLoading.value = false
  }
}

async function loadComments() {
  try {
    comments.value = await apiComments(musicId.value)
  } catch {
    comments.value = []
  }
}

async function loadRatingStat() {
  try {
    const raw = await apiRatingStat(musicId.value)
    ratingStat.value = normalizeRatingStat(raw)
    const mine = ratingStat.value.myStar
    myStarEditable.value = mine != null && mine >= 1 ? mine : 0
  } catch {
    ratingStat.value = null
    myStarEditable.value = 0
  }
}

async function loadFavoriteSide() {
  if (!auth.me || !music.value) {
    favorited.value = false
    playlists.value = []
    return
  }
  try {
    favorited.value = await apiFavoriteStatus(music.value.id)
    playlists.value = await apiMyPlaylists()
  } catch {
    favorited.value = false
    playlists.value = []
  }
}

async function loadAll() {
  if (lyricScrollRaf !== null) {
    cancelAnimationFrame(lyricScrollRaf)
    lyricScrollRaf = null
  }
  loading.value = true
  try {
    const m = await apiMusicDetail(musicId.value)
    if (!m || Number.isNaN(musicId.value)) {
      music.value = null
      return
    }
    music.value = m
    await Promise.all([loadLyrics(m), loadComments(), loadFavoriteSide(), loadRatingStat()])
  } catch {
    music.value = null
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function playThis() {
  if (!music.value) return
  if (isCurrentPlaying.value) {
    player.togglePlay()
    return
  }
  try {
    await player.setPlaylistPersist([music.value], 0)
    player.playIndex(0)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '播放失败')
  }
}

async function enqueueThis() {
  if (!music.value) return
  try {
    const ok = await player.appendToQueuePersist(music.value.id)
    if (ok) ElMessage.success('已加入播放队列')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加入失败')
  }
}

async function toggleFavorite() {
  if (!music.value) return
  try {
    favorited.value = await apiFavoriteToggle(music.value.id)
    ElMessage.success(favorited.value ? '已加入我喜欢的音乐' : '已取消喜欢')
    await loadFavoriteSide()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onCollectCommand(playlistId: string | number) {
  if (!music.value) return
  const pid = Number(playlistId)
  if (Number.isNaN(pid)) return
  try {
    await apiFavoriteAddToPlaylist(music.value.id, pid)
    ElMessage.success('已收藏到歌单')
    favorited.value = await apiFavoriteStatus(music.value.id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '收藏失败')
  }
}

watch(activeLrcIndex, (idx) => {
  if (idx < 0) return
  nextTick(() => scrollActiveLyricBuffered(idx))
})

watch(musicId, () => loadAll(), { immediate: true })

/**
 * 在本页播放当前歌曲时，自然切歌（上一首结束 next）后 player.current 已变，但路由仍是上一首 id，
 * 歌词与详情不会刷新；此处同步到「正在播放」的详情页。
 */
watch(
  () => player.current?.id,
  (newId, oldId) => {
    if (newId == null || oldId == null) return
    if (oldId !== musicId.value) return
    if (newId === musicId.value) return
    router.replace({ name: 'music-detail', params: { id: String(newId) } })
  }
)

onBeforeUnmount(() => {
  if (lyricScrollRaf !== null) cancelAnimationFrame(lyricScrollRaf)
})

watch(
  () => auth.me?.id,
  () => {
    loadFavoriteSide()
    loadRatingStat()
  }
)

async function onMyStarChange(val: number) {
  if (!auth.me) {
    goLogin()
    return
  }
  if (val < 1 || val > 5) return
  ratingSubmitting.value = true
  try {
    await apiPostRating(musicId.value, val)
    ElMessage.success('评分已保存')
    await loadRatingStat()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '评分失败')
    await loadRatingStat()
  } finally {
    ratingSubmitting.value = false
  }
}

async function sendComment() {
  if (!auth.me) {
    goLogin()
    return
  }
  const t = commentText.value.trim()
  if (!t) {
    ElMessage.warning('请输入评论内容')
    return
  }
  sending.value = true
  try {
    await apiPostComment(musicId.value, t)
    commentText.value = ''
    ElMessage.success('已发布')
    await loadComments()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '发表失败')
  } finally {
    sending.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 800px;
  margin: 0 auto;
  padding-bottom: 24px;
}
.back {
  margin-bottom: 20px;
}
.back-title {
  font-weight: 700;
}
.hero {
  margin-bottom: 24px;
  align-items: flex-start;
}
.cover-wrap {
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
}
.cover-big {
  width: 100%;
  max-height: 320px;
  object-fit: cover;
  display: block;
}
.cover-big.placeholder {
  height: 240px;
  background: var(--el-fill-color);
}
.song-title {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 800;
  line-height: 1.3;
}
.song-meta {
  margin: 0 0 12px;
  color: var(--el-text-color-secondary);
  font-size: 15px;
}
.dot {
  margin: 0 6px;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 20px;
}
.actions {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.caret {
  margin-left: 4px;
  font-size: 10px;
  opacity: 0.7;
}
.section {
  margin-bottom: 20px;
}
.sec-title {
  font-weight: 700;
}
.count {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-weight: normal;
}
.muted {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}
.lyric-panel {
  max-height: 420px;
  overflow-y: auto;
  text-align: center;
  padding: 12px 8px;
  font-size: 15px;
  line-height: 2;
}
.lyric-line {
  color: var(--el-text-color-secondary);
  padding: 6px 10px;
  border-radius: 8px;
  transition:
    color 0.15s,
    background 0.15s;
}
.lyric-line.active {
  color: var(--el-color-primary);
  font-weight: 800;
  background: var(--el-fill-color-light);
}
.lyric-plain {
  margin: 0;
  white-space: pre-wrap;
  font-size: 14px;
  line-height: 1.8;
  color: var(--el-text-color-regular);
}
.composer {
  margin-bottom: 20px;
}
.send {
  margin-top: 10px;
}
.login-hint {
  margin-bottom: 16px;
}
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.comment-item {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.comment-item:last-child {
  border-bottom: none;
}
.c-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.nick {
  font-weight: 600;
}
.time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.c-body {
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.rating-card .rating-head {
  margin-bottom: 16px;
}
.avg-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.avg-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--el-color-warning);
}
.no-rating {
  margin: 0;
  font-size: 14px;
}
.rating-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 18px;
}
.bar-row {
  display: grid;
  grid-template-columns: 44px 1fr 36px;
  align-items: center;
  gap: 10px;
  font-size: 13px;
}
.bar-label {
  color: var(--el-text-color-secondary);
}
.bar-track {
  height: 8px;
  border-radius: 4px;
  background: var(--el-fill-color);
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 4px;
  background: linear-gradient(90deg, var(--el-color-warning-light-5), var(--el-color-warning));
  transition: width 0.25s ease;
}
.bar-num {
  text-align: right;
  color: var(--el-text-color-regular);
  font-variant-numeric: tabular-nums;
}
.rating-mine {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  padding-top: 4px;
}
.mine-label {
  font-weight: 600;
  font-size: 14px;
}
.mine-hint {
  font-size: 13px;
}
</style>
