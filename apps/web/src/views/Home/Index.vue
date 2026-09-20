<template>
  <div>
    <el-card shadow="never">
      <el-skeleton :loading="loading" animated>
        <div class="flex items-center">
          <el-avatar :src="avatar" :size="70" class="mr-16px">
            <img src="@/assets/imgs/logo.svg" alt="" />
          </el-avatar>
          <div>
            <div class="text-20px">
              {{ t('workplace.welcome') }} {{ username }} {{ t('workplace.happyDay') }}
            </div>
          </div>
        </div>
      </el-skeleton>
    </el-card>
  </div>
</template>
<script lang="ts" setup>
import { useUserStore } from '@/store/modules/user'


defineOptions({ name: 'Index' })

const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)
const avatar = userStore.getUser.avatar
const username = userStore.getUser.nickname
</script>
