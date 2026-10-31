#!/bin/bash
# SUPERSEDED - this script has been replaced by python/supren (the "bestof"
# command). It is kept only as a stub so an old Synology Task Scheduler entry
# that still points here fails loudly instead of silently running stale logic.
#
# Update the Task Scheduler job's "Run command" to something like:
#   python3 -m supren bestof /volume1/photos /volume1/photo --filter "$1" --apply
#   (run from inside the python/ directory, or add --config /path/to/supren.json)
#
# See python/README.md for full usage and setup (exiftool via opkg is the
# same dependency this script already had).
echo "bestof_copy.sh is superseded by: python -m supren bestof <src> <dest> [--filter ...] --apply" >&2
echo "See python/README.md. Update this Task Scheduler job to call it instead." >&2
exit 1
