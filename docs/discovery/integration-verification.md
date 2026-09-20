# PDF OCR integration verification

Date: 2026-09-21

## Passed

- Python document-engine suite: `19 passed`.
- Internal API contract covers invalid token, invalid file, versioned response,
  upload cleanup, provider job isolation, and `document.v1` loading.
- Java OCR client isolation harness: `3 passed`; verifies the internal header,
  multipart call, schema rejection, canonical content/reading order, empty-page
  boundaries, timeout propagation, and DOCX output.
- Live PaddleOCR smoke test: a generated one-page PDF completed remotely and
  normalized as `normalized_document_v1` version `1.1`, one page, one block.
- System 1 frontend production build: passed; Vite produced `dist-prod`.
- Both System 1 YAML entrypoints parse successfully, and service profiles now
  default to `local` while remaining externally selectable.
- Known local Qwen/Paddle secret values copied from the two existing `.env`
files: zero matches in the new repository.

The integrated runtime exposes only `/health`, API documentation, and the
protected internal OCR endpoint. System 2's legacy task/image APIs and workers
cannot be enabled by configuration.

## Inherited platform blocker

The full System 1 Maven reactor does not compile from the delivered snapshot.
After restoring four missing API-log contracts from its upstream framework
shape, compilation advances to `sva-spring-boot-starter-security` and then
fails because additional delivered source files are absent, including
`LoginUser`, `LoginUserRequestInterceptor`, `OperateLogCommonApi`, and an
operate-log service package. This failure predates and is independent of the
OCR client; the isolated harness compiles and tests the exact changed Java
production sources.

No live Qwen translation call is part of this OCR-stage acceptance. The Qwen
key/model are now server-side environment settings in both System 1 entrypoint
configurations; translation remains System 1's existing responsibility.
