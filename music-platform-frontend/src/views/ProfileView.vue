<template>
  <div class="page" v-if="auth.me">
    <el-card>
      <template #header>
        <span class="title">个人资料</span>
      </template>

      <div class="avatar-row">
        <el-avatar :size="88" :src="avatarSrc">
          {{ auth.me.nickname?.slice(0, 1) }}
        </el-avatar>
        <el-upload
          class="up"
          :show-file-list="false"
          accept="image/*"
          :before-upload="onBeforeAvatar"
        >
          <el-button type="primary" plain>更换头像</el-button>
        </el-upload>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px" class="form">
        <el-form-item label="登录名">
          <el-input :model-value="auth.me.username" disabled />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="个性签名" prop="bio">
          <el-input v-model="form.bio" type="textarea" :rows="3" maxlength="256" show-word-limit placeholder="写点什么介绍自己（可选）" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="form.gender" clearable placeholder="不展示" style="width: 200px">
            <el-option label="男" value="MALE" />
            <el-option label="女" value="FEMALE" />
            <el-option label="不展示" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="form.age" :min="1" :max="150" :controls="true" placeholder="可选" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存资料</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
  <el-empty v-else description="请先登录" />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules, UploadRawFile } from 'element-plus'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { apiUploadAvatar, apiUpdateProfile } from '@/api/user'
import type { Gender } from '@/api/types'
import { resolveMediaUrl } from '@/utils/media'

const auth = useAuthStore()

const avatarSrc = computed(() =>
  auth.me?.avatarUrl ? resolveMediaUrl(auth.me.avatarUrl) : undefined
)
const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive({
  nickname: '',
  bio: '',
  gender: undefined as Gender | undefined,
  age: undefined as number | undefined
})

const rules: FormRules = {
  nickname: [{ required: true, message: '请填写昵称', trigger: 'blur' }],
  bio: [{ max: 256, message: '签名最多256字', trigger: 'blur' }]
}

function syncFromMe() {
  const m = auth.me
  if (!m) return
  form.nickname = m.nickname
  form.bio = m.bio ?? ''
  form.gender = m.gender ?? undefined
  form.age = m.age ?? undefined
}

onMounted(() => {
  syncFromMe()
})

watch(
  () => auth.me,
  () => syncFromMe(),
  { deep: true }
)

async function onBeforeAvatar(raw: UploadRawFile) {
  try {
    await apiUploadAvatar(raw)
    ElMessage.success('头像已更新')
    await auth.refreshMe()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '上传失败')
  }
  return false
}

async function save() {
  await formRef.value?.validate().catch(() => Promise.reject())
  saving.value = true
  try {
    await apiUpdateProfile({
      nickname: form.nickname.trim(),
      bio: form.bio,
      gender: form.gender ?? null,
      age: form.age ?? null
    })
    await auth.refreshMe()
    ElMessage.success('已保存')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page {
  max-width: 640px;
  margin: 0 auto;
}
.title {
  font-weight: 700;
}
.avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.form {
  max-width: 520px;
}
</style>
