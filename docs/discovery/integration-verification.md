# 完整系统1与 PDF/OCR 融合验证报告

日期：2026-09-21

## 融合结论

完整交付的 `sva-cloud` 与 `sva-ui` 已分别成为 `apps/platform` 和
`apps/web` 的权威系统1源码。系统2保留为 `services/document-engine`
内部 PDF/OCR 服务，不接管用户、权限、翻译任务、模型选择或成品管理。

当前主链路为：

`系统1上传 PDF -> 内部 OCR API -> document.v1 -> 中间 DOCX -> 系统1既有翻译流程`

Java 侧只调用受 `X-Internal-Token` 保护的
`POST /internal/v1/ocr/recognize`。Paddle 凭据留在 Python 服务，Qwen
凭据留在系统1服务端，浏览器不持有这两类密钥。

## 本轮通过项

| 验证 | 结果 |
|---|---|
| 完整后端 Maven reactor 打包 | PASS，`sva-server` 及依赖模块退出码 0 |
| Java 聚焦回归 | PASS，17 tests，0 failures，0 errors |
| Python 内部 OCR 边界测试 | PASS，8 tests；另有 3 条第三方弃用警告 |
| 系统1 YAML 解析 | PASS，33 个源码目录下的 `application*.yml/yaml` 均可解析 |
| 仓库边界测试 | PASS；能拒绝数据库 dump、`.env.local`、活动或注释中的明文配置密钥 |
| 实际仓库边界检查 | PASS，`BASELINE_BOUNDARY=PASS` |
| provider key 扫描 | PASS，未发现长格式 `sk-` 明文密钥 |
| whitespace 检查 | PASS，`git diff --check` 无错误 |

Java 17 个测试由以下测试类组成：

- `ConvertByPythonHelperTest`：3
- `ApiAccessLogFilterTest`：3
- `LogRecordConstantsTest`：1
- `LoginLogServiceImplTest`：2
- `OperateLogServiceImplTest`：3
- `SsoControllerTest`：1
- `TranFileServiceImplTest`：3
- `TempFileCleanupConfigTest`：1

完整源码中原有的明文基础设施密码、第三方 API 密钥及注释密钥已改为
`${SVA_CONFIG_PASSWORD:}` 或 `${SVA_CONFIG_SECRET:}`；快递服务的 `key` 与
`customer` 也已移出源码。运行值必须通过私有云密钥管理或本地未提交环境注入。

独立代码审查发现并修复了额外安全问题：

- 飞书 OAuth 授权码、令牌和完整响应不再写入日志，令牌状态接口不再把令牌返回浏览器；
- 原交付的自定义 RSA SSO 不能证明调用方身份，现已在 Controller 与 Service 两层强制停用，
  后续必须改为标准 OIDC/OAuth2 code flow 或带 audience、nonce、防重放的签名断言；
- 翻译文件详情与下载增加当前用户所有权校验，只有记录所有者或超级管理员可访问；
- OCR 中间 DOCX 在复制后立即删除，并纳入超时清理；OCR 模式默认失败关闭，只有显式
  设置 `TRANSDOC_PYTHON_FALLBACK_ENABLED=true` 才允许降级到 PDFBox；
- Python 健康检查不再暴露宿主机绝对工作目录；
- Java 测试与示例源码中的第三方凭据已替换为测试值或环境变量。

## 尚未通过的系统1前端基线

前端依赖已按锁文件安装到 D 盘缓存，且只批准了 7 个明确列出的依赖构建脚本。
但是完整交付源码的发布门禁尚未通过：

- `vue-tsc --noEmit` 在 8 GB Node 堆下返回大量跨 BPM、商城、支付、系统管理等
  模块的既有类型错误；
- 本机 Node 24 构建在 Vite/Rollup 收尾阶段失败；
- 改用 Node 20.20.2 后，构建运行数分钟，最终因本机 16 GB 内存下的
  `memory allocation of 256 bytes failed` 退出。

因此当前可以确认“完整前端源码已纳入”，不能确认“前端可发布构建已通过”。
在该门禁恢复前，本分支不应直接合并到 `main`。

## 数据库与启动边界

交付的 `nacos.sql`、`svaai.sql`、`svaai_zs.sql` 只作为本地私有运行输入，
未复制进 Git。`svaai` 与 `svaai_zs` 的业务含义仍需源码提供方确认；确认前
不能把二者同时导入同一 schema。详见
`docs/runbooks/system1-database-bootstrap.md`。

本轮未执行真实业务数据库启动、登录和端到端 PDF 翻译验收，因为数据库变体
尚未得到负责人确认。后端编译与服务边界已经验证，但这不等同于完整业务上线验收。

## 发布约束

早期本地中间提交曾包含交付源码原始配置值，不能直接推送其提交历史。对外发布
必须从 `main` 创建干净分支，以 squash 方式只保留最终已脱敏文件快照，再重新执行
边界检查与差异扫描。前端门禁为红时，只能发布待修复的功能分支，不能宣称可上线。
