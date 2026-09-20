# Translation Platform Integration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 以 `sva-cloud` + `sva-ui` 为唯一业务主系统，把 `pdf-translation` 收敛为可替换的内部文档处理引擎，并形成可本地运行、可测试、可审计、可迁移到公司私有云的统一翻译 Agent 平台。

**Architecture:** 新仓库采用单仓多应用结构。Java 平台拥有用户、租户、权限、任务、模型配置、术语和产物元数据；Python 文档引擎只接收内部任务，负责原生 PDF 提取、PaddleOCR-VL、复杂页视觉模型兜底、翻译和版式回填。两侧通过版本化 OpenAPI 与 `document.v1` 交换数据，用户文件通过对象存储引用传递，不经浏览器携带模型或 OCR 密钥。

**Tech Stack:** Java 21、Spring Boot 3.5、Spring Cloud、MyBatis Plus、Redis、MySQL/PostgreSQL、Vue 3、TypeScript、Vite、pnpm、Python 3.11、FastAPI、Pydantic、BabelDOC、RetainPDF、PaddleOCR-VL、OpenAI-compatible LLM API、Docker Compose。

---

## 0. 已确认基线与不可越过的边界

- 系统1由 `sva-cloud` 与 `sva-ui` 组成，是未来唯一面向用户的平台层。
- 系统1默认 PDF 路径使用 PDFBox 提取文本，不是 OCR；其 `ConvertByPythonHelper` 是现成的 Python 扩展缝隙，但当前 `/convert` 只返回 DOCX，正式融合必须升级为结构化契约。
- 系统2 `pdf-translation` 的核心复用价值是 `document.v1`、OCR provider、BabelDOC/RetainPDF 两条处理链和 PDF/双语产物渲染，不承担登录、租户、权限、长期任务状态或用户密钥管理。
- Qwen3.8-Flash 作为首选翻译模型注册到服务端模型目录；是否承担视觉 OCR 兜底必须由真实部署端点的多模态能力测试决定，不能仅凭模型名称假定。
- PaddleOCR-VL 作为扫描件主 OCR；数字 PDF 优先原生文本层；低置信度、复杂表格、公式和版面异常页才进入视觉模型兜底。
- 用户、租户、文档、任务和产物必须建立服务端所有权校验；Agent 的写操作和破坏性操作必须有权限、确认和审计。
- 所有已经在聊天、源码或配置中出现过的凭据都视为已泄露：不得复制到新仓库，迁移前必须轮换。

## 1. 目标仓库布局

最终目录固定为：

```text
trans/
├─ README.md
├─ .env.example
├─ apps/
│  ├─ platform/                 # sva-cloud
│  └─ web/                      # sva-ui
├─ services/
│  └─ document-engine/          # pdf-translation 的受控内部服务
├─ contracts/
│  ├─ document/document.v1.schema.json
│  └─ document-engine/openapi.yaml
├─ deploy/
│  ├─ compose/local.yml
│  ├─ compose/private-cloud.yml
│  └─ config/
├─ docs/
│  ├─ architecture/
│  ├─ discovery/
│  ├─ operations/
│  └─ superpowers/plans/
├─ evals/
│  ├─ manifest.jsonl
│  └─ fixtures/
└─ scripts/
   ├─ bootstrap.ps1
   ├─ verify.ps1
   └─ smoke-test.ps1
```

## 2. 完成定义

只有以下条件全部满足，才能把“融合完成”标记为通过：

- 一个账号在同一个系统1页面提交 DOCX、Excel、数字 PDF、扫描 PDF 和图片任务。
- 系统1产生全局 `taskId`，系统2只使用 `externalTaskId` 执行，不建立第二套用户任务中心。
- 数字 PDF 不误走 OCR；扫描 PDF 使用 PaddleOCR-VL；疑难页仅在策略命中后进入视觉兜底。
- Qwen3.8-Flash 的模型选择从 UI 经 API、数据库任务快照传到实际客户端，重试时保持原模型与提示词版本。
- 原文、译文、OCR 结构和产物均能追溯到租户、用户、模型版本、OCR 版本、提示词版本和输入文件哈希。
- 未授权用户无法查询、取消或下载他人任务；客户端不能提交任意 LLM/OCR URL、回调 URL或真实密钥。
- 服务重启后任务可恢复；Redis 清空不丢业务任务；重复回调和重复提交具备幂等性。
- 代表性样本的文字正确率、漏译率、术语一致性、表格/公式/版式和耗时均有基线与验收记录。
- 本地 `scripts/verify.ps1` 和 CI 使用同一组命令且全部通过。

