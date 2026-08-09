import { createRouter, createWebHistory } from 'vue-router';
import Layout from '@/layout/Layout.vue';

const routes = [
  { path: '/login', name: 'login', component: () => import('@/views/login/LoginView.vue'), meta: { title: '登录' } },
  { path: '/', component: Layout, redirect: '/dashboard', children: [
    { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '工作台', icon: 'HomeFilled' } },
    { path: 'recruit/jobs', name: 'jobs', component: () => import('@/views/recruit/job/JobList.vue'), meta: { title: '岗位管理', icon: 'Briefcase' } },
    { path: 'recruit/jobs/create', name: 'job-create', component: () => import('@/views/recruit/job/JobForm.vue'), meta: { title: '创建岗位' } },
    { path: 'recruit/jobs/:id/edit', name: 'job-edit', component: () => import('@/views/recruit/job/JobForm.vue'), meta: { title: '编辑岗位' } },
    { path: 'recruit/job/templates', name: 'job-templates', component: () => import('@/views/recruit/job/JobTemplate.vue'), meta: { title: '岗位模板', icon: 'Files' } },
    { path: 'recruit/resumes', name: 'resumes', component: () => import('@/views/recruit/resume/ResumeList.vue'), meta: { title: '简历管理', icon: 'Document' } },
    { path: 'recruit/resumes/:id', name: 'resume-detail', component: () => import('@/views/recruit/resume/ResumeDetail.vue'), meta: { title: '简历详情' } },
    { path: 'recruit/reports', name: 'reports', component: () => import('@/views/recruit/report/ReportView.vue'), meta: { title: '数据报表', icon: 'DataAnalysis' } },
    { path: 'recruit/brand', name: 'brand', component: () => import('@/views/recruit/brand/BrandConfig.vue'), meta: { title: '品牌配置', icon: 'Setting' } },
    { path: 'recruit/categories', name: 'categories', component: () => import('@/views/recruit/category/JobCategory.vue'), meta: { title: '岗位类别', icon: 'Menu' } },
    { path: 'recruit/students', name: 'students', component: () => import('@/views/system/student/StudentList.vue'), meta: { title: '学生管理', icon: 'UserFilled' } },
    { path: 'system/users', name: 'users', component: () => import('@/views/system/user/SysUserList.vue'), meta: { title: 'HR账号', icon: 'User' } },
    { path: 'system/roles', name: 'roles', component: () => import('@/views/system/role/SysRoleList.vue'), meta: { title: '角色管理', icon: 'Avatar' } },
    { path: 'system/depts', name: 'depts', component: () => import('@/views/system/dept/SysDeptList.vue'), meta: { title: '部门管理', icon: 'OfficeBuilding' } },
    { path: 'system/dict', name: 'dict', component: () => import('@/views/system/dict/SysDictList.vue'), meta: { title: '字典管理', icon: 'Collection' } },
    { path: 'system/templates', name: 'templates', component: () => import('@/views/system/template/NotifyTemplate.vue'), meta: { title: '通知模板', icon: 'ChatDotRound' } },
    { path: 'system/audit', name: 'audit', component: () => import('@/views/system/audit/AuditLog.vue'), meta: { title: '操作审计', icon: 'List' } },
  ]},
  { path: '/:pathMatch(.*)*', name: '404', component: () => import('@/views/error/404.vue'), meta: { title: '404', noAuth: true } }
];

const router = createRouter({ history: createWebHistory('/admin/'), routes });

router.beforeEach((to, from, next) => {
  // 404 页面不需要登录验证
  if (to.meta.noAuth) { next(); return; }
  const token = localStorage.getItem('admin_token');
  if (to.path !== '/login' && !token) { next('/login'); return; }
  if (to.path === '/login' && token) { next('/dashboard'); return; }
  next();
});

export default router;
