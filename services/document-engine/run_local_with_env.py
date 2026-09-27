"""Start the local document engine from an existing untracked .env file."""
from __future__ import annotations

import argparse
import os
from pathlib import Path


def load_environment(path: Path) -> None:
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in {"'", '"'}:
            value = value[1:-1]
        os.environ.setdefault(key.strip(), value)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--env-file", required=True, type=Path)
    parser.add_argument("--internal-token", required=True)
    parser.add_argument("--port", type=int, default=8031)
    args = parser.parse_args()
    load_environment(args.env_file.resolve())
    os.environ["PDF_ENGINE_INTERNAL_TOKEN"] = args.internal_token
    os.environ.setdefault("PDF_ENGINE_NATIVE_TEXT_ENABLED", "true")
    os.environ.setdefault("PDF_ENGINE_WORK_ROOT", "D:/Temp/trans-document-engine")

    import uvicorn

    uvicorn.run("app:app", host="127.0.0.1", port=args.port, log_level="info")


if __name__ == "__main__":
    main()
