#!/bin/bash
# =============================================================
# GitHub Actions – Workload Identity Federation Setup
# Run ONCE after setup.sh to enable keyless GCP auth from GitHub
# =============================================================
set -euo pipefail

PROJECT_ID="modern-cubist-412113"
PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
SA_EMAIL="solar-runner@${PROJECT_ID}.iam.gserviceaccount.com"
GITHUB_REPO="YOUR_GITHUB_USERNAME/YOUR_REPO_NAME"   # ← update this!

POOL_NAME="github-pool"
PROVIDER_NAME="github-provider"

echo "▶ Project number: $PROJECT_NUMBER"

# ---------------------------------------------------------
# 1) Workload Identity Pool
# ---------------------------------------------------------
echo "▶ Creating Workload Identity Pool..."
gcloud iam workload-identity-pools create $POOL_NAME \
  --location=global \
  --display-name="GitHub Actions Pool" \
  --project=$PROJECT_ID \
  2>/dev/null || echo "  (already exists)"

POOL_ID=$(gcloud iam workload-identity-pools describe $POOL_NAME \
  --location=global \
  --project=$PROJECT_ID \
  --format="value(name)")

# ---------------------------------------------------------
# 2) OIDC Provider for GitHub
# ---------------------------------------------------------
echo "▶ Creating OIDC Provider..."
gcloud iam workload-identity-pools providers create-oidc $PROVIDER_NAME \
  --location=global \
  --workload-identity-pool=$POOL_NAME \
  --display-name="GitHub Provider" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='${GITHUB_REPO}'" \
  --project=$PROJECT_ID \
  2>/dev/null || echo "  (already exists)"

PROVIDER_ID=$(gcloud iam workload-identity-pools providers describe $PROVIDER_NAME \
  --location=global \
  --workload-identity-pool=$POOL_NAME \
  --project=$PROJECT_ID \
  --format="value(name)")

# ---------------------------------------------------------
# 3) Allow GitHub to impersonate the Service Account
# ---------------------------------------------------------
echo "▶ Binding Service Account..."
gcloud iam service-accounts add-iam-policy-binding $SA_EMAIL \
  --role=roles/iam.workloadIdentityUser \
  --member="principalSet://iam.googleapis.com/${POOL_ID}/attribute.repository/${GITHUB_REPO}" \
  --project=$PROJECT_ID

# ---------------------------------------------------------
# 4) Print GitHub Secrets to add
# ---------------------------------------------------------
echo ""
echo "✅ Done! Add these two secrets to your GitHub repo:"
echo "   (Settings → Secrets and variables → Actions → New repository secret)"
echo ""
echo "   WIF_PROVIDER:"
echo "   $PROVIDER_ID"
echo ""
echo "   WIF_SERVICE_ACCOUNT:"
echo "   $SA_EMAIL"
echo ""
echo "   Then push to main – GitHub Actions will deploy automatically!"
