<template>
  <div class="bar">
    <div class="left">
      <img v-if="cur?.coverUrl" class="cover" :src="coverSrc" alt="cover" />
      <div class="meta" :class="{ clickable: !!cur }" @click="goDetail">
        <div class="title">{{ cur?.title || '未选择歌曲' }}</div>
        <div class="artist">{{ cur?.artist || '-' }}</div>
        <div v-if="barRating && barRating.ratingCount > 0" class="bar-rating-row">
          <el-rate :model-value="barAvgScore" disabled allow-half size="small" class="bar-rate" />
          <span class="bar-avg-num">{{ barAvgScore.toFixed(1) }}</span>
          <span class="bar-rating-n">({{ barRating.ratingCount }} 人)</span>
        </div>
      </div>
    </div>

    <div class="center">
      <div class="controls">
        <el-button circle @click="player.prev()" :disabled="!cur">
          <span>⏮</span>
        </el-button>
        <el-button circle type="primary" @click="toggle()" :disabled="!cur">
          <span>{{ player.playing ? '⏸' : '▶' }}</span>
        </el-button>
        <el-button circle @click="player.next()" :disabled="!cur">
          <span>⏭</span>
        </el-button>
        <el-button @click="player.toggleMode()" :disabled="!cur">
          {{ modeLabel(player.mode) }}
        </el-button>
      </div>
      <div class="progress">
        <el-button class="queue-btn" type="primary" link @click="goPlayQueue">播放队列</el-button>
        <span class="time">{{ fmt(currentTime) }}</span>
        <el-slider
          :model-value="progress"
          :min="0"
          :max="progSliderMax"
          :show-tooltip="false"
          class="prog-slider"
          @update:model-value="onProgSliderModelUpdate"
          @change="onSeekCommitted"
        />
        <span class="time">{{ fmt(duration) }}</span>
      </div>
    </div>

    <div class="right">
      <span class="vol">音量</span>
      <el-slider v-model="player.volume" :min="0" :max="1" :step="0.01" :show-tooltip="false" class="vol-slider" />
    </div>

    <audio
      ref="audioRef"
      preload="auto"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoaded"
      @error="onAudioError"
      @ended="player.next()"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { apiRatingStat, type RatingStat } from '@/api/interaction'
import { usePlayerStore, type PlayMode } from '@/stores/player'
import { resolveMediaUrl } from '@/utils/media'
import { normalizeRatingStat } from '@/utils/ratingStat'

const router = useRouter()
const player = usePlayerStore()
const cur = computed(() => player.current)
const audioRef = ref<HTMLAudioElement | null>(null)

const coverSrc = computed(() => (cur.value?.coverUrl ? resolveMediaUrl(cur.value.coverUrl) : ''))

const barRating = ref<RatingStat | null>(null)
const barAvgScore = computed(() => {
  const s = barRating.value
  if (!s || s.ratingCount <= 0) return 0
  return Math.min(5, Math.max(0, s.avgScore))
})

const duration = ref(0)
const currentTime = ref(0)
const progress = ref(0)
/** 为 true 时不把 audio.currentTime 写回滑块，避免播放中 timeupdate 与拖动打架 */
const draggingProgress = ref(false)

/** max 与 min 相等时 Element Plus 滑块内部会除零，表现为无法拖动 */
const progSliderMax = computed(() => {
  const d = duration.value
  if (typeof d === 'number' && d > 0 && !Number.isNaN(d)) return d
  const ad = audioRef.value?.duration
  if (typeof ad === 'number' && ad > 0 && !Number.isNaN(ad)) return ad
  return 1
})

watch(
  () => audioRef.value,
  (el) => player.bindAudio(el),
  { immediate: true, flush: 'post' }
)

watch(
  () => [player.playing, player.current?.id, player.current?.musicUrl, audioRef.value] as const,
  () => {
    if (audioRef.value && player.playing && player.current?.musicUrl) {
      player.attachAndPlay()
    }
  },
  { flush: 'post' }
)

watch(
  () => player.volume,
  (v) => {
    if (audioRef.value) audioRef.value.volume = v
  },
  { immediate: true }
)

