"""Thin wrapper around the `exiftool` binary for batch metadata reads.

Replaces ExifServiceMetaDataExtractor (metadata-extractor + Adobe XMP
libraries). exiftool is used instead of a Python EXIF library because it is
already the dependency scripts/synology/bestof_copy.sh relies on (available
via Entware on the Synology), has a Windows build, reads the widest range of
camera/phone RAW and video formats (including the MP4s the old Java code had
to skip - see ExifServiceMetaDataExtractor's MP4 workaround), and needs no
Python packages installed on either machine.
"""
from __future__ import annotations

import datetime as dt
import json
import logging
import os
import re
import shutil
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path

logger = logging.getLogger("supren.exif")

_DATE_RE = re.compile(r"^(\d{4}):(\d{2}):(\d{2})\s+(\d{2}):(\d{2}):(\d{2})")
# Tried in this order - mirrors ExifServiceMetaDataExtractor falling back from
# TAG_DATETIME_ORIGINAL to TAG_DATETIME, extended with the video-file tags
# exiftool exposes so videos get a picture date too (the old tool silently
# gave up on all MP4s).
_DATE_TAGS = ("DateTimeOriginal", "CreateDate", "DateTimeDigitized", "DateTime", "MediaCreateDate")


class ExiftoolNotFound(RuntimeError):
    def __init__(self) -> None:
        super().__init__(
            "exiftool not found on PATH.\n"
            "  Windows:  https://exiftool.org -> download, rename the .exe to "
            "exiftool.exe, put it on PATH\n"
            "  Synology: opkg install exiftool   (same binary scripts/synology/"
            "bestof_copy.sh already used)"
        )


@dataclass(frozen=True)
class Metadata:
    path: Path
    camera_model: str | None
    picture_date: dt.datetime | None
    rating: int


def _parse_date(value: str | None) -> dt.datetime | None:
    if not value:
        return None
    m = _DATE_RE.match(value)
    if not m:
        return None
    year, month, day, hour, minute, second = (int(g) for g in m.groups())
    if year == 0 or month == 0 or day == 0:
        return None  # some cameras write "0000:00:00 00:00:00" for "no date"
    try:
        return dt.datetime(year, month, day, hour, minute, second)
    except ValueError:
        return None


_exiftool_path: str | None = None


def _find_exiftool() -> str:
    global _exiftool_path
    if _exiftool_path is None:
        exe = shutil.which("exiftool") or shutil.which("exiftool.exe")
        if not exe:
            raise ExiftoolNotFound()
        _exiftool_path = exe
    return _exiftool_path


def read_batch(paths: list[Path]) -> dict[Path, Metadata]:
    """Read camera model, picture date and XMP rating for many files in one
    exiftool call. Paths are passed via an argfile (-@) rather than on the
    command line, so there is no limit on how many files can be read at once."""
    if not paths:
        return {}

    exiftool = _find_exiftool()

    fd, argfile_path = tempfile.mkstemp(suffix=".args", text=True)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as argfile:
            for p in paths:
                argfile.write(str(p) + "\n")

        cmd = [
            exiftool, "-json", "-fast2",
            "-charset", "filename=utf8",
            "-Model", "-Rating",
            *[f"-{tag}" for tag in _DATE_TAGS],
            "-@", argfile_path,
        ]
        result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace")
    finally:
        os.unlink(argfile_path)

    # exiftool exits 1 if some (but not all) files had warnings/errors - the
    # JSON for the rest is still valid and usable.
    if result.returncode not in (0, 1):
        raise RuntimeError(f"exiftool failed (exit {result.returncode}): {result.stderr.strip()}")

    try:
        records = json.loads(result.stdout or "[]")
    except json.JSONDecodeError as e:
        raise RuntimeError(f"exiftool produced invalid JSON: {e}\n{result.stdout[:500]}") from None

    out: dict[Path, Metadata] = {}
    for rec in records:
        path = Path(rec["SourceFile"])
        date_str = next((rec.get(tag) for tag in _DATE_TAGS if rec.get(tag)), None)
        rating_raw = rec.get("Rating", 0)
        try:
            rating = int(rating_raw)
        except (TypeError, ValueError):
            rating = 0
        out[path] = Metadata(
            path=path,
            camera_model=rec.get("Model"),
            picture_date=_parse_date(date_str),
            rating=rating,
        )

    missing = [p for p in paths if p not in out]
    for p in missing:
        logger.warning("%s: exiftool returned no metadata", p)
        out[p] = Metadata(path=p, camera_model=None, picture_date=None, rating=0)

    return out
