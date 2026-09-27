"""Fast-path extraction for PDFs that already contain a usable text layer."""
from __future__ import annotations

import math
import re
from pathlib import Path


def _clean_text(value: str) -> str:
    lines = [re.sub(r"[ \t]+", " ", line).strip() for line in (value or "").splitlines()]
    return "\n".join(line for line in lines if line).strip()


def _block_payload(page_index: int, order: int, bbox: list[float], text: str) -> dict:
    provider = "native_pdf_text"
    return {
        "block_id": f"native-p{page_index + 1}-b{order + 1}",
        "page_index": page_index,
        "order": order,
        "reading_order": order,
        "type": "text",
        "geometry": {"bbox": bbox},
        "content": {
            "kind": "text",
            "text": text,
            "line_texts": text.splitlines(),
            "text_flow": "flow",
        },
        "layout_role": "paragraph",
        "semantic_role": "body",
        "structure_role": "body",
        "policy": {"translate": True, "translate_reason": "native_pdf_text"},
        "provenance": {
            "provider": provider,
            "raw_label": "text",
            "raw_sub_type": "native_text_block",
            "raw_bbox": bbox,
            "raw_path": f"pages[{page_index}].blocks[{order}]",
        },
        "continuation_hint": {
            "source": "",
            "group_id": "",
            "role": "single",
            "scope": "",
            "reading_order": order,
            "confidence": 1.0,
        },
        "metadata": {"extraction": "pymupdf_text_layer"},
        "source": {"provider": provider},
    }


def try_extract_native_text(
    pdf_path: Path,
    *,
    request_id: str,
    min_chars_per_page: int = 80,
    min_page_coverage: float = 0.75,
) -> dict | None:
    """Return document.v1 for a reliable text layer, otherwise let OCR handle the file."""
    try:
        import pymupdf as fitz
    except ImportError:  # compatibility with older PyMuPDF distributions
        import fitz

    try:
        document = fitz.open(pdf_path)
    except (RuntimeError, ValueError, OSError):
        # A missing/corrupt/unreadable text layer is not fatal here; the OCR provider
        # remains the authoritative fallback and will produce the useful error if needed.
        return None
    try:
        if document.page_count == 0:
            return None
        pages: list[dict] = []
        char_counts: list[int] = []
        for page_index, page in enumerate(document):
            blocks: list[dict] = []
            for raw in page.get_text("blocks", sort=True):
                if len(raw) < 7 or int(raw[6]) != 0:
                    continue
                text = _clean_text(str(raw[4]))
                if not text:
                    continue
                bbox = [float(raw[0]), float(raw[1]), float(raw[2]), float(raw[3])]
                blocks.append(_block_payload(page_index, len(blocks), bbox, text))
            char_count = sum(len(re.sub(r"\s+", "", block["content"]["text"])) for block in blocks)
            char_counts.append(char_count)
            pages.append(
                {
                    "page_index": page_index,
                    "page": page_index + 1,
                    "width": float(page.rect.width),
                    "height": float(page.rect.height),
                    "unit": "pt",
                    "blocks": blocks,
                }
            )

        required_dense_pages = max(1, math.ceil(document.page_count * min_page_coverage))
        dense_pages = sum(count >= min_chars_per_page for count in char_counts)
        average_chars = sum(char_counts) / document.page_count
        if dense_pages < required_dense_pages or average_chars < min_chars_per_page:
            return None

        return {
            "schema": "normalized_document_v1",
            "schema_version": "1.1",
            "document_id": request_id,
            "source": {"provider": "native_pdf_text", "path": str(pdf_path)},
            "page_count": len(pages),
            "pages": pages,
            "derived": {
                "recognition_route": "native_text",
                "native_text_average_chars_per_page": average_chars,
                "native_text_dense_page_ratio": dense_pages / document.page_count,
            },
            "markers": {},
        }
    finally:
        document.close()


__all__ = ["try_extract_native_text"]
