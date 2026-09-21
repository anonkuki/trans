# 依赖安装与验证基线

> 历史说明：本文件记录 2026-09-20 不完整源码快照的安装结果。完整的
> 2026-09-18 系统1源码接入后，后端缺源码问题已经消失；当前权威结果请以
> `docs/discovery/integration-verification.md` 为准。

日期：2026-09-20
范围：`sva-cloud`、`sva-ui`、`pdf-translation` 以及新仓库 `trans`

## 1. 安装位置

| 项目 | 版本/方式 | 位置 |
|---|---|---|
| Java | Eclipse Temurin 21.0.12.1 LTS | `D:\Tools\temurin-jdk21\jdk-21.0.12.1+1` |
| Maven | Apache Maven 3.9.16 | `D:\Tools\apache-maven\apache-maven-3.9.16` |
| Maven 本地仓库 | 全部 37 个 reactor 模块的离线依赖 | `D:\Caches\maven` |
| Node.js | 24.11.1（本机既有） | `D:\fuzhu\node.exe` |
| pnpm | 11.19.0 | 使用 D 盘 store |
| pnpm store | 锁文件依赖 | `D:\Caches\pnpm-store` |
| 前端依赖 | `pnpm install --frozen-lockfile` | `D:\codeC\python\翻译系统\sva-ui\node_modules` |
| Python | 3.12.7（本机既有解释器） | 虚拟环境位于 D 盘项目内 |
| 系统2虚拟环境 | 100 个兼容包，含 editable BabelDOC 与 pytest | `D:\codeC\python\翻译系统\pdf-translation\.venv` |
| uv cache | Python 下载缓存 | `D:\Caches\uv` |
| 临时目录 | 安装与构建临时文件 | `D:\Temp\trans-deps` |

前端首次安装时，pnpm 11 拦截了依赖生命周期脚本。已通过 `pnpm approve-builds --all` 批准锁文件中的 7 个构建依赖，并生成 `sva-ui/pnpm-workspace.yaml` 的 `allowBuilds` 白名单：`@parcel/watcher`、`@swc/core`、`core-js`、`core-js-pure`、`es5-ext`、`esbuild`、`vue-demi`。随后冻结锁文件安装成功。

## 2. 实际验证结果

| 验证 | 结果 | 结论 |
|---|---|---|
| `pnpm install --frozen-lockfile` | PASS | 前端依赖完整安装，锁文件未重新解析 |
| 前端生产构建 | PASS | `dist-prod` 已生成 |
| 前端 `vue-tsc --noEmit` | FAIL | 默认 4 GB 堆先 OOM；改为 8 GB 后暴露大量既有类型错误 |
| 后端 `dependency:go-offline` | PASS | 37/37 reactor 模块依赖解析成功，耗时约 13 分 22 秒 |
| AI server 及依赖模块 package | FAIL | `sva-spring-boot-starter-web` 缺少公共日志 API 源码/依赖 |
| 系统2 `uv pip check` | PASS | 100 个已安装包兼容 |
| 系统2全套 `pytest -q` | FAIL | 测试收集时 `services` 模块桩污染，2 个模块导入失败 |
| 系统2测试逐文件隔离运行 | PASS | 8 个测试文件、12 个测试全部通过 |
| 融合计划结构检查 | PASS | 12 个任务、71 个检查项、12 组验证、无 TODO/TBD 占位符 |

## 3. 当前阻塞项

### 3.1 后端源码快照不完整

真实编译在 `sva-framework/sva-spring-boot-starter-web` 失败，缺少以下包/类型：

- `cn.iocoder.sva.framework.common.biz.infra.logger.ApiAccessLogCommonApi`
- `cn.iocoder.sva.framework.common.biz.infra.logger.ApiErrorLogCommonApi`
- `cn.iocoder.sva.framework.common.biz.infra.logger.dto.ApiAccessLogCreateReqDTO`
- `cn.iocoder.sva.framework.common.biz.infra.logger.dto.ApiErrorLogCreateReqDTO`

在当前 `sva-cloud` 全目录中搜索不到这些类型，且该模块 POM 没有提供对应依赖。应向系统1负责人取得原始 Git 仓库和锁定提交，不能在不了解真实契约时凭空补接口。

### 3.2 前端类型基线不通过

生产构建可以完成，但类型检查存在大量既有问题，主要包括：

- Vue 自动导入类型未生效，`ref`、`computed`、`watch`、`onMounted`、`useMessage` 等被报告为未定义；
- 个别业务 API 参数类型不匹配；
- 翻译词库 API 有未使用参数；
- 登录页引用了未导出的 `feishuLogin`，构建过程给出明确警告。

因此“可以产出前端静态文件”不等于“前端类型健康”。首次融合提交前应先恢复原仓库的生成配置和类型基线。

### 3.3 系统2测试隔离问题

系统2代码与依赖可运行，逐文件 12/12 测试通过；但一次运行全套测试时，前序测试写入的 `services` 模块桩未恢复，导致后续用例无法把真实 `services` 当作 package 导入。应先修复测试 fixture/teardown，再将全套 `pytest` 作为 CI 门禁。

## 4. 结论

依赖安装已经完成，剩余问题属于源码和测试基线，不是本机缺少依赖：

1. `sva-ui` 可生产构建，但类型门禁失败；
2. `sva-cloud` 依赖齐全，但当前快照缺源码或模块依赖，无法完整编译；
3. `pdf-translation` Python 环境可用，隔离测试全部通过，全套测试仍需修复污染；
4. 新仓库当前只保存融合计划与本基线报告，尚未导入或推送三套源码。
