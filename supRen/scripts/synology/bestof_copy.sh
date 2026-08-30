#!/bin/bash
# Copies rated ("BestOf") photos/videos from SRC_ROOT to DEST_ROOT using exiftool,
# replacing the manual CopyBestOfMetaDataExtractorTest workflow for unattended runs
# on the Synology (e.g. via Task Scheduler).
#
# Matches ImageService.copyBestOfToNewFolder():
#  - selects files with XMP:Rating >= 2 (same tag ExifServiceMetaDataExtractor reads)
#  - copies each file into DEST_ROOT/<first-level-subfolder-of-src>/ (flattened,
#    deeper nesting under a dated folder is not preserved - same as the Java code)
#  - uses cp -u semantics: only copies if the file is missing in DEST or newer in SRC
#
# Unlike the Java tool, MP4 files are NOT skipped here (that skip was a workaround
# for a metadata-extractor crash, not a deliberate exclusion) - drop "mp4" from
# EXTENSIONS below if you want to keep behavior identical.

set -euo pipefail

# Task Scheduler jobs don't inherit the interactive SSH PATH, so Entware's
# /opt/bin (where exiftool lives) needs to be added explicitly.
export PATH="/opt/bin:/opt/sbin:$PATH"

# ---- configure for your NAS ------------------------------------------------
SRC_ROOT="/volume1/photos"
DEST_ROOT="/volume1/photo"
LOG_DIR="/volume1/photo/logs"
LOCK_FILE="/tmp/bestof_copy.lock"
# -----------------------------------------------------------------------------

SUBFOLDER_FILTER=""   # optional, e.g. "2026" - only process matching subfolders
DRY_RUN=0
for arg in "$@"; do
  if [ "$arg" = "--dry-run" ]; then
    DRY_RUN=1
  else
    SUBFOLDER_FILTER="$arg"
  fi
done

EXTENSIONS=(jpg png gif bmp cr2 cr3 arw dng raf mts mp4 m2ts mpg gpr hif heic jpeg)

exec 200>"$LOCK_FILE"
flock -n 200 || { echo "Another run is already in progress, exiting."; exit 1; }

mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/bestof_copy_$(date +%Y%m%d_%H%M%S).log"
exec > >(tee -a "$LOG_FILE") 2>&1

echo "# Start $(date -Iseconds) (filter='${SUBFOLDER_FILTER}', dry_run=${DRY_RUN})"

EXT_ARGS=()
for ext in "${EXTENSIONS[@]}"; do
  EXT_ARGS+=(-ext "$ext")
done

# Only hand exiftool the matching top-level subfolders when a filter is given,
# instead of scanning the whole (potentially decades-spanning) tree and
# discarding non-matching years afterwards - that made every run as slow as
# a full-library scan regardless of the filter.
SCAN_TARGETS=()
if [ -n "$SUBFOLDER_FILTER" ]; then
  shopt -s nocaseglob nullglob
  for d in "$SRC_ROOT"/*"$SUBFOLDER_FILTER"*/; do
    SCAN_TARGETS+=("$d")
  done
  shopt -u nocaseglob nullglob
  if [ ${#SCAN_TARGETS[@]} -eq 0 ]; then
    echo "No subfolders under $SRC_ROOT match '${SUBFOLDER_FILTER}', nothing to do."
    exit 0
  fi
else
  SCAN_TARGETS=("$SRC_ROOT")
fi

matched=0
dirs_created=0
files_copied=0
files_uptodate=0
last_destdir=""

while IFS= read -r srcfile; do
  matched=$((matched + 1))
  rel="${srcfile#"$SRC_ROOT"/}"
  subfolder="${rel%%/*}"
  destdir="$DEST_ROOT/$subfolder"

  if [ "$destdir" != "$last_destdir" ]; then
    if [ ! -d "$destdir" ]; then
      dirs_created=$((dirs_created + 1))
      if [ "$DRY_RUN" = "1" ]; then
        echo "[dry-run] mkdir -p \"$destdir\""
      else
        mkdir -p "$destdir"
      fi
    fi
    last_destdir="$destdir"
  fi

  destfile="$destdir/$(basename "$srcfile")"
  # same "missing or source newer" logic cp -u itself uses, so dry-run can
  # predict copy vs. skip without touching anything
  if [ ! -e "$destfile" ] || [ "$srcfile" -nt "$destfile" ]; then
    files_copied=$((files_copied + 1))
    if [ "$DRY_RUN" = "1" ]; then
      echo "[dry-run] cp -up \"$srcfile\" \"$destdir/\""
    else
      cp -up "$srcfile" "$destdir/"
    fi
  else
    files_uptodate=$((files_uptodate + 1))
    if [ "$DRY_RUN" = "1" ]; then
      echo "[dry-run] skip (already up to date): \"$srcfile\""
    fi
  fi
done < <(exiftool -r "${EXT_ARGS[@]}" -i '#recycle' -i '@eaDir' -q -q -m -fast2 \
  -if '$XMP:Rating and $XMP:Rating >= 2' \
  -p '$Directory/$FileName' \
  "${SCAN_TARGETS[@]}")

echo "# --- Summary ---"
echo "# Rated files found:        ${matched}"
echo "# Directories created:      ${dirs_created}"
echo "# Files copied/updated:     ${files_copied}"
echo "# Files already up to date: ${files_uptodate}"
[ "$DRY_RUN" = "1" ] && echo "# (dry-run - nothing was actually changed)"
echo "# End $(date -Iseconds)"
