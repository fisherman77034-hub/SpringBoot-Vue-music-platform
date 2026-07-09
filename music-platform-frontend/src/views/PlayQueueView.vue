<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="head">
          <div class="title">播放队列</div>
          <div class="spacer"></div>
          <el-button v-if="auth.me" type="danger" plain :disabled="!rows.length" @click="clearAll">清空</el-button>
        </div>
      </template>

      <el-alert v-if="!auth.me" type="info" :closable="false" show-icon title="登录后可查看并编辑与服务端同步的播放队列" />
      <el-table v-else :data="rows" v-loading="loading" row-key="queueItemId">
        <el-table-column label="#" width="48">
          <template #default="{ $index }">{{ $index + 1 }}</template>
        </el-table-column>
        <el-table-column label="歌名" min-width="160">
          <template #default="{ row }">{{ row.music.title }}</template>
        </el-table-column>
        <el-table-column label="歌手" min-width="120">
          <template #default="{ row }">{{ row.music.artist }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row, $index }">
            <el-button size="small" type="primary" @click="playAt($index)">播放</el-button>
            <el-button size="small" :disabled="$index === 0" @click="moveUp($index)">上移</el-button>
            <el-button size="small" :disabled="$index === rows.length - 1" @click="moveDown($index)">下移</el-button>
            <el-button size="small" type="danger" plain @click="removeRow(row.queueItemId)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="auth.me && !loading && !rows.length" description="队列为空，可在首页或详情页加入队列" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { usePlayerStore } from '@/stores/player'
import { apiPlayQueueClear, apiPlayQueueList, apiPlayQueueRemoveItem, apiPlayQueueReorder } from '@/api/playQueue'
import type { PlayQueueEntry } from '@/api/types'

const auth = useAuthStore()
const player = usePlayerStore()
const router = useRouter()

const loading = ref(false)
const rows = ref<PlayQueueEntry[]>([])

async function load() {
  if (!auth.me) {
    rows.value = []
    return
  }
  loading.value = true
  try {
    rows.value = await apiPlayQueueList()
    await player.syncQueueFromServer()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
    rows.value = []
  } finally {
    loading.value = false
  }
}

function playAt(index: number) {
  player.playIndex(index)
}

function orderedIds(): number[] {
  return rows.value.map((r) => r.queueItemId)
}

async function moveUp(index: number) {
  if (index <= 0) return
  const ids = orderedIds()
  ;[ids[index - 1], ids[index]] = [ids[index], ids[index - 1]]
  await persistOrder(ids)
}

async function moveDown(index: number) {
  if (index >= rows.value.length - 1) return
  const ids = orderedIds()
  ;[ids[index], ids[index + 1]] = [ids[index + 1], ids[index]]
  await persistOrder(ids)
}

async function persistOrder(ids: number[]) {
  try {
    await apiPlayQueueReorder(ids)
    await load()
    ElMessage.success('顺序已更新')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function removeRow(queueItemId: number) {
  try {
    await apiPlayQueueRemoveItem(queueItemId)
    await load()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '移除失败')
  }
}

async function clearAll() {
  try {
    await apiPlayQueueClear()
    await load()
    ElMessage.success('已清空队列')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '清空失败')
  }
}

onMounted(async () => {
  await auth.refreshMe().catch(() => {})
  if (!auth.me) {
    router.replace({ name: 'login', query: { redirect: '/play-queue' } })
    return
  }
  await load()
})
</script>

<style scoped>
.page {
  max-width: 900px;
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
</style>
