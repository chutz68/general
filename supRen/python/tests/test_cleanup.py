import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from supren import cleanup
from supren import config as config_mod

REAL_CONFIG = Path(__file__).resolve().parents[1] / "supren.json"


class TestCleanup(unittest.TestCase):
    def setUp(self):
        self.cfg = config_mod.load(REAL_CONFIG)
        self.tmpdir = tempfile.TemporaryDirectory()
        self.root = Path(self.tmpdir.name)

    def tearDown(self):
        self.tmpdir.cleanup()

    def _write(self, name: str, content: bytes = b"x") -> Path:
        p = self.root / name
        p.write_bytes(content)
        return p

    def test_conflict_file_deleted_when_original_present(self):
        self._write("20181101_IP-X_1669.jpg")
        conflict = self._write("20181101_IP-X_1669_Conflict.jpg")
        actions = cleanup.plan_cleanup(self.root, self.cfg)
        action = next(a for a in actions if a.path == conflict)
        self.assertEqual(action.status, "delete")

    def test_conflict_file_renamed_when_original_missing(self):
        conflict = self._write("20181101_IP-X_1670_Conflict.jpg")
        actions = cleanup.plan_cleanup(self.root, self.cfg)
        action = next(a for a in actions if a.path == conflict)
        self.assertEqual(action.status, "rename")
        self.assertEqual(action.new_path.name, "20181101_IP-X_1670.jpg")

    def test_pattern_duplicate_deleted_when_identical(self):
        self._write("20181101_IP-X_1669.jpg", b"same bytes")
        dup = self._write("20181101_IP-X_1669_ADMIN_Aug-31-021426-2026.JPG", b"same bytes")
        actions = cleanup.plan_cleanup(self.root, self.cfg)
        action = next(a for a in actions if a.path == dup)
        self.assertEqual(action.status, "delete")

    def test_pattern_duplicate_skipped_when_content_differs(self):
        self._write("20181101_IP-X_1669.jpg", b"original bytes!!")
        dup = self._write("20181101_IP-X_1669_ADMIN_Aug-31-021426-2026.JPG", b"different bytes!")
        actions = cleanup.plan_cleanup(self.root, self.cfg)
        action = next(a for a in actions if a.path == dup)
        self.assertEqual(action.status, "skip_content")

    def test_pattern_duplicate_renamed_when_original_missing(self):
        dup = self._write("20181101_IP-X_1671_ADMIN_Aug-31-021426-2026.JPG")
        actions = cleanup.plan_cleanup(self.root, self.cfg)
        action = next(a for a in actions if a.path == dup)
        self.assertEqual(action.status, "rename")
        self.assertEqual(action.new_path.name, "20181101_IP-X_1671.JPG")

    def test_protected_names_never_touched(self):
        hdr = self._write("20181101_IP-X_1672_HDR.jpg")
        actions = cleanup.plan_cleanup(self.root, self.cfg)
        action = next(a for a in actions if a.path == hdr)
        self.assertEqual(action.status, "protected")

    def test_apply_cleanup_performs_delete_and_rename(self):
        self._write("20181101_IP-X_1669.jpg", b"same")
        dup = self._write("20181101_IP-X_1669_ADMIN_x.jpg", b"same")
        missing_dup = self._write("20181101_IP-X_1680_ADMIN_x.jpg")
        actions = cleanup.plan_cleanup(self.root, self.cfg)
        deleted, renamed = cleanup.apply_cleanup(actions)
        self.assertEqual(deleted, 1)
        self.assertEqual(renamed, 1)
        self.assertFalse(dup.exists())
        self.assertFalse(missing_dup.exists())
        self.assertTrue((self.root / "20181101_IP-X_1680.jpg").exists())


if __name__ == "__main__":
    unittest.main()
