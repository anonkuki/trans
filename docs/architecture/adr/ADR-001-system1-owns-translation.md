# ADR-001: System 1 owns translation; System 2 is the PDF engine

## Decision

`sva-cloud` and `sva-ui` remain the sole platform. The Python application is an
internal, replaceable document engine. Its integration surface is the protected
`POST /internal/v1/ocr/recognize` endpoint and the versioned `document.v1`
schema.

System 1 converts ordered OCR blocks into an intermediate DOCX and then uses
its existing document translation service and model factory. Qwen3.8-Flash is
configured in System 1 through server-side environment variables. The browser
never submits model URLs, OCR URLs, or credentials.

## Consequences

- Existing System 1 accounts, permissions, task state, and UI remain canonical.
- PaddleOCR-VL may be replaced without changing the web application.
- OCR and translation failures are separable and auditable by request ID.
- Legacy System 2 task/image endpoints and background workers remain in source
  history but are not exposed or startable in the integrated runtime.
- The first MVP preserves reading order and page boundaries; full table/image
  layout reconstruction remains a later rendering-stage enhancement.
