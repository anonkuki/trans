import importlib.util
import sys
import unittest
from pathlib import Path
from types import ModuleType


PIPELINE_ROOT = Path(__file__).resolve().parents[1] / "pipeline"
sys.path.insert(0, str(PIPELINE_ROOT))
sys.modules.setdefault("fitz", ModuleType("fitz"))


def _load_paddle_api_module():
    package = ModuleType("services.ocr_provider")
    package.__path__ = [str(PIPELINE_ROOT / "services" / "ocr_provider")]
    sys.modules["services.ocr_provider"] = package
    source_path = PIPELINE_ROOT / "services" / "ocr_provider" / "paddle_api.py"
    spec = importlib.util.spec_from_file_location(
        "services.ocr_provider.paddle_api", source_path
    )
    assert spec is not None and spec.loader is not None
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


paddle_api = _load_paddle_api_module()


class PaddleAuthHeaderTest(unittest.TestCase):
    def test_uses_aistudio_token_auth_scheme(self) -> None:
        self.assertEqual(
            paddle_api.build_headers("secret-value")["Authorization"],
            "token secret-value",
        )


if __name__ == "__main__":
    unittest.main()