## 3. Task 1：建立干净、可追溯的单仓基线

**Files:**

- Create: `README.md`
- Create: `.gitignore`
- Create: `.env.example`
- Create: `docs/discovery/source-inventory.md`
- Create: `docs/architecture/adr/ADR-001-monorepo-boundaries.md`
- Create: `scripts/bootstrap.ps1`
- Create: `scripts/verify.ps1`

- [ ] 记录三个来源的远程地址、来源提交、导入日期、许可证和本地变更，不把 `node_modules`、Maven 仓库、模型权重、用户文件或真实密钥写入 Git。
- [ ] 从系统1负责人取得 `sva-cloud`、`sva-ui` 的原始 Git 仓库与精确提交；在此之前，将当前两个无 `.git` 文件夹标记为 `unverified snapshot`，不可虚构历史。
- [ ] 对系统2先分离并审查当前未提交修改，再从已确认提交导入；不要把来源仓库的工作区改动悄悄混入首次提交。
- [ ] 创建 `apps/platform`、`apps/web`、`services/document-engine`，采用一次性导入提交，提交信息注明来源和 commit SHA。
- [ ] `.gitignore` 覆盖 `.env*`（保留 `.env.example`）、`node_modules/`、`target/`、`.venv/`、`__pycache__/`、`.pytest_cache/`、`work/`、模型权重、OCR 原始文件和输出产物。
- [ ] `bootstrap.ps1` 将 `TEMP`、`TMP`、pnpm store、Maven repository、Python venv/cache 全部指向 D 盘可配置目录。
- [ ] `README.md` 只描述已验证入口，并明确本地最小拓扑为网关、system、infra、ai-server、web、document-engine、数据库、Redis、Nacos 和对象存储。

**Verification:**

```powershell
git status --short
git ls-files | Select-String -Pattern 'node_modules|target/|\.venv|\.env$|work/'
git log --oneline --decorate -10
```

Expected: 受忽略目录不在索引中；三个来源各有独立导入提交；来源清单包含真实 SHA 或明确的 `unverified snapshot`。

**Commit:**

```bash
git add README.md .gitignore .env.example docs/architecture docs/discovery scripts
git commit -m "chore: establish integration repository baseline"
```

## 4. Task 2：先消除凭据与访问控制高风险

**Files:**

- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/resources/application.yaml`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/controller/admin/translation/TranController.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/TranFileServiceImpl.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/TaskManagerServiceImpl.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/controller/admin/translation/TranAuthorizationTest.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/TranTaskOwnershipTest.java`
- Create: `docs/operations/credential-rotation.md`

- [ ] 先写失败测试：用户 A 查询、取消、重试、下载用户 B 的任务均返回 403/业务拒绝。
- [ ] 给翻译任务、文件和产物补齐 `tenantId`、`userId` 所有权字段与查询条件；禁止普通请求通过 `username` 覆盖当前用户范围。
- [ ] 删除所有字面量密钥、密码和令牌；改为环境变量或密钥引用，`.env.example` 只保留无敏感值的变量名。
- [ ] 轮换此前已暴露的模型、OCR、Git、数据库、Redis、Nacos 和对象存储凭据，并在轮换记录中只保存凭据 ID/时间/负责人，不保存密钥值。
- [ ] 文件下载统一走后端鉴权入口，再签发短期对象存储 URL；前端不得直接拼接永久对象 URL。

**Verification:**

