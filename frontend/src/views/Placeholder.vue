<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div class="page-card placeholder">
    <el-empty :description="`${route.meta.title} 尚未交付`">
      <div class="hint">
        <p>该模块计划在 <strong>{{ route.meta.milestone || '后续里程碑' }}</strong> 交付。</p>
        <p>M0 只完成工程骨架、数据库表结构与存证核心算法，业务页面会在后续里程碑逐个补齐。</p>
      </div>
    </el-empty>
  </div>
</template>

<style scoped>
.placeholder {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.hint {
  text-align: center;
  color: #8a94a6;
  font-size: 13px;
  line-height: 1.9;
}

.hint p {
  margin: 4px 0;
}
</style>
