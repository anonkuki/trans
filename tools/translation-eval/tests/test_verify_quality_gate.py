from pathlib import Path
import sys


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from verify_quality_gate import verify_quality_gate  # noqa: E402


def _passing_comparison() -> dict:
    return {
        "candidate": {"cjkCharCount": 0, "latinSentenceResidueCount": 0},
        "numericPreservation": {"recall": 1.0, "precision": 1.0},
        "identifierPreservation": {"recall": 1.0, "precision": 1.0},
        "glossaryPreservation": {"requiredCount": 1, "matchedCount": 1, "missing": []},
        "structure": {
            "paragraphCountMatch": True,
            "tableCountMatch": True,
            "tableDimensionsMatch": True,
            "sectionCountMatch": True,
        },
    }


def test_quality_gate_passes_exact_english_draft() -> None:
    result = verify_quality_gate(_passing_comparison(), target_language="English")

    assert result == {"passed": True, "failures": []}


def test_quality_gate_lists_every_failed_invariant() -> None:
    comparison = _passing_comparison()
    comparison["candidate"]["cjkCharCount"] = 3
    comparison["numericPreservation"]["recall"] = 0.9
    comparison["identifierPreservation"]["precision"] = 0.5
    comparison["glossaryPreservation"]["missing"] = ["污染控制策略→contamination control strategy"]
    comparison["structure"]["tableDimensionsMatch"] = False

    result = verify_quality_gate(comparison, target_language="English")

    assert result["passed"] is False
    assert result["failures"] == [
        "numeric_recall_not_exact",
        "identifier_precision_not_exact",
        "missing_required_glossary_terms",
        "structure_tableDimensionsMatch_mismatch",
        "english_output_contains_cjk",
    ]


def test_quality_gate_rejects_english_sentence_residue_in_chinese_draft() -> None:
    comparison = _passing_comparison()
    comparison["candidate"]["cjkCharCount"] = 20
    comparison["candidate"]["latinSentenceResidueCount"] = 1

    result = verify_quality_gate(comparison, target_language="Chinese")

    assert result["passed"] is False
    assert result["failures"] == ["chinese_output_contains_latin_sentence"]
