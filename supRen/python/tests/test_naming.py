import datetime as dt
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from supren import config as config_mod
from supren import naming

REAL_CONFIG = Path(__file__).resolve().parents[1] / "supren.json"


class TestNaming(unittest.TestCase):
    def setUp(self):
        self.cfg = config_mod.load(REAL_CONFIG)

    def test_build_new_name_matches_legacy_outfilenamegeneratortest(self):
        # Same case as the Java OutFilenameGeneratorTest: E6D, 17 July 2014, number 334.
        picture_date = dt.datetime(2014, 7, 17)
        name = naming.build_new_name(picture_date, "E-6D", 334, "jpg", self.cfg)
        self.assertEqual(name, "20140717_E-6D_0334.jpg")

    def test_match_known_patterns(self):
        cases = {
            "IMG_7405.HEIC": "7405",
            "DSC_1234.JPG": "1234",
            "P1050099.JPG": "0099",
            "DJI_0099.JPG": "0099",
        }
        for filename, expected_number in cases.items():
            with self.subTest(filename=filename):
                m = naming.match_filename(filename, self.cfg)
                self.assertIsNotNone(m, f"{filename} should match a source pattern")
                self.assertFalse(m.already_renamed)
                self.assertEqual(m.number_from_filename, expected_number)

    def test_match_already_renamed_file(self):
        m = naming.match_filename("20140717_E-6D_0334.jpg", self.cfg)
        self.assertIsNotNone(m)
        self.assertTrue(m.already_renamed)

    def test_match_unknown_pattern(self):
        m = naming.match_filename("totally_random_name_xyz.jpg", self.cfg)
        self.assertIsNone(m)

    def test_camera_code_known_and_unknown(self):
        self.assertEqual(naming.camera_code("Canon EOS 6D", self.cfg), "E-6D")
        self.assertEqual(naming.camera_code("Some Weird Camera", self.cfg), "XXXX")
        self.assertEqual(naming.camera_code(None, self.cfg), "XXXX")

    def test_camera_code_distinguishes_trailing_space_variant(self):
        # Regression check for the "USB Camera" vs "USB Camera " (trailing
        # space) collision caught while writing the config converter.
        self.assertEqual(naming.camera_code("USB Camera", self.cfg), "DRON")
        self.assertEqual(naming.camera_code("USB Camera ", self.cfg), "USBC")


if __name__ == "__main__":
    unittest.main()
