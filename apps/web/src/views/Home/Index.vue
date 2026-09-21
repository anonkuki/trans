<template>
  <div class="home-page">
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

    <!-- 外链卡片：展示当前用户有权限且开启状态的外链，点击后跳转外部网站 -->
    <el-card v-if="linkGroups.length" shadow="never" class="link-card-wrap mt-16px">
      <div v-for="group in linkGroups" :key="group.category">
        <div v-if="group.category" class="mb-10px text-14px font-bold">{{ group.category }}</div>
        <div class="mb-16px flex flex-wrap">
          <div
            v-for="link in group.links"
            :key="link.id"
            class="link-card mr-16px mb-16px"
            @click="handleClickLink(link)"
          >
            <div class="flex items-center">
              <el-image v-if="link.iconUrl" :src="link.iconUrl" class="link-icon mr-12px" />
              <Icon v-else :icon="link.icon || 'ep:link'" class="mr-12px text-42px" />
              <div>
                <div class="text-16px font-bold">{{ link.name }}</div>
                <div v-if="link.description" class="mt-4px text-12px text-gray-500">
                  {{ link.description }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>
<script lang="ts" setup>
import { useUserStore } from '@/store/modules/user'
import * as ExternalLinkApi from '@/api/system/extlink'


defineOptions({ name: 'Index' })

const { t } = useI18n()
const userStore = useUserStore()
const loading = ref(false)
const avatar = userStore.getUser.avatar
const username = userStore.getUser.nickname

/** 首页外链列表，按分类分组展示 */
const linkList = ref<ExternalLinkApi.ExternalLinkVO[]>([])
const linkGroups = computed(() => {
  const groups: { category: string; links: ExternalLinkApi.ExternalLinkVO[] }[] = []
  linkList.value.forEach((link) => {
    const category = link.category || ''
    const group = groups.find((item) => item.category === category)
    if (group) {
      group.links.push(link)
    } else {
      groups.push({ category, links: [link] })
    }
  })
  return groups
})

/** 获取首页外链列表 */
const getLinkList = async () => {
  linkList.value = await ExternalLinkApi.getHomeExternalLinkList()
}

/** 点击外链卡片，记录点击次数后跳转外部网站 */
const handleClickLink = (link: ExternalLinkApi.ExternalLinkVO) => {
  // 记录点击，异步请求不阻塞跳转。注意：此处不 await，避免跳转等待接口返回。
  if (link.id) {
    ExternalLinkApi.clickExternalLink(link.id).catch(() => {})
  }
  // 按打开方式跳转。注意：必须使用 openTarget 判断，0 为当前窗口、1 为新窗口，避免跳转行为与配置不一致。
  if (link.openTarget === 0) {
    window.location.href = link.url
  } else {
    window.open(link.url, '_blank')
  }
}

/** 初始化 */
onMounted(() => {
  getLinkList()
})
</script>
<style lang="scss" scoped>
/* 根容器：撑满页面内容区可视高度，供外链卡片高度自适应到屏幕底部 */
.home-page {
  display: flex;
  flex-direction: column;
  min-height: calc(
    100vh - var(--top-tool-height) - var(--tags-view-height) - 2 * var(--app-content-padding)
  );
}

/* 外链卡片容器：占据剩余全部高度 */
.link-card-wrap {
  display: flex;
  flex: 1;
  flex-direction: column;

  :deep(.el-card__body) {
    flex: 1;
  }
}

.link-card {
  width: 200px;
  padding: 12px;
  cursor: pointer;
  background-color: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  transition: all 0.3s;

  &:hover {
    background-color: var(--el-color-primary-light-9);
    border-color: var(--el-color-primary);
    box-shadow: var(--el-box-shadow-light);
  }
}

.link-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
}
</style>
