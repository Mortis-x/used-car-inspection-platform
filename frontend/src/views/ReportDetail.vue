<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { useAuthStore } from '@/stores/auth'
import { getReport, submitCorrection } from '@/api/report'
import { verifyReport } from '@/api/evidence'
import { createIntention } from '@/api/intention'
import { createDispute } from '@/api/dispute'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const reportNo = route.params.reportNo

const loading = ref(true)
const detail = ref(null)
const verification = ref(null)

const correctionVisible = ref(false)
const correctionReason = ref('')
const correctionRows = ref([])
let originalItems = []

const qrVisible = ref(false)
const qrCanvas = ref(null)
const verifyUrl = `${window.location.origin}/verify?reportNo=${reportNo}`

const intentionVisible = ref(false)
const intentionForm = ref({ price: null, message: '' })

const disputeVisible = ref(false)
const disputeForm = ref({ reason: '', description: '' })

const canCorrect = computed(() =>
  report.value?.status === 'ACTIVE' && ['INSPECTOR', 'OPERATOR', 'ADMIN'].includes(auth.role))

const report = computed(() => detail.value?.report)
const vehicle = computed(() => detail.value?.vehicle)

const statusMeta = computed(() => ({
  DRAFT: { text: '待复核', type: 'warning' },
  ACTIVE: { text: '已生效', type: 'success' },
  VOID: { text: '已作废', type: 'danger' },
  SUPERSEDED: { text: '已归档', type: 'info' }
})[report.value?.status] || { text: '未知', type: 'info' })

const categoryRows = computed(() => {
  if (!detail.value) return []
  const groups = {}
  for (const item of detail.value.items) {
    const group = groups[item.category] || (groups[item.category] = { category: item.category, items: [], scoreSum: 0, weightSum: 0 })
    group.items.push(item)
    group.scoreSum += (item.score || 0) * (item.weight || 1)
    group.weightSum += item.weight || 1
  }
  const labels = {
    EXTERIOR: '外观', INTERIOR: '内饰', ENGINE: '发动机舱',
    CHASSIS: '底盘', ACCIDENT: '事故排查', WATER_FIRE: '泡水火烧排查'
  }
  return Object.values(groups).map((group) => ({
    ...group,
    label: labels[group.category] || group.category,
    categoryScore: group.weightSum ? Math.round(group.scoreSum / group.weightSum) : 0
  }))
})

const priceText = computed(() => {
  if (!report.value) return '-'
  const low = (report.value.priceLow / 100).toFixed(0)
  const high = (report.value.priceHigh / 100).toFixed(0)
  return `${Number(low).toLocaleString()} – ${Number(high).toLocaleString()} 元`
})

async function load() {
  loading.value = true
  try {
    detail.value = await getReport(reportNo)
  } finally {
    loading.value = false
  }
}

async function doVerify() {
  verification.value = await verifyReport(reportNo)
  if (verification.value.valid) {
    ElMessage.success('验真通过：报告内容与存证链一致')
  } else {
    ElMessage.error(verification.value.message)
  }
}

function print() {
  window.print()
}

function openPhoto(attachment) {
  const url = attachment.originUrl || attachment.thumbUrl
  if (url) {
    window.open(url, '_blank')
  }
}

async function openQr() {
  qrVisible.value = true
  await nextTick()
  await QRCode.toCanvas(qrCanvas.value, verifyUrl, { width: 220, margin: 1 })
}

async function submitIntention() {
  if (!intentionForm.value.price) {
    ElMessage.warning('请填写出价金额')
    return
  }
  await createIntention({
    reportNo,
    offerPrice: Math.round(Number(intentionForm.value.price) * 100),
    message: intentionForm.value.message
  })
  ElMessage.success('出价已提交，等待车商响应')
  intentionVisible.value = false
  intentionForm.value = { price: null, message: '' }
}

