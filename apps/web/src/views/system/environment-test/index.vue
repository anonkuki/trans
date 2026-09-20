<template>
  <div class="p-4">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>环境检测测试</span>
        </div>
      </template>
      
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户代理">
          {{ userAgent }}
        </el-descriptions-item>
        <el-descriptions-item label="是否飞书客户端">
          <el-tag :type="isFeishu ? 'success' : 'info'">
            {{ isFeishu ? '是' : '否' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="当前布局">
          <el-tag type="primary">
            {{ currentLayout }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      
      <div class="mt-4">
        <el-button @click="checkEnvironment" type="primary">重新检测环境</el-button>
        <el-button @click="switchToClassic" v-if="isFeishu">切换到经典布局</el-button>
        <el-button @click="switchToTop" v-if="!isFeishu">切换到顶部布局</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/store/modules/app'
import { isFeishuClient } from '@/utils/environment'

const appStore = useAppStore()
const userAgent = ref('')
const isFeishu = ref(false)
const currentLayout = ref('')

const checkEnvironment = () => {
  userAgent.value = navigator.userAgent
  isFeishu.value = isFeishuClient()
  currentLayout.value = appStore.getLayout
}

const switchToClassic = () => {
  appStore.setLayout('classic')
  currentLayout.value = 'classic'
}

const switchToTop = () => {
  appStore.setLayout('top')
  currentLayout.value = 'top'
}

onMounted(() => {
  checkEnvironment()
})
</script>