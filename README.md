# Senior-Oral-Healthcare API

SOH API is a Spring Boot API server. The current project lives under `api_server` and uses Gradle Wrapper with Java 17.

```text
Build tool: Gradle Wrapper
Java: 17
Application directory: api_server
Health check: /api/actuator/health
AWS region: ap-northeast-2
Artifact bucket: denti-backends
Artifact prefix: soh
```

`main` is not a deployment branch. It is only for final reviewed code.

## Infrastructure Overview

Terraform creates the AWS infrastructure instead of manual console setup:

- VPC, public subnets, private app subnets, optional private DB subnets.
- NAT Gateway and S3 Gateway Endpoint.
- ALB security group and EC2 security group.
- EC2 instance role/profile with S3 artifact read permission and optional SSM.
- ALB, HTTPS listener, target group, optional HTTP to HTTPS redirect, optional Route 53 alias.
- Launch Template with Amazon Linux 2023, Java 17, AWS CLI, and User Data.
- Auto Scaling Group in private app subnets with target group attachment and rolling instance refresh.

Terraform files live in:

```text
infra/terraform
```

Deployment workflow files live in:

```text
.github/workflows
```

## Branch and Deployment Policy

```text
main push -> no deploy

dev push  -> development API artifact upload and dev ASG instance refresh
prod push -> production API artifact upload and prod ASG instance refresh
```

Do not add a main branch deployment workflow.

## Agent Handoff

When working from another PC or with another Codex agent, read both files before changing deployment or infrastructure behavior:

```text
README.md
AGENTS.md
```

Keep them aligned. Whenever CI/CD, Terraform, branch policy, AWS constants, GitHub Secrets, S3 paths, ASG names, CloudFront/API routing, or deployment commands change, update both files in the same commit.

After a successful update, commit and push when possible:

```bash
git status --short
git diff --check
git add <changed files>
git commit -m "<clear summary>"
git push origin <current-branch>
```

Do not commit real `.env`, `terraform.tfvars`, Terraform state, build outputs, or local IDE files.

## Terraform Modules

Created modules:

```text
infra/terraform/modules/network
infra/terraform/modules/security
infra/terraform/modules/iam
infra/terraform/modules/alb
infra/terraform/modules/launch_template
infra/terraform/modules/autoscaling
```

Created environments:

```text
infra/terraform/environments/dev
infra/terraform/environments/prod
```

## Terraform Bootstrap

Before GitHub Actions apply can work, do this once:

1. Create or choose a Terraform state S3 bucket, for example `thomabio-terraform-state`.
2. Replace `<TERRAFORM_STATE_BUCKET>` in both backend files.
3. Copy each `terraform.tfvars.example` to `terraform.tfvars` or update the example values before apply.
4. Replace `certificate_arn` with an ACM certificate ARN in `ap-northeast-2`.
5. Confirm the artifact bucket region:

```bash
aws s3api get-bucket-location --bucket denti-backends
```

DynamoDB lock table is optional and can be added to the backend later.

## Dev Infrastructure

```bash
cd infra/terraform/environments/dev
terraform init
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
```

The dev defaults create:

```text
VPC CIDR: 10.70.0.0/16
ALB: soh-api-dev-alb
Target group: soh-api-dev-tg
Launch template: soh-api-dev-lt
ASG: soh-api-dev-asg
Origin domain: soh-api-dev.thomabio.com
Release type: dev
```

Dev uses a single NAT Gateway by default for cost control.

## Prod Infrastructure

```bash
cd infra/terraform/environments/prod
terraform init
terraform validate
terraform plan -out=tfplan
terraform apply tfplan
```

The prod defaults create:

```text
VPC CIDR: 10.80.0.0/16
ALB: soh-api-prod-alb
Target group: soh-api-prod-tg
Launch template: soh-api-prod-lt
ASG: soh-api-prod-asg
Origin domain: soh-api.thomabio.com
Release type: prod
```

