from __future__ import annotations

import argparse
from collections import Counter
from difflib import SequenceMatcher
import json
import re
from pathlib import Path

from docx import Document


NUMERIC_TOKEN_PATTERN = re.compile(
    r"\d+(?:[.,]\d+)*(?:\s*(?:%|°C|℃|mg|mL|ml|μg|µg|ug|mcg|nm|g/min|g|U|h|min|s)(?![A-Za-z]))?",
    re.IGNORECASE,
)
IDENTIFIER_PATTERN = re.compile(
    r"(?<![A-Za-z0-9])(?=[A-Z0-9-]*[A-Z])(?=[A-Z0-9-]*\d)[A-Z0-9]+(?:-[A-Z0-9]+)+(?![A-Za-z0-9])"
)
WORD_PATTERN = re.compile(r"[A-Za-z0-9]+(?:[-'][A-Za-z0-9]+)*")


def _document_text(document: Document) -> str:
    values = [paragraph.text for paragraph in document.paragraphs]
    for table in document.tables:
        for row in table.rows:
            values.extend(cell.text for cell in row.cells)
    for section in document.sections:
        for container in (
            section.header,
            section.first_page_header,
            section.even_page_header,
            section.footer,
            section.first_page_footer,
            section.even_page_footer,
        ):
            values.extend(paragraph.text for paragraph in container.paragraphs)
    return "\n".join(values)


def _profile(path: Path) -> dict[str, object]:
    document = Document(path)
    text = _document_text(document)
    return {
        "path": str(path.resolve()),
        "text": text,
        "textCharCount": len(text),
        "cjkCharCount": len(re.findall(r"[\u3400-\u9fff]", text)),
        "paragraphCount": len(document.paragraphs),
        "nonEmptyParagraphCount": sum(1 for paragraph in document.paragraphs if paragraph.text.strip()),
        "tableCount": len(document.tables),
        "tableDimensions": [
            {"rows": len(table.rows), "columns": len(table.columns)} for table in document.tables
        ],
        "sectionCount": len(document.sections),
        "numericTokens": [match.group(0).strip() for match in NUMERIC_TOKEN_PATTERN.finditer(text)],
        "identifiers": IDENTIFIER_PATTERN.findall(text),
    }


def _normalize_numeric(value: str) -> str:
    return value.casefold().replace(" ", "").replace("µ", "μ").replace("℃", "°c")


def _preservation(
    source_values: list[str],
    candidate_values: list[str],
    normalizer=lambda value: value.casefold(),
) -> dict[str, object]:
    source = Counter(normalizer(value) for value in source_values)
    candidate = Counter(normalizer(value) for value in candidate_values)
    source_display = {normalizer(value): value for value in source_values}
    candidate_display = {normalizer(value): value for value in candidate_values}
    matched = source & candidate
    missing_counter = source - candidate
    extra_counter = candidate - source
    source_count = sum(source.values())
    candidate_count = sum(candidate.values())
    matched_count = sum(matched.values())
    return {
        "sourceCount": source_count,
        "candidateCount": candidate_count,
        "matchedCount": matched_count,
        "recall": round(matched_count / source_count, 4) if source_count else 1.0,
        "precision": round(matched_count / candidate_count, 4) if candidate_count else (1.0 if not source_count else 0.0),
        "missing": sorted(
            source_display[value]
            for value, count in missing_counter.items()
            for _ in range(count)
        ),
        "extra": sorted(
            candidate_display[value]
            for value, count in extra_counter.items()
            for _ in range(count)
        ),
    }


def _word_similarity(candidate_text: str, reference_text: str) -> dict[str, float]:
    candidate_words = [word.casefold() for word in WORD_PATTERN.findall(candidate_text)]
    reference_words = [word.casefold() for word in WORD_PATTERN.findall(reference_text)]
    candidate_set = set(candidate_words)
    reference_set = set(reference_words)
    union = candidate_set | reference_set
    jaccard = len(candidate_set & reference_set) / len(union) if union else 1.0
    sequence = SequenceMatcher(None, " ".join(candidate_words), " ".join(reference_words)).ratio()
    return {"wordJaccard": round(jaccard, 4), "normalizedSequenceRatio": round(sequence, 4)}


def _public_profile(profile: dict[str, object]) -> dict[str, object]:
    return {key: value for key, value in profile.items() if key not in {"text", "numericTokens", "identifiers"}}


def compare_docx(source_path: Path, candidate_path: Path, reference_path: Path | None = None) -> dict[str, object]:
    source = _profile(source_path)
    candidate = _profile(candidate_path)
    result: dict[str, object] = {
        "schemaVersion": 1,
        "source": _public_profile(source),
        "candidate": _public_profile(candidate),
        "numericPreservation": _preservation(
            source["numericTokens"], candidate["numericTokens"], _normalize_numeric
        ),
        "identifierPreservation": _preservation(source["identifiers"], candidate["identifiers"]),
        "structure": {
            "paragraphCountMatch": source["paragraphCount"] == candidate["paragraphCount"],
            "tableCountMatch": source["tableCount"] == candidate["tableCount"],
            "tableDimensionsMatch": source["tableDimensions"] == candidate["tableDimensions"],
            "sectionCountMatch": source["sectionCount"] == candidate["sectionCount"],
        },
    }
    if reference_path is not None:
        reference = _profile(reference_path)
        result["reference"] = _public_profile(reference)
        result["referenceSimilarity"] = _word_similarity(candidate["text"], reference["text"])
    return result


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Compare a translated DOCX with its source and optional reference.")
    parser.add_argument("--source", required=True, type=Path)
    parser.add_argument("--candidate", required=True, type=Path)
    parser.add_argument("--reference", type=Path)
    parser.add_argument("--output", required=True, type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    paths = [args.source, args.candidate] + ([args.reference] if args.reference else [])
    missing = [str(path) for path in paths if path is not None and not path.is_file()]
    if missing:
        raise FileNotFoundError(f"Missing inputs: {missing}")
    result = compare_docx(args.source, args.candidate, args.reference)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"COMPARISON_OUTPUT={args.output.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
