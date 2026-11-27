<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import {
  listIntentions, counterIntention, acceptIntention, rejectIntention, cancelIntention,
  confirmIntention, declineIntention
} from '@/api/intention'

const auth = useAuthStore()
const loading = ref(false)
const rows = ref([])

const isDealer = computed(() => auth.role === 'DEALER')
const isBuyer = computed(() => auth.role === 'BUYER')

async function load() {
  loading.value = true
  try {
    rows.value = await listIntentions()
  } finally {
    loading.value = false
  }
}

function yuan(cents) {
  if (cents == null) return '-'
  return (cents / 100).toLocaleString('zh-CN')
}

async function doCounter(row) {
  const { value } = await ElMessageBox.prompt('请输入还价金额（元）', `还价 · ${row.reportNo}`, {
    confirmButtonText: '提交还价',
    cancelButtonText: '取消',
    inputPattern: /^\d+(\.\d{1,2})?$/,
    inputErrorMessage: '请输入正确的金额'
  })
  await counterIntention(row.id, Math.round(Number(value) * 100))
  ElMessage.success('还价已提交')
  load()
}

async function doAccept(row) {
  await ElMessageBox.confirm('确认接受该意向？接受后订单状态变为已成交。', '接受意向', {
    confirmButtonText: '确认接受',
    cancelButtonText: '取消'
  })
  await acceptIntention(row.id)
  ElMessage.success('已接受意向')
  load()
}

async function doReject(row) {
  await ElMessageBox.confirm('确认拒绝该意向？', '拒绝意向', {
    confirmButtonText: '确认拒绝',
    cancelButtonText: '取消'
  })
  await rejectIntention(row.id)
  ElMessage.success('已拒绝')
  load()
}

async function doCancel(row) {
  await ElMessageBox.confirm('确认取消该意向单？', '取消意向', {
    confirmButtonText: '确认取消',
    cancelButtonText: '再想想'
  })
  await cancelIntention(row.id)
  ElMessage.success('已取消')
  load()
}

async function doConfirmCounter(row) {
  await ElMessageBox.confirm(
    `确认接受车商还价 ${yuan(row.dealerReplyPrice)} 元？确认后意向成交。`,
    '接受车商还价',
    { confirmButtonText: '接受还价', cancelButtonText: '再想想' }
  )
  await confirmIntention(row.id)
  ElMessage.success('已接受还价，意向成交')
  load()
}

async function doDeclineCounter(row) {
  await ElMessageBox.confirm('拒绝车商还价后，本次意向将结束，确认吗？', '拒绝还价', {
    confirmButtonText: '确认拒绝',
    cancelButtonText: '取消'
  })
  await declineIntention(row.id)
  ElMessage.success('已拒绝车商还价')
  load()
}

function canDealerAct(row) {
  return isDealer.value && ['PENDING', 'COUNTERED'].includes(row.status)
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="title">交易意向</div>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table v-loading="loading" :data="rows" stripe empty-text="暂无意向单">
      <el-table-column label="车辆" min-width="170">
        <template #default="{ row }">
          <div>{{ row.brand }} {{ row.model }}</div>
          <div class="sub-text">{{ row.plateNo || '-' }} · {{ row.mileage }} km</div>
        </template>
      </el-table-column>
      <el-table-column prop="reportNo" label="报告编号" width="170" />
      <el-table-column prop="buyerName" label="买家" width="100" />
      <el-table-column label="买家出价" width="120">
        <template #default="{ row }">{{ yuan(row.offerPrice) }} 元</template>
      </el-table-column>
      <el-table-column label="车商还价" width="120">
        <template #default="{ row }">{{ row.dealerReplyPrice ? yuan(row.dealerReplyPrice) + ' 元' : '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 'ACCEPTED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'warning'">
            {{ row.statusLabel }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="message" label="买家留言" min-width="140" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="165" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <template v-if="canDealerAct(row)">
            <el-button link type="primary" @click="doCounter(row)">还价</el-button>
            <el-button link type="success" @click="doAccept(row)">接受</el-button>
            <el-button link type="danger" @click="doReject(row)">拒绝</el-button>
          </template>
          <template v-if="isBuyer && row.status === 'COUNTERED'">
            <el-button link type="success" @click="doConfirmCounter(row)">接受还价</el-button>
            <el-button link type="danger" @click="doDeclineCounter(row)">拒绝还价</el-button>
          </template>
          <el-button
            v-if="isBuyer && row.status === 'PENDING'"
            link
            type="warning"
            @click="doCancel(row)"
          >取消</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.title {
  font-size: 15px;
  font-weight: 600;
}

.sub-text {
  color: #98a2b3;
  font-size: 12px;
}
</style>