Prod defaults to one NAT Gateway per AZ. Production Terraform apply is workflow-dispatch only and should use GitHub Environment approval through `production-infra`.

## API Deployment Flow

Development API deployment:

1. Push `dev` branch.
2. GitHub Actions runs `deploy-api-dev.yml`.
3. Gradle/Maven auto-detect builds a Spring Boot `app.jar`. Gradle deploy builds skip `test` and `asciidoctor` because this project wires REST Docs generation into `bootJar`; run full tests separately before release approval.
4. `SOH_API_ENV_DEV` creates `.env`.
5. Uploads `s3://denti-backends/soh/dev/app.jar`.
6. Uploads `s3://denti-backends/soh/dev/.env`.
7. Starts `soh-api-dev-asg` Instance Refresh.
8. New EC2 instances run User Data and download `app.jar` and `.env` from S3.
9. systemd starts `soh-api-dev`.
10. Check `https://soh-dev.thomabio.com/api/actuator/health`.

Production API deployment:

1. Push `prod` branch.
2. GitHub Actions runs `deploy-api-prod.yml`.
3. Gradle/Maven auto-detect builds a Spring Boot `app.jar`. Gradle deploy builds skip `test` and `asciidoctor` because this project wires REST Docs generation into `bootJar`; run full tests separately before release approval.
4. `SOH_API_ENV_PROD` creates `.env`.
5. Uploads `s3://denti-backends/soh/prod/app.jar`.
6. Uploads `s3://denti-backends/soh/prod/.env`.
7. Starts `soh-api-prod-asg` Instance Refresh.
8. New EC2 instances run User Data and download `app.jar` and `.env` from S3.
9. systemd starts `soh-api-prod`.
10. Check `https://soh.thomabio.com/api/actuator/health`.

## GitHub Actions Workflows

```text
terraform-plan.yml        -> pull_request touching infra/terraform/** and workflow_dispatch
terraform-apply-dev.yml   -> workflow_dispatch only
terraform-apply-prod.yml  -> workflow_dispatch only, environment production-infra
deploy-api-dev.yml        -> dev branch push and workflow_dispatch
deploy-api-prod.yml       -> prod branch push and workflow_dispatch
```

## Required GitHub Secrets

AWS deployment credentials:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
```

API env secrets:

```text
SOH_API_ENV_DEV
SOH_API_ENV_PROD
```

`SOH_API_ENV_DEV` example:

```text
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
SERVER_SERVLET_CONTEXT_PATH=/api
FRONTEND_ORIGIN=https://soh-dev.thomabio.com
CORS_ALLOWED_ORIGINS=https://soh-dev.thomabio.com
DB_HOST=<DEV_DB_HOST>
DB_PORT=3306
DB_NAME=<DEV_DB_NAME>
DB_USERNAME=<DEV_DB_USER>
DB_PASSWORD=<DEV_DB_PASSWORD>
JWT_SECRET=<DEV_JWT_SECRET>
```

`SOH_API_ENV_PROD` example:

```text
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
SERVER_SERVLET_CONTEXT_PATH=/api
FRONTEND_ORIGIN=https://soh.thomabio.com
CORS_ALLOWED_ORIGINS=https://soh.thomabio.com
DB_HOST=<PROD_DB_HOST>
DB_PORT=3306
DB_NAME=<PROD_DB_NAME>
DB_USERNAME=<PROD_DB_USER>
DB_PASSWORD=<PROD_DB_PASSWORD>
JWT_SECRET=<PROD_JWT_SECRET>
```

Do not commit real `.env` files. GitHub Actions creates `.env`, uploads it to S3, and EC2 downloads it through the instance profile.

## GitHub Actions IAM User Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "UploadSohApiArtifacts",
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/dev/*",
        "arn:aws:s3:::denti-backends/soh/prod/*"
      ]
    },
    {
      "Sid": "ListSohApiArtifactPrefixes",
      "Effect": "Allow",
      "Action": ["s3:ListBucket", "s3:GetBucketLocation"],
      "Resource": "arn:aws:s3:::denti-backends",
      "Condition": {
        "StringLike": {
          "s3:prefix": ["soh/dev/*", "soh/prod/*"]
        }
      }
    },
    {
      "Sid": "RefreshSohApiAutoScalingGroups",
      "Effect": "Allow",
      "Action": [
        "autoscaling:StartInstanceRefresh",
        "autoscaling:DescribeInstanceRefreshes",
        "autoscaling:DescribeAutoScalingGroups"
      ],
      "Resource": [
        "arn:aws:autoscaling:ap-northeast-2:160885266674:autoScalingGroup:*:autoScalingGroupName/soh-api-dev-asg",
        "arn:aws:autoscaling:ap-northeast-2:160885266674:autoScalingGroup:*:autoScalingGroupName/soh-api-prod-asg"
      ]
    }
  ]
}
```

