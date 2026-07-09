<template>
  <div class="page">
    <el-card v-if="isAdmin">
      <template #header>
        <div class="head">
          <span class="title">歌曲管理</span>
          <span class="hint">搜索歌名、歌手或专辑；可修改元数据并替换音频、封面、歌词文件</span>
        </div>
      </template>

      <div class="toolbar">
        <el-input
          v-model="kw"
          clearable
          placeholder="实时搜索（留空为全部）"
          style="max-width: 360px"
          @clear="scheduleLoad"
          @keyup.enter="loadNowFromSearch"
        />
        <el-button type="primary" @click="load">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="records" stripe style="width: 100%" max-height="520">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column label="封面" width="72">
          <template #default="{ row }">
            <img v-if="row.coverUrl" :src="resolveMediaUrl(row.coverUrl)" class="thumb" alt="" />
            <div v-else class="thumb placeholder"></div>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="歌名" min-width="140" show-overflow-tooltip />
        <el-table-column prop="artist" label="歌手" min-width="120" show-overflow-tooltip />
        <el-table-column prop="album" label="专辑" min-width="100" show-overflow-tooltip />
        <el-table-column label="情绪" width="88">
          <template #default="{ row }">{{ moodLabel(row.moodTag) }}</template>
        </el-table-column>
        <el-table-column prop="durationMs" label="时长(ms)" width="100" />
        <el-table-column prop="playCount" label="播放" width="80" />
        <el-table-column label="歌词" width="72">
          <template #default="{ row }">
            <el-tag v-if="row.lyricUrl" type="success" size="small">有</el-tag>
            <el-tag v-else type="info" size="small">无</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="148" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="confirmDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="load"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-result v-else icon="warning" title="需要管理员账号" sub-title="请使用管理员登录后访问本页" />

    <el-drawer v-model="drawer" title="编辑歌曲" size="420px" destroy-on-close @closed="onDrawerClosed">
      <template v-if="edit">
        <div class="drawer-body">
          <div v-if="edit.coverUrl" class="preview">
            <span class="lbl">当前封面</span>
            <img :src="resolveMediaUrl(edit.coverUrl)" alt="" />
          </div>
          <el-form label-width="100px">
            <el-form-item label="歌名" required>
              <el-input v-model="form.title" maxlength="128" show-word-limit />
            </el-form-item>
            <el-form-item label="歌手" required>
              <el-input v-model="form.artist" maxlength="128" show-word-limit />
            </el-form-item>
            <el-form-item label="专辑">
              <el-input v-model="form.album" maxlength="128" show-word-limit placeholder="可留空" />
            </el-form-item>
            <el-form-item label="情绪" required>
              <el-select v-model="form.moodTag" style="width: 100%">
                <el-option v-for="m in moods" :key="m.value" :label="m.label" :value="m.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="时长(ms)">
              <el-input-number
                v-model="form.durationMs"
                :min="0"
                :step="1000"
                style="width: 100%"
                :disabled="!!files.music"
              />
              <div v-if="files.music" class="field-hint">已选择新音频，保存后将由服务端自动检测时长</div>
            </el-form-item>
            <el-form-item label="新音频">
              <input type="file" accept="audio/*,.mp3,.flac,.wav,.m4a,.ogg" @change="onPick('music', $event)" />
              <span v-if="files.music" class="fn">{{ files.music.name }}</span>
            </el-form-item>
            <el-form-item label="新封面">
              <input type="file" accept="image/*" @change="onPick('cover', $event)" />
              <span v-if="files.cover" class="fn">{{ files.cover.name }}</span>
            </el-form-item>
            <el-form-item label="新歌词">
              <input type="file" accept=".lrc,.txt" @change="onPick('lyric', $event)" />
              <span v-if="files.lyric" class="fn">{{ files.lyric.name }}</span>
            </el-form-item>
            <el-form-item label="移除资源">
              <el-checkbox v-model="form.removeCover">去掉封面</el-checkbox>
              <el-checkbox v-model="form.removeLyric">去掉歌词</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="save">保存</el-button>
            </el-form-item>
          </el-form>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { apiMusicAdminDelete, apiMusicAdminPage, apiMusicAdminUpdate } from '@/api/music'
import { resolveMediaUrl } from '@/utils/media'
import {
  clearFileInput,
  validateAdminAudioFile,
  validateAdminCoverFile,
  validateAdminLyricFile
} from '@/utils/uploadFormats'
import type { Mood, Music } from '@/api/types'

const auth = useAuthStore()
const isAdmin = computed(() => auth.me?.role === 'ADMIN')

