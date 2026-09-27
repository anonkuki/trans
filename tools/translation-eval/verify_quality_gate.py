from __future__ import annotations

import argparse
import json
from pathlib import Path


def verify_quality_gate(comparison: dict, *, target_language: str) -> dict[str, object]:
    failures: list[str] = []
    for metric_name in ("numericPreservation", "identifierPreservation"):
        metric = comparison.get(metric_name, {})
        label = "numeric" if metric_name.startswith("numeric") else "identifier"
        if metric.get("recall") != 1.0:
            failures.append(f"{label}_recall_not_exact")
        if metric.get("precision") != 1.0:
            failures.append(f"{label}_precision_not_exact")

    glossary = comparison.get("glossaryPreservation", {})
    if glossary.get("missing"):
        failures.append("missing_required_glossary_terms")

    structure = comparison.get("structure", {})
    for name in (
        "paragraphCountMatch",
        "tableCountMatch",
        "tableDimensionsMatch",
        "sectionCountMatch",
    ):
        if structure.get(name) is not True:
            failures.append(f"structure_{name}_mismatch")

    if target_language.casefold().startswith("english"):
        if comparison.get("candidate", {}).get("cjkCharCount", 0) != 0:
            failures.append("english_output_contains_cjk")
    if target_language.casefold().startswith("chinese"):
        if comparison.get("candidate", {}).get("latinSentenceResidueCount", 0) != 0:
            failures.append("chinese_output_contains_latin_sentence")

    return {"passed": not failures, "failures": failures}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Verify an exact editable-draft translation quality gate.")
    parser.add_argument("--comparison", required=True, type=Path)
    parser.add_argument("--target-language", required=True)
    parser.add_argument("--output", required=True, type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    comparison = json.loads(args.comparison.read_text(encoding="utf-8"))
    result = verify_quality_gate(comparison, target_language=args.target_language)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"QUALITY_GATE={'PASS' if result['passed'] else 'FAIL'}")
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
