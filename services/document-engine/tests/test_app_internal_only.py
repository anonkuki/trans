from __future__ import annotations

import importlib

document_engine_app = importlib.import_module("app")


def test_default_app_exposes_internal_ocr_but_not_legacy_task_api() -> None:
    paths = document_engine_app.app.openapi()["paths"]

    assert "/internal/v1/ocr/recognize" in paths
    assert "/health" in paths
    assert "/tasks" not in paths
    assert "/images/translate" not in paths
