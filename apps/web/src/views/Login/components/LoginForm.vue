<template>
  <el-form
    v-show="getShow"
    ref="formLogin"
    :model="loginData.loginForm"
    :rules="LoginRules"
    class="login-form"
    label-position="top"
    label-width="120px"
    size="large"
  >
    <el-row class="mx-[-10px]">
      <el-col :span="24" class="px-10px">
        <el-form-item>
          <LoginFormTitle class="w-full" />
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item prop="username">
          <el-input
            v-model="loginData.loginForm.username"
            :placeholder="t('login.usernamePlaceholder')"
            :prefix-icon="iconAvatar"
          />
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
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
      <el-col :span="24" class="px-10px mt-[-20px] mb-[-20px]">
        <el-form-item>
          <el-row justify="space-between" style="width: 100%">
            <el-col :span="6">
              <el-checkbox v-model="loginData.loginForm.rememberMe">
                {{ t('login.remember') }}
              </el-checkbox>
            </el-col>
            <el-col :offset="6" :span="12">
              <el-link
                class="float-right"
                type="primary"
                @click="setLoginState(LoginStateEnum.RESET_PASSWORD)"
              >
                {{ t('login.forgetPassword') }}
              </el-link>
            </el-col>
          </el-row>
        </el-form-item>
      </el-col>
      <el-col :span="24" class="px-10px">
        <el-form-item>
          <XButton
            :loading="loginLoading"
            :title="t('login.login')"
            class="w-full"
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
      <el-col :span="24" class="px-10px">
        <el-form-item>
          <el-row :gutter="5" justify="space-between" style="width: 100%">
            <el-col :span="24">
              <XButton
                :title="t('login.btnRegister')"
                class="w-full"
                @click="setLoginState(LoginStateEnum.REGISTER)"
              />
            </el-col>
          </el-row>
        </el-form-item>
      </el-col>
      <el-divider content-position="center">{{ t('login.otherLogin') }}</el-divider>
      <el-col :span="24" class="px-10px">
        <el-form-item>
          <div class="social-icons-container">
            <template v-for="(item, key) in socialList" :key="key">
              <img
                v-if="item.type === 50"
                :src="FeishuIcon"
                alt="飞书登录"
                class="social-icon cursor-pointer"
                @click="doSocialLogin(item.type)"
              />
              <Icon
                v-else
                :icon="item.icon"
                :size="30"
                class="anticon cursor-pointer"
                color="#999"
                @click="doSocialLogin(item.type)"
              />
            </template>
          </div>
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script lang="ts" setup>
import { ElLoading } from 'element-plus'
import LoginFormTitle from './LoginFormTitle.vue'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

import { useIcon } from '@/hooks/web/useIcon'
import FeishuIcon from '@/assets/imgs/feishu.svg'

import * as authUtil from '@/utils/auth'
import { usePermissionStore } from '@/store/modules/permission'
import * as LoginApi from '@/api/login'
import { LoginStateEnum, useFormValid, useLoginState } from './useLogin'
import router from '@/router'

defineOptions({ name: 'LoginForm' })

const { t } = useI18n()
const message = useMessage()
const iconHouse = useIcon({ icon: 'ep:house' })
const iconAvatar = useIcon({ icon: 'ep:avatar' })
const iconLock = useIcon({ icon: 'ep:lock' })
const formLogin = ref()
const { validForm } = useFormValid(formLogin)
const { setLoginState, getLoginState } = useLoginState()
const { currentRoute, push } = useRouter()
const permissionStore = usePermissionStore()
const redirect = ref<string>('')
const loginLoading = ref(false)
const verify = ref()
const captchaType = ref('blockPuzzle')

// 控制登录表单显示隐藏
const getShow = computed(() => unref(getLoginState) === LoginStateEnum.LOGIN)

// 表单校验规则
const required = { required: true, message: '该项为必填项', trigger: 'blur' }
const LoginRules = {
  tenantName: [required],
  username: [required],
  password: [required]
}

