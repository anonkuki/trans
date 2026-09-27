# Sample Driven Translation Quality Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the supplied pharmaceutical DOCX and PDF samples pass a reproducible editable-draft quality gate without weakening System 1 ownership, production authentication, or secret boundaries.

**Architecture:** System 1 remains the translation orchestrator. The Java document translator gains deterministic protected-token handling, whole-paragraph semantic units, glossary enforcement, mixed-language detection, and bilingual-document safety; the PDF adapter gains a native-text fast path and page-aware DOCX layout while retaining PaddleOCR for scanned pages. A local-only sample harness measures structural conservation, protected-token conservation, language residue, OCR coverage, renderability, and page-by-page visual defects.

**Tech Stack:** Java 21, Spring Boot, Apache POI, Spring AI, Python 3.12, FastAPI, PyMuPDF/PaddleOCR, pytest, JUnit 5, Poppler, bundled LibreOffice renderer.

---

## Acceptance boundary

The release gate applies to the supplied samples and produces an editable translation draft. It does not claim pixel-identical PDF reconstruction or replace pharmaceutical/GMP human approval.

- Monolingual DOCX: paragraph/table/section structure unchanged; numeric and identifier recall and precision are `1.0000`; no source-language residue outside protected names; zero failed segments; every rendered page has no clipping, overlap, broken table, or missing glyph.
- Scanned PDF representative pages: PaddleOCR remains selected; all translatable OCR blocks reach the DOCX; tables are real Word tables; wide pages/tables use readable landscape sections; zero failed translation segments; every output page is visually readable.
- Native-text PDF representative pages: native extraction is selected without a Paddle network call when the embedded text layer passes the confidence threshold; reading order and protected abbreviations/numbers are retained; zero failed segments.
- Existing Chinese-English parallel DOCX: detected as bilingual and copied unchanged instead of being translated again; no duplicate machine translation is introduced.
- Performance: quality results cannot regress; adaptive concurrency remains bounded by the global limiter.

### Task 1: Executable quality gate

**Files:**
- Modify: `tools/translation-eval/compare_documents.py`
- Modify: `tools/translation-eval/tests/test_compare_documents.py`
- Create: `tools/translation-eval/verify_quality_gate.py`
- Create: `tools/translation-eval/tests/test_verify_quality_gate.py`

- [x] **Step 1: Add failing tests for exact protected-token multiplicity, language residue, glossary terms, structural equality, and machine-readable failure reasons.**
- [x] **Step 2: Run the focused pytest files and verify RED for the missing acceptance API.**
- [x] **Step 3: Implement the smallest comparison and gate changes; keep reference similarity informational rather than a pass/fail metric.**
- [x] **Step 4: Run the focused tests and verify GREEN.**

### Task 2: Deterministic protected tokens and shared QC

**Files:**
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/common/ProtectedTokenCodec.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/tran/common/ProtectedTokenCodecTest.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/common/DocQualityChecker.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/tran/common/DocQualityCheckerTest.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/DocxTranslationServiceImpl.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/DocxTranslationServiceImplFailureTest.java`

- [x] **Step 1: Add failing tests proving repeated numbers, decimals, units, dates, percentages, document identifiers, and chemical locants survive both translation modes.**
- [x] **Step 2: Verify RED because the codec does not exist and constraint mode lacks numeric correction.**
- [x] **Step 3: Encode protected tokens before model calls, require every placeholder in the response, restore exact source text afterward, and route unresolved loss through one strict correction path.**
- [x] **Step 4: Change numeric QC from set membership to occurrence-aware comparison and verify all tests GREEN.**

### Task 3: Paragraph context and enforced terminology

**Files:**
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/DocxTranslationServiceImpl.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/DocxTranslationServiceImplFailureTest.java`

- [x] **Step 1: Add failing tests showing a multi-run paragraph is sent as one semantic unit and a missing required glossary target triggers strict correction.**
- [x] **Step 2: Verify RED against the current style-group and sentence-fragment calls.**
- [x] **Step 3: Translate the whole paragraph once while the ZIP writer preserves document formatting; require current-text-only output.**
- [x] **Step 4: Validate only terms actually present in the source; retry once with a strict term list; unresolved term loss becomes an error, not a successful segment.**
- [x] **Step 5: Run DOCX regression tests and verify GREEN.**

