import { defineStore } from 'pinia'
import { login as loginApi, fetchMe } from '@/api/auth'
import { getToken, getUser, saveAuth, clearAuth } from '@/utils/auth'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: getUser()
  }),
  getters: {
    isLogin: (state) => !!state.token,
    role: (state) => state.user?.role || '',
    displayName: (state) => state.user?.realName || state.user?.username || ''
  },
  actions: {
    async login(form) {
      const data = await loginApi(form)
      this.token = data.token
      this.user = {
        userId: data.userId,
        username: data.username,
        realName: data.realName,
        role: data.role,
        roleLabel: data.roleLabel
      }
      saveAuth(this.token, this.user)
      return data
    },
    async refreshMe() {
      const me = await fetchMe()
      this.user = { ...this.user, ...me }
      saveAuth(this.token, this.user)
      return me
    },
    logout() {
      this.token = ''
      this.user = null
      clearAuth()
    }
  }
})
