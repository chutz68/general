"""Filename matching and new-filename generation - the actual naming
convention. Replaces ImageFileValidator + OutFilenameGenerator + FilePattern.
"""
from __future__ import annotations

import datetime as dt
from dataclasses import dataclass

from .config import Config


@dataclass(frozen=True)
class MatchResult:
    pattern: str
    number_from_filename: str | None  # None when the pattern has no (?P<number>...) group
    already_renamed: bool


def match_filename(name: str, config: Config) -> MatchResult | None:
    """Match a filename against the output pattern first (so already-renamed
    files are recognized and left alone), then against the source patterns in
    the order they're listed. First match wins - same "whichever pattern
    happens to match" rule the legacy HashMap iteration had, but now
    deterministic and controlled by the order in supren.json instead of by
    hash bucket order."""
    if config.output_regex.match(name):
        return MatchResult(pattern="<output>", number_from_filename=None, already_renamed=True)
    for sp in config.source_patterns:
        m = sp.regex.match(name)
        if m:
            groups = m.groupdict()
            return MatchResult(
                pattern=sp.raw,
                number_from_filename=groups.get("number"),
                already_renamed=False,
            )
    return None


def camera_code(model: str | None, config: Config) -> str:
    if model is None:
        return config.unknown_camera
    return config.cameras.get(model, config.unknown_camera)


def build_new_name(picture_date: dt.datetime, camera_code_: str, number: int, extension: str, config: Config) -> str:
    stem = config.output_template.format(date=picture_date, camera=camera_code_, number=number)
    return f"{stem}.{extension}"
