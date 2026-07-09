<template>
  <div class="page">
    <el-row :gutter="16" class="top-section">
      <el-col :xs="24" :md="16">
        <el-card>
          <template #header>
            <div class="card-head">
              <div class="head-title">情绪推荐</div>
              <div class="spacer"></div>
              <el-select v-model="mood" placeholder="选择情绪" style="width: 140px" @change="onMoodChange">
                <el-option v-for="m in moods" :key="m.value" :label="m.label" :value="m.value" />
              </el-select>
            </div>
          </template>

          <el-carousel height="220px" indicator-position="outside" v-if="reco.length">
            <el-carousel-item v-for="(item, idx) in reco" :key="item.id">
              <div class="banner" @click="goDetail(item)">
                <img v-if="item.coverUrl" :src="coverUrl(item.coverUrl)" class="banner-img" alt="cover" />
                <div class="banner-info">
                  <div class="banner-title">{{ item.title }}</div>
                  <div class="banner-sub">{{ item.artist }} · {{ item.album || '单曲' }}</div>
                  <el-button type="primary" style="margin-top: 12px" @click.stop="playReco(idx)">播放</el-button>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
          <el-empty v-else description="暂无推荐歌曲（请先用管理员上传一些音乐）" />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="8" class="side-col">
        <el-card>
          <template #header>
            <div class="card-head">
              <div class="head-title">搜索</div>
            </div>
          </template>
          <el-input v-model="kw" placeholder="按歌名/歌手搜索" @keyup.enter="doSearch">
            <template #append>
              <el-button @click="doSearch">搜索</el-button>
            </template>
          </el-input>
          <div style="height: 10px"></div>
          <el-button @click="loadLatest" plain>最近上传</el-button>
        </el-card>

        <el-card class="playlist-card" shadow="never">
          <template #header>
            <div class="card-head">
              <div class="head-title">我的歌单</div>
              <div class="spacer"></div>
              <el-button v-if="auth.me" type="primary" link @click="router.push({ name: 'playlists' })">
                管理
              </el-button>
            </div>
          </template>
          <template v-if="auth.me">
            <el-empty v-if="!playlists.length" description="暂无歌单" :image-size="48" />
            <div v-else class="playlist-scroll">
              <div
                v-for="p in playlists"
                :key="p.id"
                class="playlist-row"
                @click="openPlaylist(p.id)"
              >
                <span class="playlist-name">{{ p.name }}</span>
                <el-tag v-if="p.defaultLiked" size="small" type="danger" effect="plain">默认</el-tag>
              </div>
            </div>
          </template>
          <div v-else class="playlist-guest">
            <p class="guest-hint">登录后可在此查看歌单</p>
            <el-button type="primary" link @click="goLoginPlaylists">去登录</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div style="height: 16px"></div>

    <el-card>
      <template #header>
        <div class="card-head">
          <div class="head-title">{{ listTitle }}</div>
        </div>
      </template>

      <el-table :data="list" style="width: 100%" @row-click="(row: Music) => goDetail(row)">
        <el-table-column label="封面" width="72">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="coverUrl(row.coverUrl)" class="mini-cover" alt="cover" />
            <div v-else class="mini-cover placeholder"></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="歌名" min-width="180" />
        <el-table-column prop="artist" label="歌手" min-width="140" />
        <el-table-column prop="album" label="专辑" min-width="140" />
        <el-table-column prop="playCount" label="播放量" width="100" />
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click.stop="playOnly(row)">播放</el-button>
            <el-button v-if="auth.me" size="small" @click.stop="enqueue(row)">加入队列</el-button>
            <template v-if="auth.me">
              <el-dropdown trigger="click" @command="(cmd: string | number) => collectToPlaylist(row, Number(cmd))">
                <el-button type="success" size="small" @click.stop>
                  收藏到歌单
                  <span class="dd-caret">▼</span>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="p in playlists" :key="p.id" :command="p.id">
                      {{ p.name }}{{ p.defaultLiked ? '（默认）' : '' }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
            <el-button v-else type="success" size="small" @click.stop="goLoginForCollect">收藏到歌单</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { resolveMediaUrl } from '@/utils/media'
import { apiMusicList, apiMusicRecommend, apiMusicSearch } from '@/api/music'
import { apiFavoriteAddToPlaylist } from '@/api/favorite'
import { apiMyPlaylists, type Playlist } from '@/api/playlist'
import { apiUpdateMood } from '@/api/auth'
import type { Mood, Music } from '@/api/types'
import { useAuthStore } from '@/stores/auth'
import { usePlayerStore } from '@/stores/player'

const auth = useAuthStore()
const player = usePlayerStore()
const router = useRouter()

function coverUrl(u: string | null) {
  return u ? resolveMediaUrl(u) : ''
}

const moods: { value: Mood; label: string }[] = [
  { value: 'HAPPY', label: '开心' },
  { value: 'SAD', label: '难过' },
  { value: 'CALM', label: '平静' },
  { value: 'ENERGETIC', label: '活力' },
  { value: 'FOCUS', label: '专注' }
]

const mood = ref<Mood>('CALM')
const reco = ref<Music[]>([])
const kw = ref('')
const list = ref<Music[]>([])
const listTitle = ref('最近上传')
const playlists = ref<Playlist[]>([])

const myMood = computed(() => auth.me?.mood)

async function loadPlaylists() {
  if (!auth.me) {
    playlists.value = []
    return
  }
  playlists.value = await apiMyPlaylists().catch(() => [])
}

onMounted(async () => {
  await auth.refreshMe().catch(() => {})
  await loadPlaylists()
  mood.value = myMood.value || 'CALM'
  await loadRecommend()
  await loadLatest()
})

watch(
  () => auth.me?.id,
  () => loadPlaylists()
)

async function loadRecommend() {
  reco.value = await apiMusicRecommend(mood.value, 6).catch((e) => {
    ElMessage.error(e?.message || '获取推荐失败')
    return []
  })
}

async function loadLatest() {
  listTitle.value = '最近上传'
  list.value = await apiMusicList(0, 20).catch((e) => {
    ElMessage.error(e?.message || '获取列表失败')
    return []
  })
}

async function doSearch() {
  listTitle.value = kw.value ? `搜索：${kw.value}` : '最近上传'
  list.value = await apiMusicSearch(kw.value, 0, 20).catch((e) => {
    ElMessage.error(e?.message || '搜索失败')
    return []
  })
}

async function enqueue(m: Music) {
  if (!auth.me) {
    ElMessage.info('请先登录后再加入队列')
    return
  }
  try {
    const ok = await player.appendToQueuePersist(m.id)
    if (ok) ElMessage.success('已加入播放队列')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加入失败')
  }
}

async function onMoodChange() {
  if (auth.me) {
    await apiUpdateMood(mood.value).catch(() => {})
    await auth.refreshMe().catch(() => {})
  }
  await loadRecommend()
}

async function playReco(idx: number) {
  if (!reco.value.length) return
  try {
    await player.setPlaylistPersist(reco.value, idx)
    player.playIndex(idx)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '播放失败')
  }
}

