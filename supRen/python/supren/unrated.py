"""Find files that already live in a 'BestOf'-like folder but carry no
rating - stragglers someone forgot to rate, or copied in by mistake.
Replaces ImageService.findBestOfPhotosWithoutRating() / FindBestOfWithoutRatingTest.
"""
from __future__ import annotations

from pathlib import Path

from . import exif
from .config import Config


def find_unrated(root: Path, config: Config, subfolder_filter: str = "BestOf") -> list[Path]:
    ext_re = config.extension_regex()
    needle = subfolder_filter.lower()
    candidates = [
        p for p in root.rglob("*")
        if p.is_file() and ext_re.search(p.name) and needle in str(p.relative_to(root)).lower()
    ]
    metadata = exif.read_batch(candidates)
    return [p for p in candidates if metadata.get(p) is None or metadata[p].rating == 0]
