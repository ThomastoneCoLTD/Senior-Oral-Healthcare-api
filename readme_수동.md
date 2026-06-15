# SOH API AWS 수동 구축 가이드

이 문서는 Terraform/GitHub Actions 없이 AWS 웹 콘솔에서 SOH API 운영 환경을 처음부터 직접 구축할 때 따라가는 절차다.

기준 리전은 `ap-northeast-2`다. AWS 콘솔 UI 명칭은 조금씩 바뀔 수 있으므로, 콘솔 상단 검색창에서 서비스 이름을 검색해서 이동하는 방식으로 진행한다.

## 0. 전체 구조

```text
사용자
-> 기존 Front CloudFront
-> /api/* behavior
-> API ALB
-> Target Group
-> Auto Scaling Group
-> Private Subnet EC2
-> systemd SOH API
-> S3 artifact bucket에서 app.jar/.env 다운로드
-> Private DB Subnet RDS MySQL
```

수동 구축 대상:

```text
VPC
Public Subnet 2개
Private App Subnet 2개
Internet Gateway
NAT Gateway
S3 Gateway Endpoint
Security Group
IAM Role / Instance Profile
ALB
Target Group
RDS MySQL
Launch Template
Auto Scaling Group
Route 53 API origin record
CloudFront /api/* behavior
S3 artifact upload
ASG Instance Refresh
```

## 1. 환경 매핑

개발 API:

```text
Branch: dev
Release type: dev
Artifact S3 path: s3://denti-backends/soh/dev/app.jar
Env S3 path: s3://denti-backends/soh/dev/.env
ASG: soh-api-dev-asg
EC2 service: soh-api-dev
ALB: soh-api-dev-alb
Target Group: soh-api-dev-tg
RDS: soh-api-dev-mysql
RDS class: db.t3.small
API origin domain: soh-api-dev.thomabio.com
Frontend URL: https://soh-dev.thomabio.com
Health URL: https://soh-dev.thomabio.com/api/actuator/health
```

운영 API:

```text
Branch: prod
Release type: prod
Artifact S3 path: s3://denti-backends/soh/prod/app.jar
Env S3 path: s3://denti-backends/soh/prod/.env
ASG: soh-api-prod-asg
EC2 service: soh-api-prod
ALB: soh-api-prod-alb
Target Group: soh-api-prod-tg
RDS: soh-api-prod-mysql
RDS class: db.t3.small
API origin domain: soh-api.thomabio.com
Frontend URL: https://soh.thomabio.com
Health URL: https://soh.thomabio.com/api/actuator/health
```

`main` 브랜치는 배포하지 않는다.

## 2. 사전 준비

AWS 콘솔에서 먼저 확인한다.

```text
AWS Account ID: 160885266674
Region: ap-northeast-2
Artifact bucket: denti-backends
Hosted zone: thomabio.com
ACM certificate: soh-api-dev.thomabio.com / soh-api.thomabio.com에 사용할 인증서
RDS engine: MySQL 8.0
RDS instance class: db.t3.small
EC2 instance type: t3.medium
```

체크:

1. AWS 콘솔 우측 상단 리전이 `서울(ap-northeast-2)`인지 확인한다.
2. S3 콘솔에서 `denti-backends` bucket이 있는지 확인한다.
3. `denti-backends` bucket의 리전이 API VPC 리전과 맞는지 확인한다.
4. ACM 콘솔에서 ALB용 인증서가 `ap-northeast-2`에 있는지 확인한다.
5. Route 53에 `thomabio.com` hosted zone이 있는지 확인한다.

S3 bucket region은 CLI가 가능하면 아래로 확인할 수 있다.

```bash
aws s3api get-bucket-location --bucket denti-backends
```

## 3. S3 Artifact Bucket 준비

AWS 콘솔:

```text
S3 -> Buckets -> denti-backends
```

없으면 생성:

```text
Bucket name: denti-backends
Region: ap-northeast-2
Block all public access: ON
Bucket Versioning: 권장 ON
Default encryption: SSE-S3 또는 SSE-KMS
```

