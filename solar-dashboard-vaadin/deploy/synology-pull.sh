#!/bin/sh
# /volume1/scripts/solar-pull.sh
#
# Polls ghcr.io for a new solar-dashboard-vaadin image. If a newer digest is
# available, pulls it and recreates the container via docker compose. Designed
# to be run every 5 min by the Synology Task Scheduler as root.
#
# Setup on the Synology (one-time):
#   1. Container Manager → Image → "ghcr.io/chutz68/solar-dashboard-vaadin:latest"
#      Login with GitHub username + Personal Access Token (read:packages scope)
#      → this writes ~/.docker/config.json with the credential
#   2. mkdir -p /volume1/scripts
#      Save this script there, chmod +x /volume1/scripts/solar-pull.sh
#   3. DSM → Control Panel → Task Scheduler → Create → Scheduled Task → User-defined script
#      User: root  |  Schedule: every 5 min  |  Command:
#        /volume1/scripts/solar-pull.sh >> /volume1/scripts/solar-pull.log 2>&1

set -eu

COMPOSE_DIR=/volume1/docker/solar-dashboard
IMAGE=ghcr.io/chutz68/solar-dashboard-vaadin:latest

cd "$COMPOSE_DIR"

# Capture digest currently in use
OLD_DIGEST="$(docker image inspect --format '{{index .RepoDigests 0}}' "$IMAGE" 2>/dev/null || echo none)"

# Pull (silent if up to date)
docker compose pull --quiet solar-dashboard

NEW_DIGEST="$(docker image inspect --format '{{index .RepoDigests 0}}' "$IMAGE" 2>/dev/null || echo none)"

if [ "$OLD_DIGEST" = "$NEW_DIGEST" ]; then
    # Up to date — nothing to do
    exit 0
fi

echo "[$(date -Iseconds)] New image detected"
echo "  old: $OLD_DIGEST"
echo "  new: $NEW_DIGEST"

# Recreate container with the new image
docker compose up -d --remove-orphans solar-dashboard

# Free disk: drop now-unused old images (keep dangling refs from last 24 h)
docker image prune -f --filter "until=24h" >/dev/null

echo "[$(date -Iseconds)] Restart complete"
