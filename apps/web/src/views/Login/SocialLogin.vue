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
        <!-- 右上角的主题、语言选择（暂时隐藏） -->
        <!-- <div class="flex items-center justify-end space-x-10px mb-6">
          <ThemeSwitch />
          <LocaleDropdown class="dark:text-white" />
        </div> -->

        <!-- 表单容器 -->
        <div class="w-full">
          <!-- 账号登录 -->
          <el-form
            v-show="getShow"
            ref="formLogin"
            :model="loginData.loginForm"
            :rules="LoginRules"
            class="login-form w-full"
            label-position="top"
            label-width="120px"
            size="large"
          >
            <el-row style="margin-right: -10px; margin-left: -10px">
              <el-col :span="24" style="padding-right: 10px; padding-left: 10px">
                <el-form-item>
                  <LoginFormTitle style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col
                v-if="loginData.tenantEnable === 'true'"
                :span="24"
                style="padding-right: 10px; padding-left: 10px"
              >
                <el-form-item prop="tenantName">
                  <el-input
                    v-model="loginData.loginForm.tenantName"
                    :placeholder="t('login.tenantNamePlaceholder')"
                    :prefix-icon="iconHouse"
                    link
                    type="primary"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24" style="padding-right: 10px; padding-left: 10px">
                <el-form-item prop="username">
                  <el-input
                    v-model="loginData.loginForm.username"
                    :placeholder="t('login.usernamePlaceholder')"
                    :prefix-icon="iconAvatar"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="24" style="padding-right: 10px; padding-left: 10px">
                <el-form-item prop="password">
                  <el-input
                    v-model="loginData.loginForm.password"
                    :placeholder="t('login.passwordPlaceholder')"
                    :prefix-icon="iconLock"
                    show-password
                    type="password"
                    @keyup.enter="getCode()"
                  />
                </el-form-item>
              </el-col>
              <el-col
                :span="24"
                style="
                  padding-right: 10px;
                  padding-left: 10px;
                  margin-top: -20px;
                  margin-bottom: -20px;
                "
              >
                <el-form-item>
                  <el-row justify="space-between" style="width: 100%">
                    <el-col :span="6">
                      <el-checkbox v-model="loginData.loginForm.rememberMe">
                        {{ t('login.remember') }}
                      </el-checkbox>
                    </el-col>
                    <el-col :offset="6" :span="12">
                      <el-link style="float: right" type="primary">
                        {{ t('login.forgetPassword') }}
                      </el-link>
                    </el-col>
                  </el-row>
                </el-form-item>
              </el-col>
              <el-col :span="24" style="padding-right: 10px; padding-left: 10px">
                <el-form-item>
                  <XButton
                    :loading="loginLoading"
                    :title="t('login.login')"
                    class="w-[100%]"
                    type="primary"
                    @click="getCode()"
                  />
                </el-form-item>
              </el-col>
              <Verify
                v-if="loginData.captchaEnable === 'true'"
                ref="verify"
                :captchaType="captchaType"
                :imgSize="{ width: '400px', height: '200px' }"
                mode="pop"
                @success="handleLogin"
              />
            </el-row>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { underlineToHump } from '@/utils'

import { ElLoading } from 'element-plus'

import { useDesign } from '@/hooks/web/useDesign'
import { useAppStore } from '@/store/modules/app'
import { useIcon } from '@/hooks/web/useIcon'
import { usePermissionStore } from '@/store/modules/permission'

import * as LoginApi from '@/api/login'
import * as authUtil from '@/utils/auth'
import { ThemeSwitch } from '@/layout/components/ThemeSwitch'
import { LocaleDropdown } from '@/layout/components/LocaleDropdown'
import { LoginStateEnum, useFormValid, useLoginState } from './components/useLogin'
import LoginFormTitle from './components/LoginFormTitle.vue'
import router from '@/router'
import { useUserStoreWithOut } from '@/store/modules/user'

// 导入背景图片
import backgroundImage from '@/assets/imgs/background.png'

defineOptions({ name: 'SocialLogin' })

const { t } = useI18n()
const message = useMessage() // 引入消息提示
const route = useRoute()

const appStore = useAppStore()
const { getPrefixCls } = useDesign()
const prefixCls = getPrefixCls('login')
const iconHouse = useIcon({ icon: 'ep:house' })
const iconAvatar = useIcon({ icon: 'ep:avatar' })
const iconLock = useIcon({ icon: 'ep:lock' })
const formLogin = ref<any>()
const { validForm } = useFormValid(formLogin)
const { getLoginState } = useLoginState()
const { push } = useRouter()
const permissionStore = usePermissionStore()
const loginLoading = ref(false)
const verify = ref()
const captchaType = ref('blockPuzzle') // blockPuzzle 滑块 clickWord 点击文字 pictureWord 文字验证码

// 背景图片 URL
const backgroundImageUrl = backgroundImage

const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN)

// 表单校验规则（与正常页面保持一致）
const required = { required: true, message: '该项为必填项', trigger: 'blur' }
const LoginRules = {
  tenantName: [required],
  username: [required],
  password: [required]
}