```powershell
rg -n --hidden -g '!**/target/**' -g '!**/node_modules/**' '(sk-|api[_-]?key\s*[:=]|secret\s*[:=]|password\s*[:=])' apps services
mvn -pl sva-module-ai/sva-module-ai-server -am -Dtest=TranAuthorizationTest,TranTaskOwnershipTest test
```

Expected: 凭据扫描只命中变量名或测试假值；越权用例全部被拒绝。

**Commit:**

```bash
git add apps/platform docs/operations/credential-rotation.md .env.example
git commit -m "security: enforce translation task ownership and externalize secrets"
```

## 5. Task 3：定义平台与文档引擎契约

**Files:**

- Create: `contracts/document/document.v1.schema.json`
- Create: `contracts/document-engine/openapi.yaml`
- Create: `docs/architecture/adr/ADR-002-document-engine-contract.md`
- Create: `services/document-engine/tests/contract/test_openapi_contract.py`
- Create: `services/document-engine/tests/contract/test_document_v1_schema.py`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/integration/document/DocumentEngineContractTest.java`

- [ ] 从 `services/document-engine/pipeline/services/document_schema/document.v1.schema.json` 提取并冻结平台级副本，版本号保持 `document.v1`，增加 schema 校验测试防止无意漂移。
- [ ] OpenAPI 仅暴露内部接口：`POST /internal/v1/jobs`、`GET /internal/v1/jobs/{engineJobId}`、`POST /internal/v1/jobs/{engineJobId}/cancel`、`GET /internal/v1/health`。
- [ ] 创建任务请求仅接受：`externalTaskId`、输入对象引用、输入 SHA-256、源/目标语言、engine policy、模型引用、术语表引用和回调签名引用；禁止客户端传真实密钥或任意 URL。
- [ ] 状态枚举固定为 `QUEUED/RUNNING/SUCCEEDED/FAILED/CANCELLED`；页级阶段通过 `stage` 表示 `CLASSIFY/EXTRACT/OCR/TRANSLATE/RENDER/PUBLISH`。
- [ ] 返回内容只含进度、结构化错误、产物对象引用、document schema 版本与 provenance；大文件不内嵌 Base64。
- [ ] 定义 `Idempotency-Key=externalTaskId`、请求签名、时间戳、重放窗口、超时、重试、最大上传量和错误码。

**Verification:**

```powershell
python -m pytest services/document-engine/tests/contract -q
mvn -pl sva-module-ai/sva-module-ai-server -am -Dtest=DocumentEngineContractTest test
```

Expected: Python 与 Java 对同一套有效/无效 fixture 给出一致结论，重复 `externalTaskId` 不创建新任务。

**Commit:**

```bash
git add contracts docs/architecture/adr/ADR-002-document-engine-contract.md services/document-engine/tests/contract apps/platform
git commit -m "feat: define versioned document engine contract"
```

## 6. Task 4：将系统2硬化为内部文档引擎

**Files:**

- Modify: `services/document-engine/app.py`
- Modify: `services/document-engine/retain.py`
- Modify: `services/document-engine/run_job.py`
- Modify: `services/document-engine/pipeline/services/ocr_provider/provider_config.py`
- Modify: `services/document-engine/pipeline/services/ocr_provider/paddle_api.py`
- Create: `services/document-engine/security/internal_auth.py`
- Create: `services/document-engine/domain/job_repository.py`
- Create: `services/document-engine/adapters/object_store.py`
- Create: `services/document-engine/tests/api/test_internal_jobs.py`
- Create: `services/document-engine/tests/security/test_endpoint_allowlist.py`
- Create: `services/document-engine/tests/integration/test_job_recovery.py`

- [ ] 先写失败测试：无内部签名请求被拒绝；请求覆盖 LLM/OCR/callback 地址被拒绝；非法对象 key 和路径穿越被拒绝。
- [ ] 保留现有引擎实现，但移除面向浏览器的模型/OCR key 与 base URL 参数，改为服务器端 registry 中的 `modelRef`、`ocrProviderRef`。
- [ ] 让 Python 任务使用系统1的 `externalTaskId`，状态写入持久化 repository；进程内字典只作热点缓存。
- [ ] 输入从对象存储拉取并校验 SHA-256；输出先写隔离临时目录，再上传对象存储并返回受控引用。
- [ ] 加入请求大小、页数、像素、解压、并发、执行时间、子进程资源和工作目录配额。
- [ ] 将允许访问的 LLM/OCR 域名或私有云服务名配置为启动时加载的白名单，禁止任务级覆盖。
- [ ] 修复现有 pytest 的 `sys.modules` 污染，使全套测试一次运行稳定通过，而不是仅逐文件通过。

**Verification:**

```powershell
python -m pytest services/document-engine/tests -q
python -m pip check
```

Expected: 全套测试单次运行通过；API 无法访问白名单外地址；模拟重启后可继续查询任务。

**Commit:**

```bash
git add services/document-engine
git commit -m "refactor: harden pdf engine as an internal service"
```

## 7. Task 5：在系统1增加 DocumentProcessingGateway

**Files:**

- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/document/DocumentProcessingGateway.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/document/DocumentEngineClient.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/document/DocumentRoutePolicy.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/TranServiceImpl.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/PdfTranslationServiceImpl.java`
- Deprecate: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/helper/ConvertByPythonHelper.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/document/DocumentRoutePolicyTest.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/document/DocumentEngineClientTest.java`

- [ ] 先写路由测试：DOCX/Excel 走现有结构化流程；有可靠文本层的 PDF 走 native；扫描件走 PaddleOCR-VL；只有低置信度/复杂版面页走 vision fallback。
- [ ] `DocumentRoutePolicy` 使用可解释信号：文本覆盖率、每页字符数、图片覆盖率、字体/文字对象、表格/公式/多栏特征和 OCR 置信度；把选择原因存入任务审计。
- [ ] `DocumentEngineClient` 根据 OpenAPI 生成或手写窄客户端，设置连接/读取超时、有限重试、熔断、幂等键和请求签名。
- [ ] `TranServiceImpl` 保留系统1任务生命周期，提交引擎后保存 `engineJobId`；轮询/回调只更新允许的状态转换。
- [ ] `PdfTranslationServiceImpl` 不再直接把 Python `/convert` 的 DOCX 当唯一结果；读取产物清单与 `document.v1` provenance。
- [ ] 保留旧 PDFBox 路径为功能开关下的回滚方案，至少经过两个版本再删除 `ConvertByPythonHelper`。

**Verification:**

```powershell
mvn -pl sva-module-ai/sva-module-ai-server -am -Dtest=DocumentRoutePolicyTest,DocumentEngineClientTest test
```

Expected: 路由 fixture 命中预期引擎；超时、重复提交、非法状态跳转和签名失败均有确定结果。

**Commit:**

```bash
git add apps/platform/sva-module-ai
git commit -m "feat: integrate document engine through a typed gateway"
```

## 8. Task 6：建立模型注册表并接入 Qwen3.8-Flash

**Files:**

- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/LlmClientServiceImpl.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/model/ModelCapabilityService.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/model/ModelSelectionIntegrationTest.java`
- Modify: `apps/web/src/views/ai/translation/index/index.vue`
- Modify: `apps/web/src/api/ai/translation/translation/tranApi.ts`
- Create: `apps/web/src/views/ai/translation/index/index.spec.ts`

