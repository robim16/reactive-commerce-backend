#!/bin/bash
# infra/localstack/init/init-aws.sh
# Ejecutado automáticamente por LocalStack al arrancar (ready.d hook).
# Requiere permisos de ejecución: git add --chmod=+x infra/localstack/init/init-aws.sh

set -e

export AWS_DEFAULT_REGION="${AWS_REGION:-us-east-1}"
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"
ENDPOINT="http://localhost:4566"

echo "=== Initializing LocalStack resources ==="

# ── S3 ────────────────────────────────────────────────────────────────────────
BUCKET="${AWS_S3_BUCKET:-reactive-commerce-assets}"
echo "Creating S3 bucket: $BUCKET"

aws --endpoint-url=$ENDPOINT s3 mb "s3://$BUCKET" \
  --region "$AWS_DEFAULT_REGION" 2>/dev/null || echo "Bucket already exists, skipping"

aws --endpoint-url=$ENDPOINT s3api put-bucket-cors \
  --bucket "$BUCKET" \
  --cors-configuration '{
    "CORSRules": [{
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET","PUT","POST","DELETE","HEAD"],
      "AllowedOrigins": ["http://localhost:3000","http://localhost:3001"],
      "ExposeHeaders": ["ETag","Content-Length"],
      "MaxAgeSeconds": 3000
    }]
  }' && echo "S3 CORS configured"

echo "S3 bucket ready: $BUCKET"

# ── DynamoDB ──────────────────────────────────────────────────────────────────
TABLE="${AWS_DYNAMODB_TABLE:-download-tokens}"
echo "Creating DynamoDB table: $TABLE"

aws --endpoint-url=$ENDPOINT dynamodb create-table \
  --table-name "$TABLE" \
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
  }]' 2>/dev/null || echo "Table already exists, skipping"

aws --endpoint-url=$ENDPOINT dynamodb update-time-to-live \
  --table-name "$TABLE" \
  --time-to-live-specification Enabled=true,AttributeName=expiresAt \
  2>/dev/null || true

echo "DynamoDB table ready: $TABLE"

# ── SES ───────────────────────────────────────────────────────────────────────
SES_FROM="${AWS_SES_FROM:-noreply@reactivecommerce.com}"
echo "Verifying SES identity: $SES_FROM"

aws --endpoint-url=$ENDPOINT ses verify-email-identity \
  --email-address "$SES_FROM" \
  --region "$AWS_DEFAULT_REGION" 2>/dev/null || true

echo "SES identity verified: $SES_FROM"

echo "=== LocalStack initialization complete ==="
