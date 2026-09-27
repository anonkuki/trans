from pathlib import Path
import sys

from docx import Document


sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from compare_documents import compare_docx  # noqa: E402


def _write_docx(path: Path, paragraphs: list[str], table_values: list[list[str]]) -> None:
    document = Document()
    for value in paragraphs:
        document.add_paragraph(value)
    if table_values:
        table = document.add_table(rows=len(table_values), cols=len(table_values[0]))
        for row_index, values in enumerate(table_values):
            for column_index, value in enumerate(values):
                table.cell(row_index, column_index).text = value
    document.save(path)


def test_compare_docx_reports_preservation_and_structure(tmp_path: Path) -> None:
    source = tmp_path / "source.docx"
    candidate = tmp_path / "candidate.docx"
    reference = tmp_path / "reference.docx"
    _write_docx(source, ["温度 20 ℃，文件 BJ-SOP-TM-0201-05。"], [["1", "2"]])
    _write_docx(candidate, ["Temperature 20 ℃, document BJ-SOP-TM-0201-05."], [["1", "2"]])
    _write_docx(reference, ["Temperature: 20 ℃. Document BJ-SOP-TM-0201-05."], [["1", "2"]])

    result = compare_docx(source, candidate, reference)

    assert result["numericPreservation"]["recall"] == 1.0
    assert result["identifierPreservation"]["recall"] == 1.0
    assert result["structure"]["tableCountMatch"] is True
    assert result["structure"]["tableDimensionsMatch"] is True
    assert result["candidate"]["cjkCharCount"] == 0
    assert result["referenceSimilarity"]["wordJaccard"] > 0.5


def test_compare_docx_exposes_missing_numbers_and_table_shape(tmp_path: Path) -> None:
    source = tmp_path / "source.docx"
    candidate = tmp_path / "candidate.docx"
    _write_docx(source, ["取样 10 mL，重复 3 次。"], [["A", "B"]])
    _write_docx(candidate, ["Take a sample and repeat 3 times."], [["A"]])

    result = compare_docx(source, candidate)

    assert result["numericPreservation"]["recall"] < 1.0
    assert "10 mL" in result["numericPreservation"]["missing"]
    assert result["structure"]["tableDimensionsMatch"] is False
    assert "referenceSimilarity" not in result


def test_numeric_preservation_normalizes_equivalent_unit_notation(tmp_path: Path) -> None:
    source = tmp_path / "source.docx"
    candidate = tmp_path / "candidate.docx"
    _write_docx(source, ["20℃；0.1ml；100µg"], [])
    _write_docx(candidate, ["20 °C; 0.1 mL; 100 μg"], [])

    result = compare_docx(source, candidate)

    assert result["numericPreservation"]["recall"] == 1.0
    assert result["numericPreservation"]["precision"] == 1.0


def test_numeric_preservation_handles_numbers_adjacent_to_labels(tmp_path: Path) -> None:
    source = tmp_path / "source.docx"
    candidate = tmp_path / "candidate.docx"
    _write_docx(source, ["pH10；750nm；5.5.3提前；R2"], [])
    _write_docx(candidate, ["pH 10; 750 nm; 5.5.3Advance; R2"], [])

    result = compare_docx(source, candidate)

    assert result["numericPreservation"]["recall"] == 1.0
    assert result["numericPreservation"]["precision"] == 1.0


def test_identifier_preservation_handles_cjk_adjacency(tmp_path: Path) -> None:
    source = tmp_path / "source.docx"
    candidate = tmp_path / "candidate.docx"
    _write_docx(source, ["依据CC-2018-0137修订"], [])
    _write_docx(candidate, ["Revised according to CC-2018-0137"], [])

    result = compare_docx(source, candidate)

    assert result["identifierPreservation"]["sourceCount"] == 1
    assert result["identifierPreservation"]["recall"] == 1.0
    assert result["identifierPreservation"]["precision"] == 1.0
