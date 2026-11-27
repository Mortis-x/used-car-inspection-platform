<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getInspection, submitInspection } from '@/api/inspection'
import { updateInspectionItem } from '@/api/inspectionItem'
import { listAttachments, uploadAttachment, getAttachmentUrl } from '@/api/attachment'

const route = useRoute()
const router = useRouter()
const orderId = Number(route.params.id)

const categoryLabels = {
  EXTERIOR: '外观',
  INTERIOR: '内饰',
  ENGINE: '发动机舱',
  CHASSIS: '底盘',
  ACCIDENT: '事故排查',
  WATER_FIRE: '泡水火烧排查'
}

const loading = ref(true)
const order = ref(null)
const vehicle = ref(null)
const items = ref([])
const selectedId = ref(null)
const attachments = ref([])
const saveHint = ref('')
const submitting = ref(false)

let saveTimer = null
let pollTimer = null

const selected = computed(() => items.value.find((item) => item.id === selectedId.value) || null)

const groups = computed(() => {
  const map = {}
  for (const item of items.value) {
    if (!map[item.category]) {
      map[item.category] = { category: item.category, label: categoryLabels[item.category] || item.category, items: [], filled: 0 }
    }
    map[item.category].items.push(item)
    if (isDone(item)) map[item.category].filled++
  }
  return Object.values(map)
})

const filledCount = computed(() => items.value.filter(isDone).length)
const totalCount = computed(() => items.value.length)
const percent = computed(() => totalCount.value ? Math.round(filledCount.value / totalCount.value * 100) : 0)
const pendingAttachments = computed(() => attachments.value.some((a) => ['PENDING', 'PROCESSING'].includes(a.status)))

function isDone(item) {
  return item.score != null && item.defectLevel != null
}

async function load() {
  loading.value = true
  try {
    const detail = await getInspection(orderId)
    order.value = detail.order
    vehicle.value = detail.vehicle
    items.value = detail.items
    if (!selectedId.value) {
      selectedId.value = items.value[0]?.id || null
    }
    await refreshAttachments()
  } finally {
    loading.value = false
  }
}

function selectItem(item) {
  selectedId.value = item.id
}

function scheduleSave() {
  saveHint.value = '保存中…'
  clearTimeout(saveTimer)
  saveTimer = setTimeout(saveNow, 450)
}

async function saveNow() {
  if (!selected.value) return
  const target = selected.value
  try {
    await updateInspectionItem(orderId, target.id, {
      score: target.score,
      defectLevel: target.defectLevel,
      remark: target.remark
    })
    saveHint.value = '已保存 ' + new Date().toLocaleTimeString('zh-CN', { hour12: false })
  } catch (e) {
    saveHint.value = '保存失败'
  }
}

async function refreshAttachments() {
  attachments.value = await listAttachments(orderId)
  syncPolling()
}

function syncPolling() {
  clearTimeout(pollTimer)
  if (pendingAttachments.value) {
    pollTimer = setTimeout(refreshAttachments, 1500)
  }
}

async function handleUpload(option) {
  try {
    await uploadAttachment(orderId, option.file, selectedId.value)
    ElMessage.success('已加入处理队列')
    await refreshAttachments()
  } catch (e) {
    option.onError?.(e)
  }
}

async function openAttachment(attachment) {
  if (attachment.status !== 'DONE') return
  const url = await getAttachmentUrl(attachment.id, 'origin')
  window.open(url, '_blank')
}

function statusMeta(status) {
  return ({
    PENDING: { text: '排队中', type: 'info' },
    PROCESSING: { text: '处理中', type: 'warning' },
    DONE: { text: '已完成', type: 'success' },
    FAILED: { text: '失败', type: 'danger' }
  })[status] || { text: status, type: 'info' }
}

function itemNameOf(itemId) {
  return items.value.find((item) => item.id === itemId)?.itemName || '通用附件'
}