// 登录表单相关数据
const loginData = reactive({
  isShowPassword: false,
  captchaEnable: import.meta.env.VITE_APP_CAPTCHA_ENABLE,
  tenantEnable: import.meta.env.VITE_APP_TENANT_ENABLE,
  loginForm: {
    tenantName: import.meta.env.VITE_APP_DEFAULT_LOGIN_TENANT || '',
    username: import.meta.env.VITE_APP_DEFAULT_LOGIN_USERNAME || '',
    password: import.meta.env.VITE_APP_DEFAULT_LOGIN_PASSWORD || '',
    captchaVerification: '',
    rememberMe: true
  }
})

// 第三方登录配置列表
const socialList = [
  { icon: 'custom:feishu', type: 50 }
]

// 获取URL地址栏中的指定参数
const getUrlParam = (name: string) => {
  const reg = new RegExp('(^|&)' + name + '=([^&]*)(&|$)', 'i')
  const r = window.location.search.substr(1).match(reg)
  return r ? decodeURIComponent(r[2]) : null
}

// 判断当前页面是否为飞书授权回调页面
const isFeishuCallback = () => {
  const type = getUrlParam('type')
  const code = getUrlParam('code')
  return type === '50' && !!code
}

// 飞书授权回调后执行自动登录
const autoFeishuLogin = async () => {
  try {
    // 获取回调返回的授权码和状态值
    const code = getUrlParam('code')
    const state = getUrlParam('state')
    const localState = sessionStorage.getItem('feishu_state')

    // 常规校验逻辑
    if (!code || !state || state !== localState) {
      message.error('飞书授权校验失败')
      // index场景下直接跳回登录页
      if (redirect.value === '/index') {
        await push({ path: '/login' })
      }
      return
    }

    // 向后端发送请求，完成飞书登录
    const res = await LoginApi.feishuLogin({
      code: code,
      state: state
    })

    // 存储登录令牌，跳转到首页
    authUtil.setToken(res.token)
    message.success('飞书登录成功')
    await push({ path: redirect.value || '/' })
    
  } catch (e) {
    console.error('飞书登录失败', e)
    // index 登录失败直接回登录页
    if (redirect.value === '/index') {
      await push({ path: '/login' })
      return
    }
    message.error('飞书登录失败，请重试')
  }
}

// 处理登录前的验证码校验逻辑
const getCode = async () => {
  // 无验证码直接登录，有验证码则显示验证组件
  if (loginData.captchaEnable === 'false') {
    await handleLogin({})
  } else {
    verify.value.show()
  }
}

// 获取并存储租户ID
const getTenantId = async () => {
  if (loginData.tenantEnable === 'true') {
    // 向后端发送请求，根据租户名称获取租户ID
    const res = await LoginApi.getTenantIdByName(loginData.loginForm.tenantName)
    authUtil.setTenantId(res)
  }
}

// 读取本地缓存的登录表单数据
const getLoginFormCache = () => {
  // 从本地存储获取历史登录信息
  const loginForm = authUtil.getLoginForm()
  if (loginForm) {
    loginData.loginForm = {
      ...loginData.loginForm,
      ...loginForm
    }
  }
}

// 根据当前域名获取租户信息
const getTenantByWebsite = async () => {
  if (loginData.tenantEnable === 'true') {
    const website = location.host
    // 向后端发送请求，根据域名查询租户信息
    const res = await LoginApi.getTenantByWebsite(website)
    if (res) {
      loginData.loginForm.tenantName = res.name
      authUtil.setTenantId(res.id)
    }
  }
}

const loading = ref()

