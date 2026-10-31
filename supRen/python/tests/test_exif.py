import datetime as dt
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from supren.exif import _parse_date


class TestParseDate(unittest.TestCase):
    def test_normal_date(self):
        self.assertEqual(_parse_date("2013:04:01 11:26:07"), dt.datetime(2013, 4, 1, 11, 26, 7))

    def test_date_with_timezone_suffix(self):
        self.assertEqual(_parse_date("2021:08:01 12:00:00+02:00"), dt.datetime(2021, 8, 1, 12, 0, 0))
        self.assertEqual(_parse_date("2021:08:01 12:00:00-04:00"), dt.datetime(2021, 8, 1, 12, 0, 0))

    def test_subsecond_suffix(self):
        self.assertEqual(_parse_date("2021:08:01 12:00:00.123"), dt.datetime(2021, 8, 1, 12, 0, 0))

    def test_zero_date_is_none(self):
        self.assertIsNone(_parse_date("0000:00:00 00:00:00"))

    def test_none_and_empty(self):
        self.assertIsNone(_parse_date(None))
        self.assertIsNone(_parse_date(""))

    def test_garbage_is_none(self):
        self.assertIsNone(_parse_date("not a date"))


if __name__ == "__main__":
    unittest.main()