// 登录表单数据（与正常页面保持一致，从环境变量读取默认值）
const loginData = reactive({
  isShowPassword: false,
  captchaEnable: import.meta.env.VITE_APP_CAPTCHA_ENABLE, // 字符串 'true'/'false'
  tenantEnable: import.meta.env.VITE_APP_TENANT_ENABLE,   // 字符串 'true'/'false'
  loginForm: {
    tenantName: import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '',
    username: import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
    password: import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
    captchaVerification: '',
    rememberMe: true // 与正常页面统一默认记住
  }
})

// 获取URL参数工具
function getUrlValue(key: string): string {
  const url = new URL(decodeURIComponent(location.href))
  return url.searchParams.get(key) ?? ''
}

// 重定向地址（与正常页面保持一致）
const redirect = ref<string>(getUrlValue('redirect') || '')

// 获取租户ID
const getTenantId = async () => {
  if (loginData.tenantEnable === 'true') {
    const res = await LoginApi.getTenantIdByName(loginData.loginForm.tenantName)
    authUtil.setTenantId(res)
  }
}

// 读取本地缓存的登录表单数据（与正常页面逻辑一致）
const getLoginFormCache = () => {
  const loginForm = authUtil.getLoginForm()
  if (loginForm) {
    loginData.loginForm = {
      ...loginData.loginForm,
      ...loginForm
    }
  }
}

// 根据当前域名获取租户信息（与正常页面保持一致）
const getTenantByWebsite = async () => {
  if (loginData.tenantEnable === 'true') {
    const website = location.host
    const res = await LoginApi.getTenantByWebsite(website)
    if (res) {
      loginData.loginForm.tenantName = res.name
      authUtil.setTenantId(res.id)
    }
  }
}

const loading = ref() // ElLoading.service 实例

// 获取验证码
const getCode = async () => {
  // 未开启验证码则直接登录，否则弹出验证码组件
  if (loginData.captchaEnable !== 'true') {
    await handleLogin({})
  } else {
    verify.value.show()
  }
}

// 普通账号密码登录（核心修复逻辑）
const handleLogin = async (params: any) => {
  loginLoading.value = true
  try {
    // 获取租户ID
    await getTenantId()

    // 表单校验
    const data = await validForm()
    if (!data) return

    // 构造登录参数（只包含账号密码、租户名、验证码，移除社交参数）
    const loginParams = {
      username: loginData.loginForm.username,
      password: loginData.loginForm.password,
      tenantName: loginData.loginForm.tenantName,
      captchaVerification: params.captchaVerification || ''
    }

    // 调用登录接口
    const res = await LoginApi.login(loginParams)

    // 显示加载中
    loading.value = ElLoading.service({
      lock: true,
      text: '正在加载系统中...',
      background: 'rgba(0, 0, 0, 0.7)'
    })

    // 记住我：存储或移除登录表单信息
    if (loginData.loginForm.rememberMe) {
      authUtil.setLoginForm(loginData.loginForm)
    } else {
      authUtil.removeLoginForm()
    }

    // 存储 token
    authUtil.setToken(res)

    // 跳转逻辑：优先 redirect，否则使用动态路由首页
    const targetPath = redirect.value || permissionStore.addRouters[0]?.path || '/'
    await push({ path: targetPath })
  } catch (error: any) {
    // 错误提示（与正常页面一致）
    message.error(error.message || '登录失败，请重试')
    console.error('登录失败', error)
  } finally {
    loginLoading.value = false
    loading.value?.close()
  }
}

// 社交登录尝试（原有逻辑，保持不变）
const tryLogin = async () => {
  try {
    const type = getUrlValue('type')
    const redirectUrl = getUrlValue('redirect')
    const code = route?.query?.code as string
    const state = route?.query?.state as string

    if (!type || !code || !state) return

    const res = await LoginApi.socialLogin(type, code, state)
    authUtil.setToken(res)

    // 设置用户信息标记为未设置，让路由守卫来处理
    const userStore = useUserStoreWithOut()
    userStore.resetState()

    router.push({ path: redirectUrl || '/' })
  } catch (err) {
    console.warn('社交登录失败，可继续使用普通登录', err)
  }
}

// 页面初始化
onMounted(async () => {
  // 加载缓存表单
  getLoginFormCache()
  // 根据域名获取租户（覆盖默认租户）
  await getTenantByWebsite()
  // 尝试社交登录（若存在回调参数）
  await tryLogin()
})

// 监听路由变化更新 redirect（与正常页面保持一致）
watch(
  () => route.query.redirect,
  (newRedirect) => {
    if (newRedirect) {
      redirect.value = newRedirect as string
    }
  },
  { immediate: true }
)
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

// 错误提示改为文档流定位，避免与下方“记住我”行重叠（该行为紧凑布局使用了负 margin）
.login-form {
  :deep(.el-form-item__error) {
    position: static;
    padding-top: 2px;
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