"""Plan and apply the rename-by-EXIF-camera-and-date operation.
Replaces ImageService.createMvAndUndoCommands() / RenameNewPhotosMetaDataExtractorTest.

Two behavior differences from the legacy Java tool, both bug fixes:

1. A file whose camera IS known but whose EXIF has no date used to produce an
   mv command with an empty stem (just ".JPG") - ImageService.java silently
   building a `mv "IMG_1234.JPG" ".JPG"` command, able to make several files
   collide into one. Here, a missing date is always a reported problem
   ("no_date"), never a rename.
2. Files that match a pattern with no image number in the filename (groups
   007, 009-011, 019-026 in the old numbering) got a number synthesized from
   HashMap/directory-listing order, which is unstable between runs. Here the
   number is assigned deterministically: sorted by filename within each
   (directory, pattern) group.
"""
from __future__ import annotations

import datetime as dt
import json
import logging
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

from . import exif
from .config import Config
from .naming import MatchResult, build_new_name, camera_code, match_filename

logger = logging.getLogger("supren.rename")


@dataclass
class RenameAction:
    path: Path
    status: str  # "rename" | "already_done" | "unknown_pattern" | "unknown_camera" | "no_date" | "no_metadata"
    new_path: Path | None = None
    detail: str = ""


@dataclass
class RenamePlan:
    actions: list[RenameAction] = field(default_factory=list)
    total_scanned: int = 0
    total_filtered_out: int = 0

    def to_rename(self) -> list[RenameAction]:
        return [a for a in self.actions if a.status == "rename"]

    def problems(self) -> list[RenameAction]:
        return [a for a in self.actions if a.status not in ("rename", "already_done")]


def _iter_candidate_files(root: Path, config: Config, days_back: int, plan: RenamePlan):
    cutoff = None
    if days_back and days_back > 0:
        cutoff = dt.datetime.now().timestamp() - days_back * 86400
    ext_re = config.extension_regex()
    for path in root.rglob("*"):
        if not path.is_file():
            continue
        if not ext_re.search(path.name):
            continue
        if cutoff is not None and path.stat().st_mtime <= cutoff:
            plan.total_filtered_out += 1
            continue
        yield path


def plan_renames(root: Path, config: Config, days_back: int = 0, force_unknown_camera: bool = False) -> RenamePlan:
    plan = RenamePlan()
    candidates = list(_iter_candidate_files(root, config, days_back, plan))
    plan.total_scanned = len(candidates)

    matches: dict[Path, MatchResult | None] = {path: match_filename(path.name, config) for path in candidates}

    # Deterministic numbering for patterns with no number group in the
    # filename (fixes the unstable-order bug - see module docstring).
    needs_synth_number: dict[tuple[Path, str], list[Path]] = defaultdict(list)
    for path, m in matches.items():
        if m is not None and not m.already_renamed and m.number_from_filename is None:
            needs_synth_number[(path.parent, m.pattern)].append(path)
    synth_number: dict[Path, int] = {}
    for paths in needs_synth_number.values():
        for i, path in enumerate(sorted(paths, key=lambda p: p.name.lower()), start=1):
            synth_number[path] = i

    metadata = exif.read_batch([p for p, m in matches.items() if m is not None and not m.already_renamed])

    for path in candidates:
        m = matches[path]
        if m is None:
            plan.actions.append(RenameAction(path, "unknown_pattern", detail="no source pattern matches this filename"))
            continue
        if m.already_renamed:
            plan.actions.append(RenameAction(path, "already_done"))
            continue

        meta = metadata.get(path)
        if meta is None:
            plan.actions.append(RenameAction(path, "no_metadata", detail="exiftool returned nothing for this file"))
            continue

        code = camera_code(meta.camera_model, config)
        is_known = code != config.unknown_camera
        if not is_known and not force_unknown_camera:
            plan.actions.append(RenameAction(path, "unknown_camera", detail=meta.camera_model or "(no camera model in EXIF)"))
            continue

        if meta.picture_date is None:
            plan.actions.append(RenameAction(path, "no_date", detail="no DateTimeOriginal/CreateDate in EXIF"))
            continue

        number = int(m.number_from_filename) if m.number_from_filename is not None else synth_number[path]
        new_name = build_new_name(meta.picture_date, code, number, path.suffix.lstrip("."), config)
        plan.actions.append(RenameAction(path, "rename", new_path=path.with_name(new_name)))

    return plan


def apply_plan(plan: RenamePlan, journal_path: Path) -> list[RenameAction]:
    """Execute the plan's rename actions and write a JSON undo journal (fixes
    the legacy mv-undo-command's broken quoting, which made it unusable).
    A destination that already exists is treated as a real collision and
    skipped (logged), never silently overwritten."""
    done: list[RenameAction] = []
    collisions: list[RenameAction] = []
    journal_entries = []

    for action in plan.to_rename():
        if action.new_path.exists():
            collisions.append(action)
            continue
        action.path.rename(action.new_path)
        journal_entries.append({"from": str(action.new_path), "to": str(action.path)})
        done.append(action)

    journal_path.write_text(
        json.dumps({"created": dt.datetime.now().isoformat(), "renames": journal_entries}, indent=2),
        encoding="utf-8",
    )

    if collisions:
        logger.warning("%d file(s) skipped: destination already exists", len(collisions))
        for c in collisions:
            logger.warning("  %s -> %s (already exists)", c.path, c.new_path)

    return done


def undo(journal_path: Path) -> int:
    data = json.loads(journal_path.read_text(encoding="utf-8"))
    count = 0
    for entry in reversed(data["renames"]):
        src, dst = Path(entry["from"]), Path(entry["to"])
        if src.exists() and not dst.exists():
            src.rename(dst)
            count += 1
        else:
            logger.warning("skipped undo of %s -> %s (state changed since rename)", src, dst)
    return count
