import datetime as dt
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from supren import config as config_mod
from supren import rename
from supren.exif import Metadata

REAL_CONFIG = Path(__file__).resolve().parents[1] / "supren.json"


def _meta(path: Path, model: str | None, date: dt.datetime | None, rating: int = 0) -> Metadata:
    return Metadata(path=path, camera_model=model, picture_date=date, rating=rating)


class TestPlanRenames(unittest.TestCase):
    def setUp(self):
        self.cfg = config_mod.load(REAL_CONFIG)
        self.tmpdir = tempfile.TemporaryDirectory()
        self.root = Path(self.tmpdir.name)

    def tearDown(self):
        self.tmpdir.cleanup()

    def _touch(self, name: str) -> Path:
        p = self.root / name
        p.write_bytes(b"fake")
        return p

    def test_known_camera_and_date_is_renamed(self):
        p = self._touch("IMG_1234.JPG")
        fake_meta = {p: _meta(p, "Canon EOS 6D", dt.datetime(2020, 1, 2))}
        with mock.patch("supren.rename.exif.read_batch", return_value=fake_meta):
            plan = rename.plan_renames(self.root, self.cfg)
        self.assertEqual(len(plan.to_rename()), 1)
        action = plan.to_rename()[0]
        self.assertEqual(action.new_path.name, "20200102_E-6D_1234.JPG")

    def test_unknown_camera_is_reported_not_renamed(self):
        p = self._touch("IMG_5555.JPG")
        fake_meta = {p: _meta(p, "Some Weird Camera", dt.datetime(2020, 1, 2))}
        with mock.patch("supren.rename.exif.read_batch", return_value=fake_meta):
            plan = rename.plan_renames(self.root, self.cfg)
        self.assertEqual(len(plan.to_rename()), 0)
        self.assertEqual(plan.actions[0].status, "unknown_camera")

    def test_force_unknown_camera_renames_with_unknown_code(self):
        p = self._touch("IMG_5555.JPG")
        fake_meta = {p: _meta(p, "Some Weird Camera", dt.datetime(2020, 1, 2))}
        with mock.patch("supren.rename.exif.read_batch", return_value=fake_meta):
            plan = rename.plan_renames(self.root, self.cfg, force_unknown_camera=True)
        self.assertEqual(len(plan.to_rename()), 1)
        self.assertIn("XXXX", plan.to_rename()[0].new_path.name)

    def test_known_camera_missing_date_is_reported_not_corrupted(self):
        # Regression test for the legacy bug: a known camera with no EXIF
        # date used to silently produce a new filename of just ".JPG".
        p = self._touch("IMG_9999.JPG")
        fake_meta = {p: _meta(p, "Canon EOS 6D", None)}
        with mock.patch("supren.rename.exif.read_batch", return_value=fake_meta):
            plan = rename.plan_renames(self.root, self.cfg)
        self.assertEqual(len(plan.to_rename()), 0)
        self.assertEqual(plan.actions[0].status, "no_date")

    def test_unmatched_filename_is_reported(self):
        self._touch("totally_random.jpg")
        with mock.patch("supren.rename.exif.read_batch", return_value={}):
            plan = rename.plan_renames(self.root, self.cfg)
        self.assertEqual(plan.actions[0].status, "unknown_pattern")

    def test_already_renamed_file_is_left_alone(self):
        self._touch("20200101_E-6D_0001.jpg")
        with mock.patch("supren.rename.exif.read_batch", return_value={}):
            plan = rename.plan_renames(self.root, self.cfg)
        self.assertEqual(plan.actions[0].status, "already_done")

    def test_synthesized_number_is_deterministic_by_filename(self):
        # pattern019 "(^[0-9]{3})" has no number group in the filename -
        # a stable number must be assigned, sorted by filename, not by
        # directory-listing/mtime order (the legacy instability bug).
        p101 = self._touch("101.jpg")
        p100 = self._touch("100.jpg")
        fake_meta = {
            p100: _meta(p100, "Canon EOS 6D", dt.datetime(2020, 1, 1)),
            p101: _meta(p101, "Canon EOS 6D", dt.datetime(2020, 1, 1)),
        }
        with mock.patch("supren.rename.exif.read_batch", return_value=fake_meta):
            plan = rename.plan_renames(self.root, self.cfg)
        by_src = {a.path.name: a.new_path.name for a in plan.to_rename()}
        self.assertEqual(by_src["100.jpg"], "20200101_E-6D_0001.jpg")
        self.assertEqual(by_src["101.jpg"], "20200101_E-6D_0002.jpg")

    def test_apply_plan_renames_and_writes_undo_journal(self):
        p = self._touch("IMG_1234.JPG")
        fake_meta = {p: _meta(p, "Canon EOS 6D", dt.datetime(2020, 1, 2))}
        with mock.patch("supren.rename.exif.read_batch", return_value=fake_meta):
            plan = rename.plan_renames(self.root, self.cfg)
        journal = self.root / "journal.json"
        done = rename.apply_plan(plan, journal)
        self.assertEqual(len(done), 1)
        self.assertFalse(p.exists())
        self.assertTrue((self.root / "20200102_E-6D_1234.JPG").exists())
        self.assertTrue(journal.exists())

        reversed_count = rename.undo(journal)
        self.assertEqual(reversed_count, 1)
        self.assertTrue(p.exists())
        self.assertFalse((self.root / "20200102_E-6D_1234.JPG").exists())

    def test_apply_plan_skips_existing_destination(self):
        p = self._touch("IMG_1234.JPG")
        self._touch("20200102_E-6D_1234.JPG")  # collision target already exists
        fake_meta = {p: _meta(p, "Canon EOS 6D", dt.datetime(2020, 1, 2))}
        with mock.patch("supren.rename.exif.read_batch", return_value=fake_meta):
            plan = rename.plan_renames(self.root, self.cfg)
        journal = self.root / "journal.json"
        done = rename.apply_plan(plan, journal)
        self.assertEqual(len(done), 0)
        self.assertTrue(p.exists())  # untouched, not overwritten


if __name__ == "__main__":
    unittest.main()