// 执行账号密码登录
const handleLogin = async (params: any) => {
  loginLoading.value = true
  try {
    // 获取租户信息
    await getTenantId()
    // 表单校验
    const data = await validForm()
    if (!data) return

    // 向后端发送请求，执行账号密码登录
    const loginDataLoginForm = { ...loginData.loginForm, ...params }
    const res = await LoginApi.login(loginDataLoginForm)
    
    // 加载页面并缓存登录信息
    loading.value = ElLoading.service({ text: '加载系统中...' })
    authUtil.setLoginForm(loginDataLoginForm)
    // 存储登录令牌
    authUtil.setToken(res)
    
    // 跳转到目标页面
    await push({ path: redirect.value || permissionStore.addRouters[0].path })
  } finally {
    loginLoading.value = false
    loading.value?.close()
  }
}

// 检测飞书浏览器登录状态（核心新功能）
const checkFeishuBrowserLogin = async () => {
  return new Promise<boolean>(async (resolve) => {
    try {
      // 尝试发起一个静默授权请求来检测登录状态
      const { clientId, state } = await LoginApi.getFeishuAuthUrl({ origin: location.origin })
      const silentAuthUrl = `https://open.feishu.cn/open-apis/authen/v1/authorize
?client_id=${clientId}
&redirect_uri=${encodeURIComponent(location.origin + '/social-login?type=50&silent=true')}
&response_type=code
&state=${state}
&prompt=none
&scope=contact:user.base:readonly`

      // 创建隐藏的iframe来发起静默授权
      const iframe = document.createElement('iframe')
      iframe.style.display = 'none'
      iframe.src = silentAuthUrl.replace(/\s/g, '')
      
      // 超时处理（3秒）
      const timeoutId = setTimeout(() => {
        document.body.removeChild(iframe)
        resolve(false) // 未登录/需要扫码
      }, 3000)
      
      // 监听iframe加载完成
      iframe.onload = () => {
        clearTimeout(timeoutId)
        document.body.removeChild(iframe)
        resolve(true) // 已登录，无需扫码
      }
      
      document.body.appendChild(iframe)
    } catch (e) {
      resolve(false) // 检测失败，视为需要扫码
    }
  })
}

// 生成飞书授权链接并跳转
const directFeishuAuth = async () => {
  if (loginData.tenantEnable === 'true') {
    await getTenantId()
    if (!authUtil.getTenantId()) {
      message.warning('请先选择租户')
      return
    }
  }

  try {
    // ===================== 核心修改：跳授权页前检测登录状态 =====================
    if (redirect.value === '/index') {
      const isFeishuLoggedIn = await checkFeishuBrowserLogin()
      if (!isFeishuLoggedIn) {
        // 未登录/需要扫码 → 直接跳回登录页，不跳授权页
        // message.warning('当前浏览器未登录飞书，无法自动登录')
        await push({ path: '/login' })
        return
      }
    }
    // ===========================================================================

    const loadingInstance = ElLoading.service({ text: '跳转飞书授权...' })
    // 向后端发送请求，获取飞书授权所需参数
    const { clientId, state } = await LoginApi.getFeishuAuthUrl({ origin: location.origin })

    // 拼接飞书授权回调地址
    const redirectUri = encodeURIComponent(
      location.origin + '/social-login?type=50&redirect=' + encodeURIComponent(redirect.value || '/')
    )

    // 拼接飞书官方授权链接
    const authUrl = `https://open.feishu.cn/open-apis/authen/v1/authorize
?client_id=${clientId}
&redirect_uri=${redirectUri}
&response_type=code
&state=${state}
&prompt=consent
&scope=contact:user.base:readonly`

    // 存储状态值，用于回调校验
    sessionStorage.setItem('feishu_state', state)
    loadingInstance.close()
    // 浏览器打开飞书授权页面
    window.location.href = authUrl.replace(/\s/g, '')
  } catch (error) {
    console.error('飞书授权失败', error)
    message.error('飞书登录初始化失败')
    ElLoading.service({}).close()
  }
}

