# Imported source inventory

| New path | Source | Provenance | Role |
|---|---|---|---|
| `apps/platform` | `SVA09969/sva-cloud` | branch `dev`, commit `92d45cd5aa8af25ee2334f2bb4425da0ea6098a9`, plus the delivered working-tree changes recorded in `complete-system1-baseline.md` | System 1 backend and business owner |
| `apps/web` | `SVA09969/sva-ui` | branch `dev`, commit `92728a79b8578e0554a78dbd60fa8f384cce8081`, plus the delivered working-tree change recorded in `complete-system1-baseline.md` | System 1 user interface |
| `services/document-engine` | `D:/codeC/python/翻译系统/pdf-translation` | branch `main`, base commit `7a2cdceca86f1f02e390408aa0defaf5addae64c`, with inherited working-tree changes listed below | Internal PDF engine |

The imported document-engine working tree already contained changes in
`babeldoc/const.py`, two vendored parser runtime files,
`pipeline/services/ocr_provider/paddle_api.py`, plus local docs and three tests.
They were preserved because they are part of the handed-over working copy.

The original no-history upload has been superseded by the complete 2026-09-18
delivery. See `complete-system1-baseline.md` for the frozen provenance and the
local-only database checksums.

Excluded from import: nested Git metadata, raw database dumps, virtual environments, Node modules,
Maven targets, frontend build output, runtime data, local tool downloads,
caches, logs, and all `.env` files. Existing literal provider keys in the
System 1 snapshot were replaced by environment placeholders before the first
source commit.
