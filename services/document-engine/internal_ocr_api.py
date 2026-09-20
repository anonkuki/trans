"""Protected System 1 -> document-engine OCR integration API."""
from __future__ import annotations

import json
import logging
import os
import secrets
import shutil
import sys
import tempfile
import uuid
from dataclasses import dataclass
from pathlib import Path
from types import SimpleNamespace
from typing import Protocol

from fastapi import APIRouter
from fastapi import File
from fastapi import Header
from fastapi import HTTPException
from fastapi import UploadFile
from starlette.concurrency import run_in_threadpool

logger = logging.getLogger("document_engine.internal_ocr")


def _ensure_pipeline_import_path() -> None:
    pipeline_dir = Path(__file__).resolve().parent / "pipeline"
    pipeline_text = str(pipeline_dir)
    if pipeline_text not in sys.path:
        sys.path.insert(0, pipeline_text)


@dataclass(frozen=True)
class InternalOcrSettings:
    token: str
    provider: str
    work_root: Path
    max_upload_bytes: int = 50 * 1024 * 1024

    @classmethod
    def from_env(cls, default_work_root: str) -> InternalOcrSettings:
        max_mb = int(os.getenv("PDF_ENGINE_MAX_UPLOAD_MB", "50"))
        return cls(
            token=os.getenv("PDF_ENGINE_INTERNAL_TOKEN", ""),
            provider=os.getenv("PDF_ENGINE_OCR_PROVIDER", "paddle").strip().lower(),
            work_root=Path(os.getenv("PDF_ENGINE_WORK_ROOT", default_work_root)),
            max_upload_bytes=max_mb * 1024 * 1024,
        )


class OcrRecognitionRunner(Protocol):
    def recognize(self, pdf_path: Path, *, request_id: str, provider: str) -> dict: ...


class ProviderOcrRecognitionRunner:
    """Adapter around the existing provider registry and document.v1 normalizer."""

    def __init__(self, work_root: Path) -> None:
        self.work_root = work_root.resolve()

    def recognize(self, pdf_path: Path, *, request_id: str, provider: str) -> dict:
        self.work_root.mkdir(parents=True, exist_ok=True)
        job_parent = Path(tempfile.mkdtemp(prefix=f"ocr-{request_id}-", dir=self.work_root))
        try:
            return self._recognize_in_job(pdf_path, request_id=request_id, provider=provider, job_parent=job_parent)
        finally:
            shutil.rmtree(job_parent, ignore_errors=True)

    def _recognize_in_job(
        self,
        pdf_path: Path,
        *,
        request_id: str,
        provider: str,
        job_parent: Path,
    ) -> dict:
        _ensure_pipeline_import_path()

        from foundation.shared.job_dirs import create_job_dirs
        job_dirs = create_job_dirs(job_parent, "job")
        args = SimpleNamespace(
                provider=provider,
                file_url="",
                file_path=str(pdf_path.resolve()),
                paddle_token=os.getenv("RETAIN_PADDLE_TOKEN", ""),
                paddle_api_url=os.getenv("RETAIN_PADDLE_API_URL", ""),
                paddle_model=os.getenv("RETAIN_PADDLE_MODEL", "PaddleOCR-VL-1.6"),
                poll_interval=int(os.getenv("PDF_ENGINE_OCR_POLL_INTERVAL", "5")),
                poll_timeout=int(os.getenv("PDF_ENGINE_OCR_POLL_TIMEOUT", "1800")),
                image_reocr=False,
                target_lang="",
                local_ocr_command=os.getenv("RETAIN_LOCAL_OCR_COMMAND", ""),
                local_ocr_raw_provider=os.getenv("RETAIN_OCR_RAW_PROVIDER", "paddle"),
                job_root=str(job_dirs.root),
                source_dir=str(job_dirs.source_dir),
                ocr_dir=str(job_dirs.ocr_dir),
                translated_dir=str(job_dirs.translated_dir),
                rendered_dir=str(job_dirs.rendered_dir),
                artifacts_dir=str(job_dirs.artifacts_dir),
                logs_dir=str(job_dirs.logs_dir),
            )
        result = self._run_provider(provider, args)
        document = json.loads(result.normalized_json_path.read_text(encoding="utf-8"))
        _validate_document_v1(document)
        return document

    @staticmethod
    def _run_provider(provider: str, args: SimpleNamespace):
        from services.ocr_provider.drivers import run_registered_ocr_provider
        from services.ocr_provider.provider_pipeline import run_paddle_provider

        return run_registered_ocr_provider(
            provider,
            args,
            paddle_driver=run_paddle_provider,
        )


