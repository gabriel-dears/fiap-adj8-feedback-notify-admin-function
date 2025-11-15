#!/bin/bash

set -e  # Para o script se qualquer comando falhar

SERVICE_NAME="notify-admin-function"
IMAGE="gcr.io/fiap-adj8-feedback-platform/notify-admin-func:latest"
REGION="southamerica-east1"
SA="fiap-functions-sa@fiap-adj8-feedback-platform.iam.gserviceaccount.com"

echo "=============================="
echo " 1) BUILD DO MAVEN"
echo "=============================="
mvn clean package -DskipTests

echo "=============================="
echo " 2) BUILD DO DOCKER"
echo "=============================="
docker build -t $IMAGE .

echo "=============================="
echo " 3) PUSH DA IMAGEM"
echo "=============================="
docker push $IMAGE

echo "=============================="
echo " 4) DEPLOY CLOUD RUN"
echo "=============================="
gcloud run deploy $SERVICE_NAME \
  --region=$REGION \
  --image=$IMAGE \
  --platform=managed \
  --no-allow-unauthenticated \
  --memory=512Mi \
  --service-account=$SA

echo "=============================="
echo " 5) TESTE VIA PUB/SUB"
echo "=============================="
gcloud pubsub topics publish notify-admin-topic \
  --message="eyJzdHVkZW50TmFtZSI6ICJHYWJyaWVsIFRlc3RlIiwgImxlc3Nvbk5hbWUiOiAiRGVwbG95IFNlcnZlcmxlc3MgY29tIFF1YXJrdXMiLCAiY29tbWVudCI6ICJPIGRlcGxveSBmb2kgZGVzYWZpYWRvciwgbWFzIG8gQ2xvdWQgUnVuIGZ1bmNpb25vdSBwZXJmZWl0YW1lbnRlIG5vIGZpbmFsISIsICJyYXRpbmciOiAiNSIsICJkYXRlIjogIjIwMjUtMTEtMTRUMTg6MzA6MDAifQo="

echo "=============================="
echo " 6) LOGS"
echo "=============================="
gcloud beta run services logs tail $SERVICE_NAME --region=$REGION
