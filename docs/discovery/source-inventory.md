# Imported source inventory

| New path | Source | Provenance | Role |
|---|---|---|---|
| `apps/platform` | `D:/codeC/python/翻译系统/sva-cloud` | Delivered folder had no `.git`; exact upstream commit is unknown | System 1 backend and business owner |
| `apps/web` | `D:/codeC/python/翻译系统/sva-ui` | Delivered folder had no `.git`; exact upstream commit is unknown | System 1 user interface |
| `services/document-engine` | `D:/codeC/python/翻译系统/pdf-translation` | branch `main`, base commit `7a2cdceca86f1f02e390408aa0defaf5addae64c`, with inherited working-tree changes listed below | Internal PDF engine |

The imported document-engine working tree already contained changes in
`babeldoc/const.py`, two vendored parser runtime files,
`pipeline/services/ocr_provider/paddle_api.py`, plus local docs and three tests.
They were preserved because they are part of the handed-over working copy.

Excluded from import: nested Git metadata, virtual environments, Node modules,
Maven targets, frontend build output, runtime data, local tool downloads,
caches, logs, and all `.env` files. Existing literal provider keys in the
System 1 snapshot were replaced by environment placeholders before the first
source commit.
