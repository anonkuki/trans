<template>
  <div>
    <div class="text-center">
      <UserAvatar :img="userInfo?.avatar" />
    </div>
    <ul class="list-group list-group-striped">
      <li class="list-group-item">
        <span class="item-label">
          <Icon class="mr-5px" icon="ep:user" />
          {{ t('profile.user.username') }}
        </span>
        <div class="item-value">{{ userInfo?.username }}</div>
      </li>
      <li class="list-group-item">
        <span class="item-label">
          <Icon class="mr-5px" icon="ep:phone" />
          {{ t('profile.user.mobile') }}
        </span>
        <div class="item-value">{{ userInfo?.mobile }}</div>
      </li>
      <li class="list-group-item">
        <span class="item-label">
          <Icon class="mr-5px" icon="fontisto:email" />
          {{ t('profile.user.email') }}
        </span>
        <div class="item-value">{{ userInfo?.email }}</div>
      </li>
      <li class="list-group-item">
        <span class="item-label">
          <Icon class="mr-5px" icon="carbon:tree-view-alt" />
          {{ t('profile.user.dept') }}
        </span>
        <div v-if="userInfo?.dept" class="item-value">{{ userInfo?.dept.name }}</div>
      </li>
      <li class="list-group-item">
        <span class="item-label">
          <Icon class="mr-5px" icon="ep:suitcase" />
          {{ t('profile.user.posts') }}
        </span>
        <div v-if="userInfo?.posts" class="item-value">
          {{ userInfo?.posts.map((post) => post.name).join(',') }}
        </div>
      </li>
      <li class="list-group-item">
        <span class="item-label">
          <Icon class="mr-5px" icon="icon-park-outline:peoples" />
          {{ t('profile.user.roles') }}
        </span>
        <div v-if="userInfo?.roles" class="item-value">
          {{ userInfo?.roles.map((role) => role.name).join(',') }}
        </div>
      </li>
      <li class="list-group-item">
        <span class="item-label">
          <Icon class="mr-5px" icon="ep:calendar" />
          {{ t('profile.user.createTime') }}
        </span>
        <div class="item-value">{{ formatDate(userInfo.createTime) }}</div>
      </li>
    </ul>
  </div>
</template>
<script lang="ts" setup>
import { formatDate } from '@/utils/formatTime'
import UserAvatar from './UserAvatar.vue'
import { useUserStore } from '@/store/modules/user'

import { getUserProfile, ProfileVO } from '@/api/system/user/profile'

defineOptions({ name: 'ProfileUser' })

const { t } = useI18n()
const userStore = useUserStore()
const userInfo = ref({} as ProfileVO)

const getUserInfo = async () => {
  const users = await getUserProfile()
  userInfo.value = users
}

// 监听 userStore 中头像的变化，同步更新本地 userInfo
watch(
  () => userStore.getUser.avatar,
  (newAvatar) => {
    if (newAvatar && userInfo.value) {
      userInfo.value.avatar = newAvatar
    }
  }
)

// 暴露刷新方法
defineExpose({
  refresh: getUserInfo
})

onMounted(async () => {
  await getUserInfo()
})
</script>

<style scoped>
.text-center {
  position: relative;
  height: 120px;
  text-align: center;
}

.list-group-striped > .list-group-item {
  padding-right: 0;
  padding-left: 0;
  border-right: 0;
  border-left: 0;
  border-radius: 0;
}

.list-group {
  padding-left: 0;
  list-style: none;
}

.list-group-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 0;
  margin-bottom: -1px;
  font-size: 13px;
  border-top: 1px solid #e7eaec;
  border-bottom: 1px solid #e7eaec;
}

/* 左侧标题：不换行、不被压缩 */
.item-label {
  flex-shrink: 0;
  white-space: nowrap;
}

/* 右侧值：过长自动换行，行高自适应撑开 li，分割线间距随之适配 */
.item-value {
  flex: 1;
  min-width: 0;
  text-align: right;
  line-height: 20px;
  word-break: break-all;
}
</style>
