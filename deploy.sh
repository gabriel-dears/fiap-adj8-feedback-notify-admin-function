#!/bin/bash
set -e

########################################
# CONFIGURAÇÕES
########################################
FUNCTION_NAME="notify-admin"
TOPIC_NAME="feedback-alerts"
REGION="us-central1"
ENTRY_POINT="fiap_adj8.feedback_platform.infra.adapter.in.NotifyAdminFunction"
SERVICE_ACCOUNT="sa-deploy-notify-admin@fiap-adj8-feedback-platform.iam.gserviceaccount.com"
RUNTIME="java17"
MEMORY="512MB"
TIMEOUT="60s"

SA_KEY_PATH="$HOME/gcp-keys/sa-deploy-notify-admin-key.json"
PROJECT_ID="fiap-adj8-feedback-platform"
echo "🔐 Autenticando com Service Account de Infra..."
gcloud auth activate-service-account --key-file="$SA_KEY_PATH"
gcloud config set project "$PROJECT_ID"

########################################
# FUNÇÕES
########################################
log() {
  echo -e "[$(date '+%Y-%m-%d %H:%M:%S')] $1"
}

gcloud auth activate-service-account \
  sa-deploy-notify-admin@fiap-adj8-feedback-platform.iam.gserviceaccount.com \
  --key-file="$HOME/gcp-keys/sa-deploy-notify-admin-key.json"

########################################
# 1. Criar tópico Pub/Sub se não existir
########################################
if ! gcloud pubsub topics describe "$TOPIC_NAME" >/dev/null 2>&1; then
  log "📌 Criando Pub/Sub topic: $TOPIC_NAME"
  gcloud pubsub topics create "$TOPIC_NAME" --quiet
else
  log "✅ Topic já existe: $TOPIC_NAME"
fi

########################################
# 2. Deploy / Update da Cloud Function
########################################

########################################
# CARREGAR VARIÁVEIS DO .env
########################################

ENV_FILE="$(dirname "$0")/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "❌ Arquivo .env não encontrado em $ENV_FILE"
  exit 1
fi

set -a
source "$ENV_FILE"
set +a

log "🔄 Gerando arquivo env.yaml para Cloud Function..."

cat > env.yaml <<EOF
ADMIN_SERVICE_BASE_URL: "$ADMIN_SERVICE_BASE_URL"
ADMIN_SERVICE_AUTH: "$ADMIN_SERVICE_AUTH"

EMAIL_SMTP_FROM: "$EMAIL_SMTP_FROM"
EMAIL_SMTP_PASSWORD: "$EMAIL_SMTP_PASSWORD"
EMAIL_SMTP_HOST: "$EMAIL_SMTP_HOST"
EMAIL_SMTP_PORT: "$EMAIL_SMTP_PORT"
EOF

if gcloud functions describe "$FUNCTION_NAME" --region "$REGION" >/dev/null 2>&1; then
  log "🔄 Função $FUNCTION_NAME já existe - atualizando..."
  gcloud functions deploy "$FUNCTION_NAME" \
    --runtime "$RUNTIME" \
    --trigger-topic "$TOPIC_NAME" \
    --entry-point "$ENTRY_POINT" \
    --region "$REGION" \
    --service-account "$SERVICE_ACCOUNT" \
    --memory "$MEMORY" \
    --timeout "$TIMEOUT" \
    --env-vars-file env.yaml \
    --quiet
else
  log "🚀 Criando função $FUNCTION_NAME..."
  gcloud functions deploy "$FUNCTION_NAME" \
    --runtime "$RUNTIME" \
    --trigger-topic "$TOPIC_NAME" \
    --entry-point "$ENTRY_POINT" \
    --region "$REGION" \
    --service-account "$SERVICE_ACCOUNT" \
    --memory "$MEMORY" \
    --timeout "$TIMEOUT" \
    --env-vars-file env.yaml \
    --quiet
fi

########################################
# 3. Enviar mensagem de teste
########################################
log "📨 Enviando mensagem de teste para topic $TOPIC_NAME..."

MESSAGE=$(jq -n \
  --arg studentName "Deploy Tester" \
  --arg lessonName "Kubernetes Basics" \
  --arg comment "This is a test alert generated after deploy" \
  --arg rating "FIVE" \
  --arg date "$(date '+%Y-%m-%dT%H:%M:%S')" \
  '{studentName: $studentName, lessonName: $lessonName, comment: $comment, rating: $rating, date: $date}')

gcloud pubsub topics publish "$TOPIC_NAME" --message="$MESSAGE" --quiet

rm -f env.yaml

########################################
# 4. Logs de validação
########################################
log "🎯 Test message enviada. Confira os logs da função:"
echo "👉 gcloud functions logs read $FUNCTION_NAME --region $REGION --limit 50"