async function submitDispute() {
  if (!disputeForm.value.reason.trim()) {
    ElMessage.warning('请填写争议事由')
    return
  }
  await createDispute({
    reportNo,
    reason: disputeForm.value.reason,
    description: disputeForm.value.description
  })
  ElMessage.success('纠纷已发起，报告与存证已自动纳入材料包')
  disputeVisible.value = false
  disputeForm.value = { reason: '', description: '' }
}

function openCorrection() {
  originalItems = detail.value.items.map((item) => ({
    id: item.id,
    itemName: item.itemName,
    score: item.score,
    defectLevel: item.defectLevel,
    remark: item.remark || ''
  }))
  correctionRows.value = originalItems.map((item) => ({ ...item }))
  correctionReason.value = ''
  correctionVisible.value = true
}

async function submitCorrectionForm() {
  if (!correctionReason.value.trim()) {
    ElMessage.warning('请填写更正原因')
    return
  }
  const changed = correctionRows.value
    .filter((row) => {
      const origin = originalItems.find((item) => item.id === row.id)
      return origin && (origin.score !== row.score
        || origin.defectLevel !== row.defectLevel
        || (origin.remark || '') !== (row.remark || ''))
    })
    .map((row) => ({
      itemId: row.id,
      score: row.score,
      defectLevel: row.defectLevel,
      remark: row.remark
    }))
  if (!changed.length) {
    ElMessage.warning('没有检测到修改内容')
    return
  }
  await submitCorrection(reportNo, { reason: correctionReason.value, items: changed })
  ElMessage.success('更正申请已提交，等待平台运营员复核')
  correctionVisible.value = false
}

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="report-page">
    <div class="toolbar no-print">
      <el-button link @click="router.back()">← 返回</el-button>
      <div class="actions">
        <el-button v-if="report?.status === 'ACTIVE' && auth.role === 'BUYER'" type="success" @click="intentionVisible = true">
          我要出价
        </el-button>
        <el-button
          v-if="report?.status === 'ACTIVE' && ['BUYER', 'DEALER'].includes(auth.role)"
          @click="disputeVisible = true"
        >发起纠纷</el-button>
        <el-button v-if="canCorrect" @click="openCorrection">申请更正</el-button>
        <el-button @click="openQr">二维码</el-button>
        <el-button @click="doVerify">验真</el-button>
        <el-button type="primary" @click="print">打印报告</el-button>
      </div>
    </div>

    <template v-if="detail">
      <div class="header-card">
        <div class="title">
          <h2>车辆检测报告</h2>
          <span class="report-no">{{ report.reportNo }}</span>
          <el-tag size="small" effect="dark" type="info">v{{ report.version }}</el-tag>
          <el-tag :type="statusMeta.type" size="small">{{ statusMeta.text }}</el-tag>
        </div>
        <div class="grade-block">
          <div class="grade">{{ report.grade }}</div>
          <div class="score">{{ report.totalScore }} 分</div>
        </div>
      </div>

      <div class="grid">
        <div class="info-card">
          <div class="card-title">车辆信息</div>
          <el-descriptions :column="2" size="small">
            <el-descriptions-item label="品牌车型">{{ vehicle?.brand }} {{ vehicle?.model }}</el-descriptions-item>
            <el-descriptions-item label="车牌">{{ vehicle?.plateNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="VIN">{{ vehicle?.vin }}</el-descriptions-item>
            <el-descriptions-item label="表显里程">{{ vehicle?.mileage }} km</el-descriptions-item>
            <el-descriptions-item label="检测单号">{{ detail.order?.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="出具时间">{{ report.issuedAt }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="info-card highlight">
          <div class="card-title">估价区间</div>
          <div class="price">{{ priceText }}</div>
          <div class="price-tip">按车况系数与基准价模型测算，仅供交易参考</div>
        </div>
      </div>

      <div class="info-card">
        <div class="card-title">六大项得分</div>
        <el-table :data="categoryRows" size="small">
          <el-table-column prop="label" label="检测大类" width="140" />
          <el-table-column prop="categoryScore" label="得分" width="90" />
          <el-table-column label="检测项">
            <template #default="{ row }">
              {{ row.items.map((item) => `${item.itemName}(${item.score ?? '未填'})`).join('、') }}
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="info-card">
        <div class="card-title">缺陷记录（{{ detail.defects.length }} 条）</div>
        <el-table :data="detail.defects" size="small" empty-text="未记录缺陷">
          <el-table-column prop="itemCode" label="检测项编号" width="130" />
          <el-table-column label="缺陷等级" width="110">
            <template #default="{ row }">{{ ['无', '轻微', '一般', '严重'][row.defectLevel] }}</template>
          </el-table-column>
          <el-table-column prop="deduct" label="扣分" width="90" />
        </el-table>
      </div>

      <div class="info-card" v-if="detail.attachments && detail.attachments.length">
        <div class="card-title">检测照片（{{ detail.attachments.length }} 张）</div>
        <div class="photo-grid">
          <div v-for="attachment in detail.attachments" :key="attachment.id" class="photo" @click="openPhoto(attachment)">
            <img v-if="attachment.thumbUrl" :src="attachment.thumbUrl" :alt="attachment.fileName" />
            <div v-else class="photo-fallback">处理中</div>
            <div class="photo-meta">
              <span class="photo-name">{{ attachment.fileName }}</span>
              <span v-if="attachment.originHash" class="photo-hash">{{ attachment.originHash.slice(0, 8) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="info-card evidence">
        <div class="card-title">存证信息</div>
        <template v-if="report.canonicalJson">
          <div class="chain-row">
            <span class="label">报告指纹</span>
            <code>{{ verification?.recordHash || '点击「验真」查询链上指纹' }}</code>
          </div>
          <div class="chain-row">
            <span class="label">链上序号</span>
            <span>{{ verification?.chainSeq ?? '-' }}</span>
          </div>
          <div class="chain-row" v-if="verification">
            <span class="label">验真结果</span>
            <el-tag :type="verification.valid ? 'success' : 'danger'" size="small">
              {{ verification.valid ? '通过' : '未通过' }}
            </el-tag>
            <span class="verify-msg">{{ verification.message }}</span>
          </div>
        </template>
        <div v-else class="empty-hint">报告尚未生效，复核通过后将写入哈希链存证</div>
      </div>

      <el-drawer v-model="correctionVisible" title="申请报告更正" size="720px">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="更正需平台运营员复核；通过后生成新版本报告，原报告归档保留且仍可验真"
          style="margin-bottom: 16px"
        />
        <el-table :data="correctionRows" size="small" height="400" border>
          <el-table-column prop="itemName" label="检测项" width="150" />
          <el-table-column label="评分" width="130">
            <template #default="{ row }">
              <el-input-number v-model="row.score" :min="0" :max="100" size="small" controls-position="right" />
            </template>
          </el-table-column>
          <el-table-column label="缺陷等级" width="120">
            <template #default="{ row }">
              <el-select v-model="row.defectLevel" size="small">
                <el-option label="无" :value="0" />
                <el-option label="轻微" :value="1" />
                <el-option label="一般" :value="2" />
                <el-option label="严重" :value="3" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="说明">
            <template #default="{ row }">
              <el-input v-model="row.remark" size="small" placeholder="更正说明" />
            </template>
          </el-table-column>
        </el-table>

        <div class="reason-box">
          <div class="reason-label">更正原因（必填）</div>
          <el-input v-model="correctionReason" type="textarea" :rows="3" placeholder="例如：复检发现左前翼子板补漆，原录入遗漏" />
        </div>

        <template #footer>
          <el-button @click="correctionVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCorrectionForm">提交复核</el-button>
        </template>
      </el-drawer>

      <el-dialog v-model="qrVisible" title="报告验真二维码" width="320px" class="no-print">
        <div class="qr-box">
          <canvas ref="qrCanvas" />
          <div class="qr-tip">扫码进入公开验真页，无需登录</div>
          <code class="qr-url">{{ verifyUrl }}</code>
        </div>
      </el-dialog>

      <el-dialog v-model="intentionVisible" title="发起购车意向" width="420px">
        <el-form label-width="90px">
          <el-form-item label="出价（元）">
            <el-input-number v-model="intentionForm.price" :min="1" :precision="2" :step="1000" style="width: 100%" />
          </el-form-item>
          <el-form-item label="留言">
            <el-input v-model="intentionForm.message" type="textarea" :rows="3" placeholder="例如：可当天过户，希望周末看车" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="intentionVisible = false">取消</el-button>
          <el-button type="primary" @click="submitIntention">提交出价</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="disputeVisible" title="发起纠纷举证" width="460px">
        <el-form label-width="90px">
          <el-form-item label="争议事由">
            <el-input v-model="disputeForm.reason" placeholder="例如：实车里程与报告不符" />
          </el-form-item>
          <el-form-item label="补充描述">
            <el-input v-model="disputeForm.description" type="textarea" :rows="4" placeholder="描述具体情况与诉求" />
          </el-form-item>
        </el-form>
        <div class="dispute-tip">提交后系统会自动把报告与存证链记录纳入材料包，可在「纠纷举证」中补充附件与说明。</div>
        <template #footer>
          <el-button @click="disputeVisible = false">取消</el-button>
          <el-button type="primary" @click="submitDispute">提交纠纷</el-button>
        </template>
      </el-dialog>
    </template>
  </div>
</template>

<style scoped>
.report-page {
  max-width: 1080px;
  margin: 0 auto;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.header-card {
  background: linear-gradient(135deg, #12203a, #1d3b6e);
  color: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.title h2 {
  margin: 0;
  font-size: 22px;
}

.report-no {
  font-family: Consolas, monospace;
  opacity: 0.75;
}

.grade-block {
  text-align: center;
}

.grade {
  font-size: 42px;
  font-weight: 700;
  color: #22c3a6;
  line-height: 1;
}

.score {
  font-size: 13px;
  opacity: 0.8;
  margin-top: 4px;
}

.grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.info-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(15, 35, 95, 0.06);
}

.grid .info-card {
  margin-bottom: 0;
}

.card-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.highlight .price {
  font-size: 24px;
  font-weight: 700;
  color: #2f6bff;
}

.price-tip {
  font-size: 12px;
  color: #98a2b3;
  margin-top: 6px;
}

.chain-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 13px;
}

.chain-row .label {
  color: #6b7688;
  width: 72px;
  flex-shrink: 0;
}

code {
  font-family: Consolas, monospace;
  font-size: 12px;
  word-break: break-all;
  color: #33405a;
}

.verify-msg {
  color: #6b7688;
}

.empty-hint {
  color: #98a2b3;
  font-size: 13px;
}

.reason-box {
  margin-top: 16px;
}

.reason-label {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.qr-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.qr-tip {
  font-size: 12px;
  color: #98a2b3;
}

.qr-url {
  font-family: Consolas, monospace;
  font-size: 11px;
  color: #6b7688;
  word-break: break-all;
  text-align: center;
}

.dispute-tip {
  font-size: 12px;
  color: #98a2b3;
  background: #f7f9fc;
  border-radius: 6px;
  padding: 8px 10px;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}

.photo {
  border: 1px solid #eef1f6;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.photo:hover {
  border-color: #c9d8ff;
}

.photo img {
  width: 100%;
  height: 100px;
  object-fit: cover;
  display: block;
}

.photo-fallback {
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f2f5fa;
  color: #b6c0d0;
  font-size: 12px;
}

.photo-meta {
  display: flex;
  justify-content: space-between;
  padding: 6px 8px;
  font-size: 11px;
  color: #6b7688;
}

.photo-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.photo-hash {
  font-family: Consolas, monospace;
  color: #98a2b3;
}

@media print {
  .no-print {
    display: none;
  }
}
</style>
