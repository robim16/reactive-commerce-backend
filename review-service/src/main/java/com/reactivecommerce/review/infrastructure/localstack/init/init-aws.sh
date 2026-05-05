#!/bin/bash
# infra/localstack/init/init-aws.sh
# Ejecutado automáticamente por LocalStack al arrancar (ready.d hook).
# Crea todos los recursos AWS necesarios para el entorno de desarrollo.

set -e

export AWS_DEFAULT_REGION="${AWS_REGION:-us-east-1}"
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"
ENDPOINT="http://localhost:4566"

echo "Initializing LocalStack resources..."

# ── S3 ────────────────────────────────────────────────────────────────────────
echo "Creating S3 bucket: ${AWS_S3_BUCKET:-reactive-commerce-assets}"
aws --endpoint-url=$ENDPOINT s3 mb "s3://${AWS_S3_BUCKET:-reactive-commerce-assets}" || true

# Habilitar CORS en el bucket para que el frontend pueda subir directamente
aws --endpoint-url=$ENDPOINT s3api put-bucket-cors \
  --bucket "${AWS_S3_BUCKET:-reactive-commerce-assets}" \
  --cors-configuration '{
    "CORSRules": [{
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "PUT", "POST", "DELETE", "HEAD"],
      "AllowedOrigins": ["http://localhost:3000"],
      "ExposeHeaders": ["ETag"],
      "MaxAgeSeconds": 3000
    }]
  }'

echo "S3 bucket ready"

# ── DynamoDB ──────────────────────────────────────────────────────────────────
echo "Creating DynamoDB table: ${AWS_DYNAMODB_TABLE:-download-tokens}"
aws --endpoint-url=$ENDPOINT dynamodb create-table \
  --table-name "${AWS_DYNAMODB_TABLE:-download-tokens}" \
  --attribute-definitions \
    AttributeName=id,AttributeType=S \
    AttributeName=orderId,AttributeType=S \
  --key-schema \
    AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --global-secondary-indexes '[{
    "IndexName": "orderId-index",
    "KeySchema": [{"AttributeName":"orderId","KeyType":"HASH"}],
    "Projection": {"ProjectionType":"ALL"}
  }]' || true

# Habilitar TTL nativo (expiresAt en epoch seconds)
aws --endpoint-url=$ENDPOINT dynamodb update-time-to-live \
  --table-name "${AWS_DYNAMODB_TABLE:-download-tokens}" \
  --time-to-live-specification Enabled=true,AttributeName=expiresAt || true

echo "DynamoDB table ready"

# ── SES ───────────────────────────────────────────────────────────────────────
echo "Verifying SES email identity: ${AWS_SES_FROM:-noreply@reactivecommerce.com}"
aws --endpoint-url=$ENDPOINT ses verify-email-identity \
  --email-address "${AWS_SES_FROM:-noreply@reactivecommerce.com}" || true

echo "SES identity verified"

echo "LocalStack initialization complete."
