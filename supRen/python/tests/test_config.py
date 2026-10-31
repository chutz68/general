import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from supren import config as config_mod

REAL_CONFIG = Path(__file__).resolve().parents[1] / "supren.json"


class TestConfigLoad(unittest.TestCase):
    def test_loads_real_config(self):
        cfg = config_mod.load(REAL_CONFIG)
        self.assertGreater(len(cfg.cameras), 0)
        self.assertGreater(len(cfg.source_patterns), 0)
        self.assertEqual(cfg.unknown_camera, "XXXX")

    def test_missing_file_raises_config_error(self):
        with self.assertRaises(config_mod.ConfigError):
            config_mod.load("does_not_exist.json")

    def test_missing_required_key_raises(self):
        tmp = Path(__file__).parent / "_tmp_bad.json"
        tmp.write_text(json.dumps({"extensions": ["jpg"]}), encoding="utf-8")
        try:
            with self.assertRaises(config_mod.ConfigError):
                config_mod.load(tmp)
        finally:
            tmp.unlink()

    def test_invalid_regex_raises(self):
        data = json.loads(REAL_CONFIG.read_text(encoding="utf-8"))
        data["source_patterns"].append("(unclosed")
        tmp = Path(__file__).parent / "_tmp_badregex.json"
        tmp.write_text(json.dumps(data), encoding="utf-8")
        try:
            with self.assertRaises(config_mod.ConfigError):
                config_mod.load(tmp)
        finally:
            tmp.unlink()


if __name__ == "__main__":
    unittest.main()
