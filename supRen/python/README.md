# supren (Python)

Modern replacement for the Java `supRen` tool. Four functions, same underlying
rules as the legacy code, carried over from:

| Command   | Replaces |
|-----------|----------|
| `rename`  | `RenameNewPhotosMetaDataExtractorTest` / `ImageService.createMvAndUndoCommands` |
| `bestof`  | `CopyBestOfMetaDataExtractorTest` / `ImageService.copyBestOfToNewFolder`, and `scripts/synology/bestof_copy.sh` |
| `unrated` | `FindBestOfWithoutRatingTest` / `ImageService.findBestOfPhotosWithoutRating` |
| `cleanup` | `scripts/synology/cleanup_conflicts.sh` |

Runs unmodified on Windows, macOS, Linux and Synology DSM: it's stdlib-only
Python 3.10+ plus the `exiftool` binary, no `pip install` needed anywhere.

## Setup

1. Install `exiftool`:
   - **Windows**: download from https://exiftool.org, rename the `.exe` to
     `exiftool.exe`, put it on `PATH`.
   - **Synology** (DSM, via Entware/opkg - same binary `bestof_copy.sh`
     already depended on): `opkg install exiftool`.
   - **macOS**: `brew install exiftool`.
2. Copy `supren.json` next to where you run the tool, or pass `--config
   /path/to/supren.json`. Edit it to add a camera, add a source filename
   pattern, or change the output naming convention - see "Editing the
   config" below.

## Usage

Every command defaults to a **dry-run**: it prints what it would do and
changes nothing. Pass `--apply` to actually do it.

```bash
# Rename new photos under D:\photosimport by camera + EXIF date
python -m supren rename "D:\photosimport"
python -m supren rename "D:\photosimport" --apply
python -m supren rename "D:\photosimport" --days-back 7     # only recent files
python -m supren rename "D:\photosimport" --force-unknown-cameras

# Undo the most recent rename --apply
python -m supren undo supren_undo_20261018_154200.json

# Copy rated (XMP Rating >= 2) photos into a BestOf tree, optionally
# filtered to subfolders whose path contains a given substring (e.g. a year)
python -m supren bestof "D:\photos" "D:\photo" --filter 2026 --apply

# List files inside *BestOf* subfolders that have no rating at all
python -m supren unrated "D:\photos"

# Clean up Synology<->pCloud sync-conflict duplicates
python -m supren cleanup /volume1/photo --apply
```

Run any command with `-h` for its full option list.

## What changed vs. the Java tool

- **One config format.** `ruro.properties` numbered every pattern/camera
  (`infilePattern001`, `infilePatternImgNumGroup001`, `cameraModel001`,
  `cameraModel4ch001`, ...) purely because `java.util.Properties` is flat.
  `supren.json` expresses the same information directly - an ordered list of
  regexes (the image-number group is named `(?P<number>...)` inline instead
  of looked up by index) and one `{camera model: 4-char code}` map - so
  nothing needs numbering. See `tools/convert_config.py` for how the existing
  `ruro.properties` was converted; `outfileCase` was dropped (it was read but
  never actually used by the Java code).
- **Renames happen directly**, not via a generated `mv`-script you run
  yourself. `--apply` performs them and writes a JSON undo journal
  (`supren undo <journal>`) - the legacy mv-undo-command had broken quoting
  and was never actually usable.
- **A known camera with a missing EXIF date is now a reported problem
  (`no_date`)**, never a rename. The old code could silently produce a
  destination filename of just `.JPG`, risking several files colliding into
  one.
- **Deterministic numbering.** A handful of filename patterns have no image
  number in the name at all; the Java code numbered those by
  directory-listing order, which changed between runs. Here it's sorted by
  filename, same every time.
- **`exiftool` instead of `metadata-extractor`+Adobe XMP.** Reads RAW/HEIC
  formats and - unlike the old code - video files (MP4 was hard-skipped in
  `ExifServiceMetaDataExtractor` as a workaround for a library crash).
- **`bestof_copy.sh` and `cleanup_conflicts.sh` retired** in favor of this
  tool - one implementation instead of a Java version plus a parallel bash
  rewrite.

## Editing the config

`supren.json`:

```jsonc
{
  "extensions": ["jpg", "heic", ...],
  "output": {
    "template": "{date:%Y%m%d}_{camera}_{number:04d}",
    "matches": "(^[0-9]{8})(_)([a-zA-Z0-9\\-]{4})(_)([0-9]{4})"
  },
  "unknown_camera": "XXXX",
  "cameras": { "Canon EOS 6D": "E-6D", ... },
  "source_patterns": [
    "^IMG_(?P<number>[0-9]{4})",
    ...
  ],
  "bestof": { "min_rating": 2 },
  "cleanup_conflicts": { "conflict_suffix": "_conflict", "protect_regex": "HDR|Pano|Bearbeitet" }
}
```

- **New camera**: add one entry to `cameras`: `"iPhone 18 Pro": "IP18"`. The
  key must match the EXIF `Model` tag exactly (run `exiftool -Model yourfile.jpg`
  to check) - note that a few legacy entries differ only by a trailing space,
  which really does mean two different physical devices.
- **New source filename pattern**: append a regex to `source_patterns`. If
  the camera's own image number is embedded in the filename, capture it as
  `(?P<number>...)`; if there's no number in the filename, omit the named
  group - a number gets assigned automatically.
- **First match wins** in `source_patterns`, same rule as before but now
  controlled by list order instead of hash-bucket order. A few existing
  patterns overlap (e.g. a generic 3-letter-prefix pattern vs. GoPro's
  specific `GOPR` pattern) - reorder entries if you notice a file getting
  matched by the wrong one.

## Tests

```bash
python -m unittest discover -s tests -v
```

No `exiftool` needed for the test suite - EXIF reads are mocked out for the
logic tests (`test_rename.py`, `test_bestof.py`); only the real CLI against
real files needs the binary installed.
