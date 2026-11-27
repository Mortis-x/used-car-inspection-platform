<script setup>
import { onMounted, ref } from 'vue'
import { getAuditLogs, getVerifyRecords } from '@/api/audit'

const activeTab = ref('audit')
const loading = ref(false)

const auditRows = ref([])
const auditTotal = ref(0)
const auditPage = ref(1)
const auditAction = ref('')

const verifyRows = ref([])
const verifyTotal = ref(0)
const verifyPage = ref(1)
const verifyReportNo = ref('')
const verifyResult = ref('')

const actionLabels = {
  LOGIN: '登录', CREATE: '新增', UPDATE: '修改', WITHDRAW: '撤回',
  APPLY: '提交复核', ISSUE: '出证', REJECT: '驳回', CORRECT_APPLY: '申请更正',
  UPLOAD: '上传附件', EXPORT: '导出', VERIFY: '验真'
}

async function loadAudit() {
  loading.value = true
  try {
    const data = await getAuditLogs({ page: auditPage.value, size: 15, action: auditAction.value })
    auditRows.value = data.records
    auditTotal.value = data.total
  } finally {
    loading.value = false
  }
}

async function loadVerify() {
  loading.value = true
  try {
    const data = await getVerifyRecords({
      page: verifyPage.value,
      size: 15,
      reportNo: verifyReportNo.value,
      result: verifyResult.value
    })
    verifyRows.value = data.records
    verifyTotal.value = data.total
  } finally {
    loading.value = false
  }
}

function switchTab(name) {
  activeTab.value = name
  if (name === 'audit') {
    loadAudit()
  } else {
    loadVerify()
  }
}

function searchAudit() {
  auditPage.value = 1
  loadAudit()
}

function searchVerify() {
  verifyPage.value = 1
  loadVerify()
}

function diffList(row) {
  if (!row || !row.diffFields) return []
  try {
    const parsed = JSON.parse(row.diffFields)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

onMounted(loadAudit)
</script>

<template>
  <div class="page-card">
    <el-radio-group v-model="activeTab" @change="switchTab" style="margin-bottom: 16px">
      <el-radio-button value="audit">操作日志</el-radio-button>
      <el-radio-button value="verify">验真记录</el-radio-button>
    </el-radio-group>

    <template v-if="activeTab === 'audit'">
      <div class="filter">
        <el-select v-model="auditAction" placeholder="全部操作类型" clearable style="width: 200px" @change="searchAudit">
          <el-option v-for="(label, key) in actionLabels" :key="key" :label="label" :value="key" />
        </el-select>
        <el-button @click="searchAudit">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="auditRows" size="small" stripe>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column prop="username" label="操作人" width="100" />
        <el-table-column prop="role" label="角色" width="110" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">{{ actionLabels[row.action] || row.action }}</template>
        </el-table-column>
        <el-table-column prop="bizType" label="对象" width="110" />
        <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
      </el-table>
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="auditTotal"
        :page-size="15"
        :current-page="auditPage"
        @current-change="(page) => { auditPage = page; loadAudit() }"
      />
    </template>

    <template v-else>
      <div class="filter">
        <el-input v-model="verifyReportNo" placeholder="报告编号" clearable style="width: 220px" @keyup.enter="searchVerify" />
        <el-select v-model="verifyResult" placeholder="全部结果" clearable style="width: 150px" @change="searchVerify">
          <el-option label="通过" value="PASS" />
          <el-option label="未通过" value="FAIL" />
          <el-option label="未找到" value="NOT_FOUND" />
        </el-select>
        <el-button @click="searchVerify">查询</el-button>
      </div>
      <el-table v-loading="loading" :data="verifyRows" size="small" stripe>
        <el-table-column type="expand" width="40">
          <template #default="{ row }">
            <div v-if="diffList(row).length" class="diff-panel">
              <el-table :data="diffList(row)" size="small" border>
                <el-table-column prop="field" label="不一致字段" width="160" />
                <el-table-column prop="stored" label="存证时的值" min-width="220" show-overflow-tooltip />
                <el-table-column prop="current" label="当前库中的值" min-width="220" show-overflow-tooltip />
              </el-table>
              <div class="diff-tip">重新验真前请确认数据是否应恢复为存证时的值。</div>
            </div>
            <div v-else class="diff-panel empty">该次验真没有字段差异。</div>
          </template>
        </el-table-column>
        <el-table-column prop="verifyTime" label="验真时间" width="170" />
        <el-table-column prop="reportNo" label="报告编号" width="180" />
        <el-table-column label="结果" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.result === 'PASS' ? 'success' : 'danger'">
              {{ row.result === 'PASS' ? '通过' : row.result === 'FAIL' ? '未通过' : '未找到' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="差异" width="80">
          <template #default="{ row }">
            <el-tag v-if="diffList(row).length" size="small" type="warning">{{ diffList(row).length }} 项</el-tag>
            <span v-else>—</span>
          </template>
        </el-table-column>
        <el-table-column prop="detail" label="说明" min-width="240" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="140" />
        <el-table-column prop="userAgent" label="终端" min-width="200" show-overflow-tooltip />
      </el-table>
      <el-pagination
        class="pager"
        layout="total, prev, pager, next"
        :total="verifyTotal"
        :page-size="15"
        :current-page="verifyPage"
        @current-change="(page) => { verifyPage = page; loadVerify() }"
      />
    </template>
  </div>
</template>

<style scoped>
.filter {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.diff-panel {
  padding: 8px 20px 12px 48px;
}

.diff-panel.empty {
  color: #909399;
  font-size: 13px;
}

.diff-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}
</style>
