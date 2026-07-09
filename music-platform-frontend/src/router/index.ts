import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'
import PlaylistView from '@/views/PlaylistView.vue'
import ProfileView from '@/views/ProfileView.vue'
import AdminUploadView from '@/views/AdminUploadView.vue'
import AdminMusicManageView from '@/views/AdminMusicManageView.vue'
import MusicDetailView from '@/views/MusicDetailView.vue'
import PlayQueueView from '@/views/PlayQueueView.vue'

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/play-queue', name: 'play-queue', component: PlayQueueView },
    { path: '/music/:id', name: 'music-detail', component: MusicDetailView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/playlists', name: 'playlists', component: PlaylistView },
    { path: '/profile', name: 'profile', component: ProfileView },
    { path: '/admin/upload', name: 'admin-upload', component: AdminUploadView },
    { path: '/admin/songs', name: 'admin-songs', component: AdminMusicManageView }
  ]
})

