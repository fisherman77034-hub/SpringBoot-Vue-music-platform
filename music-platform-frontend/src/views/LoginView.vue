<template>
  <div class="wrap">
    <el-card class="card">
      <template #header>
        <div class="head">
          <div class="title">{{ tab === 'login' ? '登录' : '注册' }}</div>
          <div class="spacer"></div>
          <el-segmented v-model="tab" :options="tabs" />
        </div>
      </template>

      <el-form label-position="top" :model="form" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item v-if="tab === 'register'" label="昵称">
          <el-input v-model="form.nickname" autocomplete="nickname" />
        </el-form-item>

        <el-button type="primary" native-type="submit" style="width: 100%" :loading="loading">
          {{ tab === 'login' ? '登录' : '注册并登录' }}
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { usePlayerStore } from '@/stores/player'
import { apiRegister } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const player = usePlayerStore()

const tabs = [
  { label: '登录', value: 'login' },
  { label: '注册', value: 'register' }
]

const tab = ref<'login' | 'register'>('login')
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  nickname: ''
})

async function submit() {
  loading.value = true
  try {
    if (tab.value === 'register') {
      await apiRegister(form.username, form.password, form.nickname)
    }
    await auth.login(form.username, form.password)
    await player.syncQueueFromServer().catch(() => {})
    ElMessage.success('登录成功')
    const redir = route.query.redirect
    const path = typeof redir === 'string' && redir.startsWith('/') && !redir.startsWith('//') ? redir : '/'
    router.replace(path)
  } catch (e: any) {
    ElMessage.error(e?.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap {
  min-height: calc(100vh - 88px);
  display: grid;
  place-items: center;
}
.card {
  width: 420px;
  max-width: calc(100vw - 24px);
}
.head {
  display: flex;
  align-items: center;
  gap: 12px;
}
.title {
  font-weight: 800;
}
.spacer {
  flex: 1;
}
</style>

