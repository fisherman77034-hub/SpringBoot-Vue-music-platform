<template>
  <div class="page">
    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <el-card>
          <template #header>
            <div class="head">
              <div class="title">我的歌单</div>
              <div class="spacer"></div>
              <el-button type="primary" @click="openCreate = true">新建</el-button>
            </div>
          </template>

          <el-menu :default-active="String(activeId)" @select="onSelect">
            <el-menu-item v-for="p in playlists" :key="p.id" :index="String(p.id)">
              <span>{{ p.name }}</span>
              <el-tag v-if="p.defaultLiked" size="small" type="danger" effect="plain" class="pl-tag">默认收藏</el-tag>
            </el-menu-item>
          </el-menu>

          <el-empty v-if="!playlists.length" description="暂无歌单" />
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16">
        <el-card>
          <template #header>
            <div class="head">
              <div class="title">歌单歌曲</div>
              <div class="spacer"></div>
              <el-button
                v-if="activePlaylist && !activePlaylist.defaultLiked"
                type="danger"
                plain
                @click="confirmDeletePlaylist"
              >
                删除歌单
              </el-button>
            </div>
          </template>

          <el-table :data="songs" v-loading="loadingSongs" @row-click="(row: Music) => goDetail(row)">
            <el-table-column prop="title" label="歌名" min-width="160" />
            <el-table-column prop="artist" label="歌手" min-width="120" />
            <el-table-column label="操作" width="260">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click.stop="playOnly(row)">播放</el-button>
                <el-button size="small" @click.stop="beginMoveDialog(row)">移到其它歌单</el-button>
                <el-button size="small" type="danger" plain @click.stop="remove(row.id)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty v-if="activeId && !songs.length && !loadingSongs" description="歌单暂无歌曲" />
          <el-empty v-if="!activeId" description="请选择一个歌单" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="openCreate" title="新建歌单" width="420px">
      <el-input v-model="newName" placeholder="输入歌单名称" />
      <template #footer>
        <el-button @click="openCreate = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveDialogVisible" title="移到其它歌单" width="440px" @closed="moveTargetId = null">
      <p v-if="moveRow" class="move-hint">
        将「{{ moveRow.title }}」从当前歌单移至所选歌单（默认「我喜欢的音乐」中的歌曲移出后会取消红心状态，移入则会标记为喜欢）。
      </p>
      <el-select v-model="moveTargetId" placeholder="选择目标歌单" style="width: 100%">
        <el-option
          v-for="p in moveTargets"
          :key="p.id"
          :label="p.name + (p.defaultLiked ? '（默认收藏）' : '')"
          :value="p.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="moveDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!moveTargetId || !moveRow || !activeId" @click="confirmMove">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  apiCreatePlaylist,
  apiMyPlaylists,
  apiPlaylistDelete,
  apiPlaylistRemove,
  apiPlaylistSongs,
  type Playlist
} from '@/api/playlist'
import { apiFavoriteMovePlaylist } from '@/api/favorite'
import type { Music } from '@/api/types'
import { usePlayerStore } from '@/stores/player'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const player = usePlayerStore()

const playlists = ref<Playlist[]>([])
const activeId = ref<number | null>(null)
const songs = ref<Music[]>([])
const loadingSongs = ref(false)

const openCreate = ref(false)
const newName = ref('')

const moveDialogVisible = ref(false)
const moveRow = ref<Music | null>(null)
const moveTargetId = ref<number | null>(null)

const moveTargets = computed(() => {
  if (!activeId.value) return playlists.value
  return playlists.value.filter((p) => p.id !== activeId.value)
})

const activePlaylist = computed(() => playlists.value.find((p) => p.id === activeId.value) ?? null)

onMounted(async () => {
  await auth.refreshMe().catch(() => {})
  if (!auth.me) {
    router.replace('/login')
    return
  }
  await loadPlaylists()
})

async function loadPlaylists() {
  playlists.value = await apiMyPlaylists().catch((e) => {
    ElMessage.error(e?.message || '获取歌单失败')
    return []
  })

  const q = route.query.playlist
  let fromQuery: number | null = null
  if (q != null && q !== '') {
    const raw = Array.isArray(q) ? q[0] : q
    const n = Number(raw)
    if (!Number.isNaN(n) && playlists.value.some((p) => p.id === n)) {
      fromQuery = n
    }
  }

  if (fromQuery != null) {
    activeId.value = fromQuery
    await loadSongs()
    await router.replace({ name: 'playlists' })
    return
  }

  if (activeId.value != null && playlists.value.some((p) => p.id === activeId.value)) {
    await loadSongs()
    return
  }

  if (!activeId.value && playlists.value.length) {
    activeId.value = playlists.value[0].id
    await loadSongs()
  }
}

async function onSelect(id: string) {
  activeId.value = Number(id)
  await loadSongs()
}

async function loadSongs() {
  if (!activeId.value) return
  loadingSongs.value = true
  try {
    songs.value = await apiPlaylistSongs(activeId.value)
  } catch (e: any) {
    ElMessage.error(e?.message || '获取歌曲失败')
    songs.value = []
  } finally {
    loadingSongs.value = false
  }
}

function goDetail(row: Music) {
  router.push({ name: 'music-detail', params: { id: String(row.id) } })
}

async function playOnly(row: Music) {
  try {
    const idx = songs.value.findIndex((x) => x.id === row.id)
    await player.setPlaylistPersist(songs.value, Math.max(0, idx))
    player.playIndex(Math.max(0, idx))
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '播放失败')
  }
}

function beginMoveDialog(row: Music) {
  moveRow.value = row
  moveTargetId.value = moveTargets.value[0]?.id ?? null
  moveDialogVisible.value = true
}

async function confirmMove() {
  if (!moveRow.value || !activeId.value || !moveTargetId.value) return
  try {
    await apiFavoriteMovePlaylist(moveRow.value.id, activeId.value, moveTargetId.value)
    moveDialogVisible.value = false
    ElMessage.success('已移动')
    await loadPlaylists()
    await loadSongs()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '移动失败')
  }
}

async function create() {
  if (!newName.value.trim()) return
  try {
    const id = await apiCreatePlaylist(newName.value.trim())
    openCreate.value = false
    newName.value = ''
    await loadPlaylists()
    activeId.value = id
    await loadSongs()
    ElMessage.success('创建成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '创建失败')
  }
}

async function remove(musicId: number) {
  if (!activeId.value) return
  await apiPlaylistRemove(activeId.value, musicId).catch((e) => {
    ElMessage.error(e?.message || '移除失败')
  })
  await loadSongs()
}

async function confirmDeletePlaylist() {
  const p = activePlaylist.value
  if (!p || p.defaultLiked || !activeId.value) return
  try {
    await ElMessageBox.confirm(
      `确定删除歌单「${p.name}」吗？歌单将从列表中移除，其中的歌曲不会再出现在该歌单中（不会从曲库删除）。`,
      '删除歌单',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  const deletedId = activeId.value
  try {
    await apiPlaylistDelete(deletedId)
    ElMessage.success('歌单已删除')
    songs.value = []
    activeId.value = null
    await loadPlaylists()
    if (playlists.value.length) {
      activeId.value = playlists.value[0].id
      await loadSongs()
    }
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}
</script>

<style scoped>
.page {
  max-width: 1180px;
  margin: 0 auto;
}
.head {
  display: flex;
  align-items: center;
  gap: 10px;
}
.title {
  font-weight: 800;
}
.spacer {
  flex: 1;
}
.pl-tag {
  margin-left: 8px;
}
.move-hint {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  margin: 0 0 12px;
  line-height: 1.5;
}
</style>