폴더는 콘솔에서 명시적으로 만들어도 되고, 파일 업로드 시 key로 자동 생성해도 된다.

```text
soh/dev/app.jar
soh/dev/.env
soh/prod/app.jar
soh/prod/.env
```

주의:

- `.env`는 민감정보이므로 repository에 커밋하지 않는다.
- EC2는 Instance Profile로 S3를 읽는다.
- User Data나 `.env`에 AWS access key를 넣지 않는다.

## 4. VPC 생성

AWS 콘솔:

```text
VPC -> Your VPCs -> Create VPC
```

개발 VPC:

```text
Name: soh-api-dev-vpc
IPv4 CIDR: 10.70.0.0/16
IPv6: 없음
Tenancy: Default
```

운영 VPC:

```text
Name: soh-api-prod-vpc
IPv4 CIDR: 10.80.0.0/16
IPv6: 없음
Tenancy: Default
```

생성 후 VPC 설정에서 아래를 확인한다.

```text
DNS resolution: Enabled
DNS hostnames: Enabled
```

## 5. Subnet 생성

AWS 콘솔:

```text
VPC -> Subnets -> Create subnet
```

개발:

```text
VPC: soh-api-dev-vpc

Public subnet 1
Name: soh-api-dev-public-1
AZ: ap-northeast-2a
CIDR: 10.70.0.0/24

Public subnet 2
Name: soh-api-dev-public-2
AZ: ap-northeast-2c
CIDR: 10.70.1.0/24

Private app subnet 1
Name: soh-api-dev-private-app-1
AZ: ap-northeast-2a
CIDR: 10.70.10.0/24

Private app subnet 2
Name: soh-api-dev-private-app-2
AZ: ap-northeast-2c
CIDR: 10.70.11.0/24

Private DB subnet 1
Name: soh-api-dev-private-db-1
AZ: ap-northeast-2a
CIDR: 10.70.20.0/24

Private DB subnet 2
Name: soh-api-dev-private-db-2
AZ: ap-northeast-2c
CIDR: 10.70.21.0/24
```

운영:

```text
VPC: soh-api-prod-vpc

Public subnet 1
Name: soh-api-prod-public-1
AZ: ap-northeast-2a
CIDR: 10.80.0.0/24

Public subnet 2
Name: soh-api-prod-public-2
AZ: ap-northeast-2c
CIDR: 10.80.1.0/24

Private app subnet 1
Name: soh-api-prod-private-app-1
AZ: ap-northeast-2a
CIDR: 10.80.10.0/24

Private app subnet 2
Name: soh-api-prod-private-app-2
AZ: ap-northeast-2c
CIDR: 10.80.11.0/24

Private DB subnet 1
Name: soh-api-prod-private-db-1
AZ: ap-northeast-2a
CIDR: 10.80.20.0/24

Private DB subnet 2
Name: soh-api-prod-private-db-2
AZ: ap-northeast-2c
CIDR: 10.80.21.0/24
```

Public subnet은 생성 후:

```text
Subnet 선택 -> Actions -> Edit subnet settings
Enable auto-assign public IPv4 address: ON
```

Private subnet은 auto-assign public IPv4를 켜지 않는다.

## 6. Internet Gateway 생성 및 연결

AWS 콘솔:

```text
VPC -> Internet gateways -> Create internet gateway
```

개발:

```text
Name: soh-api-dev-igw
Attach to VPC: soh-api-dev-vpc
```

운영:

```text
Name: soh-api-prod-igw
Attach to VPC: soh-api-prod-vpc
```

## 7. Route Table 구성

AWS 콘솔:

```text
VPC -> Route tables -> Create route table
```

개발 public route table:

```text
Name: soh-api-dev-public-rt
VPC: soh-api-dev-vpc
Route: 0.0.0.0/0 -> soh-api-dev-igw
Subnet associations: soh-api-dev-public-1, soh-api-dev-public-2
```

운영 public route table:

