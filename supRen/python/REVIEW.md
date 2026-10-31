# supren (Python): review and status

Date: 2026-10-31. Scope: `python/supren/*`, `python/supren.json`, the
replaced `scripts/synology/*.sh`, compared with the Java original
(`ImageService`, `ImageFileValidator`, `OutFilenameGenerator`,
`ExifServiceMetaDataExtractor`) and `src/main/resources/ruro.properties`.

## TL;DR

The Python port is **not broken at its core**. The rename, bestof, unrated and
cleanup logic runs correctly end to end once an `exiftool` binary is available.
It does **not work in practice** for these reasons:

1. **`exiftool` is not installed on this PC.** Every command stops with
   `error: exiftool not found on PATH`. The Java tool didn't need it.
2. **It can't run from Synology Task Scheduler as set up.** The CLI must be
   started from inside `python/` (`No module named supren` otherwise). The
   config default `supren.json` is relative to the current directory. Unlike
   the old bash script, `/opt/bin` (Entware, where exiftool lives) is not
   added to `PATH`.
3. **The working bash script was replaced by a stub that always fails.**
   `scripts/synology/bestof_copy.sh` (uncommitted change) now only prints
   "superseded" and does `exit 1`. If that file was deployed to the NAS, the
   scheduled BestOf copy has stopped.
4. **Real bugs in `bestof`** (see below): `--filter` also matches file names,
   and `#recycle` / `@eaDir` are not excluded.

The 36 unit tests pass (`python -m unittest discover -s tests`), but they mock
exiftool, so they never tested any of the points above.

## Components

| Python command | Replaces | Status |
|---|---|---|
| `rename` (+ `undo`) | `ImageService.createMvAndUndoCommands` / `RenameNewPhotosMetaDataExtractorTest` | Logic OK; needs exiftool |
| `bestof` | `ImageService.copyBestOfToNewFolder` + `scripts/synology/bestof_copy.sh` | **Bugs** (filter, recycle bin, speed, no PATH/lock/log) |
| `unrated` | `ImageService.findBestOfPhotosWithoutRating` | Logic OK; needs exiftool |
| `cleanup` | `scripts/synology/cleanup_conflicts.sh` | Can't compare: original script is gone |
| `supren.json` | `ruro.properties` | Converted correctly; in sync (incl. `iPhone 16 Pro` → `IP16`) |

## How it was checked

- Read every Python module and the Java classes it replaces.
- Ran the unit tests: 36/36 OK.
- Ran the real CLI end to end against a stub `exiftool` that returns
  exiftool-style JSON (forward-slash `SourceFile` paths, as exiftool
  outputs on Windows), on a test tree with `2025_Ferien/sub/IMG_2026.JPG`,
  `#recycle/IMG_0001.JPG`, `2024/@eaDir/IMG_0002.JPG`.
  - `rename`: planned correctly (`IMG_0003.JPG -> 20251230_E-6D_0003.JPG`).
  - `bestof --filter 2026`: **copied `2025_Ferien/sub/IMG_2026.JPG`**. Wrong:
    the folder doesn't contain 2026, only the file name does.
  - `bestof`: **copied `#recycle/IMG_0001.JPG` to `dest/#recycle/`** and the
    `@eaDir` file. Wrong.
  - Running from a different working directory: `No module named supren`.
- Not checked: real exiftool output (not installed here), Python version on
  the NAS.

## Findings

### Blocking

| # | Where | Problem | Fix |
|---|---|---|---|
| B1 | environment | `exiftool` is not on `PATH` on Windows, so every command fails. | Install exiftool (exiftool.org, rename to `exiftool.exe`, put it on PATH), or allow an `exiftool` path in `supren.json` / `--exiftool`. |
| B2 | `cli.py` | `--config` defaults to `supren.json` in the **current directory**, and the module only loads from `python/`. Task Scheduler runs from `/`. | Default the config to `Path(__file__).parent.parent / "supren.json"`; give a wrapper script that `cd`s into `python/` first. |
| B3 | `exif.py` | The bash script did `export PATH="/opt/bin:/opt/sbin:$PATH"` because Task Scheduler doesn't inherit the SSH PATH. Python only uses `shutil.which("exiftool")`, so the scheduled run fails with *exiftool not found*. | Also check `/opt/bin/exiftool` as a fallback, or set PATH in the wrapper script. |
| B4 | `scripts/synology/bestof_copy.sh` | The working script (exiftool `-if '$XMP:Rating >= 2'`, lock, log, dry-run) was replaced by an `exit 1` stub. This change isn't committed. | `git checkout -- scripts/synology/bestof_copy.sh` restores it. Keep it until the Python version has run on the NAS. |

