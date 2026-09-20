from __future__ import annotations

import json
from pathlib import Path

from fastapi import FastAPI
from fastapi.testclient import TestClient
from internal_ocr_api import InternalOcrSettings
from internal_ocr_api import ProviderOcrRecognitionRunner
from internal_ocr_api import _validate_document_v1
from internal_ocr_api import create_internal_ocr_router


class FakeRunner:
    def __init__(self) -> None:
        self.calls: list[tuple[Path, str, str]] = []

    def recognize(self, pdf_path: Path, *, request_id: str, provider: str) -> dict:
        self.calls.append((pdf_path, request_id, provider))
        assert pdf_path.read_bytes().startswith(b"%PDF")
        return {
            "schema": "normalized_document_v1",
            "schema_version": "1.1",
            "document_id": request_id,
            "source": {"provider": provider},
            "page_count": 1,
            "pages": [
                {
                    "page_index": 0,
                    "width": 595,
                    "height": 842,
                    "unit": "pt",
                    "blocks": [
                        {
                            "block_id": "b1",
                            "page_index": 0,
                            "order": 0,
                            "type": "text",
                            "geometry": {"bbox": [0, 0, 1, 1]},
                            "content": {"kind": "text", "text": "Hello OCR"},
                            "layout_role": "paragraph",
                            "semantic_role": "body",
                            "structure_role": "unknown",
                            "policy": {"translate": True, "translate_reason": ""},
                            "provenance": {
                                "provider": provider,
                                "raw_label": "",
                                "raw_sub_type": "",
                                "raw_bbox": [0, 0, 1, 1],
                                "raw_path": "",
                            },
                            "metadata": {},
                            "source": {"provider": provider},
                            "continuation_hint": {
                                "source": "",
                                "group_id": "",
                                "role": "",
                                "scope": "",
                                "reading_order": 0,
                                "confidence": 1.0,
                            },
                        }
                    ],
                }
            ],
            "derived": {},
            "markers": {},
        }


def _client(runner: FakeRunner) -> TestClient:
    app = FastAPI()
    app.include_router(
        create_internal_ocr_router(
            InternalOcrSettings(
                token="integration-secret",  # noqa: S106 - non-production test credential
                provider="paddle",
                work_root=Path("D:/Temp/trans-ocr-tests"),
                max_upload_bytes=1024,
            ),
            runner,
        )
    )
    return TestClient(app)


def test_recognize_requires_internal_token() -> None:
    runner = FakeRunner()
    response = _client(runner).post(
        "/internal/v1/ocr/recognize",
        files={"file": ("sample.pdf", b"%PDF-1.4\n", "application/pdf")},
    )

    assert response.status_code == 401
    assert runner.calls == []


def test_recognize_rejects_non_pdf_before_runner() -> None:
    runner = FakeRunner()
    response = _client(runner).post(
        "/internal/v1/ocr/recognize",
        headers={"X-Internal-Token": "integration-secret"},
        files={"file": ("note.txt", b"not a pdf", "text/plain")},
    )

    assert response.status_code == 415
    assert runner.calls == []


def test_recognize_returns_versioned_document_contract() -> None:
    runner = FakeRunner()
    response = _client(runner).post(
        "/internal/v1/ocr/recognize",
        headers={"X-Internal-Token": "integration-secret"},
        files={"file": ("sample.pdf", b"%PDF-1.4\n", "application/pdf")},
    )

    assert response.status_code == 200
    payload = response.json()
    assert payload["contract_version"] == "1.0"
    assert payload["provider"] == "paddle"
    assert payload["document"]["schema"] == "normalized_document_v1"
    assert payload["document"]["pages"][0]["blocks"][0]["content"]["text"] == "Hello OCR"
    assert len(runner.calls) == 1
    assert not runner.calls[0][0].exists(), "uploaded PDF must be removed after OCR"


def test_provider_runner_builds_isolated_job_and_reads_document(monkeypatch, tmp_path: Path) -> None:
    source_pdf = tmp_path / "source.pdf"
    source_pdf.write_bytes(b"%PDF-1.4\n")
    work_root = tmp_path / "jobs"
    runner = ProviderOcrRecognitionRunner(work_root)
    captured = {}

    def fake_run_provider(provider, args):
        captured["provider"] = provider
        captured["args"] = args
        normalized = Path(args.ocr_dir) / "normalized" / "document.v1.json"
        normalized.parent.mkdir(parents=True, exist_ok=True)
        normalized.write_text(
            """{
              "schema":"normalized_document_v1",
              "schema_version":"1.1",
              "document_id":"req-2",
              "source":{},
              "page_count":0,
              "pages":[],
              "derived":{},
              "markers":{}
            }""",
            encoding="utf-8",
        )
        return type("Result", (), {"normalized_json_path": normalized})()

    monkeypatch.setattr(runner, "_run_provider", fake_run_provider)
    document = runner.recognize(source_pdf, request_id="req-2", provider="paddle")

    assert document["schema_version"] == "1.1"
    assert captured["provider"] == "paddle"
    assert captured["args"].file_path == str(source_pdf.resolve())
    assert list(work_root.iterdir()) == [], "provider job data must be deleted after the response"


def test_recognize_rejects_malformed_document_contract() -> None:
    class MalformedRunner:
        def recognize(self, pdf_path: Path, *, request_id: str, provider: str) -> dict:
            document = FakeRunner().recognize(pdf_path, request_id=request_id, provider=provider)
            del document["pages"][0]["blocks"][0]["content"]
            return document

    response = _client(MalformedRunner()).post(
        "/internal/v1/ocr/recognize",
        headers={"X-Internal-Token": "integration-secret"},
        files={"file": ("sample.pdf", b"%PDF-1.4\n", "application/pdf")},
    )

    assert response.status_code == 502


def test_runtime_and_published_contracts_accept_toc_roles(tmp_path: Path) -> None:
    pdf_path = tmp_path / "source.pdf"
    pdf_path.write_bytes(b"%PDF-1.4\n")
    document = FakeRunner().recognize(pdf_path, request_id="req-toc", provider="paddle")
    block = document["pages"][0]["blocks"][0]
    block["layout_role"] = "toc"
    block["semantic_role"] = "table_of_contents"
    block["structure_role"] = "table_of_contents"

    _validate_document_v1(document)

    repository_root = Path(__file__).resolve().parents[3]
    schema_paths = (
        repository_root / "contracts/document-engine/document.v1.schema.json",
        repository_root / "services/document-engine/pipeline/services/document_schema/document.v1.schema.json",
    )
    for schema_path in schema_paths:
        schema = json.loads(schema_path.read_text(encoding="utf-8"))
        properties = schema["$defs"]["block"]["properties"]
        assert "toc" in properties["layout_role"]["enum"]
        assert "table_of_contents" in properties["semantic_role"]["enum"]
        assert properties["structure_role"]["type"] == "string"
