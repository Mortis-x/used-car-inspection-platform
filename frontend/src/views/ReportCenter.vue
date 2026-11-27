<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMarketReports } from '@/api/report'

const router = useRouter()
const loading = ref(false)
const rows = ref([])
const keyword = ref('')
const grade = ref('')

const gradeType = { S: 'success', A: 'primary', B: 'warning', C: 'danger', D: 'info' }

async function load() {
  loading.value = true
  try {
    rows.value = await getMarketReports({ keyword: keyword.value, grade: grade.value })
  } finally {
    loading.value = false
  }
}

function reset() {
  keyword.value = ''
  grade.value = ''
  load()
}

function wan(cents) {
  if (cents == null) return '-'
  return (cents / 100 / 10000).toFixed(1) + ' 万'
}

function openReport(row) {
  router.push(`/reports/${row.reportNo}`)
}

function openVerify(row) {
  window.open(`/verify?reportNo=${row.reportNo}`, '_blank')
}

onMounted(load)
</script>

<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="filters">
        <el-input
          v-model="keyword"
          placeholder="品牌 / 车型 / 车牌"
          clearable
          style="width: 220px"
          @keyup.enter="load"
        />
        <el-select v-model="grade" placeholder="全部等级" clearable style="width: 130px" @change="load">
          <el-option v-for="item in ['S', 'A', 'B', 'C', 'D']" :key="item" :label="item + ' 级'" :value="item" />
        </el-select>
        <el-button type="primary" @click="load">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
      <div class="hint">共 {{ rows.length }} 份已生效报告</div>
    </div>

    <el-table v-loading="loading" :data="rows" stripe empty-text="暂无已生效报告">
      <el-table-column label="车辆" min-width="190">
        <template #default="{ row }">
          <div class="vehicle">{{ row.brand }} {{ row.model }}</div>
          <div class="sub-text">{{ row.plateNo || '未上牌' }} · {{ row.mileage }} km</div>
        </template>
      </el-table-column>
      <el-table-column label="车况等级" width="110">
        <template #default="{ row }">
          <el-tag :type="gradeType[row.grade] || 'info'" effect="dark" size="small">{{ row.grade }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalScore" label="综合得分" width="100" />
      <el-table-column label="估价区间" width="170">
        <template #default="{ row }">{{ wan(row.priceLow) }} ~ {{ wan(row.priceHigh) }}</template>
      </el-table-column>
      <el-table-column prop="reportNo" label="报告编号" width="180" />
      <el-table-column prop="version" label="版本" width="70">
        <template #default="{ row }">v{{ row.version }}</template>
      </el-table-column>
      <el-table-column prop="issuedAt" label="出具时间" width="170" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openReport(row)">查看报告</el-button>
          <el-button link @click="openVerify(row)">公开验真</el-button>
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
  flex-wrap: wrap;
  gap: 10px;
}

.filters {
  display: flex;
  gap: 10px;
  align-items: center;
}

.hint {
  font-size: 12px;
  color: #98a2b3;
}

.vehicle {
  font-weight: 600;
}

.sub-text {
  color: #98a2b3;
  font-size: 12px;
  margin-top: 2px;
}
</style>
