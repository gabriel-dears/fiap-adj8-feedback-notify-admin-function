#!/bin/bash
set -e

FUNCTION_NAME="notify-admin"
TOPIC_NAME="feedback-alerts"
REGION="us-central1"
ENTRY_POINT="fiap_adj8.feedback_platform.infra.adapter.in.NotifyAdminFunction"

echo "🚀 Deploying $FUNCTION_NAME..."

# 1. Create Pub/Sub topic if not exists
if ! gcloud pubsub topics describe $TOPIC_NAME >/dev/null 2>&1; then
  echo "📌 Creating Pub/Sub topic: $TOPIC_NAME"
  gcloud pubsub topics create $TOPIC_NAME
else
  echo "✅ Topic already exists: $TOPIC_NAME"
fi

# 2. Deploy Cloud Function
gcloud functions deploy $FUNCTION_NAME \
  --runtime java17 \
  --trigger-topic $TOPIC_NAME \
  --entry-point $ENTRY_POINT \
  --region $REGION

echo "✅ Function deployed successfully"

# 3. Send validation message
echo "📨 Sending validation message to topic..."

gcloud pubsub topics publish "$TOPIC_NAME" \
  --message="{
    \"studentName\":\"Deploy Tester\",
    \"lessonName\":\"Kubernetes Basics\",
    \"comment\":\"This is a test alert generated after deploy\",
    \"rating\":\"FIVE\",
    \"date\":\"$(date '+%Y-%m-%dT%H:%M:%S')\"
  }"

echo "🎯 Test message sent. Check logs to verify execution."
echo "👉 gcloud functions logs read $FUNCTION_NAME --region $REGION --limit 50"
