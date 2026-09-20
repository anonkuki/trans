import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


class CacheDirectoryEnvironmentTest(unittest.TestCase):
    def test_babeldoc_cache_dir_overrides_home_cache(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            expected = Path(temp_dir) / "babeldoc-cache"
            env = os.environ.copy()
            env["BABELDOC_CACHE_DIR"] = str(expected)

            result = subprocess.run(
                [
                    sys.executable,
                    "-c",
                    "from babeldoc.const import CACHE_FOLDER; print(CACHE_FOLDER)",
                ],
                cwd=Path(__file__).resolve().parents[1],
                env=env,
                check=True,
                capture_output=True,
                text=True,
            )

            self.assertEqual(Path(result.stdout.strip()), expected)


if __name__ == "__main__":
    unittest.main()