- [ ] 注册 `Qwen3.8-Flash` 的内部 model ID、部署 endpoint reference、能力标签、上下文窗口、启用状态和版本，不把 endpoint/key 下发到浏览器。
- [ ] UI 只展示当前租户可用模型；未知或未配置模型由服务端拒绝，不静默回退。
- [ ] 创建任务时把 `modelId`、provider deployment revision、prompt version、glossary version 写入不可变任务快照。
- [ ] 修复翻译缓存键：使用 SHA-256，包含 tenant、source/target language、model deployment revision、role、prompt version、glossary version 和规范化原文。
- [ ] 用 fake OpenAI-compatible server 做集成测试，证明 UI 选择的模型到达实际客户端；同一文本在不同模型/租户/提示词下不串缓存。
- [ ] 单独执行多模态能力探测；仅探测通过后才给该部署加 `VISION` 能力并允许疑难页兜底。

**Verification:**

```powershell
mvn -pl sva-module-ai/sva-module-ai-server -am -Dtest=ModelSelectionIntegrationTest test
pnpm --dir apps/web ts:check
pnpm --dir apps/web test --run
```

Expected: 选择链路完整；未知模型失败；缓存隔离测试通过；密钥不出现在浏览器网络请求或任务 JSON 中。

**Commit:**

