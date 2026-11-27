<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getStatistics, exportStatistics } from '@/api/statistics'

function defaultRange() {
  const end = new Date()
  const start = new Date()
  start.setDate(start.getDate() - 29)
  const fmt = (date) => date.toISOString().slice(0, 10)
  return [fmt(start), fmt(end)]
}

const range = ref(defaultRange())
const loading = ref(false)
const data = ref(null)

const gradeColors = { S: '#0f9d76', A: '#2f6bff', B: '#7a5af8', C: '#e6a23c', D: '#d64545' }

const maxGrade = computed(() => Math.max(1, ...(data.value?.gradeDistribution || []).map((item) => item.count)))
const maxDaily = computed(() => Math.max(1, ...(data.value?.dailyTrend || []).map((item) => Math.max(item.orders, item.reports))))

const statusLabels = {
  PENDING: '待检测', INSPECTING: '检测中', PENDING_REPORT: '待出报告',
  PENDING_APPROVAL: '待复核', ISSUED: '已出报告', WITHDRAWN: '已撤回'
}

async function load() {
  if (!range.value || range.value.length !== 2) return
  loading.value = true
  try {
    data.value = await getStatistics({ from: range.value[0], to: range.value[1] })
  } finally {
    loading.value = false
  }
}

async function doExport() {
  if (!range.value || range.value.length !== 2) return
  const blob = await exportStatistics({ from: range.value[0], to: range.value[1] })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `检测统计_${range.value[0]}_${range.value[1]}.xlsx`
  link.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

function print() {
  window.print()
}

onMounted(load)
</script>

<template>
  <div class="stats-page">
    <div class="toolbar no-print">
      <el-date-picker
        v-model="range"
        type="daterange"
        value-format="YYYY-MM-DD"
        unlink-panels
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        @change="load"
      />
      <div class="actions">
        <el-button @click="load">刷新</el-button>
        <el-button @click="doExport">导出 Excel</el-button>
        <el-button type="primary" @click="print">打印</el-button>
      </div>
    </div>

    <div v-loading="loading">
      <template v-if="data">
        <div class="cards">
          <div class="stat-card">
            <div class="label">检测单总数</div>
            <div class="value">{{ data.totalOrders }}</div>
          </div>
          <div class="stat-card">
            <div class="label">已出报告</div>
            <div class="value">{{ data.issuedReports }}</div>
          </div>
          <div class="stat-card">
            <div class="label">意向成交</div>
            <div class="value">{{ data.dealCount }}</div>
          </div>
          <div class="stat-card">
            <div class="label">纠纷案件</div>
            <div class="value warning">{{ data.disputeCount }}</div>
          </div>
          <div class="stat-card">
            <div class="label">待审批任务</div>
            <div class="value warning">{{ data.pendingApprovals }}</div>
          </div>
          <div class="stat-card">
            <div class="label">车辆档案</div>
            <div class="value">{{ data.totalVehicles }}</div>
          </div>
          <div class="stat-card">
            <div class="label">缺陷项数</div>
            <div class="value danger">{{ data.defectItems }}</div>
          </div>
          <div class="stat-card">
            <div class="label">平均得分</div>
            <div class="value">{{ data.avgScore }}</div>
          </div>
          <div class="stat-card">
            <div class="label">验真次数</div>
            <div class="value">{{ data.verifyTotal }}</div>
            <div class="sub">通过 {{ data.verifyPass }} / 失败 {{ data.verifyFail }}</div>
          </div>
        </div>

        <div class="grid">
          <div class="panel">
            <div class="panel-title">车况等级分布</div>
            <div v-if="!data.gradeDistribution.length" class="empty">暂无已出报告</div>
            <div v-for="item in data.gradeDistribution" :key="item.name" class="bar-row">
              <span class="bar-label">{{ item.name }} 级</span>
              <div class="bar-track">
                <div
                  class="bar-fill"
                  :style="{ width: (item.count / maxGrade * 100) + '%', background: gradeColors[item.name] || '#2f6bff' }"
                />
              </div>
              <span class="bar-value">{{ item.count }}</span>
            </div>
          </div>

          <div class="panel">
            <div class="panel-title">检测单状态分布</div>
            <el-table :data="data.statusDistribution" size="small">
              <el-table-column label="状态">
                <template #default="{ row }">{{ statusLabels[row.name] || row.name }}</template>
              </el-table-column>
              <el-table-column prop="count" label="数量" width="100" />
            </el-table>
          </div>
        </div>

        <div class="panel">
          <div class="panel-title">近 14 天趋势（检测单 / 已出报告）</div>
          <div class="trend">
            <div v-for="item in data.dailyTrend" :key="item.date" class="trend-col">
              <div class="bars">
                <div class="bar orders" :style="{ height: (item.orders / maxDaily * 120) + 'px' }" :title="'检测单 ' + item.orders" />
                <div class="bar reports" :style="{ height: (item.reports / maxDaily * 120) + 'px' }" :title="'报告 ' + item.reports" />
              </div>
              <div class="date">{{ item.date.slice(5) }}</div>
            </div>
          </div>
          <div class="legend">
            <span><i class="dot orders" /> 检测单</span>
            <span><i class="dot reports" /> 已出报告</span>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px;
  box-shadow: 0 1px 4px rgba(15, 35, 95, 0.06);
}

.stat-card .label {
  font-size: 12px;
  color: #98a2b3;
}

.stat-card .value {
  font-size: 26px;
  font-weight: 700;
  margin-top: 6px;
}

.stat-card .value.warning {
  color: #e6a23c;
}

.stat-card .value.danger {
  color: #d64545;
}

.stat-card .sub {
  font-size: 11px;
  color: #98a2b3;
  margin-top: 4px;
}

.grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.panel {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 1px 4px rgba(15, 35, 95, 0.06);
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 14px;
}

.bar-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.bar-label {
  width: 48px;
  font-size: 13px;
  color: #47536b;
}

.bar-track {
  flex: 1;
  height: 10px;
  background: #f0f3f9;
  border-radius: 5px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.4s ease;
}

.bar-value {
  width: 32px;
  text-align: right;
  font-size: 13px;
  color: #6b7688;
}

.trend {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  height: 150px;
  padding-top: 10px;
}

.trend-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.bars {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 120px;
}

.bar {
  width: 8px;
  border-radius: 3px 3px 0 0;
  min-height: 2px;
}

.bar.orders {
  background: #2f6bff;
}

.bar.reports {
  background: #22c3a6;
}

.date {
  font-size: 10px;
  color: #98a2b3;
}

.legend {
  display: flex;
  gap: 18px;
  margin-top: 12px;
  font-size: 12px;
  color: #6b7688;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 5px;
}

.dot.orders {
  background: #2f6bff;
}

.dot.reports {
  background: #22c3a6;
}

.empty {
  color: #98a2b3;
  font-size: 13px;
}

@media print {
  .no-print {
    display: none;
  }
}
</style>
