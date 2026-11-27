<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  listDisputes, getDispute, claimDispute, addDisputeEvidence, resolveDispute, closeDispute
} from '@/api/dispute'
import { listAttachments } from '@/api/attachment'

const auth = useAuthStore()
const router = useRouter()
const loading = ref(false)
const rows = ref([])
const statusFilter = ref('')

const drawerVisible = ref(false)
const detail = ref(null)
const attachments = ref([])
const submitting = ref(false)

const evidenceForm = ref({ type: 'REMARK', refId: null, remark: '' })
const resolveForm = ref({ result: 'SUPPORTED', conclusion: '' })

const isArbitrator = computed(() => auth.role === 'ARBITRATOR')
const isApplicant = computed(() => detail.value?.dispute?.applicantId === auth.user?.userId)
const canClose = computed(() => {
  if (!detail.value) return false
  if (!['OPEN', 'ARBITRATING'].includes(detail.value.dispute.status)) return false
  return isArbitrator.value || isApplicant.value
})
const canAddEvidence = computed(() => {
  if (!detail.value) return false
  if (['RESOLVED', 'CLOSED'].includes(detail.value.dispute.status)) return false
  return isArbitrator.value || detail.value.dispute.applicantId === auth.user?.userId
})

async function load() {
  loading.value = true
  try {
    rows.value = await listDisputes({ status: statusFilter.value })
  } finally {
    loading.value = false
  }
}

async function openDetail(row) {
  drawerVisible.value = true
  detail.value = null
  const data = await getDispute(row.id)
  detail.value = data
  evidenceForm.value = { type: 'REMARK', refId: null, remark: '' }
  resolveForm.value = { result: 'SUPPORTED', conclusion: '' }
  if (isArbitrator.value || data.dispute.applicantId === auth.user?.userId) {
    attachments.value = await listAttachments(data.dispute.orderId)
  }
}

async function refreshDetail() {
  if (detail.value) {
    detail.value = await getDispute(detail.value.dispute.id)
  }
}

async function doClaim() {
  await claimDispute(detail.value.dispute.id)
  ElMessage.success('已受理，案件进入仲裁中')
  await refreshDetail()
  load()
}

function openFullReport() {
  router.push(`/reports/${detail.value.dispute.reportNo}`)
}

async function submitEvidence() {
  submitting.value = true
  try {
    await addDisputeEvidence(detail.value.dispute.id, evidenceForm.value)
    ElMessage.success('材料已提交')
    evidenceForm.value = { type: 'REMARK', refId: null, remark: '' }
    await refreshDetail()
  } finally {
    submitting.value = false
  }
}

async function doClose() {
  const withdraw = !isArbitrator.value
  try {
    await ElMessageBox.confirm(
      withdraw ? '确认撤回该纠纷申请？撤回后案件将关闭。' : '确认关闭该纠纷案件？关闭后将不能再补充材料或裁决。',
      withdraw ? '撤回纠纷' : '关闭案件',
      { type: 'warning', confirmButtonText: '确认', cancelButtonText: '取消' }
    )
  } catch {
    return
  }
  await closeDispute(detail.value.dispute.id)
  ElMessage.success(withdraw ? '已撤回纠纷申请' : '案件已关闭')
  await refreshDetail()
  load()
}

async function submitResolve() {
  if (!resolveForm.value.conclusion.trim()) {
    ElMessage.warning('请填写裁决结论')
    return
  }
  submitting.value = true
  try {
    await resolveDispute(detail.value.dispute.id, resolveForm.value)
    ElMessage.success('裁决已提交')
    await refreshDetail()
    load()
  } finally {
    submitting.value = false
  }
}