```bash
git add apps/platform apps/web
git commit -m "feat: add capability-aware qwen model selection"
```

## 9. Task 7：落地 OCR 组合策略与质量门控

**Files:**

- Create: `services/document-engine/pipeline/services/ocr_provider/paddle_vl_driver.py`
- Create: `services/document-engine/pipeline/services/ocr_provider/qwen_vision_driver.py`
- Create: `services/document-engine/pipeline/services/quality/page_quality.py`
- Create: `services/document-engine/pipeline/services/routing/page_router.py`
- Create: `services/document-engine/tests/routing/test_page_router.py`
- Create: `services/document-engine/tests/quality/test_page_quality.py`
- Create: `evals/manifest.jsonl`
- Create: `docs/architecture/ocr-routing.md`

- [ ] 为每页计算质量信号：字符覆盖、乱码率、块重叠、阅读顺序、表格结构、公式保真、OCR 置信度和语言一致性。
- [ ] 默认路由固定为 `native -> PaddleOCR-VL -> vision fallback -> human review`，不能让整个文档无条件同时跑两个昂贵模型。
- [ ] Qwen 视觉输出必须适配到同一 `document.v1`，保留 provider、model、page、block、bbox、confidence 和 raw artifact 引用。
- [ ] 对低置信度结果进入人工复核队列，不得把猜测内容静默当高置信度结果发布。
- [ ] 建立脱敏评测集，至少覆盖数字 PDF、扫描 PDF、旋转页、双栏、复杂表格、公式、印章/水印、中英混排、低分辨率图片和超长文档。
- [ ] 对 Paddle 与 Qwen 分别记录 CER/WER、版面块 F1、表格结构、公式正确率、漏译率、术语一致性、P95 耗时和资源占用。

**Verification:**

```powershell
python -m pytest services/document-engine/tests/routing services/document-engine/tests/quality -q
python services/document-engine/tools/run_eval.py --manifest evals/manifest.jsonl --output D:\Temp\trans-eval\result.json
```

Expected: 每个样本有可复现路由原因和指标；不满足门槛的页进入人工复核，不自动发布。

**Commit:**

```bash
git add services/document-engine evals docs/architecture/ocr-routing.md
git commit -m "feat: add confidence-driven ocr routing and evaluation"
```

## 10. Task 8：统一任务持久化、事件与产物

**Files:**

- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/resources/db/migration/V001__translation_task.sql`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/resources/db/migration/V002__translation_artifact.sql`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/TaskManagerServiceImpl.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/TaskRecoveryIntegrationTest.java`
- Create: `docs/architecture/translation-state-machine.md`

- [ ] 数据库作为任务真相源，Redis 只保存进度热点、锁和可重建缓存。
- [ ] 任务表保存 tenant/user、输入哈希、语言、策略、模型快照、engineJobId、状态、阶段、进度、错误码、重试次数和审计时间。
- [ ] 产物表保存类型、对象 key、SHA-256、大小、MIME、生成器版本和保留策略。
- [ ] 状态机拒绝倒退和非法跳转；回调使用事件 ID 去重；取消与重试保持幂等。
- [ ] 服务启动时扫描 `RUNNING`/`QUEUED` 任务，与引擎状态对账并恢复，不因 Redis 丢失而失败。

**Verification:**

```powershell
mvn -pl sva-module-ai/sva-module-ai-server -am -Dtest=TaskRecoveryIntegrationTest test
```

Expected: 模拟 Java、Python、Redis 分别重启后，任务最终状态一致且无重复产物。

**Commit:**

```bash
git add apps/platform docs/architecture/translation-state-machine.md
git commit -m "feat: persist translation tasks and artifacts"
```

## 11. Task 9：在现有 UI 中完成统一工作台

**Files:**

- Modify: `apps/web/src/views/ai/translation/index/index.vue`
- Modify: `apps/web/src/api/ai/translation/translation/tranApi.ts`
- Create: `apps/web/src/views/ai/translation/index/components/OcrPolicyPanel.vue`
- Create: `apps/web/src/views/ai/translation/index/components/TaskProvenanceDrawer.vue`
- Create: `apps/web/src/views/ai/translation/index/components/PageReviewPanel.vue`
- Create: `apps/web/src/views/ai/translation/index/index.spec.ts`
- Create: `apps/web/e2e/translation.spec.ts`

- [ ] 保留系统1现有 UI 风格和入口，不嵌入系统2独立页面。
- [ ] 增加“自动/仅原生/PaddleOCR-VL/人工复核优先”策略；普通用户默认只能选“自动”，高级策略受权限控制。
- [ ] 任务进度展示平台状态、文档阶段和页级异常，不直接显示内部服务地址或密钥。
- [ ] 下载统一调用后端授权 API，产物包括单语、双语、结构化结果和质量报告，并按权限显示。
- [ ] 低置信度页面提供原图、识别文本、译文、bbox 与修改审计；提交修改属于写操作，必须服务端校验权限。
- [ ] 用组件测试和 E2E 覆盖上传、模型/术语选择、进度、失败重试、取消、越权、复核和下载。

**Verification:**

```powershell
pnpm --dir apps/web ts:check
pnpm --dir apps/web test --run
pnpm --dir apps/web exec playwright test e2e/translation.spec.ts
pnpm --dir apps/web build:prod
```

Expected: 四类文档任务均在一个工作台闭环；无死按钮；错误状态可操作；浏览器控制台无错误。

**Commit:**

```bash
git add apps/web
git commit -m "feat: deliver unified translation and ocr workbench"
```

## 12. Task 10：增加受控 Agent 编排层

**Files:**

- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/agent/TranslationAgent.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/agent/TranslationToolRegistry.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/agent/AgentAuditService.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/agent/TranslationAgentPolicyTest.java`
- Create: `docs/architecture/agent-tool-policy.md`

- [ ] 第一版 Agent 仅编排确定性工具：分类、抽取、OCR、术语检索、翻译、质量检查、渲染、发布和人工复核。
- [ ] 每个工具声明输入 schema、输出 schema、只读/写入/破坏性级别、超时、重试和所需权限。
- [ ] 写入或发布动作需要服务端权限；覆盖原产物、删除和外发必须显式确认并记录审计。
- [ ] 模型不能构造任意 URL、文件路径、SQL 或 shell；工具参数在服务端按 schema 和业务范围校验。
- [ ] Agent 决策记录工具名、参数摘要、输入/输出哈希、模型、提示词版本、操作者与时间，但日志不记录原文或密钥。

**Verification:**

```powershell
mvn -pl sva-module-ai/sva-module-ai-server -am -Dtest=TranslationAgentPolicyTest test
```

Expected: 未授权写操作、提示词注入导致的越权工具调用、任意 URL/路径和未确认删除全部被拒绝并留审计事件。

**Commit:**

```bash
git add apps/platform docs/architecture/agent-tool-policy.md
git commit -m "feat: add policy-controlled translation agent orchestration"
```

## 13. Task 11：本地一键环境与私有云迁移面

**Files:**

- Create: `deploy/compose/local.yml`
- Create: `deploy/compose/private-cloud.yml`
- Create: `deploy/config/document-engine.yaml`
- Create: `scripts/smoke-test.ps1`
- Create: `docs/operations/local-runbook.md`
- Create: `docs/operations/private-cloud-runbook.md`
- Create: `.github/workflows/verify.yml`

