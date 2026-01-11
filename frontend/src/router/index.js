import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/votes'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue')
  },
  {
    path: '/signup',
    name: 'Signup',
    component: () => import('@/views/SignupView.vue')
  },
  {
    path: '/votes',
    name: 'VoteList',
    component: () => import('@/views/VoteListView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/votes/create',
    name: 'VoteCreate',
    component: () => import('@/views/VoteCreateView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/votes/:id',
    name: 'VoteDetail',
    component: () => import('@/views/VoteDetailView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/votes/:id/result',
    name: 'VoteResult',
    component: () => import('@/views/VoteResultView.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')

  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if ((to.name === 'Login' || to.name === 'Signup') && token) {
    next('/votes')
  } else {
    next()
  }
})

export default router
