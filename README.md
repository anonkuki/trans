# Trans Translation Platform

This repository makes System 1 the only user-facing translation platform:

- `apps/platform`: System 1 Spring Cloud backend; owns users, permissions,
  translation tasks, model selection, translation, and deliverables.
- `apps/web`: System 1 Vue frontend.
- `services/document-engine`: System 2 retained as an internal PDF recognition
  engine. Its new versioned endpoint returns `document.v1`; it does not own
  System 1 users or translation tasks.
- `contracts`: the boundary shared by the two services.

## PDF flow

`System 1 upload -> internal OCR API -> document.v1 -> intermediate DOCX ->
System 1 Qwen translation pipeline`

The Python service receives only the PDF and internal authentication. Paddle
credentials stay in the Python service. Qwen credentials stay in System 1.
Copy `.env.example` to a local secret store or inject the variables through the
private-cloud platform; never commit the populated file.

## Local verification

Use the D-drive environments prepared for the inherited projects:

```powershell
& 'D:\codeC\python\翻译系统\pdf-translation\.venv\Scripts\python.exe' `
  -m pytest services/document-engine/tests -q

$env:JAVA_HOME = 'D:\Caches\tools\temurin-17\jdk-17.0.20.1+1'
$env:MAVEN_OPTS = '-Dmaven.repo.local=D:\Caches\maven'
& 'D:\Caches\tools\apache-maven-3.9.16\bin\mvn.cmd' `
  -f verification/java-ocr-client/pom.xml test

pnpm --dir apps/web run build:prod
```

Before pushing, also parse both changed YAML entrypoints, run a staged secret
scan, and run `git diff --cached --check`. The exact latest results are recorded
in `docs/discovery/integration-verification.md`.

The inherited System 1 snapshot has additional missing framework source files
beyond this integration. `docs/discovery/dependency-installation-baseline.md`
records that baseline separately from the OCR integration.
