<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  listVehicles, createVehicle, updateVehicle,
  listMaintenance, addMaintenance
} from '@/api/vehicle'
import { listInspections } from '@/api/inspection'

const auth = useAuthStore()

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 10 })

const canEdit = computed(() => ['DEALER', 'INSPECTOR', 'OPERATOR', 'ADMIN'].includes(auth.role))
const canAddMaintenance = computed(() => ['INSPECTOR', 'OPERATOR', 'ADMIN'].includes(auth.role))

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  vin: '', plateNo: '', brand: '', model: '', color: '',
  firstRegDate: '', mileage: 0, transferCount: 0
})
const rules = {
  vin: [{ required: true, message: '请输入 VIN', trigger: 'blur' }],
  brand: [{ required: true, message: '请输入品牌', trigger: 'blur' }],
  model: [{ required: true, message: '请输入车型', trigger: 'blur' }],
  mileage: [{ required: true, message: '请输入表显里程', trigger: 'blur' }]
}

const detailVisible = ref(false)
const detail = ref(null)
const maintenance = ref([])
const orders = ref([])

const maintenanceVisible = ref(false)
const maintenanceForm = reactive({
  recordType: 'MAINTAIN', recordDate: '', mileage: null, provider: '', content: '', amount: null
})

async function load() {
  loading.value = true
  try {
    const data = await listVehicles(query)
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    vin: '', plateNo: '', brand: '', model: '', color: '',
    firstRegDate: '', mileage: 0, transferCount: 0
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    vin: row.vin, plateNo: row.plateNo, brand: row.brand, model: row.model,
    color: row.color, firstRegDate: row.firstRegDate, mileage: row.mileage,
    transferCount: row.transferCount
  })
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  if (editingId.value) {
    await updateVehicle(editingId.value, form)
  } else {
    await createVehicle(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function openDetail(row) {
  detail.value = row
  detailVisible.value = true
  maintenance.value = await listMaintenance(row.id)
  const page = await listInspections({ vehicleId: row.id, page: 1, size: 20 })
  orders.value = page.records
}

function openMaintenance() {
  Object.assign(maintenanceForm, {
    recordType: 'MAINTAIN', recordDate: '', mileage: null, provider: '', content: '', amount: null
  })
  maintenanceVisible.value = true
}

async function saveMaintenance() {
  const payload = { ...maintenanceForm }
  if (payload.amount != null) {
    payload.amount = Math.round(Number(payload.amount) * 100)
  }
  await addMaintenance(detail.value.id, payload)
  ElMessage.success('维保记录已添加')
  maintenanceVisible.value = false
  maintenance.value = await listMaintenance(detail.value.id)
}

async function removeHint() {
  await ElMessageBox.alert('车辆档案承担历史检测与举证作用，平台不提供删除功能。', '提示')
}

const statusType = (status) => ({
  PENDING: 'info', INSPECTING: 'warning', PENDING_REPORT: 'primary',
  PENDING_APPROVAL: 'warning', ISSUED: 'success', WITHDRAWN: 'danger'
})[status] || 'info'

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="filters">
        <el-input
          v-model="query.keyword"
          placeholder="VIN / 车牌 / 品牌 / 车型"
          clearable
          style="width: 260px"
          @keyup.enter="query.page = 1; load()"
          @clear="query.page = 1; load()"
        />
        <el-button type="primary" plain @click="query.page = 1; load()">查询</el-button>
      </div>
      <div>
        <el-button v-if="canEdit" type="primary" @click="openCreate">新建车辆档案</el-button>
        <el-button plain @click="removeHint">删除说明</el-button>
      </div>
    </div>

    <el-table v-loading="loading" :data="rows" stripe height="520">
      <el-table-column prop="vin" label="VIN" width="190" />
      <el-table-column prop="plateNo" label="车牌" width="110" />
      <el-table-column label="品牌车型" min-width="200">
        <template #default="{ row }">{{ row.brand }} {{ row.model }}</template>
      </el-table-column>
      <el-table-column prop="mileage" label="表显里程(km)" width="130" />
      <el-table-column prop="transferCount" label="过户次数" width="100" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">档案</el-button>
          <el-button v-if="canEdit" link type="primary" @click="openEdit(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑车辆档案' : '新建车辆档案'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
        <el-form-item label="VIN" prop="vin">
          <el-input v-model="form.vin" maxlength="17" show-word-limit placeholder="17 位车架号" />
        </el-form-item>
        <el-form-item label="车牌号">
          <el-input v-model="form.plateNo" placeholder="沪A12345" />
        </el-form-item>
        <el-form-item label="品牌" prop="brand">
          <el-input v-model="form.brand" />
        </el-form-item>
        <el-form-item label="车型" prop="model">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="颜色">
          <el-input v-model="form.color" />
        </el-form-item>
        <el-form-item label="初次登记">
          <el-date-picker v-model="form.firstRegDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="表显里程" prop="mileage">
          <el-input-number v-model="form.mileage" :min="0" :step="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="过户次数">
          <el-input-number v-model="form.transferCount" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="detail ? `${detail.brand} ${detail.model}` : '车辆档案'" size="620px">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="VIN">{{ detail.vin }}</el-descriptions-item>
          <el-descriptions-item label="车牌">{{ detail.plateNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="颜色">{{ detail.color || '-' }}</el-descriptions-item>
          <el-descriptions-item label="初次登记">{{ detail.firstRegDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="表显里程">{{ detail.mileage }} km</el-descriptions-item>
          <el-descriptions-item label="过户次数">{{ detail.transferCount }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-head">
          <span>历史检测单</span>
        </div>
        <el-table :data="orders" size="small" empty-text="暂无检测单">
          <el-table-column prop="orderNo" label="检测单号" width="160" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag size="small" :type="statusType(row.status)">{{ row.statusLabel }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="inspectorName" label="检测师" width="90" />
          <el-table-column prop="createdAt" label="创建时间" />
        </el-table>

        <div class="section-head">
          <span>维保记录</span>
          <el-button v-if="canAddMaintenance" size="small" type="primary" plain @click="openMaintenance">
            添加记录
          </el-button>
        </div>
        <el-table :data="maintenance" size="small" empty-text="暂无维保记录">
          <el-table-column prop="recordDate" label="日期" width="110" />
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              {{ { MAINTAIN: '保养', REPAIR: '维修', ACCIDENT: '事故' }[row.recordType] || row.recordType }}
            </template>
          </el-table-column>
          <el-table-column prop="provider" label="机构" width="120" />
          <el-table-column prop="content" label="内容" />
          <el-table-column label="金额" width="100">
            <template #default="{ row }">
              {{ row.amount == null ? '-' : (row.amount / 100).toFixed(2) + ' 元' }}
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-drawer>

    <el-dialog v-model="maintenanceVisible" title="添加维保记录" width="520px">
      <el-form :model="maintenanceForm" label-width="80px">
        <el-form-item label="类型">
          <el-select v-model="maintenanceForm.recordType" style="width: 100%">
            <el-option label="保养" value="MAINTAIN" />
            <el-option label="维修" value="REPAIR" />
            <el-option label="事故" value="ACCIDENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="maintenanceForm.recordDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="里程">
          <el-input-number v-model="maintenanceForm.mileage" :min="0" :step="1000" style="width: 100%" />
        </el-form-item>
        <el-form-item label="机构">
          <el-input v-model="maintenanceForm.provider" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="maintenanceForm.content" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="金额(元)">
          <el-input-number v-model="maintenanceForm.amount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="maintenanceVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMaintenance">保存</el-button>
      </template>
    </el-dialog>
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

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 22px 0 10px;
  font-weight: 600;
  font-size: 14px;
}
</style>
