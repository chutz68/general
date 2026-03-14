#!/bin/bash
# =============================================================
# Solar BQ Job – GCP Setup Script
# Run once to set up all required GCP resources
# =============================================================
set -euo pipefail

PROJECT_ID="modern-cubist-412113"
REGION="europe-west6"           # Zürich
SA_NAME="solar-runner"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"
REPO_NAME="solar"
JOB_NAME="solar-bq-job"
IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/solar-job:latest"

echo "▶ Project: $PROJECT_ID | Region: $REGION"

# ---------------------------------------------------------
# 1) Enable required APIs
# ---------------------------------------------------------
echo "▶ Enabling GCP APIs..."
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  cloudscheduler.googleapis.com \
  secretmanager.googleapis.com \
  artifactregistry.googleapis.com \
  bigquery.googleapis.com \
  --project=$PROJECT_ID

# ---------------------------------------------------------
# 2) Artifact Registry repository
# ---------------------------------------------------------
echo "▶ Creating Artifact Registry..."
gcloud artifacts repositories create $REPO_NAME \
  --repository-format=docker \
  --location=$REGION \
  --project=$PROJECT_ID \
  --description="Solar Manager Docker images" \
  2>/dev/null || echo "  (already exists)"

# ---------------------------------------------------------
# 3) Service Account
# ---------------------------------------------------------
echo "▶ Creating Service Account..."
gcloud iam service-accounts create $SA_NAME \
  --display-name="Solar Cloud Run Runner" \
  --project=$PROJECT_ID \
  2>/dev/null || echo "  (already exists)"

# Grant permissions
for ROLE in \
  roles/bigquery.dataEditor \
  roles/bigquery.jobUser \
  roles/secretmanager.secretAccessor \
  roles/artifactregistry.reader; do
  gcloud projects add-iam-policy-binding $PROJECT_ID \
    --member="serviceAccount:$SA_EMAIL" \
    --role="$ROLE" --quiet
done
echo "  ✔ IAM roles assigned"

# ---------------------------------------------------------
# 4) Secrets in Secret Manager
# ---------------------------------------------------------
echo "▶ Creating secrets (fill in values when prompted)..."

create_secret() {
  local SECRET_ID=$1
  local DESCRIPTION=$2
  if gcloud secrets describe $SECRET_ID --project=$PROJECT_ID &>/dev/null; then
    echo "  Secret '$SECRET_ID' already exists – adding new version..."
    echo -n "Enter value for $SECRET_ID: " && read -rs SECRET_VALUE && echo
    echo -n "$SECRET_VALUE" | gcloud secrets versions add $SECRET_ID \
      --data-file=- --project=$PROJECT_ID
  else
    echo -n "Enter value for $SECRET_ID ($DESCRIPTION): " && read -rs SECRET_VALUE && echo
    echo -n "$SECRET_VALUE" | gcloud secrets create $SECRET_ID \
      --data-file=- \
      --replication-policy=automatic \
      --project=$PROJECT_ID
  fi
  echo "  ✔ Secret '$SECRET_ID' ready"
}

create_secret "solarmanager-token"    "Solarmanager Basic Auth Token (e.g. Basic xxxx)"
create_secret "openweathermap-apikey" "OpenWeatherMap API Key"

# ---------------------------------------------------------
# 5) Build & Push Docker image
# ---------------------------------------------------------
echo "▶ Building Docker image via Cloud Build..."
gcloud builds submit . \
  --config=cloudbuild.yaml \
  --project=$PROJECT_ID \
  --substitutions=SHORT_SHA=latest

# ---------------------------------------------------------
# 6) Cloud Run Job
# ---------------------------------------------------------
echo "▶ Deploying Cloud Run Job..."
gcloud run jobs deploy $JOB_NAME \
  --image=$IMAGE \
  --region=$REGION \
  --task-timeout=600 \
  --max-retries=0 \
  --service-account=$SA_EMAIL \
  --set-env-vars=GCP_PROJECT_ID=$PROJECT_ID \
  --set-secrets="SECRET_SM_TOKEN=solarmanager-token:latest,SECRET_OWM_APIKEY=openweathermap-apikey:latest" \
  --project=$PROJECT_ID

# ---------------------------------------------------------
# 7) Cloud Scheduler (every 5 minutes, offset by 1 min)
# ---------------------------------------------------------
# Cron: 1,6,11,16,21,26,31,36,41,46,51,56 * * * *
# → runs at xx:01, xx:06, ... (1 min after each 5-min mark,
#   matching the original wait_until_condition() logic)
echo "▶ Creating Cloud Scheduler job..."
gcloud scheduler jobs create http solar-bq-scheduler \
  --location=$REGION \
  --schedule="1,6,11,16,21,26,31,36,41,46,51,56 * * * *" \
  --uri="https://${REGION}-run.googleapis.com/apis/run.googleapis.com/v1/namespaces/${PROJECT_ID}/jobs/${JOB_NAME}:run" \
  --message-body="{}" \
  --oauth-service-account-email=$SA_EMAIL \
  --project=$PROJECT_ID \
  2>/dev/null || \
gcloud scheduler jobs update http solar-bq-scheduler \
  --location=$REGION \
  --schedule="1,6,11,16,21,26,31,36,41,46,51,56 * * * *" \
  --project=$PROJECT_ID

echo ""
echo "✅ Setup complete!"
echo ""
echo "   Test manually:"
echo "   gcloud run jobs execute $JOB_NAME --region=$REGION --project=$PROJECT_ID"
echo ""
echo "   View logs:"
echo "   gcloud logging read 'resource.type=cloud_run_job AND resource.labels.job_name=$JOB_NAME' --limit=50 --project=$PROJECT_ID"
