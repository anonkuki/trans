<template>
  <Dialog v-model="dialogVisible" title="上传文件">
    <el-form
      ref="formRef"
      v-loading="formLoading"
      :model="formData"
      :rules="formRules"
      label-width="100px"
    >
      <el-form-item label="模板标识" prop="tempName">
        <el-input v-model="formData.tempName" placeholder="请输入模板标识" />
      </el-form-item>
      <el-form-item label="文件上传" prop="file">
        <el-upload
          ref="uploadRef"
          class="upload-demo"
          drag
          :auto-upload="false"
          :on-change="handleFileChange"
          :file-list="fileList"
          :limit="1"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">支持各种文件格式</div>
          </template>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :disabled="formLoading" type="primary" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script lang="ts" setup>
import * as TempFileApi from '@/api/system/tempFile'
import { FormRules } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'

defineOptions({ name: 'SystemTempFileUploadForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const formLoading = ref(false) // 表单的加载中
const formData = ref({
  tempName: ''
})
const formRules = reactive<FormRules>({
  tempName: [{ required: true, message: '模板标识不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref
const fileList = ref<any[]>([]) // 文件列表
const uploadedFile = ref<File | null>(null) // 上传的文件

/** 打开弹窗 */
const open = () => {
  dialogVisible.value = true
  resetForm()
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  if (!formRef) return
  const valid = await formRef.value.validate()
  if (!valid) return
  
  // 检查是否已选择文件
  if (!uploadedFile.value) {
    message.error('请选择要上传的文件')
    return
  }
  
  // 提交请求
  formLoading.value = true
  try {
    // 上传文件（后端会自动创建模板记录）
    const uploadResult = await TempFileApi.uploadFile(uploadedFile.value, formData.value.tempName)
    
    message.success(t('common.createSuccess'))
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } catch (error) {
    message.error('上传失败')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = {
    tempName: ''
  }
  fileList.value = []
  uploadedFile.value = null
  formRef.value?.resetFields()
}

/** 文件选择变化 */
const handleFileChange = (file) => {
  if (file.raw) {
    uploadedFile.value = file.raw
  }
}
</script>

<style scoped>
.upload-demo {
  width: 100%;
}
</style>