// 处理第三方登录点击事件
const doSocialLogin = async (type: number) => {
  // 飞书登录，执行授权跳转
  if (type === 50) {
    await directFeishuAuth()
    return
  }
  message.error('未配置该登录方式')
}

// 监听路由变化，获取重定向地址
watch(
  () => currentRoute.value,
  (route) => {
    redirect.value = route?.query?.redirect as string
  },
  { immediate: true }
)

declare global {
  interface Window {
    h5sdk?: any;
    tt?: any;
  }
}

// 飞书免登检测
const checkFeishuSso = async () => {
  const userAgent = navigator.userAgent.toLowerCase();
  const isFeishuClient = userAgent.includes('feishu') || userAgent.includes('lark') || !!window.h5sdk;
  if (!isFeishuClient) {
    return;
  }
  const { clientId, state } = await LoginApi.getFeishuAuthUrl({ origin: location.origin })
  try {
    await loadFeishuSdk();
    window.h5sdk.ready(async () => {
      try {
        const res = await new Promise((resolve, reject) => {
          window.tt.requestAccess({
            // appID: 'cli_a9562cb01f3b5bc9',
            // appID: 'cli_a9467258cdb85bde',
            appID: clientId,
            scopeList: [],
            success: resolve,
            fail: (err) => {
              alert('requestAccess失败，错误详情：' + JSON.stringify(err));
              reject(err);
            }
          });
        });
        const loginRes = await LoginApi.feishuSsoLogin(res.code);
        authUtil.setToken(loginRes);
        message.success('免登成功');
        
        // 使用 redirect.value 获取正确的重定向地址
        const targetPath = redirect.value || '/'
        
        // 使用 nextTick 确保状态更新完成
        await new Promise(resolve => setTimeout(resolve, 50))
        
        // 直接使用 window.location.replace 进行跳转，避免与路由守卫冲突
        // replace 方法不会在历史记录中留下当前页面，用户体验更好
        window.location.replace(targetPath)

      } catch (e) {
        console.log('飞书免登失败')
      }
    });
  } catch (e) {
    console.log('飞书SDK加载失败')
  }
};

// 动态加载飞书JS-SDK
const loadFeishuSdk = () => {
  return new Promise((resolve, reject) => {
    if (window.h5sdk) {
      resolve(null);
      return;
    }
    const script = document.createElement('script');
    script.src = 'https://lf1-cdn-tos.bytegoofy.com/goofy/lark/op/h5-js-sdk-1.5.26.js';
    script.onload = resolve;
    script.onerror = reject;
    document.body.appendChild(script);
  });
};

// 页面挂载后初始化数据
onMounted(async () => {
  // 基础初始化
  getLoginFormCache()
  await getTenantByWebsite()
  
  // 飞书回调则执行登录
  if (isFeishuCallback()) {
    autoFeishuLogin()
    return
  }

  // 检测飞书客户端免登
  checkFeishuSso()
  console.log(redirect.value)
  // 所有携带redirect的地址，自动跳飞书授权页
  // if (redirect.value && redirect.value !== '/index') {
  //   setTimeout(() => {
  //     directFeishuAuth()
  //   }, 300)
  // }
})
</script>

<style lang="scss" scoped>
:deep(.anticon) {
  &:hover {
    color: var(--el-color-primary) !important;
  }
}

.social-icons-container {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 24px;
  width: 100%;
  flex-wrap: wrap;
}

.social-icon {
  width: 30px;
  height: 30px;
  transition: all 0.3s;
  cursor: pointer;

  &:hover {
    opacity: 0.8;
    transform: scale(1.05);
    filter: brightness(0.9);
  }
}

:deep(.anticon) {
  font-size: 30px;
  transition: all 0.3s;

  &:hover {
    transform: scale(1.05);
  }
}

.login-code {
  float: right;
  width: 100%;
  height: 38px;

  img {
    width: 100%;
    height: auto;
    max-width: 100px;
    vertical-align: middle;
    cursor: pointer;
  }
}
</style>