```text
Name: soh-api-prod-public-rt
VPC: soh-api-prod-vpc
Route: 0.0.0.0/0 -> soh-api-prod-igw
Subnet associations: soh-api-prod-public-1, soh-api-prod-public-2
```

Private route table은 NAT Gateway 생성 후 연결한다.

## 8. NAT Gateway 생성

AWS 콘솔:

```text
VPC -> NAT gateways -> Create NAT gateway
```

개발은 비용 절감을 위해 기본 1개:

```text
Name: soh-api-dev-nat-1
Subnet: soh-api-dev-public-1
Connectivity type: Public
Elastic IP: Allocate Elastic IP
```

운영은 권장 2개:

```text
Name: soh-api-prod-nat-1
Subnet: soh-api-prod-public-1
Elastic IP: Allocate Elastic IP

Name: soh-api-prod-nat-2
Subnet: soh-api-prod-public-2
Elastic IP: Allocate Elastic IP
```

운영 비용을 줄여야 하면 NAT 1개로 시작할 수 있지만, AZ 장애 내성은 낮아진다.

## 9. Private Route Table 구성

개발:

```text
Name: soh-api-dev-private-rt
VPC: soh-api-dev-vpc
Route: 0.0.0.0/0 -> soh-api-dev-nat-1
Subnet associations:
  soh-api-dev-private-app-1
  soh-api-dev-private-app-2
  soh-api-dev-private-db-1
  soh-api-dev-private-db-2
```

운영 NAT 2개 구성:

```text
Name: soh-api-prod-private-rt-1
VPC: soh-api-prod-vpc
Route: 0.0.0.0/0 -> soh-api-prod-nat-1
Subnet associations:
  soh-api-prod-private-app-1
  soh-api-prod-private-db-1

Name: soh-api-prod-private-rt-2
VPC: soh-api-prod-vpc
Route: 0.0.0.0/0 -> soh-api-prod-nat-2
Subnet associations:
  soh-api-prod-private-app-2
  soh-api-prod-private-db-2
```

## 10. S3 Gateway Endpoint 생성

S3 Gateway Endpoint는 private subnet EC2가 NAT 없이 S3에 접근할 수 있게 해준다.

AWS 콘솔:

```text
VPC -> Endpoints -> Create endpoint
```

설정:

```text
Service category: AWS services
Service type filter: Gateway
Service: com.amazonaws.ap-northeast-2.s3
VPC: 대상 VPC
Route tables: private route table 선택
Policy: Full access 또는 denti-backends read-only 정책
```

개발:

```text
Name: soh-api-dev-s3-gateway-endpoint
VPC: soh-api-dev-vpc
Route tables: soh-api-dev-private-rt
```

운영:

```text
Name: soh-api-prod-s3-gateway-endpoint
VPC: soh-api-prod-vpc
Route tables:
  soh-api-prod-private-rt-1
  soh-api-prod-private-rt-2
```

주의:

- S3 Gateway Endpoint는 리전 종속이다.
- `denti-backends`가 다른 리전에 있으면 이 경로로 정상 동작하지 않을 수 있다.

## 11. IAM Role / Instance Profile 생성

AWS 콘솔:

```text
IAM -> Roles -> Create role
```

Trusted entity:

```text
Trusted entity type: AWS service
Use case: EC2
```

개발 role:

```text
Role name: soh-api-dev-ec2-role
Attach policy: AmazonSSMManagedInstanceCore
Inline policy: ReadSohDevApiArtifacts
```

Inline policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadSohDevApiArtifacts",
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/dev/app.jar",
        "arn:aws:s3:::denti-backends/soh/dev/.env"
      ]
    }
  ]
}
```

운영 role:

```text
Role name: soh-api-prod-ec2-role
Attach policy: AmazonSSMManagedInstanceCore
Inline policy: ReadSohProdApiArtifacts
```

Inline policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "ReadSohProdApiArtifacts",
      "Effect": "Allow",
      "Action": ["s3:GetObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/prod/app.jar",
        "arn:aws:s3:::denti-backends/soh/prod/.env"
      ]
    }
  ]
}
```

