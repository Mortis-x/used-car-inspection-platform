<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { getDashboardSummary } from '@/api/dashboard'

const auth = useAuthStore()
const router = useRouter()
const summary = ref(null)

const today = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const milestones = [
  { code: 'M0', title: '工程骨架与核心契约', detail: 'Compose 中间件、16 张表、JWT 登录、规范化与哈希链单测', status: 'done' },
  { code: 'M1', title: '业务主线', detail: '车辆档案、检测模板、检测单状态机、录入页、附件异步、撤回与审批', status: 'done' },
  { code: 'M2', title: '报告与存证', detail: '评分规则、报告版本、哈希链写入、分布式锁、缺项拦截、报告更正', status: 'done' },
  { code: 'M3', title: '验真与治理', detail: '公开验真与扫码、字段级差异、统计导出、审计、意向单、纠纷举证', status: 'done' }
]

const techPoints = [
  { title: '哈希的是数据，不是文件', detail: 'PDF 每次生成字节都不同，所以对结构化 JSON 做规范化后计算 SHA-256。' },
  { title: '哈希链串行写入', detail: '每条记录依赖前一条指纹，靠单写 + 行锁保证顺序，任何断裂都会在验真时暴露。' },
  { title: '附件异步处理', detail: '线程池压缩上传，50 张手机原图 30 秒内处理完，提交接口 1 秒内返回。' },
  { title: '分布式锁防重复出具', detail: '以车辆为锁粒度，配合同周期唯一有效报告约束，避免并发下重复出证。' }
]

const accounts = [
  { role: '检测师', username: 'inspector', scene: '现场逐项检测、拍照上传' },
  { role: '车商', username: 'dealer', scene: '建档、发起检测委托、处理意向' },
  { role: '买家', username: 'buyer', scene: '浏览报告、扫码验真、出价与纠纷' },
  { role: '平台运营员', username: 'operator', scene: '报告复核、更正审批、统计与审计' },
  { role: '纠纷仲裁员', username: 'arbitrator', scene: '受理纠纷、查看材料包并裁决' }
]

const roleCards = computed(() => {
  const s = summary.value
  if (!s) return []
  const config = {
    INSPECTOR: [
      { label: '待检测', value: s.inspecting, path: '/inspections' },
      { label: '待出报告', value: s.pendingReport, path: '/inspections' },
      { label: '已生效报告', value: s.activeReports, path: '/report-center' }
    ],
    DEALER: [
      { label: '我的车辆', value: s.myVehicles, path: '/vehicles' },
      { label: '待检测单', value: s.pendingOrders, path: '/inspections' },
      { label: '待响应意向', value: s.dealerIntentions, path: '/intentions' }
    ],
    BUYER: [
      { label: '可购报告', value: s.activeReports, path: '/report-center' },
      { label: '进行中意向', value: s.myIntentions, path: '/intentions' },
      { label: '我的纠纷', value: s.myDisputes, path: '/disputes' }
    ],
    OPERATOR: [
      { label: '待审批', value: s.pendingApprovals, path: '/approvals' },
      { label: '已生效报告', value: s.activeReports, path: '/report-center' },
      { label: '今日验真', value: s.todayVerifies, path: '/audit' }
    ],
    ARBITRATOR: [
      { label: '待受理', value: s.openDisputes, path: '/disputes' },
      { label: '仲裁中', value: s.arbitratingDisputes, path: '/disputes' },
      { label: '今日验真', value: s.todayVerifies, path: '/audit' }
    ]
  }
  return config[auth.role] || []
})

