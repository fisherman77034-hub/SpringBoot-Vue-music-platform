<template>
  <div class="layout">
    <header class="header">
      <span class="brand" @click="$router.push('/')">在线音乐</span>
      <nav class="nav">
        <router-link to="/">首页</router-link>
        <router-link to="/play-queue">播放队列</router-link>
        <router-link to="/playlists">歌单</router-link>
      </nav>
      <div class="spacer" />
      <template v-if="auth.me">
        <el-dropdown trigger="click">
          <span class="user-trigger">{{ auth.me.nickname }}</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="auth.me?.role === 'ADMIN'" divided @click="$router.push('/admin/upload')">
                上传音乐
              </el-dropdown-item>
              <el-dropdown-item v-if="auth.me?.role === 'ADMIN'" @click="$router.push('/admin/songs')">
                管理歌曲
              </el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
      <router-link v-else class="login-link" to="/login">登录</router-link>
    </header>
    <main class="main">
      <router-view />
    </main>
    <PlayerBar />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import PlayerBar from '@/components/PlayerBar.vue'

const auth = useAuthStore()
const router = useRouter()

onMounted(() => {
  void auth.refreshMe()
})

function logout() {
  auth.logout()
  router.push('/')
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  box-sizing: border-box;
}
.header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 20px;
  height: 56px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-bg-color);
}
.brand {
  font-weight: 800;
  cursor: pointer;
  user-select: none;
}
.nav {
  display: flex;
  align-items: center;
  gap: 14px;
}
.nav a {
  color: var(--el-text-color-regular);
  text-decoration: none;
}
.nav a.router-link-active {
  color: var(--el-color-primary);
  font-weight: 600;
}
.spacer {
  flex: 1;
}
.user-trigger {
  cursor: pointer;
  color: var(--el-text-color-primary);
}
.login-link {
  color: var(--el-color-primary);
  text-decoration: none;
}
.main {
  padding: 16px;
  padding-bottom: 88px;
}
</style>
