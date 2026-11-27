<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { verifyPublicReport } from '@/api/publicVerify'

const route = useRoute()
const reportNo = ref(route.query.reportNo || '')
const loading = ref(false)
const result = ref(null)
const searched = ref(false)

const levelText = ['无', '轻微', '一般', '严重']

async function doVerify() {
  if (!reportNo.value.trim()) {
    return
  }
  loading.value = true
  searched.value = true
  try {
    result.value = await verifyPublicReport(reportNo.value.trim())
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (reportNo.value) {
    doVerify()
  }
})
</script>

<template>
  <div class="verify-page">
    <header class="brand">
      <div class="logo">车况可信存证</div>
      <div class="slogan">二手车检测报告 · 公开验真</div>
    </header>

    <main class="card">
      <h1>报告真伪查询</h1>
      <p class="hint">输入报告编号，或扫描报告二维码，比对链上存证指纹</p>
      <div class="search">
        <el-input
          v-model="reportNo"
          size="large"
          placeholder="例如 RP20261126350665"
          clearable
          @keyup.enter="doVerify"
        />
        <el-button type="primary" size="large" :loading="loading" @click="doVerify">立即验真</el-button>
      </div>

      <template v-if="searched && result">
        <div :class="['result', result.valid ? 'pass' : 'fail']">
          <div class="result-head">
            <span class="badge">{{ result.valid ? '验真通过' : '验真未通过' }}</span>
            <span class="report-no">{{ result.reportNo }}</span>
          </div>
          <p class="message">{{ result.message }}</p>

          <div class="summary" v-if="result.totalScore != null">
            <div class="item">
              <span class="label">车况等级</span>
              <span class="value grade">{{ result.grade }}</span>
            </div>
            <div class="item">
              <span class="label">综合得分</span>
              <span class="value">{{ result.totalScore }} 分</span>
            </div>
            <div class="item">
              <span class="label">报告版本</span>
              <span class="value">v{{ result.version }}</span>
            </div>
            <div class="item">
              <span class="label">出具时间</span>
              <span class="value">{{ result.issuedAt }}</span>
            </div>
            <div class="item" v-if="result.brand">
              <span class="label">车辆</span>
              <span class="value">{{ result.brand }} {{ result.model }}</span>
            </div>
            <div class="item" v-if="result.mileage != null">
              <span class="label">表显里程</span>
              <span class="value">{{ result.mileage }} km</span>
            </div>
            <div class="item" v-if="result.plateMasked">
              <span class="label">车牌</span>
              <span class="value">{{ result.plateMasked }}</span>
            </div>
            <div class="item" v-if="result.chainSeq">
              <span class="label">链上序号</span>
              <span class="value">#{{ result.chainSeq }}</span>
            </div>
          </div>

          <div class="hash" v-if="result.recordHash">
            <span class="label">存证指纹</span>
            <code>{{ result.recordHash }}</code>
          </div>

          <div class="diff" v-if="result.diffs && result.diffs.length">
            <div class="diff-title">不一致字段（存储值 → 当前值）</div>
            <el-table :data="result.diffs" size="small">
              <el-table-column prop="field" label="字段" width="110" />
              <el-table-column prop="stored" label="存证时的值" />
              <el-table-column prop="current" label="当前库中的值" />
            </el-table>
          </div>

          <div class="defects" v-if="result.defects && result.defects.length">
            <div class="diff-title">缺陷摘要</div>
            <el-table :data="result.defects" size="small">
              <el-table-column prop="itemCode" label="检测项" width="140" />
              <el-table-column label="缺陷等级" width="110">
                <template #default="{ row }">{{ levelText[row.defectLevel] }}</template>
              </el-table-column>
              <el-table-column prop="deduct" label="扣分" width="90" />
            </el-table>
          </div>
        </div>
      </template>

      <div class="chain-tip">
        本平台采用中心化哈希链存证：每份报告生成 SHA-256 指纹并与前序记录串联，
        任何字段被改动都会导致指纹不一致并精准定位到字段。
      </div>
    </main>
  </div>
</template>

<style scoped>
.verify-page {
  min-height: 100vh;
  background: radial-gradient(circle at 20% 20%, #1c3d6e 0%, #0d1a2f 55%, #070e1a 100%);
  color: #fff;
  padding: 40px 20px 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.brand {
  text-align: center;
  margin-bottom: 28px;
}

.logo {
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 2px;
}

.slogan {
  margin-top: 6px;
  font-size: 13px;
  color: #8fa4c4;
  letter-spacing: 1px;
}

.card {
  width: 100%;
  max-width: 860px;
  background: rgba(255, 255, 255, 0.98);
  color: #1c2740;
  border-radius: 16px;
  padding: 36px 40px;
  box-shadow: 0 24px 60px rgba(0, 0, 0, 0.35);
}

.card h1 {
  margin: 0 0 8px;
  font-size: 22px;
}

.hint {
  margin: 0 0 22px;
  color: #6b7688;
  font-size: 13px;
}

.search {
  display: flex;
  gap: 12px;
}

.result {
  margin-top: 26px;
  border-radius: 12px;
  padding: 20px 22px;
  border: 1px solid #e6ebf3;
  background: #fbfcfe;
}

.result.pass {
  border-color: #b7e7d8;
  background: linear-gradient(180deg, #f2fcf8, #ffffff);
}

.result.fail {
  border-color: #f3c3c3;
  background: linear-gradient(180deg, #fff5f5, #ffffff);
}

.result-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.badge {
  font-weight: 700;
  font-size: 15px;
}

.pass .badge {
  color: #0f9d76;
}

.fail .badge {
  color: #d64545;
}

.report-no {
  font-family: Consolas, monospace;
  color: #6b7688;
  font-size: 13px;
}

.message {
  margin: 10px 0 16px;
  font-size: 13px;
  color: #47536b;
}

.summary {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px 20px;
  margin-bottom: 16px;
}

.summary .item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary .label {
  font-size: 12px;
  color: #98a2b3;
}

.summary .value {
  font-size: 15px;
  font-weight: 600;
}

.grade {
  color: #2f6bff;
  font-size: 22px !important;
}

.hash {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  font-size: 12px;
  color: #6b7688;
  margin-bottom: 16px;
}

.hash code {
  font-family: Consolas, monospace;
  word-break: break-all;
  color: #33405a;
}

.diff,
.defects {
  margin-top: 8px;
}

.diff-title {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.chain-tip {
  margin-top: 26px;
  font-size: 12px;
  color: #98a2b3;
  line-height: 1.8;
  border-top: 1px dashed #e6ebf3;
  padding-top: 16px;
}

@media (max-width: 640px) {
  .card {
    padding: 24px 18px;
  }

  .search {
    flex-direction: column;
  }
}
</style>
