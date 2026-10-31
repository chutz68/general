"""Copy rated ("BestOf") photos/videos into a destination tree.

Replaces both ImageService.copyBestOfToNewFolder() (CopyBestOfMetaDataExtractorTest)
and scripts/synology/bestof_copy.sh - same selection rule (XMP Rating >=
config's bestof.min_rating) and the same destination layout: files are
flattened into DEST/<first-level-subfolder-of-SRC>/, matching what the legacy
Java code did (ImageService.java splits on the first path separator under
baseDir) and what bestof_copy.sh did independently afterwards.
"""
from __future__ import annotations

import shutil
from dataclasses import dataclass
from pathlib import Path

from . import exif
from .config import Config


@dataclass
class CopyAction:
    src: Path
    dest: Path
    status: str  # "copy" | "up_to_date"


def plan_bestof_copy(src_root: Path, dest_root: Path, config: Config, subfolder_filter: str | None = None) -> list[CopyAction]:
    ext_re = config.extension_regex()
    candidates = [p for p in src_root.rglob("*") if p.is_file() and ext_re.search(p.name)]
    if subfolder_filter:
        needle = subfolder_filter.lower()
        candidates = [p for p in candidates if needle in str(p.relative_to(src_root)).lower()]

    metadata = exif.read_batch(candidates)
    actions: list[CopyAction] = []
    for path in candidates:
        meta = metadata.get(path)
        if meta is None or meta.rating < config.bestof_min_rating:
            continue
        rel = path.relative_to(src_root)
        top_subfolder = rel.parts[0] if len(rel.parts) > 1 else ""
        dest_dir = dest_root / top_subfolder if top_subfolder else dest_root
        dest = dest_dir / path.name
        # cp -u semantics: copy if missing, or if the source is newer.
        if dest.exists() and dest.stat().st_mtime >= path.stat().st_mtime:
            actions.append(CopyAction(path, dest, "up_to_date"))
        else:
            actions.append(CopyAction(path, dest, "copy"))
    return actions


def apply_bestof_copy(actions: list[CopyAction]) -> int:
    copied = 0
    for action in actions:
        if action.status != "copy":
            continue
        action.dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(action.src, action.dest)
        copied += 1
    return copied
