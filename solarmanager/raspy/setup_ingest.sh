#!/bin/bash
# =============================================================
# Raspberry Pi ingest_5m Setup Script
# =============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT_PATH="$SCRIPT_DIR/ingest_5m.py"
ENV_FILE="$SCRIPT_DIR/.env"

echo "▶ Installing Python dependencies..."
pip3 install requests google-cloud-bigquery google-cloud-storage --break-system-packages

echo "▶ Checking .env file..."
if [ ! -f "$ENV_FILE" ]; then
    echo "  Creating .env file – please fill in your credentials!"
    cat > "$ENV_FILE" << 'EOF'
GOOGLE_APPLICATION_CREDENTIALS=/home/wern/isg/gcp-key.json
GCP_PROJECT_ID=modern-cubist-412113
SOLARMANAGER_TOKEN=Basic d2VybmVyLnJvZGVsQGdtYWlsLmNvbTpTb2xXZXJuSHkyNU9jdCQyNQ==
OWM_APIKEY=your_openweathermap_api_key_here
EOF
    echo "  ⚠️  Please update OWM_APIKEY in $ENV_FILE"
    exit 1
fi

echo "▶ Testing script..."
set -a && source "$ENV_FILE" && set +a
python3 "$SCRIPT_PATH"

echo "▶ Setting up cron job..."
# Runs at xx:01, xx:06, xx:11 ... (1 min after each 5-min interval)
CRON_LINE="1,6,11,16,21,26,31,36,41,46,51,56 * * * * source $ENV_FILE && python3 $SCRIPT_PATH >> $SCRIPT_DIR/ingest_5m.log 2>&1"

( crontab -l 2>/dev/null | grep -v "ingest_5m.py" ; echo "$CRON_LINE" ) | crontab -

echo ""
echo "✅ Setup complete!"
echo "   Logs: tail -f $SCRIPT_DIR/ingest_5m.log"
echo "   Cron: crontab -l"
