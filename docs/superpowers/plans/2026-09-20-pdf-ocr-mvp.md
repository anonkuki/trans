# System 1 + PDF OCR Engine MVP Plan

**Goal:** Keep `sva-cloud` and `sva-ui` as the only business system. Reuse
`pdf-translation` only as a protected internal PDF recognition engine and feed
its structured OCR result into System 1's existing document translation flow.

## Scope

- Import the three inherited source trees into a clean monorepo without local
  environments, generated data, build output, Git metadata, or credentials.
- Add `POST /internal/v1/ocr/recognize` to the Python document engine.
- Require a server-side internal token and a server-side OCR provider; callers
  cannot submit provider URLs, Paddle credentials, or model credentials.
- Return the existing `document.v1` structure with request/task provenance.
- Change System 1's existing `ConvertByPythonHelper` seam to consume that
  structure and create an intermediate DOCX for its established translation
  service. System 1 remains owner of users, tasks, models, translation, and
  deliverables.
- Reuse System 1's existing Qwen/OpenAI-compatible model factory. Provide only
  environment-variable configuration examples; never copy a live key.

## Test-first checkpoints

- [x] Unauthorized Python OCR calls fail.
- [x] Invalid/non-PDF uploads fail before invoking OCR.
- [x] An injected fake OCR runner produces a versioned `document.v1` response.
- [x] The production runner builds an OCR-only provider spec and reads the
      normalized document artifact.
- [x] The Java client sends the internal token and accepts `document.v1`.
- [x] The Java converter preserves ordered text blocks and page boundaries.
- [x] Existing PDF fallback behavior remains available when the engine fails.
- [x] Python focused tests, Java focused tests/buildable module, frontend build,
      secret scan, and repository hygiene checks are recorded.