### Bugs (wrong results)

| # | Where | Problem | Fix |
|---|---|---|---|
| F1 | `bestof.py` `plan_bestof_copy` | `--filter` is matched against the **whole relative path, including the file name**. `--filter 2026` also picks up `2025_Ferien/IMG_2026.JPG` and every file whose image number is 2026. Java only checked the directory part; bash only checked top-level folder names. | Match only `rel.parts[0]` (top-level folder), like bash did. |
| F2 | `bestof.py`, `rename.py`, `cleanup.py`, `unrated.py` | `rglob("*")` goes into `#recycle` and `@eaDir`. Rated photos in the recycle bin get copied back into BestOf. Synology thumbnails get read or renamed. Cleanup changes files in the recycle bin. The bash script excluded both (`-i '#recycle' -i '@eaDir'`). | One shared file walker that skips `#recycle`, `@eaDir` and `#snapshot`. |
| F3 | `bestof.py` | Without `--filter` it hands **the whole library** to a single exiftool call, with no progress output and all JSON buffered in memory. The bash script narrowed the scan to matching top-level folders first. | Find the top-level folders first (like bash), batch exiftool calls (e.g. 1000 files each), print progress. |
| F4 | `exif.py` | MP4/MOV `CreateDate` is stored in **UTC**. Videos shot late in the evening can get the next day's date in their name. | Pass `-api QuickTimeUTC` so exiftool converts to local time. |

### Changed vs. Java (decide if that's what you want)

| # | Topic | Java | Python |
|---|---|---|---|
| D1 | Date format | `SimpleDateFormat("YYYYMMdd")`: `YYYY` is the **week year**, so 29–31 Dec could get the next year (e.g. `20261230` for 30.12.2025). | `%Y`, the correct calendar year. Python is right, but old and new names differ for those days. |
| D2 | Synthetic image number (patterns with no number in the name) | Counted per pattern across the **whole tree**, sorted by last-modified (newest first). | Counted per **folder + pattern**, sorted by file name, starting at 1. Avoids clashes better per folder, but numbers differ from Java. |
| D3 | Pattern order | `HashMap` order, effectively random. | List order in `supren.json`. Deterministic. |
| D4 | Rating tag | Only `XMP:Rating`. | `-Rating` without a group, so `EXIF:Rating` (written by Windows) is also accepted. |
| D5 | Rename | Wrote an `mv` script for you to run. | Renames directly with `--apply` and writes a JSON undo journal. |
| D6 | Known camera, no date | `mv "X.JPG" ".JPG"` (bug). | Reported as `no_date`, not renamed. |
| D7 | MP4 | Skipped. | Read. |

### Smaller issues

- `exif.py`: `DateTime` and `DateTimeDigitized` aren't exiftool tag names
  (they're `ModifyDate` / `CreateDate`), so those fallbacks never return anything.
  Harmless.
- `exif.py`: `"USB Camera "` (with a trailing space) vs. `"USB Camera"` only
  works if exiftool keeps the trailing space. Not verified.
- `cli.py rename`: the output shows only base names, not the folder. With many
  subfolders you can't tell which file is meant.
- No lock file and no log file (the bash script had both, which matters for
  Task Scheduler).
- README says Python 3.10+. DSM 7 ships Python 3.8. The code uses
  `from __future__ import annotations` and might run on 3.8, but that wasn't tested.
- README says `opkg install exiftool`. On Entware the package may have a
  different name (`perl-image-exiftool`). Check with `opkg list | grep -i exif`.
- `scripts/synology/cleanup_conflicts.sh` is untracked and now only a stub.
  The **original bash logic isn't in git**, so you can't compare the Python
  `cleanup` with it.
- Tests: no integration test with a real exiftool and no test for the filter
  or exclusion rules. That's why F1–F3 went unnoticed.

## Recommended next steps

1. **Now:** `git checkout -- scripts/synology/bestof_copy.sh`, so the
   Synology job keeps working with the proven bash script.
2. Install exiftool on Windows, run `python -m supren rename <test folder>`
   (dry-run) on a copy of real photos, and compare with the Java output.
3. Fix F1, F2, B2 and B3, plus tests for each.
4. Fix F3 and F4, and add a lock and log for the NAS run.
5. Only then switch the Synology job to Python and retire the bash script.