EC2에서 S3를 읽을 때 AWS access key를 쓰지 않는다. 반드시 이 IAM role을 사용한다.

## 12. Security Group 생성

AWS 콘솔:

```text
EC2 -> Security Groups -> Create security group
```

개발 ALB SG:

```text
Name: soh-api-dev-alb-sg
VPC: soh-api-dev-vpc
Inbound:
  HTTPS 443 from 0.0.0.0/0
  HTTPS 443 from ::/0
Outbound:
  All traffic
```

운영 ALB SG:

```text
Name: soh-api-prod-alb-sg
VPC: soh-api-prod-vpc
Inbound:
  HTTPS 443 from 0.0.0.0/0
  HTTPS 443 from ::/0
Outbound:
  All traffic
```

HTTP 80 redirect listener를 쓸 경우 ALB SG에 `HTTP 80` inbound도 추가한다.

운영 전 권장:

```text
ALB inbound를 CloudFront origin-facing managed prefix list로 제한
```

개발 EC2 SG:

```text
Name: soh-api-dev-ec2-sg
VPC: soh-api-dev-vpc
Inbound:
  TCP 8080 from soh-api-dev-alb-sg
Outbound:
  All traffic
```

운영 EC2 SG:

```text
Name: soh-api-prod-ec2-sg
VPC: soh-api-prod-vpc
Inbound:
  TCP 8080 from soh-api-prod-alb-sg
Outbound:
  All traffic
```

개발 RDS SG:

```text
Name: soh-api-dev-rds-sg
VPC: soh-api-dev-vpc
Inbound:
  TCP 3306 from soh-api-dev-ec2-sg
Outbound:
  All traffic
```

운영 RDS SG:

```text
Name: soh-api-prod-rds-sg
VPC: soh-api-prod-vpc
Inbound:
  TCP 3306 from soh-api-prod-ec2-sg
Outbound:
  All traffic
```

SSH는 기본적으로 열지 않는다. 접속은 SSM Session Manager를 사용한다.

## 13. RDS MySQL 생성

AWS 콘솔:

```text
RDS -> Subnet groups -> Create DB subnet group
RDS -> Databases -> Create database
```

DB subnet group:

```text
dev name: soh-api-dev-db-subnet-group
dev VPC: soh-api-dev-vpc
dev subnets:
  soh-api-dev-private-db-1
  soh-api-dev-private-db-2

prod name: soh-api-prod-db-subnet-group
prod VPC: soh-api-prod-vpc
prod subnets:
  soh-api-prod-private-db-1
  soh-api-prod-private-db-2
```

공통 선택:

```text
Engine type: MySQL
Engine version: MySQL 8.0
Templates:
  dev -> Free tier 또는 Dev/Test
  prod -> Production 또는 Dev/Test에서 운영 정책에 맞게 조정
DB instance class: db.t3.small
Storage type: gp3
Allocated storage: 20 GiB
Storage autoscaling: Enable
Maximum storage threshold: 100 GiB
Public access: No
Master username: sohadmin
Credential management: Manage master credentials in AWS Secrets Manager 권장
Initial database name: thomastone
```

개발 RDS:

```text
DB instance identifier: soh-api-dev-mysql
VPC: soh-api-dev-vpc
DB subnet group: soh-api-dev-db-subnet-group
Subnets:
  soh-api-dev-private-db-1
  soh-api-dev-private-db-2
VPC security group: soh-api-dev-rds-sg
Multi-AZ: No
Backup retention: 3 days
Deletion protection: Off
```

운영 RDS:

```text
DB instance identifier: soh-api-prod-mysql
VPC: soh-api-prod-vpc
DB subnet group: soh-api-prod-db-subnet-group
Subnets:
  soh-api-prod-private-db-1
  soh-api-prod-private-db-2
VPC security group: soh-api-prod-rds-sg
Multi-AZ: No, 운영 HA가 승인되면 Yes로 변경
Backup retention: 7 days 이상
Deletion protection: On
Final snapshot: 삭제 시 반드시 생성
```

생성 후 확인:

