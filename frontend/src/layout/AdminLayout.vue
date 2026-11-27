<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = [
  { path: '/dashboard', title: '工作台', icon: 'HomeFilled' },
  { path: '/vehicles', title: '车辆档案', icon: 'Van' },
  { path: '/inspections', title: '检测单', icon: 'Document' },
  { path: '/approvals', title: '审批待办', icon: 'Stamp', roles: ['OPERATOR', 'ADMIN'] },
  { path: '/report-center', title: '报告中心', icon: 'Document' },
  { path: '/intentions', title: '交易意向', icon: 'ShoppingCart' },
  { path: '/disputes', title: '纠纷举证', icon: 'Warning' },
  { path: '/statistics', title: '统计看板', icon: 'DataAnalysis', roles: ['OPERATOR', 'ARBITRATOR', 'ADMIN'] },
  { path: '/audit', title: '审计日志', icon: 'Tickets', roles: ['OPERATOR', 'ARBITRATOR', 'ADMIN'] }
]

const visibleMenus = computed(() =>
  menus.filter((menu) => !menu.roles || menu.roles.includes(auth.role))
)

const activeMenu = computed(() => route.path)

async function handleLogout() {
  await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <div class="brand-mark">V</div>
        <div class="brand-text">
          <div class="brand-title">车况存证</div>
          <div class="brand-sub">Inspection & Evidence</div>
        </div>
      </div>
      <el-menu :default-active="activeMenu" router class="menu">
        <el-menu-item v-for="menu in visibleMenus" :key="menu.path" :index="menu.path">
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <el-link href="/verify" target="_blank" type="info" :underline="false">公开验真页 →</el-link>
      </div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-title">{{ route.meta.title }}</div>
        <div class="header-right">
          <el-tag size="small" effect="plain">{{ auth.user?.roleLabel || auth.role }}</el-tag>
          <el-dropdown @command="(cmd) => cmd === 'logout' && handleLogout()">
            <span class="user">
              {{ auth.displayName }}
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #12203a;
  display: flex;
  flex-direction: column;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  color: #fff;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2f6bff, #22c3a6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

.brand-title {
  font-size: 15px;
  font-weight: 600;
}

.brand-sub {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.5px;
}

.menu {
  border-right: none;
  background: transparent;
  flex: 1;
}

.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.72);
}

.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}

.menu :deep(.el-menu-item.is-active) {
  background: rgba(47, 107, 255, 0.22);
  color: #fff;
}

.aside-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.header {
  background: #fff;
  border-bottom: 1px solid #e8edf5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-size: 14px;
  outline: none;
}

.main {
  padding: 20px;
  overflow-y: auto;
}
</style>