- [ ] 本地 compose 固定服务健康检查、依赖顺序、非默认密码、命名卷、D 盘数据目录、CPU/内存限制和日志轮转。
- [ ] 模型和 OCR endpoint 只从服务端环境/密钥系统注入；本地可指向 API，私有云可切换到 OpenAI-compatible 内网 endpoint，无需改业务代码。
- [ ] `smoke-test.ps1` 创建测试用户和四类样本任务，轮询完成，校验产物哈希与所有权，再清理测试数据。
- [ ] CI 分为 secret scan、Java unit/integration、Python tests、frontend type/test/build、contract drift 和 container health。
- [ ] 私有云 runbook 包含 GPU/CPU 节点、对象存储、数据库备份、密钥系统、日志脱敏、网络白名单、升级/回滚和容量指标。

**Verification:**

```powershell
docker compose -f deploy/compose/local.yml up -d --build
pwsh -File scripts/smoke-test.ps1
pwsh -File scripts/verify.ps1
docker compose -f deploy/compose/local.yml down
```

Expected: 健康检查全绿；四类任务闭环；测试脚本退出码 0；停止后持久化数据仍可恢复。

**Commit:**

```bash
git add deploy scripts docs/operations .github/workflows/verify.yml
git commit -m "ops: add reproducible local and private-cloud deployment"
```

## 14. Task 12：灰度迁移、业务验收与回滚

**Files:**

- Create: `docs/acceptance/acceptance-matrix.md`
- Create: `docs/acceptance/benchmark-report.md`
- Create: `docs/operations/cutover-and-rollback.md`
- Create: `evals/baselines/system1-current.json`
- Create: `evals/baselines/integrated.json`

- [ ] 用同一批脱敏业务样本对比旧系统1、系统2和融合系统，禁止只凭主观挑选展示样本。
- [ ] 验收指标至少包含：文本准确率、漏译、术语、表格/公式、版式、可搜索性、P50/P95 耗时、失败率、人工复核率和资源使用。
- [ ] 先开启内部测试租户，再灰度 5%、25%、50%、100%；每级有明确进入条件、观察期和回滚阈值。
- [ ] 保留旧 PDFBox 路径和旧任务读取能力；回滚只切路由，不删除新任务/产物或回退数据库迁移。
- [ ] 生产切换前执行备份恢复演练、服务重启恢复、对象存储不可用、OCR/LLM 超时、限流和权限攻击测试。
- [ ] 业务负责人、技术负责人、安全负责人分别签署结果；未达标项记录影响与豁免期限，不以“能跑”代替验收。

**Verification:**

```powershell
pwsh -File scripts/verify.ps1
pwsh -File scripts/smoke-test.ps1 -Profile acceptance
git status --short
```

Expected: 自动检查全部通过；人工验收矩阵无未解释失败；回滚演练有可复现记录；工作区只含预期报告更新。

**Commit:**

```bash
git add docs/acceptance docs/operations/cutover-and-rollback.md evals/baselines
git commit -m "docs: record integration acceptance and rollback evidence"
```

## 15. 推荐执行顺序与里程碑

1. **M0 接管可控（Task 1—2）**：来源可追溯、密钥完成轮换、权限缺口封堵。
2. **M1 黑盒闭环（Task 3—5）**：系统1通过版本化内部契约调用系统2，旧路径可回滚。
3. **M2 模型与 OCR 质量（Task 6—7）**：Qwen3.8-Flash 选择链路闭环，PaddleOCR-VL 主 OCR，疑难页有门控兜底。
4. **M3 生产可靠性（Task 8—9）**：任务可恢复、产物可追溯、统一 UI 可操作。
5. **M4 Agent 与私有云（Task 10—12）**：受控工具编排、私有云部署、灰度和回滚验收。

Task 1—5 是首个可交付版本，先完成接口级融合；Task 6—9 才是质量与业务闭环；Task 10 不能提前于权限、持久化和审计基础。每个里程碑均应使用独立版本标签，未通过当前里程碑验收不得删除旧链路。