```text
RDS endpoint를 기록한다.
Secrets Manager에서 master credential secret ARN과 password를 확인한다.
GitHub Secret SOH_API_ENV_DEV / SOH_API_ENV_PROD의 SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD에 반영한다.
```

주의:

- RDS는 private DB subnet에 두고 public access를 열지 않는다.
- RDS SG inbound는 EC2 SG에서 오는 TCP 3306만 허용한다.
- 운영 DB 삭제 보호를 끄거나 final snapshot을 생략하지 않는다.

## 14. Target Group 생성

AWS 콘솔:

```text
EC2 -> Target Groups -> Create target group
```

개발:

```text
Target type: Instances
Name: soh-api-dev-tg
Protocol: HTTP
Port: 8080
VPC: soh-api-dev-vpc
Health check protocol: HTTP
Health check path: /api/actuator/health
Success codes: 200
Advanced health check:
  Interval: 30
  Timeout: 5
  Healthy threshold: 2
  Unhealthy threshold: 3
Deregistration delay: 45 seconds
```

운영:

```text
Target type: Instances
Name: soh-api-prod-tg
Protocol: HTTP
Port: 8080
VPC: soh-api-prod-vpc
Health check path: /api/actuator/health
Success codes: 200
Deregistration delay: 45 seconds
```

초기 생성 시 target 등록은 비워도 된다. ASG가 자동으로 붙인다.

## 15. ALB 생성

AWS 콘솔:

```text
EC2 -> Load Balancers -> Create load balancer -> Application Load Balancer
```

개발:

```text
Name: soh-api-dev-alb
Scheme: Internet-facing
IP address type: IPv4
VPC: soh-api-dev-vpc
Mappings:
  ap-northeast-2a -> soh-api-dev-public-1
  ap-northeast-2c -> soh-api-dev-public-2
Security group: soh-api-dev-alb-sg
Listener:
  HTTPS 443 -> forward to soh-api-dev-tg
Certificate: ap-northeast-2 ACM certificate
```

운영:

```text
Name: soh-api-prod-alb
Scheme: Internet-facing
IP address type: IPv4
VPC: soh-api-prod-vpc
Mappings:
  ap-northeast-2a -> soh-api-prod-public-1
  ap-northeast-2c -> soh-api-prod-public-2
Security group: soh-api-prod-alb-sg
Listener:
  HTTPS 443 -> forward to soh-api-prod-tg
Certificate: ap-northeast-2 ACM certificate
```

선택: HTTP 80 redirect

```text
Listener HTTP 80
Action: Redirect to HTTPS 443
Status: HTTP_301
```

이 경우 ALB SG에 `HTTP 80` inbound가 필요하다.

## 16. Route 53 API Origin Record 생성

AWS 콘솔:

```text
Route 53 -> Hosted zones -> thomabio.com -> Create record
```

개발:

```text
Record name: soh-api-dev
Record type: A
Alias: ON
Route traffic to: Alias to Application and Classic Load Balancer
Region: ap-northeast-2
Load balancer: soh-api-dev-alb
```

운영:

```text
Record name: soh-api
Record type: A
Alias: ON
Route traffic to: Alias to Application and Classic Load Balancer
Region: ap-northeast-2
Load balancer: soh-api-prod-alb
```

생성 후 확인:

```bash
nslookup soh-api-dev.thomabio.com
nslookup soh-api.thomabio.com
```

## 17. Launch Template 생성

AWS 콘솔:

```text
EC2 -> Launch Templates -> Create launch template
```

공통:

```text
AMI: Amazon Linux 2023 최신 x86_64
Instance type: t3.medium
Key pair: None 권장
Network settings:
  Do not include subnet in launch template
Security group:
  dev -> soh-api-dev-ec2-sg
  prod -> soh-api-prod-ec2-sg
IAM instance profile:
  dev -> soh-api-dev-ec2-role
  prod -> soh-api-prod-ec2-role
Advanced details -> User data: 아래 스크립트
```

개발 Launch Template:

