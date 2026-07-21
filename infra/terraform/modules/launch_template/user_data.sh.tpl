#!/bin/bash
exec > /var/log/userdata.log 2>&1
set -euo pipefail

APP_NAME="${app_name}"
APP_DIR="/var/www/soh-api"
JAR_NAME="app.jar"
ENV_FILE=".env"

S3_BUCKET="${artifact_bucket}"
S3_PATH="${artifact_prefix}"
RELEASE_TYPE="${release_type}"

AWS_REGION="${aws_region}"
S3_REGION="${aws_region}"
SPRING_PROFILE="${spring_profile}"
SERVICE_USER="ec2-user"

echo "===== SOH API USERDATA START ====="
date

if command -v dnf >/dev/null 2>&1; then
  PKG="dnf"
else
  PKG="yum"
fi

$PKG update -y
$PKG install -y java-17-amazon-corretto awscli

mkdir -p "$APP_DIR"
chown "$SERVICE_USER:$SERVICE_USER" "$APP_DIR"
chmod 750 "$APP_DIR"

aws s3 cp "s3://$S3_BUCKET/$S3_PATH/$RELEASE_TYPE/$JAR_NAME" "$APP_DIR/$JAR_NAME" --region "$S3_REGION"
aws s3 cp "s3://$S3_BUCKET/$S3_PATH/$RELEASE_TYPE/$ENV_FILE" "$APP_DIR/$ENV_FILE" --region "$S3_REGION"

chown "$SERVICE_USER:$SERVICE_USER" "$APP_DIR/$JAR_NAME" "$APP_DIR/$ENV_FILE"
chmod 755 "$APP_DIR/$JAR_NAME"
chmod 600 "$APP_DIR/$ENV_FILE"

cat > "/etc/systemd/system/$APP_NAME.service" <<EOF
[Unit]
Description=SOH API ${environment} Server
After=network.target

[Service]
User=$SERVICE_USER
WorkingDirectory=$APP_DIR

EnvironmentFile=$APP_DIR/$ENV_FILE
Environment=SPRING_PROFILES_ACTIVE=$SPRING_PROFILE

ExecStart=/usr/bin/java -Djava.security.egd=file:/dev/./urandom -jar $APP_DIR/$JAR_NAME

Restart=always
RestartSec=10

StandardOutput=append:$APP_DIR/app.log
StandardError=append:$APP_DIR/error.log

LimitNOFILE=65535

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "$APP_NAME"
systemctl restart "$APP_NAME"
systemctl status "$APP_NAME" --no-pager || true

echo "===== SOH API USERDATA FINISHED ====="
date
