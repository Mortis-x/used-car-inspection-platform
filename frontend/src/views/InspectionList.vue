<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  listInspections, getInspection, createInspection, startInspection,
  submitInspection, withdrawInspection, getOrderTimeline
} from '@/api/inspection'
import { listVehicles } from '@/api/vehicle'
import { listTemplates, listUsersByRole } from '@/api/template'
import { generateReport } from '@/api/report'

const auth = useAuthStore()
const router = useRouter()

const categoryLabels = {
  EXTERIOR: '外观',
  INTERIOR: '内饰',
  ENGINE: '发动机舱',
  CHASSIS: '底盘',
  ACCIDENT: '事故排查',
  WATER_FIRE: '泡水火烧排查'
}

const statusOptions = [
  { value: 'PENDING', label: '待检测' },
  { value: 'INSPECTING', label: '检测中' },
  { value: 'PENDING_REPORT', label: '待出报告' },
  { value: 'ISSUED', label: '已出报告' },
  { value: 'WITHDRAWN', label: '已撤回' }
]

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ status: '', keyword: '', page: 1, size: 10 })

const canCreate = computed(() => ['DEALER', 'OPERATOR', 'ADMIN'].includes(auth.role))
const isInspector = computed(() => auth.role === 'INSPECTOR')

const dialogVisible = ref(false)
const form = reactive({ vehicleId: null, templateId: null, inspectorId: null, remark: '' })
const vehicleOptions = ref([])
const templateOptions = ref([])
const inspectorOptions = ref([])

const detailVisible = ref(false)
const timeline = ref([])

const timelineActionLabels = {
  CREATE: '创建检测单', START: '开始检测', SUBMIT: '提交检测结果', WITHDRAW: '撤回检测单',
  APPLY: '提交报告复核', ISSUE: '报告出具并存证', REJECT: '复核驳回',
  CORRECT_APPLY: '申请报告更正', UPLOAD: '上传附件'
}
const detail = ref(null)

const groupedItems = computed(() => {
  if (!detail.value) return []
  const groups = {}
  for (const item of detail.value.items) {
    if (!groups[item.category]) {
      groups[item.category] = { category: item.category, label: categoryLabels[item.category] || item.category, items: [] }
    }
    groups[item.category].items.push(item)
  }
  return Object.values(groups)
})

const statusType = (status) => ({
  PENDING: 'info', INSPECTING: 'warning', PENDING_REPORT: 'primary',
  PENDING_APPROVAL: 'warning', ISSUED: 'success', WITHDRAWN: 'danger'
})[status] || 'info'

