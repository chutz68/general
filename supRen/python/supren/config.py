"""Load and validate supren.json.

This replaces UserPropertyReader + ImageFileValidator's property-side logic.
The legacy ruro.properties numbered each pattern/camera (infilePattern001,
infilePatternImgNumGroup001, cameraModel001, cameraModel4ch001, ...) purely so
a flat java.util.Properties file could express a 1:1 relationship between two
keys. supren.json expresses that relationship directly - a list of pattern
strings (with the image-number group named inline as (?P<number>...) instead
of looked up by index) and a single dict of {camera model: 4-char code} - so
there is nothing left to number.
"""
from __future__ import annotations

import json
import re
from dataclasses import dataclass
from pathlib import Path

REQUIRED_KEYS = (
    "extensions",
    "output",
    "unknown_camera",
    "cameras",
    "source_patterns",
    "bestof",
    "cleanup_conflicts",
)


@dataclass(frozen=True)
class SourcePattern:
    raw: str
    regex: re.Pattern


@dataclass(frozen=True)
class Config:
    extensions: tuple[str, ...]
    output_template: str
    output_matches_raw: str
    output_regex: re.Pattern
    unknown_camera: str
    cameras: dict[str, str]
    source_patterns: tuple[SourcePattern, ...]
    bestof_min_rating: int
    conflict_suffix: str
    protect_regex: re.Pattern
    path: Path

    def extension_regex(self) -> re.Pattern:
        exts = "|".join(re.escape(e) for e in self.extensions)
        return re.compile(rf"\.(?:{exts})$", re.IGNORECASE)


class ConfigError(ValueError):
    pass


def load(path: str | Path) -> Config:
    path = Path(path)
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        raise ConfigError(f"config file not found: {path}") from None
    except json.JSONDecodeError as e:
        raise ConfigError(f"{path}: invalid JSON ({e})") from None

    missing = [k for k in REQUIRED_KEYS if k not in data]
    if missing:
        raise ConfigError(f"{path}: missing required key(s): {', '.join(missing)}")

    extensions = tuple(e.lower().lstrip(".") for e in data["extensions"])
    if not extensions:
        raise ConfigError(f"{path}: 'extensions' must not be empty")
    ext_alt = "|".join(re.escape(e) for e in extensions)
    ext_suffix = rf"\.(?:{ext_alt})$"

    try:
        source_patterns = tuple(
            SourcePattern(raw=p, regex=re.compile(p + ext_suffix, re.IGNORECASE))
            for p in data["source_patterns"]
        )
    except re.error as e:
        raise ConfigError(f"{path}: invalid source_patterns regex ({e})") from None

    output = data["output"]
    for key in ("template", "matches"):
        if key not in output:
            raise ConfigError(f"{path}: 'output.{key}' is required")
    try:
        output_regex = re.compile(output["matches"] + ext_suffix, re.IGNORECASE)
    except re.error as e:
        raise ConfigError(f"{path}: invalid output.matches regex ({e})") from None

    cleanup = data["cleanup_conflicts"]
    try:
        protect_regex = re.compile(cleanup.get("protect_regex", "$a"), re.IGNORECASE)
    except re.error as e:
        raise ConfigError(f"{path}: invalid cleanup_conflicts.protect_regex ({e})") from None

    cameras = dict(data["cameras"])
    if not cameras:
        raise ConfigError(f"{path}: 'cameras' must not be empty")

    return Config(
        extensions=extensions,
        output_template=output["template"],
        output_matches_raw=output["matches"],
        output_regex=output_regex,
        unknown_camera=data["unknown_camera"],
        cameras=cameras,
        source_patterns=source_patterns,
        bestof_min_rating=int(data["bestof"].get("min_rating", 2)),
        conflict_suffix=cleanup.get("conflict_suffix", "_conflict"),
        protect_regex=protect_regex,
        path=path,
    )
