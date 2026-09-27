from __future__ import annotations

from pathlib import Path

import pymupdf as fitz

from native_text_extractor import try_extract_native_text
from internal_ocr_api import _validate_document_v1


def _write_pdf(path: Path, page_texts: list[str]) -> None:
    document = fitz.open()
    for text in page_texts:
        page = document.new_page(width=595, height=842)
        if text:
            page.insert_textbox(fitz.Rect(50, 50, 545, 790), text, fontsize=11)
    document.save(path)
    document.close()


def test_extracts_native_text_in_page_and_reading_order(tmp_path: Path) -> None:
    pdf_path = tmp_path / "native.pdf"
    first = "Quality risk management applies throughout the product lifecycle. " * 3
    second = "Continuous improvement must be supported by documented evidence. " * 3
    _write_pdf(pdf_path, [first, second])

    document = try_extract_native_text(pdf_path, request_id="native-1", min_chars_per_page=40)

    assert document is not None
    _validate_document_v1(document)
    assert document["page_count"] == 2
    assert document["derived"]["recognition_route"] == "native_text"
    assert "Quality risk management" in document["pages"][0]["blocks"][0]["content"]["text"]
    assert "Continuous improvement" in document["pages"][1]["blocks"][0]["content"]["text"]


def test_returns_none_for_scanned_or_blank_pdf(tmp_path: Path) -> None:
    pdf_path = tmp_path / "scan.pdf"
    _write_pdf(pdf_path, [""])

    assert try_extract_native_text(pdf_path, request_id="scan-1", min_chars_per_page=20) is None