async function load() {
  loading.value = true
  try {
    const data = await listInspections(query)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

async function openCreate() {
  Object.assign(form, { vehicleId: null, templateId: null, inspectorId: null, remark: '' })
  if (!vehicleOptions.value.length) {
    const page = await listVehicles({ page: 1, size: 100 })
    vehicleOptions.value = page.records
  }
  if (!templateOptions.value.length) {
    templateOptions.value = await listTemplates()
  }
  if (!inspectorOptions.value.length) {
    inspectorOptions.value = await listUsersByRole('INSPECTOR')
  }
  dialogVisible.value = true
}

async function save() {
  if (!form.vehicleId || !form.templateId || !form.inspectorId) {
    ElMessage.warning('请选择车辆、模板和检测师')
    return
  }
  await createInspection(form)
  ElMessage.success('检测单创建成功')
  dialogVisible.value = false
  query.page = 1
  load()
}

async function openDetail(row) {
  const [data, logs] = await Promise.all([getInspection(row.id), getOrderTimeline(row.id)])
  detail.value = data
  timeline.value = logs
  detailVisible.value = true
}

function canStart(row) {
  return isInspector.value && row.status === 'PENDING' && row.inspectorId === auth.user?.userId
}

function canSubmit(row) {
  return isInspector.value && row.status === 'INSPECTING' && row.inspectorId === auth.user?.userId
}

function canWithdraw(row) {
  return ['PENDING', 'INSPECTING', 'PENDING_REPORT'].includes(row.status)
}

function canFill(row) {
  return isInspector.value && row.status === 'INSPECTING' && row.inspectorId === auth.user?.userId
}

function canGenerate(row) {
  return row.status === 'PENDING_REPORT' && ['INSPECTOR', 'OPERATOR', 'ADMIN'].includes(auth.role)
}

async function doGenerate(row) {
  await ElMessageBox.confirm('将按六大项权重汇总评分并提交平台复核，确定生成报告吗？', '生成报告', {
    confirmButtonText: '生成并提交复核',
    cancelButtonText: '取消'
  })
  const result = await generateReport(row.id, '检测完成，申请出具报告')
  ElMessage.success(`报告 ${result.reportNo} 已生成，总分 ${result.totalScore}，等待复核`)
  load()
}

function openReport(row) {
  router.push(`/reports/${row.reportNo}`)
}

function goFill(row) {
  router.push(`/inspections/${row.id}/fill`)
}

async function doStart(row) {
  await startInspection(row.id)
  ElMessage.success('已开始检测')
  load()
}

async function doSubmit(row) {
  try {
    await submitInspection(row.id)
    ElMessage.success('检测结果已提交，等待生成报告')
    load()
  } catch (e) {
    // 缺项拦截的具体原因已由请求层提示
  }
}

async function doWithdraw(row) {
  const { value } = await ElMessageBox.prompt('请填写撤回原因', `撤回检测单 ${row.orderNo}`, {
    confirmButtonText: '确认撤回',
    cancelButtonText: '取消',
    inputPattern: /\S+/,
    inputErrorMessage: '撤回原因不能为空'
  })
  await withdrawInspection(row.id, value)
  ElMessage.success('已撤回')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="filters">
        <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px" @change="query.page = 1; load()">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input
          v-model="query.keyword"
          placeholder="单号 / VIN / 车牌 / 车型"
          clearable
          style="width: 240px"
          @keyup.enter="query.page = 1; load()"
          @clear="query.page = 1; load()"
        />
        <el-button type="primary" plain @click="query.page = 1; load()">查询</el-button>
      </div>
      <el-button v-if="canCreate" type="primary" @click="openCreate">新建检测单</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe height="520">
      <el-table-column prop="orderNo" label="检测单号" width="165" />
      <el-table-column label="车辆" min-width="220">
        <template #default="{ row }">
          <div>{{ row.brand }} {{ row.model }}</div>
          <div class="sub-text">{{ row.plateNo || '未上牌' }} · {{ row.vin }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="inspectorName" label="检测师" width="90" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="statusType(row.status)">{{ row.statusLabel }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          <el-button v-if="canStart(row)" link type="warning" @click="doStart(row)">开始检测</el-button>
          <el-button v-if="canFill(row)" link type="primary" @click="goFill(row)">录入</el-button>
          <el-button v-if="canSubmit(row)" link type="success" @click="doSubmit(row)">提交</el-button>
          <el-button v-if="canGenerate(row)" link type="success" @click="doGenerate(row)">出报告</el-button>
          <el-button v-if="row.reportNo" link type="primary" @click="openReport(row)">查看报告</el-button>
          <el-button v-if="canWithdraw(row)" link type="danger" @click="doWithdraw(row)">撤回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        layout="total, prev, pager, next"
        :total="total"
        @current-change="load"
      />
    </div>

    <el-dialog v-model="dialogVisible" title="新建检测单" width="560px">
      <el-form :model="form" label-width="96px">
        <el-form-item label="车辆">
          <el-select v-model="form.vehicleId" filterable placeholder="选择车辆" style="width: 100%">
            <el-option
              v-for="vehicle in vehicleOptions"
              :key="vehicle.id"
              :label="`${vehicle.brand} ${vehicle.model}（${vehicle.plateNo || vehicle.vin}）`"
              :value="vehicle.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="检测模板">
          <el-select v-model="form.templateId" placeholder="选择模板" style="width: 100%">
            <el-option v-for="template in templateOptions" :key="template.id" :label="template.name" :value="template.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="检测师">
          <el-select v-model="form.inspectorId" placeholder="指派检测师" style="width: 100%">
            <el-option
              v-for="user in inspectorOptions"
              :key="user.id"
              :label="`${user.realName}（${user.orgName || user.username}）`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">创建</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" title="检测单详情" size="640px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="检测单号">{{ detail.order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag size="small" :type="statusType(detail.order.status)">{{ detail.statusLabel }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="车辆">
            {{ detail.vehicle?.brand }} {{ detail.vehicle?.model }}
          </el-descriptions-item>
          <el-descriptions-item label="VIN">{{ detail.vehicle?.vin }}</el-descriptions-item>
          <el-descriptions-item label="撤回原因" :span="2">
            {{ detail.order.withdrawReason || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-head">检测项（{{ detail.items.length }} 项）</div>
        <div v-for="group in groupedItems" :key="group.category" class="category-block">
          <div class="category-title">{{ group.label }}</div>
          <el-table :data="group.items" size="small">
            <el-table-column prop="itemCode" label="编号" width="90" />
            <el-table-column prop="itemName" label="检测项" min-width="140" />
            <el-table-column label="得分" width="80">
              <template #default="{ row }">{{ row.score == null ? '未填' : row.score }}</template>
            </el-table-column>
            <el-table-column label="缺陷" width="90">
              <template #default="{ row }">
                {{ ['无', '轻微', '一般', '严重'][row.defectLevel] ?? '未填' }}
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" />
          </el-table>
        </div>

        <div class="section-head">流转记录（{{ timeline.length }}）</div>
        <el-timeline v-if="timeline.length">
          <el-timeline-item
            v-for="log in timeline"
            :key="log.id"
            :timestamp="log.createdAt"
            placement="top"
          >
            <div class="timeline-title">
              {{ timelineActionLabels[log.action] || log.action }}
              <span class="timeline-user">{{ log.username }}（{{ log.role }}）</span>
            </div>
            <div class="timeline-detail">{{ log.detail }}</div>
          </el-timeline-item>
        </el-timeline>
        <div v-else class="timeline-empty">暂无流转记录</div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.sub-text {
  color: #98a2b3;
  font-size: 12px;
  margin-top: 2px;
}

.section-head {
  margin: 22px 0 10px;
  font-weight: 600;
  font-size: 14px;
}

.category-block {
  margin-bottom: 18px;
}

.category-title {
  font-size: 13px;
  font-weight: 600;
  color: #2f6bff;
  margin-bottom: 8px;
}

.timeline-title {
  font-size: 13px;
  font-weight: 600;
}

.timeline-user {
  color: #98a2b3;
  font-weight: 400;
  font-size: 12px;
  margin-left: 8px;
}

.timeline-detail {
  font-size: 12px;
  color: #6b7688;
  margin-top: 3px;
}

.timeline-empty {
  color: #98a2b3;
  font-size: 13px;
}
</style>
