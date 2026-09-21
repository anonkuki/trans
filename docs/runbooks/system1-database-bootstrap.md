# System 1 database bootstrap

The delivered SQL files are local runtime inputs. They may contain account,
configuration, or business data and must never be committed to this public
repository.

## Delivered files

| File | SHA-256 | Purpose |
|---|---|---|
| `nacos.sql` | `F99CB26A67F930DA59972963FA9C8602F90821DECBBC90C83F926B9010C5007C` | Nacos configuration database |
| `svaai.sql` | `0C59B9401AF07989EAB0FFFF5B34DAE4FB81885D54F7457A241FB2DA92742090` | Candidate System 1 application database |
| `svaai_zs.sql` | `9D1D0D9BF1469008F7888717367EE25C1136DFF61199A31609DCABF4A61672EC` | Candidate System 1 application database variant |

The owner must choose between `svaai.sql` and `svaai_zs.sql` after confirming
which environment the suffix represents. Do not import both into the same
schema and do not infer that the larger file is newer.

## Safe local procedure

1. Set `SYSTEM1_DELIVERY_DATABASE_DIR` to the delivered database directory.
2. Recalculate SHA-256 and compare it with the table above.
3. Create disposable local MySQL schemas for Nacos and the application.
4. Import `nacos.sql` into the Nacos schema.
5. Import exactly one owner-approved application dump into an empty application schema.
6. Point only local Nacos and application profiles at those schemas.
7. Start Nacos, Redis, the required Java services, the document engine, and then the frontend.
8. Verify login and one PDF OCR job before retaining the local schemas.

Example checksum command:

```powershell
Get-ChildItem -LiteralPath $env:SYSTEM1_DELIVERY_DATABASE_DIR -File |
  Get-FileHash -Algorithm SHA256
```

## Rollback

Stop the local services and drop only the disposable schemas created for this
bootstrap. Keep the delivered SQL files unchanged so their checksums remain
valid. Never delete or modify the owner's delivery copy as part of rollback.
