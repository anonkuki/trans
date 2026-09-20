<template>
  <div
    :class="prefixCls"
    class="relative h-[100%] lt-md:px-10px lt-sm:px-10px lt-xl:px-10px"
    :style="{ backgroundImage: `url(${backgroundImageUrl})` }"
  >
    <div
      class="relative mx-auto h-full flex items-center justify-end pr-10% lt-md:justify-center lt-md:pr-0"
    >
      <!-- 右侧白色表单卡片 -->
      <div
        class="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl p-30px w-full max-w-500px mx-20px lt-sm:p-20px"
      >
        <!-- 右上角的主题、语言选择 -->
        <div class="flex items-center justify-end space-x-10px mb-6">
          <ThemeSwitch />
          <LocaleDropdown />
        </div>

        <!-- 表单容器 -->
        <!-- <Transition appear enter-active-class="animate__animated animate__bounceInRight"> -->
          <div class="w-full">
            <!-- 账号登录 -->
            <LoginForm class="w-full" />
            <!-- 手机登录 -->
            <MobileForm class="w-full" />
            <!-- 二维码登录 -->
            <QrCodeForm class="w-full" />
            <!-- 注册 -->
            <RegisterForm class="w-full" />
            <!-- 三方登录 -->
            <SSOLoginVue class="w-full" />
            <!-- 忘记密码 -->
            <ForgetPasswordForm class="w-full" />
          </div>
        <!-- </Transition> -->
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { underlineToHump } from '@/utils'
import { useDesign } from '@/hooks/web/useDesign'
import { useAppStore } from '@/store/modules/app'
import { ThemeSwitch } from '@/layout/components/ThemeSwitch'
import { LocaleDropdown } from '@/layout/components/LocaleDropdown'
import {
  LoginForm,
  MobileForm,
  QrCodeForm,
  RegisterForm,
  SSOLoginVue,
  ForgetPasswordForm
} from './components'

// 导入背景图片
import backgroundImage from '@/assets/imgs/background.png'

defineOptions({ name: 'Login' })

const { t } = useI18n()
const appStore = useAppStore()
const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('login')

// 背景图片 URL
const backgroundImageUrl = backgroundImage
</script>

<style lang="scss" scoped>
$prefix-cls: #{$namespace}-login;

.#{$prefix-cls} {
  overflow: auto;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

.dark {
  .bg-white {
    background-color: #1f2937 !important;
  }
}
</style>

<style lang="scss">
.dark .login-form {
  .el-divider__text {
    background-color: var(--login-bg-color);
  }

  .el-card {
    background-color: var(--login-bg-color);
  }
}
</style>
