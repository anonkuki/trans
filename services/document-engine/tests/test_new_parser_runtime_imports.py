import unittest


class NewParserRuntimeImportsTest(unittest.TestCase):
    def test_runtime_reuses_vendored_pdfminer_font_data(self):
        from babeldoc.format.pdf.new_parser.runtime.font_data_runtime import (
            FontMetricsDB,
        )
        from babeldoc.format.pdf.new_parser.runtime.font_encoding_runtime import (
            EncodingDB,
        )

        self.assertTrue(EncodingDB.std2unicode)
        self.assertTrue(FontMetricsDB.get_metrics("Helvetica"))


if __name__ == "__main__":
    unittest.main()
