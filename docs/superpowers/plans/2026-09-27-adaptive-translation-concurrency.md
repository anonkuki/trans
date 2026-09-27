# Adaptive Translation Concurrency Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the fixed per-document translation pool with configurable 12/10/8 adaptive concurrency and add a process-wide Qwen request cap that can be reused on the work computer.

**Architecture:** A pure `TranslationConcurrencyPolicy` selects a per-document worker count from translatable segment count and character count, while preserving the existing fixed-concurrency fallback. A reusable `TranslationConcurrencyLimiter` wraps the actual `ChatModel.call` so concurrent documents share one process-wide permit pool. DOCX and Excel use the same policy; all settings are environment-driven and require no production credentials in source control.

**Tech Stack:** Java 17, Spring Boot configuration properties, `ExecutorService`, fair `Semaphore`, JUnit 5, Maven.

---

### Task 1: Adaptive per-document policy

**Files:**
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/config/TranslationConcurrencyPolicy.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/config/TransDocProperties.java`
- Test: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/tran/config/TranslationConcurrencyPolicyTest.java`

- [x] **Step 1: Write failing policy tests**

Cover small `12`, medium `10`, large `8`, character-triggered large classification, task-count capping, global-limit capping, and the legacy fixed-concurrency fallback.

- [x] **Step 2: Run the focused test and verify RED**

Run from `apps/platform`:

```powershell
mvn -Dtest=TranslationConcurrencyPolicyTest -Dsurefire.failIfNoSpecifiedTests=false -pl sva-module-ai/sva-module-ai-server -am test
```

Expected: compilation failure because `TranslationConcurrencyPolicy` and the adaptive properties do not exist.

- [x] **Step 3: Add the minimal policy and properties**

Use these defaults:

```text
adaptive enabled=false by default; the work computer explicitly enables it after test-environment configuration
small: <=30 segments AND <=8000 characters -> 12
medium: <=150 segments AND <=50000 characters -> 10
large: otherwise -> 8
global maximum=24
```

Clamp the result to at least `1`, at most the number of tasks, and at most the global limit. If adaptive mode is disabled, use the existing `concurrency` property.

- [x] **Step 4: Run the focused test and verify GREEN**

Expected: all policy tests pass.

### Task 2: Process-wide model request limiter

**Files:**
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/config/TranslationConcurrencyLimiter.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/LlmClientServiceImpl.java`
- Test: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/tran/config/TranslationConcurrencyLimiterTest.java`

- [x] **Step 1: Write a failing concurrency test**

Start more workers than available permits, block them inside the protected operation, and assert that the maximum observed active operation count never exceeds the configured limit.

- [x] **Step 2: Run the focused test and verify RED**

Expected: compilation failure because the limiter does not exist.

- [x] **Step 3: Implement the fair semaphore wrapper**

`TranslationConcurrencyLimiter.execute(Callable<T>)` must acquire before executing, release in `finally`, and restore the interrupt flag if permit acquisition is interrupted. `LlmClientServiceImpl` lazily creates one limiter from `transdoc.global-concurrency` and wraps only the remote `ChatModel.call`.

- [x] **Step 4: Run limiter and language-validation tests**

Expected: limiter tests pass and the existing URL/email language-validation tests remain green.

### Task 3: Apply adaptive selection to DOCX and Excel

**Files:**
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/DocxTranslationServiceImpl.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/ExcelTranslationServiceImpl.java`
- Test: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/tran/config/TranslationConcurrencyPolicyTest.java`

- [x] **Step 1: Compute workload before creating each executor**

DOCX sums nonblank paragraph lengths. Excel sums unique translatable strings. Both call the shared policy with task count and character count and log the selected worker count.

- [x] **Step 2: Run focused translation tests**

Expected: policy, failure/retry, table conversion, Qwen compatibility, and language-validation tests all pass.

### Task 4: Configuration and work-computer handoff

**Files:**
- Modify: `.env.example`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/resources/application.yaml`
- Create: `docs/translation-performance-plan.md`

- [x] **Step 1: Expose every tuning value as an environment variable**

Add `TRANSDOC_ADAPTIVE_CONCURRENCY_ENABLED`, small/medium/large concurrency, segment and character thresholds, and `TRANSDOC_GLOBAL_CONCURRENCY` without adding credentials.

- [x] **Step 2: Write the deployment-safe performance plan**

Document the measured baseline, recommended first production values, monitoring fields, rollback switch (`adaptive=false` plus fixed concurrency `6`), staged rollout, and acceptance thresholds.

### Task 5: Verify and publish

**Files:**
- Verify all files above.

- [x] **Step 1: Run focused Java regression tests**

Expected: zero failures.

- [x] **Step 2: Run repository boundary and secret checks**

Expected: `BASELINE_BOUNDARY=PASS`, `BOUNDARY_CHECK_TEST=PASS`, no credential matches, and `git diff --check` clean.

- [ ] **Step 3: Commit and push the existing feature branch**

Push to `origin/integration/system1-pdf-ocr`; do not merge to `main`, create production configuration, or access production infrastructure from this computer.
