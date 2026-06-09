import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const routes = [
  {
    path: '/auth',
    name: 'auth',
    component: () => import('../views/AuthView.vue')
  },
  {
    path: '/share/:code',
    name: 'collection-share',
    component: () => import('../views/CollectionShareView.vue')
  },
  {
    path: '/',
    component: () => import('../views/AppLayout.vue'),
    children: [
      {
        path: '',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'materials',
        name: 'materials',
        component: () => import('../views/MaterialsView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'collections',
        name: 'collections',
        component: () => import('../views/CollectionsView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'collections/:id',
        name: 'collection-detail',
        component: () => import('../views/CollectionDetailView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'projects',
        name: 'projects',
        component: () => import('../views/ProjectsView.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: 'admin',
        name: 'admin',
        component: () => import('../views/AdminView.vue'),
        meta: { requiresAuth: true, roles: ['ADMIN'] }
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('../views/ProfileView.vue'),
        meta: { requiresAuth: true }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  const authStore = useAuthStore();

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { path: '/auth' };
  }

  if (to.path === '/auth' && authStore.isLoggedIn) {
    return { path: '/' };
  }

  if (to.meta.roles && !to.meta.roles.includes(authStore.role)) {
    return { path: '/' };
  }

  return true;
});

export default router;
