import { defineStore } from 'pinia';

const USER_KEY = 'clip_hub_user';
const TOKEN_KEY = 'clip_hub_token';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null')
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    role: (state) => state.user?.role || ''
  },
  actions: {
    setAuth(payload) {
      this.token = payload.token;
      this.user = {
        userId: payload.userId,
        username: payload.username,
        displayName: payload.displayName,
        role: payload.role,
        teamId: payload.teamId
      };
      localStorage.setItem(TOKEN_KEY, this.token);
      localStorage.setItem(USER_KEY, JSON.stringify(this.user));
    },
    clearAuth() {
      this.token = '';
      this.user = null;
      localStorage.removeItem(TOKEN_KEY);
      localStorage.removeItem(USER_KEY);
    }
  }
});
