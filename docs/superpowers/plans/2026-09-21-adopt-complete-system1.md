# Adopt Complete System 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Replace the incomplete System 1 snapshots in `apps/platform` and `apps/web` with the delivered 2026-09-18 source while preserving System 2 as an internal PDF/OCR sidecar.

**Architecture:** The delivered `sva-cloud` and `sva-ui` repositories become the authoritative business platform snapshots. `services/document-engine` remains independently deployable, and the Java AI module calls only its versioned internal OCR endpoint. Raw database dumps remain local runtime inputs and are never committed to the public GitHub repository.

**Tech Stack:** Java 17/21, Maven, Spring Boot 3.5, Vue 3, TypeScript, Vite, pnpm, Python 3.11, FastAPI, pytest.

---

### Task 1: Freeze the delivered-source provenance

**Files:**
- Create: `docs/discovery/complete-system1-baseline.md`
- Modify: `docs/discovery/source-inventory.md`

- [x] **Step 1: Record both source repositories**

Record backend commit `92d45cd5aa8af25ee2334f2bb4425da0ea6098a9`, frontend commit `92728a79b8578e0554a78dbd60fa8f384cce8081`, their `dev` branches, and every delivered working-tree modification.

- [x] **Step 2: Record database handling**

Document `nacos.sql`, `svaai.sql`, and `svaai_zs.sql` as local-only runtime inputs. Record SHA-256 and size, but do not copy data dumps into Git.

- [x] **Step 3: Verify provenance paths**

Run:

```powershell
git -C D:\codeC\python\翻译系统\2026-09-18\sva-cloud status --short --branch
git -C D:\codeC\python\翻译系统\2026-09-18\sva-ui status --short --branch
```

Expected: both repositories remain unchanged and their delivered modifications are still present.

- [x] **Step 4: Commit provenance**

```powershell
git add docs/discovery/complete-system1-baseline.md docs/discovery/source-inventory.md
git commit -m "docs: freeze complete system1 source baseline"
```

### Task 2: Adopt the complete backend snapshot

**Files:**
- Replace tracked snapshot: `apps/platform/**`
- Preserve for later TDD port: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/helper/ConvertByPythonHelperTest.java`

- [x] **Step 1: Export the current OCR client test to D: temporary storage**

```powershell
Copy-Item apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/helper/ConvertByPythonHelperTest.java D:\Temp\ConvertByPythonHelperTest.java
```

- [x] **Step 2: Replace the backend snapshot from the delivered Git working tree**

Use the delivered repository's tracked file list plus its 81 physically delivered source files hidden by the erroneous unanchored `LOG*` rule. Remove the `LOG*` rule in the integration snapshot so logger packages, `LoginUser.java`, logback resources, and SQL migration sources can be tracked. Exclude `.git`, build output, IDE files, runtime logs, flattened Maven output, and the unsafe gateway `AccessLog`/`AccessLogFilter` pair that records complete credentials and response bodies.

- [x] **Step 3: Confirm critical complete-source paths exist**

Run:

```powershell
Test-Path apps/platform/sva-framework/sva-spring-boot-starter-security/src/main/java/cn/iocoder/sva/framework/security/core/LoginUser.java
Test-Path apps/platform/sva-module-system/sva-module-system-server/src/main/java/cn/iocoder/sva/module/system/service/logger/LoginLogServiceImpl.java
Test-Path apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/controller/admin/translation/TranTextController.java
```

Expected: all return `True`.

- [x] **Step 4: Compile the authoritative backend before OCR adaptation**

```powershell
mvn -q -pl sva-server -am -DskipTests package
```

Expected: exit code 0, or a separately recorded delivered-baseline failure not caused by OCR adaptation.

- [x] **Step 5: Commit the backend baseline**

```powershell
git add apps/platform
git commit -m "chore: adopt complete system1 backend snapshot"
```

### Task 3: Reapply the PDF/OCR adapter with TDD

**Files:**
- Test: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/helper/ConvertByPythonHelperTest.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/helper/ConvertByPythonHelper.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/config/TransDocProperties.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/resources/application.yaml`
- Modify: `apps/platform/sva-server/src/main/resources/application.yaml`

- [x] **Step 1: Restore the OCR contract test only**

The test requires a PDF-only request to `POST /internal/v1/ocr/recognize`, `X-Internal-Token` authentication, JSON block ordering, and rejection of invalid or non-PDF inputs.

- [x] **Step 2: Run the test and verify RED**