### Task 4: Bilingual-document safety

**Files:**
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/common/BilingualDocumentDetector.java`
- Create: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/tran/common/BilingualDocumentDetectorTest.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/tran/impl/DocxTranslationServiceImpl.java`

- [x] **Step 1: Add failing tests for alternating English-Chinese paragraphs, ordinary monolingual files, and short mixed headings.**
- [x] **Step 2: Verify RED, then implement a conservative threshold requiring multiple adjacent opposite-language pairs.**
- [x] **Step 3: For detected bilingual documents, copy the document unchanged and return an explicit successful no-op result; never invoke the model.**
- [x] **Step 4: Verify the EU GMP sample is detected and ordinary SOP files are not.**

### Task 5: Hybrid PDF extraction and readable OCR DOCX layout

**Files:**
- Create: `services/document-engine/native_text_extractor.py`
- Create: `services/document-engine/tests/test_native_text_extractor.py`
- Modify: `services/document-engine/internal_ocr_api.py`
- Modify: `services/document-engine/tests/test_internal_ocr_api.py`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/helper/ConvertByPythonHelper.java`
- Modify: `apps/platform/sva-module-ai/sva-module-ai-server/src/test/java/cn/iocoder/sva/module/ai/service/translation/helper/ConvertByPythonHelperTest.java`

- [x] **Step 1: Add failing tests: text-rich PDFs choose native extraction, image-only PDFs call Paddle, page dimensions become Word sections, and wide tables/pages become landscape.**
- [x] **Step 2: Verify RED for missing hybrid route and page-aware rendering.**
- [x] **Step 3: Extract embedded text with PyMuPDF when every content page exceeds the configured text threshold; otherwise fall back to the existing Paddle provider.**
- [x] **Step 4: Build one Word section per source page, retain page orientation, use real tables with a valid grid and repeated header rows, bound wide-table font size, and keep OCR reading order.**
- [x] **Step 5: Run Python and Java PDF adapter tests and verify GREEN.**

### Task 6: Real sample iteration and visual QA

**Files:**
- Local-only: `测试文件夹/03_运行结果/质量优化-2026-09-28/`
- Local-only: `测试文件夹/04_逐页渲染/质量优化-2026-09-28/`
- Create: `docs/translation-quality-acceptance-2026-09-28.md`

- [x] **Step 1: Restart only the local document engine with a local internal token; keep credentials out of files and Git.**
- [x] **Step 2: Run the Chinese SOP to English and English SOP to Chinese through Qwen3.8-Flash; run the scanned PDF representative pages through PaddleOCR plus Qwen; run the native-text PDF representative pages through the native route plus Qwen; run the EU GMP file through bilingual detection.**
- [x] **Step 3: Run the executable quality gate. Any failed invariant returns to the relevant earlier task with a new failing regression test.**
- [x] **Step 4: Render every produced DOCX through Word/PDF/PNG because LibreOffice is unavailable, inspect every page, and record page-level PASS or defect evidence. Any clipping, overlap, broken table, missing glyph, or untranslated residue returns to the relevant earlier task.**
- [x] **Step 5: Record scope, exact commands, metrics, known non-goals, and human-review boundary in the acceptance report.**

### Task 7: Final verification and GitHub publication

**Files:**
- Verify every changed file and the acceptance report.

- [x] **Step 1: Run all focused and relevant Java tests, all document-engine/evaluation pytest tests, repository boundary tests, `git diff --check`, and staged secret scan.**
- [x] **Step 2: Self-review the complete diff for security, fallback behavior, sample hard-coding, concurrency regressions, and unbounded token growth.**
- [x] **Step 3: Commit to `integration/system1-pdf-ocr`, push to `origin`, fetch, and require local HEAD to equal remote HEAD. Do not merge `main` or touch test/production servers.**
