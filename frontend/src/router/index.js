import { createRouter, createWebHistory } from 'vue-router'
import { getToken, getUser } from '@/utils/auth'
import { ElMessage } from 'element-plus'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'vehicles',
        name: 'vehicles',
        component: () => import('@/views/VehicleList.vue'),
        meta: { title: '车辆档案' }
      },
      {
        path: 'inspections',
        name: 'inspections',
        component: () => import('@/views/InspectionList.vue'),
        meta: { title: '检测单' }
      },
      {
        path: 'inspections/:id/fill',
        name: 'inspection-fill',
        component: () => import('@/views/FillInspection.vue'),
        meta: { title: '检测录入' }
      },
      {
        path: 'approvals',
        name: 'approvals',
        component: () => import('@/views/ApprovalCenter.vue'),
        meta: { title: '审批待办', roles: ['OPERATOR', 'ADMIN'] }
      },
      {
        path: 'reports/:reportNo',
        name: 'report-detail',
        component: () => import('@/views/ReportDetail.vue'),
        meta: { title: '检测报告' }
      },
      {
        path: 'statistics',
        name: 'statistics',
        component: () => import('@/views/Statistics.vue'),
        meta: { title: '统计看板', roles: ['OPERATOR', 'ARBITRATOR', 'ADMIN'] }
      },
      {
        path: 'audit',
        name: 'audit',
        component: () => import('@/views/AuditLogs.vue'),
        meta: { title: '审计日志', roles: ['OPERATOR', 'ARBITRATOR', 'ADMIN'] }
      },
      {
        path: 'report-center',
        name: 'report-center',
        component: () => import('@/views/ReportCenter.vue'),
        meta: { title: '报告中心' }
      },
      {
        path: 'intentions',
        name: 'intentions',
        component: () => import('@/views/Intentions.vue'),
        meta: { title: '交易意向' }
      },
      {
        path: 'disputes',
        name: 'disputes',
        component: () => import('@/views/Disputes.vue'),
        meta: { title: '纠纷举证' }
      }
    ]
  },
  {
    path: '/verify',
    name: 'verify',
    component: () => import('@/views/PublicVerify.vue'),
    meta: { public: true, title: '公开验真' }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} - 车况可信存证平台`
    : '二手车检测报告与车况可信存证平台'
  if (to.meta.public) {
    return true
  }
  if (!getToken()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  const user = getUser()
  if (to.meta.roles && user && !to.meta.roles.includes(user.role)) {
    ElMessage.error('当前角色无权访问该页面')
    return { path: '/dashboard' }
  }
  return true
})

export default router
