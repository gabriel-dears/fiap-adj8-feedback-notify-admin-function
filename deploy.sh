#!/bin/bash

# Stop the script if any command fails
set -e

# --- Configuration Variables ---
SERVICE_NAME="notify-admin-function"
AWS_ACCOUNT_ID="559935791057"
AWS_REGION="sa-east-1"
LAMBDA_ROLE_ARN="arn:aws:iam::559935791057:role/fiap-lambda-execution-role"
SUBNET_ID_1="subnet-0ac90f6cde3ee6ae9"
LAMBDA_SG_ID="sg-05afa452993f6bf37"


# Full Image URI
NOTIFY_IMAGE_URI="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${SERVICE_NAME}:latest"

aws ecr get-login-password --region sa-east-1 | docker login --username AWS --password-stdin 559935791057.dkr.ecr.sa-east-1.amazonaws.com

echo "=============================="
echo " 1) BUILD & TAG DO DOCKER"
echo "=============================="
# Builds the image with a local tag
docker build -t ${SERVICE_NAME}:latest .

# Tags the image with the ECR URI
docker tag ${SERVICE_NAME}:latest ${NOTIFY_IMAGE_URI}

echo "=============================="
echo " 2) PUSH DA IMAGEM PARA ECR"
echo "=============================="
docker push ${NOTIFY_IMAGE_URI}

echo "=============================="
echo " 3) VERIFICAR E DEPLOY/UPDATE AWS LAMBDA"
echo "=============================="

# Check if the Lambda function exists
if aws lambda get-function --function-name ${SERVICE_NAME} --region ${AWS_REGION} > /dev/null 2>&1; then
    # --- Function Exists: UPDATE ---
    echo " Função ${SERVICE_NAME} existe. Realizando UPDATE..."

    # Update Function Code (Image)
    aws lambda update-function-code \
        --function-name ${SERVICE_NAME} \
        --image-uri ${NOTIFY_IMAGE_URI} \
        --region ${AWS_REGION}

    # Update Function Configuration (in case you change Memory/Timeout/VPC settings)
    aws lambda update-function-configuration \
        --function-name ${SERVICE_NAME} \
        --handler "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest" \
        --timeout 30 \
        --memory-size 512 \
        --region ${AWS_REGION} \
        --vpc-config SubnetIds=${SUBNET_ID_1},SecurityGroupIds=${LAMBDA_SG_ID}

    echo " ✅ UPDATE COMPLETO."

else
    # --- Function Does Not Exist: CREATE ---
    echo " Função ${SERVICE_NAME} não existe. Realizando CREATE..."

    # Create Function
    aws lambda create-function \
        --function-name ${SERVICE_NAME} \
        --package-type Image \
        --code ImageUri=${NOTIFY_IMAGE_URI} \
        --role ${LAMBDA_ROLE_ARN} \
        --timeout 30 \
        --memory-size 512 \
        --region ${AWS_REGION} \
        --vpc-config SubnetIds=${SUBNET_ID_1},SecurityGroupIds=${LAMBDA_SG_ID}

    echo " ✅ CRIAÇÃO COMPLETA."

fi
#echo "=============================="
#echo " 4) TESTE VIA PUB/SUB"
#echo "=============================="
#gcloud pubsub topics publish notify-admin-topic \
#  --message="eyJzdHVkZW50TmFtZSI6ICJHYWJyaWVsIFRlc3RlIiwgImxlc3Nvbk5hbWUiOiAiRGVwbG95IFNlcnZlcmxlc3MgY29tIFF1YXJrdXMiLCAiY29tbWVudCI6ICJPIGRlcGxveSBmb2kgZGVzYWZpYWRvciwgbWFzIG8gQ2xvdWQgUnVuIGZ1bmNpb25vdSBwZXJmZWl0YW1lbnRlIG5vIGZpbmFsISIsICJyYXRpbmciOiAiNSIsICJkYXRlIjogIjIwMjUtMTEtMTRUMTg6MzA6MDAifQo="

#echo "=============================="
#echo " 5) LOGS"
#echo "=============================="
#gcloud beta run services logs tail $SERVICE_NAME --region=$REGION
