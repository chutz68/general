"""Command-line entry point: `python -m supren <command> ...` (run from the
python/ directory, or point --config at supren.json's actual location)."""
from __future__ import annotations

import argparse
import datetime as dt
import logging
import sys
from pathlib import Path

from . import bestof, cleanup, config as config_mod, rename, unrated
from .config import ConfigError
from .exif import ExiftoolNotFound


def _add_common(parser: argparse.ArgumentParser) -> None:
    parser.add_argument("--config", default="supren.json", help="Path to supren.json (default: %(default)s)")
    parser.add_argument("--apply", action="store_true", help="Actually perform the changes (default is dry-run)")


def cmd_rename(args: argparse.Namespace) -> int:
    cfg = config_mod.load(args.config)
    plan = rename.plan_renames(
        Path(args.directory), cfg, days_back=args.days_back, force_unknown_camera=args.force_unknown_cameras
    )

    for action in plan.to_rename():
        print(f"RENAME  {action.path.name}  ->  {action.new_path.name}")

    problems = plan.problems()
    if problems:
        print(f"\n# {len(problems)} file(s) could not be planned:", file=sys.stderr)
        for action in problems:
            print(f"#  {action.status:16s} {action.path}  {action.detail}", file=sys.stderr)

    print(f"\n{len(plan.to_rename())} to rename, {len(problems)} skipped, {plan.total_scanned} scanned.")

    if not args.apply:
        print("(dry-run - pass --apply to actually rename. Nothing was changed.)")
        return 0

    journal_path = Path(args.journal) if args.journal else Path(f"supren_undo_{dt.datetime.now():%Y%m%d_%H%M%S}.json")
    done = rename.apply_plan(plan, journal_path)
    print(f"Renamed {len(done)} file(s). Undo journal: {journal_path}")
    return 0


def cmd_undo(args: argparse.Namespace) -> int:
    count = rename.undo(Path(args.journal))
    print(f"Reversed {count} rename(s) from {args.journal}.")
    return 0


def cmd_bestof(args: argparse.Namespace) -> int:
    cfg = config_mod.load(args.config)
    actions = bestof.plan_bestof_copy(Path(args.source), Path(args.dest), cfg, subfolder_filter=args.filter)
    to_copy = [a for a in actions if a.status == "copy"]
    up_to_date = [a for a in actions if a.status == "up_to_date"]

    for a in to_copy:
        print(f"COPY  {a.src}  ->  {a.dest}")

    print(f"\n{len(to_copy)} to copy, {len(up_to_date)} already up to date.")
    if not args.apply:
        print("(dry-run - pass --apply to actually copy. Nothing was changed.)")
        return 0
    copied = bestof.apply_bestof_copy(actions)
    print(f"Copied {copied} file(s).")
    return 0


def cmd_unrated(args: argparse.Namespace) -> int:
    cfg = config_mod.load(args.config)
    files = unrated.find_unrated(Path(args.directory), cfg, subfolder_filter=args.filter)
    for f in files:
        print(f)
    print(f"\n{len(files)} file(s) with no rating under a '{args.filter}' folder.")
    return 0


def cmd_cleanup(args: argparse.Namespace) -> int:
    cfg = config_mod.load(args.config)
    actions = cleanup.plan_cleanup(Path(args.directory), cfg, size_only=args.size_only)

    for a in actions:
        if a.status == "delete":
            print(f"DELETE  {a.path}  ({a.detail})")
        elif a.status == "rename":
            print(f"RENAME  {a.path}  ->  {a.new_path.name}")
        elif a.status in ("skip_size", "skip_content"):
            print(f"SKIP    {a.path}  ({a.detail})")

    counts: dict[str, int] = {}
    for a in actions:
        counts[a.status] = counts.get(a.status, 0) + 1
    print("\n" + ", ".join(f"{v} {k}" for k, v in counts.items()) if counts else "\nnothing found")

    if not args.apply:
        print("(dry-run - pass --apply to actually delete/rename. Nothing was changed.)")
        return 0
    deleted, renamed = cleanup.apply_cleanup(actions)
    print(f"Deleted {deleted}, renamed {renamed}.")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(prog="supren", description="Modern replacement for the Java supRen tool.")
    sub = parser.add_subparsers(dest="command", required=True)

    p_rename = sub.add_parser(
        "rename", help="Rename photos/videos by EXIF camera + date (replaces RenameNewPhotosMetaDataExtractorTest)"
    )
    p_rename.add_argument("directory")
    p_rename.add_argument("--days-back", type=int, default=0, help="Only consider files modified in the last N days (0 = all)")
    p_rename.add_argument(
        "--force-unknown-cameras", action="store_true",
        help="Also rename files from unrecognized cameras, using the unknown-camera code"
    )
    p_rename.add_argument("--journal", help="Where to write the undo journal (default: supren_undo_<timestamp>.json)")
    _add_common(p_rename)
    p_rename.set_defaults(func=cmd_rename)

    p_undo = sub.add_parser("undo", help="Reverse a previous 'rename --apply' using its undo journal")
    p_undo.add_argument("journal")
    p_undo.set_defaults(func=cmd_undo)

    p_bestof = sub.add_parser(
        "bestof",
        help="Copy rated photos/videos into a BestOf tree "
             "(replaces CopyBestOfMetaDataExtractorTest and scripts/synology/bestof_copy.sh)",
    )
    p_bestof.add_argument("source")
    p_bestof.add_argument("dest")
    p_bestof.add_argument("--filter", default=None, help="Only consider source paths containing this substring (e.g. a year)")
    _add_common(p_bestof)
    p_bestof.set_defaults(func=cmd_bestof)

    p_unrated = sub.add_parser("unrated", help="List files in a BestOf folder that have no rating (replaces FindBestOfWithoutRatingTest)")
    p_unrated.add_argument("directory")
    p_unrated.add_argument("--filter", default="BestOf", help="Subfolder name to look under (default: %(default)s)")
    p_unrated.add_argument("--config", default="supren.json")
    p_unrated.set_defaults(func=cmd_unrated)

    p_cleanup = sub.add_parser("cleanup", help="Clean up sync-conflict duplicates (replaces scripts/synology/cleanup_conflicts.sh)")
    p_cleanup.add_argument("directory")
    p_cleanup.add_argument(
        "--size-only", action="store_true",
        help="Compare duplicates by size only, skip byte-for-byte content comparison (faster, less safe)"
    )
    _add_common(p_cleanup)
    p_cleanup.set_defaults(func=cmd_cleanup)

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(message)s")
    try:
        return args.func(args)
    except (ConfigError, ExiftoolNotFound) as e:
        print(f"error: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
