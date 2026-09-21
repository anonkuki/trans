# Complete System 1 baseline

Snapshot date: 2026-09-21

## Authoritative repositories

| Component | Source repository | Branch | HEAD | Delivered working-tree changes |
|---|---|---|---|---|
| Backend | `SVA09969/sva-cloud` | `dev` | `92d45cd5aa8af25ee2334f2bb4425da0ea6098a9` | `.gitignore`; `sva-module-ai/sva-module-ai-server/src/main/java/cn/iocoder/sva/module/ai/service/translation/TranTextServiceImpl.java` |
| Frontend | `SVA09969/sva-ui` | `dev` | `92728a79b8578e0554a78dbd60fa8f384cce8081` | `src/views/ai/translation/index/index.vue` |

The integration snapshot must use the files as delivered, including the listed
working-tree changes. The source repositories are read-only inputs: migration
must not commit, reset, clean, or otherwise modify them.

## Local-only database inputs

| File | Bytes | SHA-256 |
|---|---:|---|
| `nacos.sql` | 71,370 | `F99CB26A67F930DA59972963FA9C8602F90821DECBBC90C83F926B9010C5007C` |
| `svaai_zs.sql` | 48,574,114 | `9D1D0D9BF1469008F7888717367EE25C1136DFF61199A31609DCABF4A61672EC` |
| `svaai.sql` | 56,093,335 | `0C59B9401AF07989EAB0FFFF5B34DAE4FB81885D54F7457A241FB2DA92742090` |

These dumps are local runtime inputs, not source artifacts. They can contain
configuration, account, or business data and must not be committed to the
public integration repository. Their hashes identify the delivered copies
without exposing their contents.

## Adoption boundary

- `apps/platform` is sourced from the complete backend working tree.
- `apps/web` is sourced from the complete frontend working tree.
- `services/document-engine` remains the System 2 PDF/OCR sidecar.
- The Java adaptation is limited to the existing Python conversion seam and a
  versioned internal OCR contract.
- Raw database dumps, nested Git metadata, dependencies, caches, logs, IDE
  state, and generated build output are excluded.