```text
Name: soh-api-dev-lt
Service name: soh-api-dev
Release type: dev
Spring profile: dev
```

운영 Launch Template:

```text
Name: soh-api-prod-lt
Service name: soh-api-prod
Release type: prod
Spring profile: prod
```

User Data는 환경에 맞게 `APP_NAME`, `RELEASE_TYPE`, `SPRING_PROFILE`만 바꾼다.

개발 User Data:

```bash
#!/bin/bash
exec > /var/log/userdata.log 2>&1
set -euo pipefail

APP_NAME="soh-api-dev"
APP_DIR="/var/www/soh-api"
JAR_NAME="app.jar"
ENV_FILE=".env"

S3_BUCKET="denti-backends"
S3_PATH="soh"
RELEASE_TYPE="dev"

AWS_REGION="ap-northeast-2"
S3_REGION="ap-northeast-2"
SPRING_PROFILE="dev"
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
Description=SOH API dev Server
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
```

운영은 아래 값만 바꾼다.

```text
APP_NAME="soh-api-prod"
RELEASE_TYPE="prod"
SPRING_PROFILE="prod"
Description=SOH API prod Server
```

## 18. Auto Scaling Group 생성

AWS 콘솔:

```text
EC2 -> Auto Scaling Groups -> Create Auto Scaling group
```

개발:

```text
Name: soh-api-dev-asg
Launch template: soh-api-dev-lt
VPC: soh-api-dev-vpc
Subnets:
  soh-api-dev-private-app-1
  soh-api-dev-private-app-2
Load balancing: Attach to existing load balancer target group
Target group: soh-api-dev-tg
Health checks:
  ELB health checks: ON
  Health check grace period: 300 seconds
Group size:
  Desired: 1
  Min: 1
  Max: 2
```

운영:

```text
Name: soh-api-prod-asg
Launch template: soh-api-prod-lt
VPC: soh-api-prod-vpc
Subnets:
  soh-api-prod-private-app-1
  soh-api-prod-private-app-2
Load balancing: Attach to existing load balancer target group
Target group: soh-api-prod-tg
Health checks:
  ELB health checks: ON
  Health check grace period: 300 seconds
Group size:
  Desired: 2
  Min: 2
  Max: 4
```

Instance Refresh 기본값:

```text
Strategy: Rolling
Min healthy percentage: 100
Instance warmup: 180 seconds
```

## 19. API Artifact 수동 업로드

로컬에서 JAR 생성:

```powershell
cd C:\Users\ethanj\Documents\GitHub\Senior-Oral-Healthcare-api\api_server
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat clean bootJar -x test -x asciidoctor
```

산출물:

```text
api_server/build/libs/*.jar
```

JAR 파일 이름을 `app.jar`로 바꿔 업로드 준비한다.

개발 `.env` 예시:

```text
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
SERVER_SERVLET_CONTEXT_PATH=/api
FRONTEND_ORIGIN=https://soh-dev.thomabio.com
CORS_ALLOWED_ORIGINS=https://soh-dev.thomabio.com
SPRING_DATASOURCE_URL=jdbc:mysql://<DEV_RDS_ENDPOINT>:3306/thomastone?serverTimezone=Asia/Seoul&zeroDateTimeBehavior=convertToNull
SPRING_DATASOURCE_USERNAME=sohadmin
SPRING_DATASOURCE_PASSWORD=<DEV_RDS_PASSWORD_FROM_SECRETS_MANAGER>
JWT_SECRET=<DEV_JWT_SECRET>
```

운영 `.env` 예시:

```text
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
SERVER_SERVLET_CONTEXT_PATH=/api
FRONTEND_ORIGIN=https://soh.thomabio.com
CORS_ALLOWED_ORIGINS=https://soh.thomabio.com
SPRING_DATASOURCE_URL=jdbc:mysql://<PROD_RDS_ENDPOINT>:3306/thomastone?serverTimezone=Asia/Seoul&zeroDateTimeBehavior=convertToNull
SPRING_DATASOURCE_USERNAME=sohadmin
SPRING_DATASOURCE_PASSWORD=<PROD_RDS_PASSWORD_FROM_SECRETS_MANAGER>
JWT_SECRET=<PROD_JWT_SECRET>
```