watch(
  () => player.current?.id,
  async (id) => {
    draggingProgress.value = false
    currentTime.value = 0
    progress.value = 0
    duration.value = 0
    barRating.value = null
    if (id == null || Number.isNaN(id)) return
    try {
      const raw = await apiRatingStat(id)
      barRating.value = normalizeRatingStat(raw)
    } catch {
      barRating.value = null
    }
  },
  { immediate: true }
)

function toggle() {
  if (!cur.value) return
  player.togglePlay()
}

function goDetail() {
  if (!cur.value) return
  router.push({ name: 'music-detail', params: { id: String(cur.value.id) } })
}

function goPlayQueue() {
  router.push({ name: 'play-queue' })
}

function onTimeUpdate() {
  if (!audioRef.value) return
  const t = audioRef.value.currentTime
  currentTime.value = t
  if (!draggingProgress.value) {
    progress.value = t
  }
  player.reportPlaybackTime(t)
}

function onLoaded() {
  if (!audioRef.value) return
  duration.value = audioRef.value.duration || 0
}

let lastAudioErrHint = 0
function onAudioError() {
  const el = audioRef.value
  const code = el?.error?.code
  const src = el?.currentSrc || el?.src
  console.error('[audio]', { code, src, message: el?.error?.message })
  const now = Date.now()
  if (now - lastAudioErrHint > 5000) {
    lastAudioErrHint = now
    let hint = `无法播放（错误码 ${code ?? '?'}）。在新标签页打开链接检查是否 404 或文件是否为空：${(src || '').slice(0, 100)}`
    if (code === 4) {
      hint =
        '无法解码该音频（错误码 4：格式不支持）。请确认文件为浏览器可播的 MP3（建议用标准 CBR/VBR MP3），勿把其它格式改后缀冒充；可换一首或用 FFmpeg 转码：ffmpeg -i 源文件 -c:a libmp3lame -q:a 2 输出.mp3'
    } else if (code === 3) {
      hint = '音频解码失败（错误码 3），文件可能损坏或不完整，请重新上传或转码后再试。'
    }
    ElMessage.error(hint)
  }
}

/** 仅用户拖动/点击轨道时由 ElSlider 发出，用于与 timeupdate 解耦；勿用 mousedown（松手未触发 change 时会永久锁住）。 */
function onProgSliderModelUpdate(val: number) {
  draggingProgress.value = true
  progress.value = val
}

function onSeekCommitted() {
  draggingProgress.value = false
  if (!audioRef.value) return
  /** change 携带的 val 有时仍是旧的 props.modelValue，以当前 progress 为准 */
  const raw = progress.value
  const max = duration.value || audioRef.value.duration || 0
  const t = max > 0 ? Math.min(Math.max(0, raw), max) : Math.max(0, raw)
  audioRef.value.currentTime = t
  progress.value = t
  currentTime.value = t
}

function fmt(sec: number) {
  if (!sec || Number.isNaN(sec)) return '00:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function modeLabel(m: PlayMode) {
  return m === 'ORDER' ? '顺序' : m === 'SHUFFLE' ? '随机' : '单曲'
}
</script>

<style scoped>
.bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  min-height: 72px;
  height: auto;
  display: grid;
  grid-template-columns: 280px 1fr 220px;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  background: var(--el-bg-color);
  border-top: 1px solid var(--el-border-color);
  z-index: 10;
}
.left {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
}
.cover {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
}
.meta {
  min-width: 0;
}
.meta.clickable {
  cursor: pointer;
}
.meta.clickable:hover .title {
  color: var(--el-color-primary);
}
.title {
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.artist {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.bar-rating-row {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
  flex-wrap: wrap;
  min-height: 22px;
}
.bar-rate {
  height: 22px;
}
.bar-rate :deep(.el-rate__icon) {
  margin-right: 2px;
}
.bar-avg-num {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-regular);
}
.bar-rating-n {
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.center {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.controls {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: center;
}
.progress {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  min-width: 0;
}
.queue-btn {
  flex-shrink: 0;
  padding: 0 6px;
  font-size: 13px;
  font-weight: 600;
}
.prog-slider {
  flex: 1;
  min-width: 80px;
}
.time {
  width: 42px;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  text-align: center;
}
.right {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
}
.vol {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.vol-slider {
  width: 140px;
}
audio {
  display: none;
}
</style>