def _validate_document_v1(document: dict) -> None:
    _ensure_pipeline_import_path()
    from services.document_schema.validator import DocumentSchemaValidationError
    from services.document_schema.validator import validate_document_payload

    try:
        validate_document_payload(document)
    except DocumentSchemaValidationError as exc:
        raise RuntimeError(f"OCR provider returned invalid document.v1: {exc}") from exc


def create_internal_ocr_router(
    settings: InternalOcrSettings,
    runner: OcrRecognitionRunner | None = None,
) -> APIRouter:
    router = APIRouter(tags=["internal-ocr"])
    recognition_runner = runner or ProviderOcrRecognitionRunner(settings.work_root)

    @router.post(
        "/internal/v1/ocr/recognize",
        responses={
            401: {"description": "Missing or invalid internal token"},
            413: {"description": "PDF exceeds the configured size limit"},
            415: {"description": "Upload is not a PDF"},
            502: {"description": "OCR provider or document validation failed"},
            503: {"description": "Internal OCR token is not configured"},
        },
    )
    async def recognize_pdf(
        file: UploadFile = File(..., description="PDF to recognize"),  # noqa: B008
        x_internal_token: str | None = Header(default=None, alias="X-Internal-Token"),
    ) -> dict:
        if not settings.token:
            raise HTTPException(status_code=503, detail="internal OCR API is not configured")
        if not x_internal_token or not secrets.compare_digest(x_internal_token, settings.token):
            raise HTTPException(status_code=401, detail="unauthorized")

        filename = (file.filename or "").lower()
        if not filename.endswith(".pdf"):
            raise HTTPException(status_code=415, detail="only PDF uploads are accepted")
        data = await file.read(settings.max_upload_bytes + 1)
        if len(data) > settings.max_upload_bytes:
            raise HTTPException(status_code=413, detail="PDF exceeds configured size limit")
        if not data.startswith(b"%PDF"):
            raise HTTPException(status_code=415, detail="uploaded file is not a PDF")

        request_id = uuid.uuid4().hex
        settings.work_root.mkdir(parents=True, exist_ok=True)
        upload_dir = Path(tempfile.mkdtemp(prefix=f"upload-{request_id}-", dir=settings.work_root))
        pdf_path = upload_dir / "source.pdf"
        try:
            pdf_path.write_bytes(data)
            document = await run_in_threadpool(
                recognition_runner.recognize,
                pdf_path,
                request_id=request_id,
                provider=settings.provider,
            )
            _validate_document_v1(document)
            return {
                "contract_version": "1.0",
                "request_id": request_id,
                "provider": settings.provider,
                "document": document,
            }
        except HTTPException:
            raise
        except Exception as exc:  # noqa: BLE001
            logger.exception("OCR recognition failed request_id=%s", request_id)
            raise HTTPException(status_code=502, detail=f"OCR recognition failed ({request_id})") from exc
        finally:
            shutil.rmtree(upload_dir, ignore_errors=True)

    return router


__all__ = [
    "InternalOcrSettings",
    "OcrRecognitionRunner",
    "ProviderOcrRecognitionRunner",
    "create_internal_ocr_router",
]
