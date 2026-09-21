# Trans Translation Platform

本仓库以系统1为唯一业务主体：

- `apps/platform`：完整系统1 Spring Cloud 后端，负责用户、权限、翻译任务、
  模型选择、翻译流程和成品；
- `apps/web`：完整系统1 Vue 前端；
- `services/document-engine`：由系统2保留的内部 PDF/OCR 服务；
- `contracts`：Java 与 Python 服务之间的版本化边界。

PDF 主链路：

`系统1上传 -> 内部 OCR API -> document.v1 -> 中间 DOCX -> 系统1翻译流程`

系统2不拥有系统1账号或任务。Paddle 凭据只进入 Python 服务，Qwen 凭据只进入
系统1服务端。复制 `.env.example` 到本地密钥存储或由私有云注入；不要提交填充值。

## 本地验证

```powershell
# Python OCR 边界
& 'D:\codeC\python\翻译系统\pdf-translation\.venv\Scripts\python.exe' `
  -m pytest services/document-engine/tests/test_internal_ocr_api.py `
  services/document-engine/tests/test_paddle_auth_header.py -q

# 完整后端
$env:JAVA_HOME = 'D:\Caches\tools\temurin-17\jdk-17.0.20.1+1'
$env:MAVEN_OPTS = '-Dmaven.repo.local=D:\Caches\maven-repository'
& 'D:\Caches\tools\apache-maven-3.9.16\bin\mvn.cmd' `
  -f apps/platform/pom.xml -q -pl sva-server -am -DskipTests package

# 仓库与密钥边界
powershell -NoProfile -ExecutionPolicy Bypass `
  -File verification/check_complete_system1_baseline.ps1
```

完整验证证据与当前前端阻塞项见
`docs/discovery/integration-verification.md`。数据库导入边界见
`docs/runbooks/system1-database-bootstrap.md`。
