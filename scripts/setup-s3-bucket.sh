#!/usr/bin/env bash
# Create / configure the public-read assets bucket for Pansari Wala uploads.
# Requires: aws CLI + credentials with s3:CreateBucket, PutBucketPolicy, PutBucketCors, etc.
#
# Usage:
#   export AWS_REGION=ap-south-1
#   export S3_BUCKET=pansariwala-assets
#   bash scripts/setup-s3-bucket.sh
set -euo pipefail

BUCKET="${S3_BUCKET:-pansariwala-assets}"
REGION="${AWS_REGION:-ap-south-1}"

echo "Bucket: s3://$BUCKET ($REGION)"

if aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
  echo "Bucket already exists."
else
  if [[ "$REGION" == "us-east-1" ]]; then
    aws s3api create-bucket --bucket "$BUCKET" --region "$REGION"
  else
    aws s3api create-bucket \
      --bucket "$BUCKET" \
      --region "$REGION" \
      --create-bucket-configuration LocationConstraint="$REGION"
  fi
  echo "Created bucket."
fi

# Public GetObject via bucket policy (no object ACLs). Turn off "Block public policy" blockers.
aws s3api put-public-access-block \
  --bucket "$BUCKET" \
  --public-access-block-configuration \
  'BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=false,RestrictPublicBuckets=false'

POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::${BUCKET}/*"
    }
  ]
}
EOF
)
aws s3api put-bucket-policy --bucket "$BUCKET" --policy "$POLICY"
echo "Applied public GetObject policy."

CORS=$(cat <<'EOF'
{
  "CORSRules": [
    {
      "AllowedHeaders": ["*"],
      "AllowedMethods": ["GET", "HEAD"],
      "AllowedOrigins": [
        "https://pansariwala.shop",
        "https://www.pansariwala.shop",
        "https://api.pansariwala.shop",
        "http://localhost:8080",
        "http://127.0.0.1:8080"
      ],
      "ExposeHeaders": ["ETag", "Content-Length", "Content-Type"],
      "MaxAgeSeconds": 86400
    }
  ]
}
EOF
)
aws s3api put-bucket-cors --bucket "$BUCKET" --cors-configuration "$CORS"
echo "Applied CORS."

echo
echo "Next on the API host (/opt/pansari/env):"
echo "  S3_BUCKET=$BUCKET"
echo "  AWS_REGION=$REGION"
echo "  AWS_ACCESS_KEY_ID=<iam-user-access-key>"
echo "  AWS_SECRET_ACCESS_KEY=<secret>"
echo "  REQUIRE_S3=true"
echo "Then: sudo systemctl restart pansari-server"
echo
echo "IAM user needs at least: s3:PutObject, s3:GetObject, s3:DeleteObject on arn:aws:s3:::${BUCKET}/*"
echo "Test URL shape after upload:"
echo "  https://${BUCKET}.s3.${REGION}.amazonaws.com/partners/user-image/<id>.jpg"
