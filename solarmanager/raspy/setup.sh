#!/bin/bash
# =============================================================
# Raspberry Pi ISG Setup Script
# Run once to install dependencies and configure cron
# =============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_PATH="$SCRIPT_DIR/isg_to_gcs.py"

echo "▶ Installing Python dependencies..."
pip3 install requests beautifulsoup4 google-cloud-storage --break-system-packages

echo "▶ Checking GCP credentials..."
if [ ! -f "$SCRIPT_DIR/gcp-key.json" ]; then
    echo "  ⚠️  gcp-key.json not found!"
    echo "  Please copy your GCP Service Account key to: $SCRIPT_DIR/gcp-key.json"
    exit 1
fi

export GOOGLE_APPLICATION_CREDENTIALS="$SCRIPT_DIR/gcp-key.json"

echo "▶ Testing ISG connection..."
python3 -c "import requests; r = requests.get('http://192.168.1.180/?s=1,1', timeout=5); print('  ✔ ISG reachable')" 2>/dev/null || echo "  ✖ ISG not reachable!"

echo "▶ Testing script..."
python3 "$SCRIPT_PATH"

echo "▶ Setting up cron job (every 5 min, 2 min before Cloud Run)..."
# Runs at xx:04, xx:09, xx:14 ... (2 min before Cloud Run at xx:06, xx:11, xx:16 ...)
CRON_LINE="4,9,14,19,24,29,34,39,44,49,54,59 * * * * GOOGLE_APPLICATION_CREDENTIALS=$SCRIPT_DIR/gcp-key.json python3 $SCRIPT_PATH >> $SCRIPT_DIR/isg.log 2>&1"

( crontab -l 2>/dev/null | grep -v "isg_to_gcs.py" ; echo "$CRON_LINE" ) | crontab -

echo ""
echo "✅ Setup complete!"
echo ""
echo "   Logs: tail -f $SCRIPT_DIR/isg.log"
echo "   Cron: crontab -l"