S3 콘솔 업로드:

```text
S3 -> denti-backends -> soh/dev/
Upload app.jar
Upload .env

S3 -> denti-backends -> soh/prod/
Upload app.jar
Upload .env
```

절대 주의:

- `.env`를 git에 커밋하지 않는다.
- `.env`에 AWS key를 넣지 않는다.
- dev 파일을 `soh/prod/`에 올리지 않는다.
- prod 파일을 `soh/dev/`에 올리지 않는다.

## 20. ASG Instance Refresh 수동 실행

AWS 콘솔:

```text
EC2 -> Auto Scaling Groups -> 대상 ASG 선택 -> Instance refresh 탭 -> Start instance refresh
```

개발:

```text
ASG: soh-api-dev-asg
Min healthy percentage: 100
Instance warmup: 180 seconds
```

운영:

```text
ASG: soh-api-prod-asg
Min healthy percentage: 100
Instance warmup: 180 seconds
```

진행 중 확인:

```text
Auto Scaling Groups -> Instance refresh 탭
EC2 -> Instances -> 새 인스턴스 상태 확인
Target Groups -> Targets -> health 상태 확인
```

## 21. CloudFront /api/* 연결

기존 프론트 CloudFront distribution을 수정한다.

개발 CloudFront:

```text
Distribution ID: E14WPL6NG95U7H
Domain: soh-dev.thomabio.com
API origin: soh-api-dev.thomabio.com
```

운영 CloudFront:

```text
Distribution ID: E3BD44T0U5EBYT
Domain: soh.thomabio.com
API origin: soh-api.thomabio.com
```

AWS 콘솔:

```text
CloudFront -> Distributions -> 대상 distribution 선택
Origins -> Create origin
```

Origin 설정:

```text
Origin domain:
  dev -> soh-api-dev.thomabio.com
  prod -> soh-api.thomabio.com
Protocol: HTTPS only
HTTPS port: 443
```

Behavior 추가:

```text
Behaviors -> Create behavior
Path pattern: /api/*
Origin: 방금 만든 API origin
Viewer protocol policy: Redirect HTTP to HTTPS 또는 HTTPS only
Allowed methods: GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE
Cache policy: CachingDisabled
Origin request policy: Authorization, Content-Type, query string 등 API에 필요한 값 전달
```

SPA fallback 주의:

```text
CloudFront custom error response 403/404 -> /index.html 200
```

이 설정이 있으면 API의 403/404도 `index.html`로 바뀔 수 있다. `/api/*`는 fallback 대상에서 제외되도록 CloudFront Function을 사용한다.

예시:

```js
function handler(event) {
  var request = event.request;
  var uri = request.uri;

  if (uri.startsWith('/api/')) {
    return request;
  }

  if (uri.endsWith('/')) {
    request.uri = uri + 'index.html';
  } else if (!uri.includes('.')) {
    request.uri = '/index.html';
  }

  return request;
}
```

## 22. 동작 확인

S3 확인:

```bash
aws s3 ls s3://denti-backends/soh/dev/ --region ap-northeast-2
aws s3 ls s3://denti-backends/soh/prod/ --region ap-northeast-2
```

Target Group 확인:

```text
EC2 -> Target Groups -> soh-api-*-tg -> Targets
Health status가 healthy인지 확인
```

SSM 접속:

```text
EC2 -> Instances -> 대상 인스턴스 선택 -> Connect -> Session Manager
```

EC2 내부 로그:

```bash
sudo cat /var/log/userdata.log
sudo cat /var/log/cloud-init-output.log
sudo systemctl status soh-api-dev
sudo systemctl status soh-api-prod
tail -f /var/www/soh-api/app.log
tail -f /var/www/soh-api/error.log
curl -i http://localhost:8080/api/actuator/health
```

외부 확인:

```bash
curl -i https://soh-dev.thomabio.com/api/actuator/health
curl -i https://soh.thomabio.com/api/actuator/health
```

