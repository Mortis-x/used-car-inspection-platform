<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: 'inspector', password: '123456' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demoAccounts = [
  { label: '检测师', username: 'inspector' },
  { label: '车商', username: 'dealer' },
  { label: '买家', username: 'buyer' },
  { label: '平台运营员', username: 'operator' },
  { label: '纠纷仲裁员', username: 'arbitrator' }
]

function fill(account) {
  form.username = account.username
  form.password = '123456'
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    ElMessage.success('登录成功')
    router.push(route.query.redirect || '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="brand-panel">
      <div class="brand-top">
        <div class="logo">V</div>
        <span>车况可信存证平台</span>
      </div>
      <h1>让每一份检测报告<br />都可以被验证</h1>
      <p class="desc">
        报告内容固化为结构化数据，出具时计算 SHA-256 指纹并写入哈希链。
        任何字段被改动，验真页都会立刻发现并指出具体差异。
      </p>
      <ul class="points">
        <li><span>01</span> 内容指纹：任一字段变化，指纹完全不同</li>
        <li><span>02</span> 哈希链：每条存证首尾相连，无法单独篡改</li>
        <li><span>03</span> 公开验真：扫码三秒出结论，核对行为留痕</li>
      </ul>
      <div class="footnote">不阻止修改，而让修改无法隐藏</div>
    </div>

    <div class="form-panel">
      <div class="form-box">
        <h2>账号登录</h2>
        <p class="sub">请使用平台分配的账号，或直接选择下方演示角色</p>
        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" show-password placeholder="密码">
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-button type="primary" class="submit" :loading="loading" @click="submit">
            登 录
          </el-button>
        </el-form>

        <div class="demo">
          <div class="demo-title">演示账号（密码均为 123456）</div>
          <div class="demo-list">
            <el-tag
              v-for="account in demoAccounts"
              :key="account.username"
              class="demo-tag"
              effect="plain"
              @click="fill(account)"
            >
              {{ account.label }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
}

.brand-panel {
  flex: 1.1;
  background: radial-gradient(circle at 20% 20%, #1f3f7a 0%, #0d1526 60%);
  color: #fff;
  padding: 48px 56px;
  display: flex;
  flex-direction: column;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.85);
}

.logo {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #2f6bff, #22c3a6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}

h1 {
  margin: 72px 0 20px;
  font-size: 40px;
  line-height: 1.35;
  font-weight: 600;
}

.desc {
  max-width: 480px;
  color: rgba(255, 255, 255, 0.62);
  line-height: 1.9;
  font-size: 14px;
}

.points {
  list-style: none;
  padding: 0;
  margin: 36px 0 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.78);
}

.points span {
  display: inline-block;
  margin-right: 10px;
  color: #4f8bff;
  font-weight: 600;
}

.footnote {
  margin-top: auto;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.35);
  letter-spacing: 1px;
}

.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

.form-box {
  width: 360px;
}

h2 {
  margin: 0;
  font-size: 24px;
}

.sub {
  color: #8a94a6;
  font-size: 13px;
  margin: 8px 0 28px;
}

.submit {
  width: 100%;
  margin-top: 4px;
}

.demo {
  margin-top: 32px;
  border-top: 1px dashed #e4e9f2;
  padding-top: 18px;
}

.demo-title {
  font-size: 12px;
  color: #8a94a6;
  margin-bottom: 10px;
}

.demo-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.demo-tag {
  cursor: pointer;
}

@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
}
</style>
