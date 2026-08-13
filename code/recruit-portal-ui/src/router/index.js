import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';

const routes = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  { path: '/register', name: 'register', component: () => import('../views/RegisterView.vue') },
  { path: '/jobs', name: 'jobs', component: () => import('../views/JobListView.vue') },
  { path: '/jobs/:id', name: 'job-detail', component: () => import('../views/JobDetailView.vue') },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/ProfileView.vue'),
    meta: { requiresAuth: true },
    children: [
      // 空路径子路由：确保 /profile 本身能被 Vue Router 匹配，
      // 不指定 component —— 基本资料和隐私设置由 ProfileView 内联渲染
      { path: '', name: 'profile-home' },
      {
        path: 'education',
        name: 'education',
        meta: { requiresAuth: true },
        component: () => import('../views/EducationView.vue'),
      },
      {
        path: 'resume',
        name: 'resume',
        meta: { requiresAuth: true },
        component: () => import('../views/ResumeUploadView.vue'),
      },
      {
        path: 'internship',
        name: 'internship',
        meta: { requiresAuth: true },
        component: () => import('../views/ProfileExtraView.vue'),
        props: { type: 'internship' },
      },
      {
        path: 'certificate',
        name: 'certificate',
        meta: { requiresAuth: true },
        component: () => import('../views/ProfileExtraView.vue'),
        props: { type: 'certificate' },
      },
      {
        path: 'activity',
        name: 'activity',
        meta: { requiresAuth: true },
        component: () => import('../views/ProfileExtraView.vue'),
        props: { type: 'activity' },
      },
      {
        path: 'security',
        name: 'security',
        meta: { requiresAuth: true },
        component: () => import('../views/SecurityView.vue'),
      },
    ],
  },
  { path: '/messages', name: 'messages', meta: { requiresAuth: true }, component: () => import('../views/MessagesView.vue') },
  { path: '/apply/:jobId', name: 'apply', meta: { requiresAuth: true }, component: () => import('../views/ApplyView.vue') },
  { path: '/my-applications', name: 'my-applications', meta: { requiresAuth: true }, component: () => import('../views/MyApplications.vue') },
  { path: '/privacy', name: 'privacy', component: () => import('../views/PrivacyView.vue') },
  // 404 兜底 —— 必须放在最后
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFoundView.vue') },
];

const router = createRouter({ history: createWebHistory(), routes });

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !localStorage.getItem('access_token')) {
    next({ path: '/login', query: { redirect: to.fullPath } });
  } else {
    next();
  }
});

export default router;