If `aws_region` changes, update the Auto Scaling ARNs as well.

## EC2 Instance Role Policies

Dev EC2 role:

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

Prod EC2 role:

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

Terraform also attaches `AmazonSSMManagedInstanceCore` by default so access can use SSM Session Manager. SSH is not opened by default.

## Security Notes

Initial ALB security group ingress allows HTTPS 443 from IPv4/IPv6 public internet. Before production traffic, restrict this to the CloudFront origin-facing managed prefix list where possible.

The EC2 security group only accepts TCP 8080 from the ALB security group. EC2 outbound is broad in the base module for package install, S3, DB, and service dependencies; narrow it later when dependency destinations are finalized.

## CloudFront API Integration

Existing frontend CloudFront distributions are not managed by this Terraform. Import them later only if you intentionally move CloudFront under Terraform.

Development CloudFront `E14WPL6NG95U7H`:

1. Add origin: `soh-api-dev.thomabio.com`.
2. Origin protocol policy: HTTPS only.
3. Add behavior path pattern: `/api/*`.
4. Allowed methods: GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE.
5. Cache policy: CachingDisabled.
6. Origin request policy: forward Authorization, Content-Type, query string, and other API-required values.

Production CloudFront `E3BD44T0U5EBYT`:

1. Add origin: `soh-api.thomabio.com`.
2. Origin protocol policy: HTTPS only.
3. Add behavior path pattern: `/api/*`.
4. Allowed methods: GET, HEAD, OPTIONS, PUT, POST, PATCH, DELETE.
5. Cache policy: CachingDisabled.
6. Origin request policy: forward Authorization, Content-Type, query string, and other API-required values.

If SPA fallback uses CloudFront custom error response 403/404 -> `/index.html` 200, API 403/404 can accidentally become `index.html`. Prefer a CloudFront Function that rewrites only non-API frontend routes:

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

## Operations Commands

S3 artifact check:

```bash
aws s3 ls s3://denti-backends/soh/dev/ --region ap-northeast-2
aws s3 ls s3://denti-backends/soh/prod/ --region ap-northeast-2
```

EC2 User Data logs:

```bash
sudo cat /var/log/userdata.log
sudo cat /var/log/cloud-init-output.log
```

systemd:

```bash
sudo systemctl status soh-api-dev
sudo systemctl status soh-api-prod
```

App logs:

```bash
tail -f /var/www/soh-api/app.log
tail -f /var/www/soh-api/error.log
```

Health checks:

```bash
curl -i http://localhost:8080/api/actuator/health
curl -i https://soh-dev.thomabio.com/api/actuator/health
curl -i https://soh.thomabio.com/api/actuator/health
```

## Validation

Local Terraform validation can use backend disabled until the state bucket placeholder is replaced:

```bash
terraform fmt -recursive infra/terraform
cd infra/terraform/environments/dev && terraform init -backend=false && terraform validate
cd ../prod && terraform init -backend=false && terraform validate
```

Local API build:

```bash
cd api_server
./gradlew clean bootJar
```

If tests or REST Docs require external services, document the reason and use a deployment build variant such as `./gradlew clean bootJar -x test -x asciidoctor` only after confirming the project task name.
