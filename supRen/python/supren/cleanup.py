"""Port of scripts/synology/cleanup_conflicts.sh: cleans up duplicates left
behind by a Synology <-> pCloud sync tool.

Pass 1 ("_conflict" files): a sync-conflict copy like
"20181101_IP-X_1669_conflict.jpg" is deleted if the original
"20181101_IP-X_1669.jpg" exists next to it, or renamed to the original name
if it doesn't (nothing is ever lost).

Pass 2 (pattern duplicates): a file that sits next to an original named
"<canonical>.ext" (the rename convention from config.output_matches_raw,
e.g. "20181101_IP-X_1669.jpg") but carries extra text before the extension,
e.g. "20181101_IP-X_1669_ADMIN_Aug-31-021426-2026.JPG":
  - original present, variant byte-identical -> variant deleted
  - original present, variant differs        -> left alone ("skip")
  - original missing                         -> variant renamed to the
                                                 canonical name
The original file is never modified or deleted by this pass. Filenames
containing HDR/Pano/Bearbeitet (config's cleanup_conflicts.protect_regex) are
never touched - they were named that way on purpose.
"""
from __future__ import annotations

import filecmp
import re
from dataclasses import dataclass
from pathlib import Path

from .config import Config


@dataclass
class CleanupAction:
    path: Path
    status: str  # "delete" | "rename" | "skip_size" | "skip_content" | "protected"
    new_path: Path | None = None
    detail: str = ""


def _find_case_insensitive(directory: Path, name: str) -> Path | None:
    target = name.lower()
    try:
        for entry in directory.iterdir():
            if entry.is_file() and entry.name.lower() == target:
                return entry
    except FileNotFoundError:
        return None
    return None


def _conflict_actions(root: Path, config: Config) -> list[CleanupAction]:
    suffix = config.conflict_suffix.lower()
    actions: list[CleanupAction] = []
    for path in root.rglob("*"):
        if not path.is_file() or path.suffix.lower() != ".jpg":
            continue
        if not path.stem.lower().endswith(suffix):
            continue
        original_stem = path.stem[: -len(suffix)]
        original = _find_case_insensitive(path.parent, original_stem + path.suffix)
        if original is not None:
            actions.append(CleanupAction(path, "delete", detail=f"original present: {original.name}"))
        else:
            actions.append(CleanupAction(path, "rename", new_path=path.with_name(original_stem + path.suffix)))
    return actions


def _pattern_duplicate_actions(root: Path, config: Config, size_only: bool) -> list[CleanupAction]:
    # Built from config.output_matches_raw so it tracks the configured naming
    # convention instead of hardcoding "8 digits, 4 chars, 4 digits" the way
    # the original bash script did.
    variant_re = re.compile(config.output_matches_raw + r"(?P<extra>[^0-9].*)\.(?P<ext>[A-Za-z0-9]+)$", re.IGNORECASE)
    conflict_suffix = config.conflict_suffix.lower()

    actions: list[CleanupAction] = []
    # Dry-run chaining: once one variant is planned to become the original,
    # further variants of the same file compare against that planned name
    # instead of each independently reporting "original missing".
    planned_originals: dict[str, Path] = {}

    for path in sorted(root.rglob("*")):
        if not path.is_file():
            continue
        m = variant_re.match(path.name)
        if not m:
            continue
        if path.stem.lower().endswith(conflict_suffix):
            continue  # handled by pass 1
        if config.protect_regex.search(path.name):
            actions.append(CleanupAction(path, "protected"))
            continue

        original_name = path.name[: m.start("extra")] + "." + m.group("ext")
        planned_key = str(path.parent / original_name.lower())

        original = _find_case_insensitive(path.parent, original_name) or planned_originals.get(planned_key)

        if original is None:
            target = path.with_name(original_name)
            actions.append(CleanupAction(path, "rename", new_path=target))
            planned_originals[planned_key] = target
            continue

        if path.stat().st_size != original.stat().st_size:
            actions.append(CleanupAction(path, "skip_size", detail=f"size differs from {original.name}"))
            continue
        if not size_only and not filecmp.cmp(path, original, shallow=False):
            actions.append(CleanupAction(path, "skip_content", detail=f"same size, content differs from {original.name}"))
            continue

        actions.append(CleanupAction(path, "delete", detail=f"identical to {original.name}"))

    return actions


def plan_cleanup(root: Path, config: Config, size_only: bool = False) -> list[CleanupAction]:
    return _conflict_actions(root, config) + _pattern_duplicate_actions(root, config, size_only)


def apply_cleanup(actions: list[CleanupAction]) -> tuple[int, int]:
    deleted = renamed = 0
    for action in actions:
        if action.status == "delete":
            action.path.unlink()
            deleted += 1
        elif action.status == "rename":
            action.path.rename(action.new_path)
            renamed += 1
    return deleted, renamed