async function submit() {
  if (filledCount.value < totalCount.value) {
    ElMessage.warning(`还有 ${totalCount.value - filledCount.value} 个检测项未填写完整`)
    return
  }
  await ElMessageBox.confirm('提交后将进入只读状态，修改需走更正流程，确定提交吗？', '提交检测结果', {
    confirmButtonText: '确认提交',
    cancelButtonText: '再检查一下'
  })
  submitting.value = true
  try {
    await submitInspection(orderId)
    ElMessage.success('提交成功，已进入待出报告状态')
    router.push('/inspections')
  } finally {
    submitting.value = false
  }
}

function back() {
  router.push('/inspections')
}

onMounted(load)
onBeforeUnmount(() => {
  clearTimeout(saveTimer)
  clearTimeout(pollTimer)
})
</script>

<template>
  <div v-loading="loading" class="fill-page">
    <div class="topbar">
      <div class="left">
        <el-button link @click="back">← 返回</el-button>
        <span class="order-no">{{ order?.orderNo }}</span>
        <el-tag size="small" type="warning">检测中</el-tag>
      </div>
      <div class="vehicle" v-if="vehicle">
        {{ vehicle.brand }} {{ vehicle.model }} · {{ vehicle.plateNo || '未上牌' }} · {{ vehicle.mileage }} km
      </div>
      <div class="actions">
        <span class="save-hint">{{ saveHint }}</span>
        <el-button @click="back">退出</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">提交检测</el-button>
      </div>
    </div>

    <div class="body">
      <aside class="tree">
        <div v-for="group in groups" :key="group.category" class="group">
          <div class="cat">
            <span>{{ group.label }}</span>
            <span class="cat-count">{{ group.filled }}/{{ group.items.length }}</span>
          </div>
          <div
            v-for="item in group.items"
            :key="item.id"
            :class="['item', { active: item.id === selectedId, done: isDone(item) }]"
            @click="selectItem(item)"
          >
            <span class="dot" />
            <span class="name">{{ item.itemName }}</span>
            <span class="score">{{ item.score ?? '—' }}</span>
          </div>
        </div>
      </aside>

      <section v-if="selected" class="editor">
        <h3>
          {{ selected.itemName }}
          <small>{{ selected.itemCode }}</small>
        </h3>
        <div class="rule">缺陷等级默认扣分：轻微 3 分 / 一般 6 分 / 严重 10 分，最终评分由评分引擎在报告阶段计算。</div>

        <div class="field">
          <label>评分（0–100）</label>
          <div class="score-row">
            <el-slider v-model="selected.score" :min="0" :max="100" :step="1" @change="scheduleSave" />
            <el-input-number v-model="selected.score" :min="0" :max="100" @change="scheduleSave" />
          </div>
        </div>

        <div class="field">
          <label>缺陷等级（必选）</label>
          <el-radio-group v-model="selected.defectLevel" @change="scheduleSave">
            <el-radio-button :value="0">无</el-radio-button>
            <el-radio-button :value="1">轻微</el-radio-button>
            <el-radio-button :value="2">一般</el-radio-button>
            <el-radio-button :value="3">严重</el-radio-button>
          </el-radio-group>
        </div>

        <div class="field">
          <label>缺陷描述</label>
          <el-input
            v-model="selected.remark"
            type="textarea"
            :rows="3"
            placeholder="描述缺陷位置、程度，便于报告逐条展示"
            @input="scheduleSave"
          />
        </div>

        <div class="field">
          <label>当前检测项附件</label>
          <el-upload
            :show-file-list="false"
            :http-request="handleUpload"
            multiple
            accept="image/*,video/*"
          >
            <el-button type="primary" plain>拍照 / 选择文件上传</el-button>
          </el-upload>
          <div class="upload-tip">上传后自动压缩、生成缩略图并计算原图指纹，右侧可查看处理进度</div>
        </div>
      </section>

      <aside class="attachments">
        <div class="panel-title">
          附件（{{ attachments.length }}）
          <span class="poll-hint" v-if="pendingAttachments">处理中…</span>
        </div>
        <div v-if="!attachments.length" class="empty">暂无附件</div>
        <div
          v-for="attachment in attachments"
          :key="attachment.id"
          class="attachment"
          @click="openAttachment(attachment)"
        >
          <div class="thumb">
            <img v-if="attachment.thumbUrl" :src="attachment.thumbUrl" alt="thumb" />
            <div v-else class="thumb-fallback">…</div>
          </div>
          <div class="meta">
            <div class="name">{{ attachment.fileName }}</div>
            <div class="sub">{{ itemNameOf(attachment.itemId) }}</div>
            <div class="sub hash" v-if="attachment.originHash">
              指纹 {{ attachment.originHash.slice(0, 8) }}
            </div>
            <el-tag size="small" :type="statusMeta(attachment.status).type">
              {{ statusMeta(attachment.status).text }}
            </el-tag>
          </div>
        </div>
      </aside>
    </div>

    <div class="footer">
      <div class="progress-text">已完成 {{ filledCount }} / {{ totalCount }} 项</div>
      <el-progress :percentage="percent" :stroke-width="10" style="flex: 1" />
    </div>
  </div>
