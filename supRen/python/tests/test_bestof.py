import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from supren import bestof
from supren import config as config_mod
from supren.exif import Metadata

REAL_CONFIG = Path(__file__).resolve().parents[1] / "supren.json"


class TestBestof(unittest.TestCase):
    def setUp(self):
        self.cfg = config_mod.load(REAL_CONFIG)
        self.src_tmp = tempfile.TemporaryDirectory()
        self.dest_tmp = tempfile.TemporaryDirectory()
        self.src = Path(self.src_tmp.name)
        self.dest = Path(self.dest_tmp.name)

    def tearDown(self):
        self.src_tmp.cleanup()
        self.dest_tmp.cleanup()

    def test_rated_file_is_planned_for_copy_flattened_into_top_subfolder(self):
        sub = self.src / "2026_Holiday"
        sub.mkdir()
        p = sub / "20200102_E-6D_0001.jpg"
        p.write_bytes(b"x")
        fake_meta = {p: Metadata(path=p, camera_model="Canon EOS 6D", picture_date=None, rating=2)}
        with mock.patch("supren.bestof.exif.read_batch", return_value=fake_meta):
            actions = bestof.plan_bestof_copy(self.src, self.dest, self.cfg)
        self.assertEqual(len(actions), 1)
        self.assertEqual(actions[0].status, "copy")
        self.assertEqual(actions[0].dest, self.dest / "2026_Holiday" / "20200102_E-6D_0001.jpg")

    def test_unrated_file_is_not_copied(self):
        p = self.src / "20200102_E-6D_0001.jpg"
        p.write_bytes(b"x")
        fake_meta = {p: Metadata(path=p, camera_model="Canon EOS 6D", picture_date=None, rating=0)}
        with mock.patch("supren.bestof.exif.read_batch", return_value=fake_meta):
            actions = bestof.plan_bestof_copy(self.src, self.dest, self.cfg)
        self.assertEqual(len(actions), 0)

    def test_subfolder_filter_excludes_non_matching_paths(self):
        sub2025 = self.src / "2025_Trip"
        sub2026 = self.src / "2026_Trip"
        sub2025.mkdir()
        sub2026.mkdir()
        p2025 = sub2025 / "a.jpg"
        p2026 = sub2026 / "b.jpg"
        p2025.write_bytes(b"x")
        p2026.write_bytes(b"x")
        fake_meta = {
            p2025: Metadata(path=p2025, camera_model="Canon EOS 6D", picture_date=None, rating=3),
            p2026: Metadata(path=p2026, camera_model="Canon EOS 6D", picture_date=None, rating=3),
        }
        with mock.patch("supren.bestof.exif.read_batch", return_value=fake_meta):
            actions = bestof.plan_bestof_copy(self.src, self.dest, self.cfg, subfolder_filter="2026")
        self.assertEqual(len(actions), 1)
        self.assertEqual(actions[0].src, p2026)

    def test_apply_copies_and_creates_dirs(self):
        sub = self.src / "2026_Holiday"
        sub.mkdir()
        p = sub / "a.jpg"
        p.write_bytes(b"photo bytes")
        fake_meta = {p: Metadata(path=p, camera_model="Canon EOS 6D", picture_date=None, rating=5)}
        with mock.patch("supren.bestof.exif.read_batch", return_value=fake_meta):
            actions = bestof.plan_bestof_copy(self.src, self.dest, self.cfg)
        copied = bestof.apply_bestof_copy(actions)
        self.assertEqual(copied, 1)
        dest_file = self.dest / "2026_Holiday" / "a.jpg"
        self.assertTrue(dest_file.exists())
        self.assertEqual(dest_file.read_bytes(), b"photo bytes")


if __name__ == "__main__":
    unittest.main()
