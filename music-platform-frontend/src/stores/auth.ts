import { defineStore } from 'pinia'
import { apiLogin, apiMe } from '@/api/auth'
import type { UserProfile } from '@/api/types'

type State = {
  token: string
  me: UserProfile | null
}

export const useAuthStore = defineStore('auth', {
  state: (): State => ({
    token: localStorage.getItem('token') || '',
    me: null
  }),
  actions: {
    async login(username: string, password: string) {
      const res = await apiLogin(username, password)
      this.token = res.token
      localStorage.setItem('token', this.token)
      await this.refreshMe()
    },
    logout() {
      this.token = ''
      this.me = null
      localStorage.removeItem('token')
    },
    async refreshMe() {
      if (!this.token) {
        this.me = null
        return
      }
      this.me = await apiMe()
    }
  }
})