</template>

<style scoped>
.fill-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 84px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(15, 35, 95, 0.06);
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 20px;
  border-bottom: 1px solid #eef1f6;
}

.left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-no {
  font-weight: 600;
}

.vehicle {
  color: #6b7688;
  font-size: 13px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.save-hint {
  font-size: 12px;
  color: #98a2b3;
  min-width: 90px;
  text-align: right;
}

.body {
  flex: 1;
  display: grid;
  grid-template-columns: 260px 1fr 300px;
  min-height: 0;
}

.tree {
  border-right: 1px solid #eef1f6;
  overflow-y: auto;
  padding: 12px 0;
}

.cat {
  display: flex;
  justify-content: space-between;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #2f6bff;
}

.cat-count {
  color: #98a2b3;
  font-weight: 400;
}

.item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  font-size: 13px;
  cursor: pointer;
  color: #33405a;
}

.item:hover {
  background: #f5f8ff;
}

.item.active {
  background: #eef3ff;
  color: #2f6bff;
  font-weight: 600;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d9dfea;
  flex-shrink: 0;
}

.item.done .dot {
  background: #22c3a6;
}

.name {
  flex: 1;
}

.score {
  color: #98a2b3;
  font-size: 12px;
}

.editor {
  padding: 24px 28px;
  overflow-y: auto;
}

.editor h3 {
  margin: 0 0 8px;
  font-size: 18px;
}

.editor h3 small {
  color: #98a2b3;
  font-weight: 400;
  margin-left: 8px;
  font-size: 12px;
}

.rule {
  font-size: 12px;
  color: #98a2b3;
  background: #f7f9fc;
  border-radius: 6px;
  padding: 8px 12px;
  margin-bottom: 24px;
}

.field {
  margin-bottom: 26px;
}

.field label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 10px;
}

.score-row {
  display: flex;
  align-items: center;
  gap: 20px;
}

.score-row :deep(.el-slider) {
  flex: 1;
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #98a2b3;
}

.attachments {
  border-left: 1px solid #eef1f6;
  overflow-y: auto;
  padding: 14px;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 12px;
  display: flex;
  justify-content: space-between;
}

.poll-hint {
  color: #e6a23c;
  font-weight: 400;
  font-size: 12px;
}

.empty {
  color: #98a2b3;
  font-size: 13px;
  text-align: center;
  margin-top: 40px;
}

.attachment {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #eef1f6;
  margin-bottom: 10px;
  cursor: pointer;
}

.attachment:hover {
  border-color: #c9d8ff;
}

.thumb {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  overflow: hidden;
  background: #f2f5fa;
  flex-shrink: 0;
}

.thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.thumb-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #b6c0d0;
}

.meta {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.meta .name {
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta .sub {
  font-size: 11px;
  color: #98a2b3;
}

.meta .hash {
  font-family: Consolas, monospace;
}

.footer {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 20px;
  border-top: 1px solid #eef1f6;
}

.progress-text {
  font-size: 13px;
  color: #6b7688;
  white-space: nowrap;
}

@media (max-width: 1100px) {
  .body {
    grid-template-columns: 1fr;
    overflow-y: auto;
  }

  .tree,
  .attachments {
    border: none;
    border-top: 1px solid #eef1f6;
  }

  .vehicle {
    display: none;
  }
}
</style>