```powershell
mvn -q -pl sva-module-ai/sva-module-ai-server -am -Dtest=ConvertByPythonHelperTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL because the delivered helper still uses the legacy `/convert` DOCX contract.

- [x] **Step 3: Port the smallest adapter implementation**

Port only the tested client behavior from the prior integration: endpoint URL, internal token header, timeout settings, PDF validation, JSON response parsing, and stable block reading order. Keep the delivered `PdfTranslationServiceImpl` and all other System 1 business logic unchanged.

- [x] **Step 4: Run the test and verify GREEN**

Run the same Maven command. Expected: all `ConvertByPythonHelperTest` cases pass.

- [x] **Step 5: Commit the OCR adapter**

```powershell
git add apps/platform/sva-module-ai apps/platform/sva-server
git commit -m "feat: connect complete system1 to pdf ocr sidecar"
```

### Task 4: Adopt the complete frontend snapshot

**Files:**
- Replace tracked snapshot: `apps/web/**`

- [x] **Step 1: Replace the frontend using the delivered tracked file list**

Copy the delivered Git working-tree versions, including its current `src/views/ai/translation/index/index.vue` modification. Exclude `.git`, `node_modules`, build output, ZIP artifacts, and IDE metadata.

- [x] **Step 2: Verify delivered feature entrypoints**

Run:

```powershell
Test-Path apps/web/src/views/ai/translation/word/index.vue
Test-Path apps/web/src/views/ai/calendar/calendar.vue
Test-Path apps/web/src/views/system/extlink/index.vue
```

Expected: all return `True`.

- [x] **Step 3: Install dependencies on D: and run checks**

```powershell
$env:PNPM_HOME='D:\Caches\pnpm'
$env:PNPM_STORE_DIR='D:\Caches\pnpm-store'
pnpm install --frozen-lockfile
pnpm typecheck
pnpm build:dev
```

Expected: each command exits 0, or delivered-baseline failures are recorded separately.

- [x] **Step 4: Commit the frontend baseline**

```powershell
git add apps/web
git commit -m "chore: adopt complete system1 frontend snapshot"
```

### Task 5: Protect local database and secret boundaries

**Files:**
- Modify: `.gitignore`
- Create: `docs/runbooks/system1-database-bootstrap.md`
- Create: `verification/check_complete_system1_baseline.ps1`

- [x] **Step 1: Add a failing repository-boundary check**

The script must fail when a raw `*.sql` dump from `2026-09-18/数据库`, nested `.git`, plaintext API key, or committed `.env.local` is present in the integration repository.

- [x] **Step 2: Run it and verify RED against a temporary forbidden fixture**

Create the fixture under `D:\Temp`, pass it to the checker, and expect exit code 1. Remove only that temporary fixture afterward.

- [x] **Step 3: Implement the repository-boundary checker and run GREEN**

Run:

```powershell
powershell -ExecutionPolicy Bypass -File verification/check_complete_system1_baseline.ps1
```

Expected: `BASELINE_BOUNDARY=PASS`.

- [x] **Step 4: Document local database initialization**

Document dump selection, checksum validation, local MySQL/Nacos import order, rollback, and the rule that dumps never enter Git.

- [x] **Step 5: Commit safeguards**

```powershell
git add .gitignore docs/runbooks/system1-database-bootstrap.md verification/check_complete_system1_baseline.ps1
git commit -m "docs: define private database bootstrap boundary"
```

### Task 6: Integrated verification and publication

**Files:**
- Modify: `docs/discovery/integration-verification.md`
- Modify: `README.md`

- [x] **Step 1: Verify backend compilation and focused OCR tests**

```powershell
mvn -q -pl sva-server -am -DskipTests package
mvn -q -pl sva-module-ai/sva-module-ai-server -am -Dtest=ConvertByPythonHelperTest -Dsurefire.failIfNoSpecifiedTests=false test
```

- [x] **Step 2: Verify the document engine**

```powershell
python -m pytest services/document-engine/tests/test_internal_ocr_api.py services/document-engine/tests/test_paddle_auth_header.py -q
```

- [x] **Step 3: Verify the frontend and repository boundary**

```powershell
pnpm --dir apps/web typecheck
pnpm --dir apps/web build:dev
powershell -ExecutionPolicy Bypass -File verification/check_complete_system1_baseline.ps1
git diff --check
```

- [x] **Step 4: Record exact pass/fail evidence**

Update `integration-verification.md` and `README.md` with only freshly executed results. Clearly separate inherited baseline failures from integration regressions.

- [x] **Step 5: Commit verification evidence**

```powershell
git add docs/discovery/integration-verification.md README.md
git commit -m "docs: record complete system1 integration verification"
```

- [x] **Step 6: Request code review, push the feature branch, and verify the remote SHA**

Push only after no unresolved Critical or Important review findings remain. Do not merge into `main` if the complete test baseline is red; publish the feature branch with the exact remaining failures instead.