## 23. 장애 대응 체크리스트

Target Group이 unhealthy:

1. EC2가 private subnet에 있는지 확인한다.
2. EC2 SG가 ALB SG로부터 TCP 8080을 허용하는지 확인한다.
3. systemd 서비스가 실행 중인지 확인한다.
4. `/api/actuator/health`가 200을 반환하는지 확인한다.
5. ALB target group health path가 `/api/actuator/health`인지 확인한다.

EC2가 S3에서 다운로드 실패:

1. EC2 IAM Role이 정확한 S3 object ARN을 허용하는지 확인한다.
2. S3 Gateway Endpoint가 private route table에 연결됐는지 확인한다.
3. `denti-backends` bucket region이 `ap-northeast-2`인지 확인한다.
4. S3 object path가 dev/prod에 맞는지 확인한다.

EC2가 RDS에 연결 실패:

1. RDS가 private DB subnet group에 생성됐는지 확인한다.
2. RDS SG가 EC2 SG로부터 TCP 3306을 허용하는지 확인한다.
3. `.env`의 `SPRING_DATASOURCE_URL` endpoint와 database name이 맞는지 확인한다.
4. Secrets Manager의 password를 `SPRING_DATASOURCE_PASSWORD`에 정확히 반영했는지 확인한다.
5. RDS status가 `Available`이고 backup/maintenance 작업 중이 아닌지 확인한다.

CloudFront에서 API가 index.html로 반환됨:

1. `/api/*` behavior가 default behavior보다 위에 있는지 확인한다.
2. `/api/*` origin이 API ALB origin인지 확인한다.
3. SPA fallback이 `/api/*`에 적용되지 않도록 CloudFront Function을 적용한다.

배포 후 이전 버전이 계속 보임:

1. S3에 새 `app.jar`가 업로드됐는지 확인한다.
2. ASG Instance Refresh가 시작됐는지 확인한다.
3. 새 인스턴스가 Target Group에서 healthy가 됐는지 확인한다.
4. 기존 인스턴스가 종료됐는지 확인한다.

## 24. 수동 구축 완료 체크리스트

```text
[ ] VPC 생성
[ ] Public subnet 2개 생성
[ ] Private app subnet 2개 생성
[ ] Private DB subnet 2개 생성
[ ] Internet Gateway 연결
[ ] NAT Gateway 생성
[ ] Public/private route table 연결
[ ] S3 Gateway Endpoint 연결
[ ] EC2 IAM role 생성
[ ] ALB/EC2/RDS Security Group 생성
[ ] RDS MySQL 생성
[ ] RDS endpoint와 Secrets Manager password를 .env/GitHub Secret에 반영
[ ] Target Group 생성
[ ] ALB HTTPS listener 생성
[ ] Route 53 API origin record 생성
[ ] Launch Template 생성
[ ] Auto Scaling Group 생성
[ ] S3 app.jar/.env 업로드
[ ] ASG Instance Refresh 실행
[ ] Target Group healthy 확인
[ ] CloudFront /api/* behavior 추가
[ ] Health endpoint 확인
[ ] README.md / AGENTS.md 변경사항 반영
```

## 참고 자료

- AWS VPC 생성: https://docs.aws.amazon.com/vpc/latest/userguide/create-vpc.html
- S3 Gateway Endpoint: https://docs.aws.amazon.com/vpc/latest/privatelink/vpc-endpoints-s3.html
- RDS DB instance in VPC: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_VPC.WorkingWithRDSInstanceinaVPC.html
- RDS security groups: https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Overview.RDSSecurityGroups.html
- ALB Target Group 생성: https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-target-group.html
- ALB HTTPS Listener: https://docs.aws.amazon.com/elasticloadbalancing/latest/application/create-https-listener.html
- ASG Instance Refresh: https://docs.aws.amazon.com/autoscaling/ec2/userguide/start-instance-refresh.html
- CloudFront origin request policy: https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/understanding-how-origin-request-policies-and-cache-policies-work-together.html