const kw = ref('')
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const records = ref<Music[]>([])
const loading = ref(false)

let debounceTimer = 0
function scheduleLoad() {
  window.clearTimeout(debounceTimer)
  debounceTimer = window.setTimeout(() => {
    page.value = 1
    load()
  }, 320)
}

function loadNowFromSearch() {
  window.clearTimeout(debounceTimer)
  page.value = 1
  load()
}

watch(kw, () => scheduleLoad())

const moods: { label: string; value: Mood }[] = [
  { label: '开心', value: 'HAPPY' },
  { label: '难过', value: 'SAD' },
  { label: '平静', value: 'CALM' },
  { label: '活力', value: 'ENERGETIC' },
  { label: '专注', value: 'FOCUS' }
]

function moodLabel(m: Mood) {
  return moods.find((x) => x.value === m)?.label ?? m
}

async function load() {
  loading.value = true
  try {
    const res = await apiMusicAdminPage(kw.value.trim(), page.value - 1, pageSize.value)
    records.value = res.records
    total.value = res.total
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (isAdmin.value) load()
})

watch(isAdmin, (v) => {
  if (v) load()
})

const drawer = ref(false)
const edit = ref<Music | null>(null)
const saving = ref(false)

const form = reactive({
  title: '',
  artist: '',
  album: '',
  moodTag: 'CALM' as Mood,
  durationMs: 0,
  removeCover: false,
  removeLyric: false
})

const files = reactive({
  music: null as File | null,
  cover: null as File | null,
  lyric: null as File | null
})

function openEdit(row: Music) {
  edit.value = row
  form.title = row.title
  form.artist = row.artist
  form.album = row.album ?? ''
  form.moodTag = row.moodTag
  form.durationMs = row.durationMs
  form.removeCover = false
  form.removeLyric = false
  files.music = null
  files.cover = null
  files.lyric = null
  drawer.value = true
}

function onPick(kind: 'music' | 'cover' | 'lyric', e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) {
    files[kind] = null
    return
  }
  const err =
    kind === 'music'
      ? validateAdminAudioFile(f)
      : kind === 'cover'
        ? validateAdminCoverFile(f)
        : validateAdminLyricFile(f)
  if (err) {
    ElMessage.warning(err)
    clearFileInput(e)
    files[kind] = null
    return
  }
  files[kind] = f
}

function onDrawerClosed() {
  edit.value = null
}

async function confirmDelete(row: Music) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.title}」— ${row.artist}？将一并移除歌单/收藏/队列中的引用，且不可恢复。`,
      '删除歌曲',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  try {
    await apiMusicAdminDelete(row.id)
    ElMessage.success('已删除')
    if (edit.value?.id === row.id) {
      drawer.value = false
    }
    await load()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function save() {
  if (!edit.value) return
  if (!form.title.trim() || !form.artist.trim()) {
    ElMessage.warning('请填写歌名与歌手')
    return
  }
  if (files.music) {
    const e = validateAdminAudioFile(files.music)
    if (e) {
      ElMessage.warning(e)
      return
    }
  }
  if (files.cover) {
    const e = validateAdminCoverFile(files.cover)
    if (e) {
      ElMessage.warning(e)
      return
    }
  }
  if (files.lyric) {
    const e = validateAdminLyricFile(files.lyric)
    if (e) {
      ElMessage.warning(e)
      return
    }
  }
  saving.value = true
  try {
    await apiMusicAdminUpdate(edit.value.id, {
      title: form.title.trim(),
      artist: form.artist.trim(),
      album: form.album.trim(),
      moodTag: form.moodTag,
      durationMs: form.durationMs,
      musicFile: files.music,
      coverFile: files.cover,
      lyricFile: files.lyric,
      removeCover: form.removeCover,
      removeLyric: form.removeLyric
    })
    ElMessage.success('已保存')
    drawer.value = false
    await load()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 1100px;
  margin: 0 auto;
}
.head {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.title {
  font-weight: 700;
}
.hint {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.toolbar {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.thumb {
  width: 40px;
  height: 40px;
  object-fit: cover;
  border-radius: 6px;
}
.thumb.placeholder {
  background: var(--el-fill-color);
  border: 1px dashed var(--el-border-color);
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.drawer-body {
  padding-right: 8px;
}
.preview {
  margin-bottom: 16px;
}
.preview .lbl {
  display: block;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
}
.preview img {
  max-width: 100%;
  max-height: 160px;
  border-radius: 8px;
  object-fit: contain;
}
.fn {
  margin-left: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.field-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
</style>
