<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listApprovals, approveTask, rejectTask } from '@/api/approval'

const activeTab = ref('PENDING')
const loading = ref(false)
const rows = ref([])

const tabs = [
  { name: 'PENDING', label: '待审批' },
  { name: 'APPROVED', label: '已通过' },
  { name: 'REJECTED', label: '已驳回' }
]

async function load() {
  loading.value = true
  try {
    rows.value = await listApprovals(activeTab.value)
  } finally {
    loading.value = false
  }
}

function switchTab(name) {
  activeTab.value = name
  load()
}

async function approve(row) {
  const { value } = await ElMessageBox.prompt('复核意见（可留空）', `通过 ${row.reportNo}`, {
    confirmButtonText: row.bizType === 'CORRECTION' ? '通过更正' : '通过并出证',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：检测照片与结论一致'
  })
  await approveTask(row.id, value)
  ElMessage.success(row.bizType === 'CORRECTION'
    ? '更正已通过，已生成新版本报告并存证'
    : '已通过，报告已生效并存证')
  load()
}

async function reject(row) {
  const { value } = await ElMessageBox.prompt('请填写驳回原因（必填）', `驳回 ${row.reportNo}`, {
    confirmButtonText: '确认驳回',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '驳回原因不能为空'
  })
  await rejectTask(row.id, value)
  ElMessage.success('已驳回，检测单退回检测中状态')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="tabs">
      <el-radio-group v-model="activeTab" @change="switchTab">
        <el-radio-button v-for="tab in tabs" :key="tab.name" :value="tab.name">{{ tab.label }}</el-radio-button>
      </el-radio-group>
      <el-button plain @click="load">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe empty-text="暂无审批任务">
      <el-table-column prop="orderNo" label="检测单号" width="170" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.bizType === 'CORRECTION' ? 'warning' : 'primary'">
            {{ row.bizType === 'CORRECTION' ? '报告更正' : '报告复核' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="车辆" min-width="180">
        <template #default="{ row }">
          <div>{{ row.brand }} {{ row.model }}</div>
          <div class="sub-text">{{ row.plateNo || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="reportNo" label="报告编号" width="165" />
      <el-table-column label="评分" width="110">
        <template #default="{ row }">
          <span v-if="row.totalScore != null">{{ row.totalScore }} 分 / {{ row.grade }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="applyName" label="申请人" width="100" />
      <el-table-column prop="applyTime" label="申请时间" width="170" />
      <el-table-column prop="statusLabel" label="状态" width="90" />
      <el-table-column v-if="activeTab === 'PENDING'" label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="success" @click="approve(row)">通过</el-button>
          <el-button link type="danger" @click="reject(row)">驳回</el-button>
        </template>
      </el-table-column>
      <el-table-column v-else prop="opinion" label="审批意见" min-width="160" />
    </el-table>
  </div>
</template>

<style scoped>
.tabs {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.sub-text {
  color: #98a2b3;
  font-size: 12px;
}
</style>