const quickLinks = computed(() => {
  const config = {
    INSPECTOR: [
      { label: '检测单', path: '/inspections' },
      { label: '报告中心', path: '/report-center' },
      { label: '纠纷举证', path: '/disputes' }
    ],
    DEALER: [
      { label: '车辆档案', path: '/vehicles' },
      { label: '报告中心', path: '/report-center' },
      { label: '交易意向', path: '/intentions' }
    ],
    BUYER: [
      { label: '报告中心', path: '/report-center' },
      { label: '交易意向', path: '/intentions' },
      { label: '纠纷举证', path: '/disputes' }
    ],
    OPERATOR: [
      { label: '审批待办', path: '/approvals' },
      { label: '统计看板', path: '/statistics' },
      { label: '审计日志', path: '/audit' }
    ],
    ARBITRATOR: [
      { label: '纠纷举证', path: '/disputes' },
      { label: '统计看板', path: '/statistics' },
      { label: '审计日志', path: '/audit' }
    ]
  }
  return config[auth.role] || []
})

const statusMeta = () => ({ text: '已完成', type: 'success' })

async function load() {
  summary.value = await getDashboardSummary()
}

onMounted(load)
</script>

<template>
  <div class="dashboard">
    <div class="welcome page-card">
      <div>
        <h2>{{ auth.displayName }}，欢迎回来</h2>
        <p class="sub">{{ today }} · 当前角色：{{ auth.user?.roleLabel }}</p>
      </div>
      <el-tag type="success" effect="dark" size="large">全部功能可用</el-tag>
    </div>

    <div class="stat-row" v-if="roleCards.length">
      <div v-for="card in roleCards" :key="card.label" class="stat-card" @click="router.push(card.path)">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.value }}</div>
      </div>
    </div>

    <div class="page-card quick-card" v-if="quickLinks.length">
      <div class="card-title">快捷入口</div>
      <div class="quick-links">
        <el-button v-for="link in quickLinks" :key="link.path" plain @click="router.push(link.path)">
          {{ link.label }}
        </el-button>
      </div>
    </div>

    <div class="grid">
      <div class="page-card">
        <div class="card-title">里程碑进度</div>
        <el-timeline class="timeline">
          <el-timeline-item
            v-for="item in milestones"
            :key="item.code"
            :timestamp="item.code"
            placement="top"
            type="success"
          >
            <div class="ms-head">
              <span class="ms-title">{{ item.title }}</span>
              <el-tag size="small" :type="statusMeta(item.status).type" effect="plain">
                {{ statusMeta(item.status).text }}
              </el-tag>
            </div>
            <div class="ms-detail">{{ item.detail }}</div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <div class="page-card">
        <div class="card-title">四个技术亮点</div>
        <div class="tech-list">
          <div v-for="point in techPoints" :key="point.title" class="tech-item">
            <div class="tech-title">{{ point.title }}</div>
            <div class="tech-detail">{{ point.detail }}</div>
          </div>
        </div>
      </div>
    </div>

    <div class="page-card">
      <div class="card-title">演示账号（密码均为 123456）</div>
      <el-table :data="accounts" size="small" stripe>
        <el-table-column prop="role" label="角色" width="140" />
        <el-table-column prop="username" label="用户名" width="160" />
        <el-table-column prop="scene" label="典型操作" />
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.welcome {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.sub {
  margin: 0;
  color: #8a94a6;
  font-size: 13px;
}

.stat-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(15, 35, 95, 0.06);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(15, 35, 95, 0.1);
}

.stat-label {
  font-size: 12px;
  color: #98a2b3;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  margin-top: 8px;
}

.quick-links {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.grid {
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: 16px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
}

.timeline {
  padding-left: 4px;
}

.ms-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ms-title {
  font-weight: 600;
  font-size: 14px;
}

.ms-detail {
  margin-top: 6px;
  color: #6b7688;
  font-size: 13px;
  line-height: 1.7;
}

.tech-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tech-item {
  border-left: 3px solid #2f6bff;
  padding-left: 12px;
}

.tech-title {
  font-size: 14px;
  font-weight: 600;
}

.tech-detail {
  margin-top: 4px;
  color: #6b7688;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1100px) {
  .grid,
  .stat-row {
    grid-template-columns: 1fr;
  }
}
</style>
