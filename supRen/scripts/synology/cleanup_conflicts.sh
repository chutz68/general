#!/usr/bin/env bash
# SUPERSEDED - this script has been ported into python/supren (the "cleanup"
# command), which is driven by the same supren.json used for renaming instead
# of a hardcoded naming convention. Kept only as a stub so an old cron entry
# or manual habit that still calls this file fails loudly instead of running
# stale logic.
#
# Equivalent calls:
#   bash cleanup_conflicts.sh -n [dir]   ->  python -m supren cleanup [dir]
#   bash cleanup_conflicts.sh [dir]      ->  python -m supren cleanup [dir] --apply
#   bash cleanup_conflicts.sh -s [dir]   ->  python -m supren cleanup [dir] --size-only --apply
#
# See python/README.md for full usage and setup.
echo "cleanup_conflicts.sh is superseded by: python -m supren cleanup <dir> [--size-only] --apply" >&2
echo "See python/README.md. Update any scheduled job or habit to call it instead." >&2
exit 1