const evidenceTypeOptions = [
  { value: 'REMARK', label: '补充说明' },
  { value: 'CHAIN', label: '存证链记录' },
  { value: 'VERIFY', label: '验真记录汇总' },
  { value: 'ATTACHMENT', label: '检测附件' }
]

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="left">
        <span class="title">纠纷举证</span>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px" @change="load">
          <el-option label="待受理" value="OPEN" />
          <el-option label="仲裁中" value="ARBITRATING" />
          <el-option label="已裁决" value="RESOLVED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe empty-text="暂无纠纷案件">
      <el-table-column prop="id" label="案件号" width="90" />
      <el-table-column label="车辆" min-width="160">
        <template #default="{ row }">
          <div>{{ row.brand }} {{ row.model }}</div>
          <div class="sub-text">{{ row.plateNo || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="reportNo" label="报告编号" width="170" />
      <el-table-column label="申请人" width="120">
        <template #default="{ row }">
          {{ row.applicantName }}
          <span class="sub-text">（{{ row.applicantRole === 'BUYER' ? '买家' : '车商' }}）</span>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="争议事由" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'RESOLVED' ? 'success' : row.status === 'ARBITRATING' ? 'warning' : 'info'">
            {{ row.statusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="arbitratorName" label="仲裁员" width="100" />
      <el-table-column prop="createdAt" label="发起时间" width="165" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-drawer v-model="drawerVisible" title="纠纷举证材料包" size="760px">
      <div v-if="detail" class="detail">
        <el-descriptions :column="2" size="small" border>
          <el-descriptions-item label="案件号">{{ detail.dispute.id }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small">{{ detail.dispute.status }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="争议事由" :span="2">{{ detail.dispute.reason }}</el-descriptions-item>
          <el-descriptions-item label="详细描述" :span="2">{{ detail.dispute.description || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报告编号">{{ detail.report?.reportNo }}</el-descriptions-item>
          <el-descriptions-item label="车况">
            {{ detail.report?.totalScore }} 分 / {{ detail.report?.grade }} 级 / v{{ detail.report?.version }}
          </el-descriptions-item>
          <el-descriptions-item label="链上序号">#{{ detail.chain?.seqNo }}</el-descriptions-item>
          <el-descriptions-item label="验真次数">{{ detail.verifyCount }}</el-descriptions-item>
          <el-descriptions-item label="存证指纹" :span="2">
            <code class="hash">{{ detail.chain?.recordHash }}</code>
          </el-descriptions-item>
          <el-descriptions-item label="裁决结果" :span="2" v-if="detail.dispute.result">
            {{ detail.dispute.result === 'SUPPORTED' ? '支持申请人' : '驳回申请' }} —— {{ detail.dispute.conclusion }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">
          举证材料（{{ detail.evidence.length }}）
          <el-button link type="primary" style="margin-left: 12px" @click="openFullReport">查看完整报告</el-button>
        </div>
        <el-timeline>
          <el-timeline-item
            v-for="item in detail.evidence"
            :key="item.id"
            :timestamp="item.createdAt"
            placement="top"
          >
            <div class="evidence">
              <div class="evidence-head">
                <span class="evidence-title">{{ item.title }}</span>
                <el-tag size="small" type="info">{{ item.type }}</el-tag>
              </div>
              <pre class="evidence-content">{{ item.content }}</pre>
            </div>
          </el-timeline-item>
        </el-timeline>

        <div v-if="isArbitrator && detail.dispute.status === 'OPEN'" class="section">
          <el-button type="primary" plain @click="doClaim">受理案件</el-button>
        </div>

        <div v-if="canClose" class="section">
          <el-button type="danger" plain @click="doClose">
            {{ isArbitrator ? '关闭案件' : '撤回申请' }}
          </el-button>
        </div>

        <div v-if="canAddEvidence" class="section">
          <div class="section-title">补充材料</div>
          <div class="evidence-form">
            <el-select v-model="evidenceForm.type" style="width: 160px">
              <el-option v-for="option in evidenceTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
            </el-select>
            <el-select
              v-if="evidenceForm.type === 'ATTACHMENT'"
              v-model="evidenceForm.refId"
              placeholder="选择检测附件"
              style="width: 260px"
            >
              <el-option
                v-for="attachment in attachments"
                :key="attachment.id"
                :label="attachment.fileName"
                :value="attachment.id"
              />
            </el-select>
            <el-input
              v-if="evidenceForm.type === 'REMARK'"
              v-model="evidenceForm.remark"
              placeholder="填写补充说明"
              style="flex: 1"
            />
            <el-button type="primary" :loading="submitting" @click="submitEvidence">提交材料</el-button>
          </div>
        </div>

        <div v-if="isArbitrator && ['OPEN', 'ARBITRATING'].includes(detail.dispute.status)" class="section">
          <div class="section-title">裁决</div>
          <el-radio-group v-model="resolveForm.result" style="margin-bottom: 10px">
            <el-radio-button value="SUPPORTED">支持申请人</el-radio-button>
            <el-radio-button value="REJECTED">驳回申请</el-radio-button>
          </el-radio-group>
          <el-input v-model="resolveForm.conclusion" type="textarea" :rows="3" placeholder="填写裁决结论与依据" />
          <el-button type="primary" style="margin-top: 10px" :loading="submitting" @click="submitResolve">提交裁决</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.title {
  font-size: 15px;
  font-weight: 600;
}

.sub-text {
  color: #98a2b3;
  font-size: 12px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  margin: 20px 0 12px;
}

.hash {
  font-family: Consolas, monospace;
  font-size: 11px;
  word-break: break-all;
}

.evidence-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.evidence-title {
  font-size: 13px;
  font-weight: 600;
}

.evidence-content {
  margin: 8px 0 0;
  font-size: 12px;
  color: #6b7688;
  background: #f7f9fc;
  border-radius: 6px;
  padding: 10px 12px;
  white-space: pre-wrap;
  word-break: break-all;
}

.section {
  border-top: 1px dashed #e6ebf3;
  margin-top: 18px;
  padding-top: 6px;
}

.evidence-form {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
