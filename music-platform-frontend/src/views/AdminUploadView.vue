<template>
  <div class="page">
    <el-card v-if="isAdmin">
      <template #header>
        <div class="head">
          <span class="title">上传歌曲（管理员）</span>
          <el-tag type="info">
            音频：mp3/flac/wav/m4a/ogg 等；封面：jpg/png/gif/webp；歌词：仅 .lrc 或 .txt
          </el-tag>
        </div>
      </template>

      <el-form :model="form" label-width="100px" class="form">
        <el-form-item label="歌名" required>
          <el-input v-model="form.title" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="歌手" required>
          <el-input v-model="form.artist" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="专辑">
          <el-input v-model="form.album" maxlength="128" show-word-limit placeholder="可选" />
        </el-form-item>
        <el-form-item label="情绪标签" required>
          <el-select v-model="form.moodTag" style="width: 200px">
            <el-option v-for="m in moods" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="音频文件" required>
          <input type="file" accept="audio/*,.mp3,.flac,.wav,.m4a,.ogg" @change="onMusic" />
          <span v-if="form.musicFile" class="fn">{{ form.musicFile.name }}</span>
        </el-form-item>
        <el-form-item label="封面图">
          <input type="file" accept="image/*" @change="onCover" />
          <span v-if="form.coverFile" class="fn">{{ form.coverFile.name }}</span>
        </el-form-item>
        <el-form-item label="歌词文件">
          <input type="file" accept=".lrc,.txt" @change="onLyric" />
          <span v-if="form.lyricFile" class="fn">{{ form.lyricFile.name }}</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="submit">上传</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-result v-else icon="warning" title="需要管理员账号" sub-title="请使用管理员登录后访问本页" />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { apiMusicAdminUpload } from '@/api/music'
import type { Mood } from '@/api/types'
import {
  clearFileInput,
  validateAdminAudioFile,
  validateAdminCoverFile,
  validateAdminLyricFile
} from '@/utils/uploadFormats'

const auth = useAuthStore()
const isAdmin = computed(() => auth.me?.role === 'ADMIN')
const loading = ref(false)

const moods: { label: string; value: Mood }[] = [
  { label: '开心', value: 'HAPPY' },
  { label: '难过', value: 'SAD' },
  { label: '平静', value: 'CALM' },
  { label: '活力', value: 'ENERGETIC' },
  { label: '专注', value: 'FOCUS' }
]

const form = reactive({
  title: '',
  artist: '',
  album: '',
  moodTag: 'CALM' as Mood,
  musicFile: null as File | null,
  coverFile: null as File | null,
  lyricFile: null as File | null
})

function onMusic(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) {
    form.musicFile = null
    return
  }
  const err = validateAdminAudioFile(f)
  if (err) {
    ElMessage.warning(err)
    clearFileInput(e)
    form.musicFile = null
    return
  }
  form.musicFile = f
}
function onCover(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) {
    form.coverFile = null
    return
  }
  const err = validateAdminCoverFile(f)
  if (err) {
    ElMessage.warning(err)
    clearFileInput(e)
    form.coverFile = null
    return
  }
  form.coverFile = f
}
function onLyric(e: Event) {
  const f = (e.target as HTMLInputElement).files?.[0]
  if (!f) {
    form.lyricFile = null
    return
  }
  const err = validateAdminLyricFile(f)
  if (err) {
    ElMessage.warning(err)
    clearFileInput(e)
    form.lyricFile = null
    return
  }
  form.lyricFile = f
}

async function submit() {
  if (!form.title.trim() || !form.artist.trim()) {
    ElMessage.warning('请填写歌名与歌手')
    return
  }
  if (!form.musicFile) {
    ElMessage.warning('请选择音频文件')
    return
  }
  const audioErr = validateAdminAudioFile(form.musicFile)
  if (audioErr) {
    ElMessage.warning(audioErr)
    return
  }
  if (form.coverFile) {
    const cErr = validateAdminCoverFile(form.coverFile)
    if (cErr) {
      ElMessage.warning(cErr)
      return
    }
  }
  if (form.lyricFile) {
    const lErr = validateAdminLyricFile(form.lyricFile)
    if (lErr) {
      ElMessage.warning(lErr)
      return
    }
  }
  loading.value = true
  try {
    const id = await apiMusicAdminUpload({
      title: form.title.trim(),
      artist: form.artist.trim(),
      album: form.album.trim() || undefined,
      moodTag: form.moodTag,
      musicFile: form.musicFile,
      coverFile: form.coverFile,
      lyricFile: form.lyricFile
    })
    ElMessage.success(`上传成功，歌曲 id：${id}`)
    form.title = ''
    form.artist = ''
    form.album = ''
    form.coverFile = null
    form.lyricFile = null
    form.musicFile = null
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 720px;
  margin: 0 auto;
}
.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}
.title {
  font-weight: 700;
}
.form {
  max-width: 560px;
}
.fn {
  margin-left: 10px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