function goDetail(m: Music) {
  router.push({ name: 'music-detail', params: { id: String(m.id) } })
}

function goLoginForCollect() {
  ElMessage.info('请先登录后再收藏到歌单')
  router.push({ name: 'login', query: { redirect: '/' } })
}

function goLoginPlaylists() {
  router.push({ name: 'login', query: { redirect: '/' } })
}

function openPlaylist(id: number) {
  router.push({ name: 'playlists', query: { playlist: String(id) } })
}

async function collectToPlaylist(row: Music, playlistId: number) {
  try {
    await apiFavoriteAddToPlaylist(row.id, playlistId)
    ElMessage.success('已收藏到歌单')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '收藏失败')
  }
}

/** 在当前列表中播放；若歌曲不在列表则单曲播放 */
async function playOnly(m: Music) {
  try {
    const idx = list.value.findIndex((x) => x.id === m.id)
    if (idx >= 0) {
      await player.setPlaylistPersist(list.value, idx)
      player.playIndex(idx)
    } else {
      await player.setPlaylistPersist([m], 0)
      player.playIndex(0)
    }
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '播放失败')
  }
}
</script>

<style scoped>
.page {
  max-width: 1180px;
  margin: 0 auto;
}
.card-head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.head-title {
  font-weight: 700;
}
.spacer {
  flex: 1;
}
.banner {
  display: grid;
  grid-template-columns: 220px 1fr;
  height: 220px;
  background: linear-gradient(135deg, rgba(64, 158, 255, 0.15), rgba(144, 147, 153, 0.08));
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
}
.banner-img {
  width: 220px;
  height: 220px;
  object-fit: cover;
}
.banner-info {
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.banner-title {
  font-size: 22px;
  font-weight: 800;
}
.banner-sub {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
}
.mini-cover {
  width: 46px;
  height: 46px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid var(--el-border-color);
}
.placeholder {
  background: rgba(144, 147, 153, 0.12);
}
.dd-caret {
  margin-left: 2px;
  font-size: 10px;
  opacity: 0.75;
}
.top-section {
  align-items: flex-start;
}
.side-col {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.playlist-card {
  flex: 0 1 auto;
  min-height: 0;
  width: 100%;
}
.playlist-card :deep(.el-card__body) {
  padding-top: 12px;
}
.playlist-scroll {
  --playlist-row-h: 48px;
  /* 固定可视区域为两行歌单，其余纵向滚动 */
  max-height: calc(2 * var(--playlist-row-h));
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 2px;
}
.playlist-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  box-sizing: border-box;
  min-height: var(--playlist-row-h);
}
.playlist-row:hover {
  background: var(--el-fill-color-light);
}
.playlist-name {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.playlist-guest {
  padding: 8px 0 4px;
}
.guest-hint {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}
</style>

