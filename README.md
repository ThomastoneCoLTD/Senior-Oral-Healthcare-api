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

- VPC, public subnets, private app subnets, and private DB subnets.
- NAT Gateway and S3 Gateway Endpoint.
- ALB security group, EC2 security group, and RDS security group.
- EC2 instance role/profile with S3 artifact read permission and optional SSM.
- ALB, HTTPS listener, target group, optional HTTP to HTTPS redirect, optional Route 53 alias.
- RDS MySQL instance in private DB subnets with AWS-managed master password in Secrets Manager.
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

## Manual AWS Console Setup Guide

If this environment must be built manually from the AWS web console, use:

```text
readme_수동.md
```

That file documents the AWS Console steps from zero setup through S3 artifacts, VPC, IAM, ALB, ASG, Route 53, CloudFront `/api/*` routing, and release verification.
It also covers the manual RDS MySQL setup that Terraform normally creates.

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
readme_수동.md
```

Keep them aligned. Whenever CI/CD, Terraform, branch policy, AWS constants, GitHub Secrets, S3 paths, ASG names, CloudFront/API routing, manual AWS setup steps, or deployment commands change, update the relevant documentation in the same commit.

After a successful update, commit and push when possible:

```bash
git status --short
git diff --check
git add <changed files>
git commit -m "<clear summary>"
git push origin <current-branch>
```

Do not commit real `.env`, `terraform.tfvars`, Terraform state, build outputs, or local IDE files.

## New Project Startup Checklist

Use this checklist whenever starting a new SOH-style project or moving this project to a new environment.

1. Confirm repositories and branches.
   - Confirm API and frontend repository names.
   - Decide which branches deploy each environment.
   - Confirm `main` is not a deployment branch unless intentionally changed.
   - Write unusual branch mappings explicitly in `README.md` and `AGENTS.md`.

2. Confirm build systems.
   - Frontend: confirm package manager, lockfile, build command, and output directory.
   - API: confirm Gradle/Maven, Java version, app directory, JAR output, and health path.
   - Add workflow build commands that match the repository layout.

3. Prepare GitHub Secrets.
   - Register AWS deployment credentials only as GitHub Secrets.
   - Register environment-specific app secrets such as API `.env` content.
   - Do not commit real `.env`, AWS keys, DB passwords, JWT secrets, or private tokens.

4. Prepare AWS bootstrap resources.
   - Create or choose the Terraform state S3 bucket.
   - Replace Terraform backend placeholders.
   - Confirm S3 artifact bucket region.
   - Prepare ACM certificate ARNs in the target ALB region.
   - Confirm Route 53 hosted zone ownership if DNS will be created by Terraform.
   - Confirm RDS engine, instance class, subnet group, deletion protection, backup retention, and Secrets Manager password handling.

5. Prepare CI/CD.
   - Ensure no workflow deploys from `main`.
   - Ensure dev workflows write only dev artifact paths and refresh only dev ASGs.
   - Ensure prod workflows write only prod artifact paths and refresh only prod ASGs.
   - Keep deploy target names, S3 paths, CloudFront IDs, and ASG names guarded in workflows.

6. Prepare AWS IAM.
   - GitHub Actions IAM user needs artifact upload, ASG refresh, and Terraform plan/apply permissions.
   - EC2 instance roles should read only their own `app.jar` and `.env` from S3.
   - EC2 User Data must use the instance profile, not long-lived AWS access keys.

7. Prepare CloudFront/API routing.
   - Add `/api/*` behavior to the frontend CloudFront distribution.
   - Disable caching for API behavior.
   - Forward Authorization, Content-Type, query strings, and required headers.
   - Ensure SPA fallback does not rewrite `/api/*` errors to `index.html`.

8. Validate before release.
   - Run YAML parsing checks for GitHub Actions.
   - Run Terraform fmt and validate where Terraform CLI is available.
   - Run local frontend/API builds where possible.
   - Check for forbidden legacy values such as old S3 paths, old regions, OIDC settings, or main-branch deploy triggers.

9. Commit and push.
   - Update `README.md` and `AGENTS.md` together.
   - Run `git diff --check`.
   - Commit a clear summary.
   - Push to the current branch when possible.

10. Confirm deployment.
    - Check uploaded S3 artifacts.
    - Confirm ASG Instance Refresh started.
    - Check EC2 User Data and systemd logs.
    - Check internal and external health endpoints.

## Terraform Modules

Created modules:

```text
infra/terraform/modules/network
infra/terraform/modules/security
infra/terraform/modules/iam
infra/terraform/modules/alb
infra/terraform/modules/rds
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
3. For local apply, copy each `terraform.tfvars.example` to `terraform.tfvars` and fill real values.
   For GitHub Actions apply, store the filled tfvars content in `SOH_TERRAFORM_TFVARS_DEV` and `SOH_TERRAFORM_TFVARS_PROD`.
4. Replace `certificate_arn` with an ACM certificate ARN in `ap-northeast-2`.
5. Review the RDS values. Current examples use EC2 `t3.medium` and RDS `db.t3.small`.
6. Confirm the artifact bucket region:

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
EC2 instance type: t3.medium
RDS: soh-api-dev-mysql, MySQL 8.0, db.t3.small, single-AZ
RDS database: thomastone
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
EC2 instance type: t3.medium
RDS: soh-api-prod-mysql, MySQL 8.0, db.t3.small, single-AZ
RDS database: thomastone
```

Prod defaults to one NAT Gateway per AZ. Production Terraform apply is workflow-dispatch only and should use GitHub Environment approval through `production-infra`.
Prod RDS has deletion protection enabled and requires a final snapshot on destroy unless intentionally changed.

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

Terraform apply tfvars secrets:

```text
SOH_TERRAFORM_TFVARS_DEV
SOH_TERRAFORM_TFVARS_PROD
```

Each `SOH_TERRAFORM_TFVARS_*` secret should contain the filled content of that environment's `terraform.tfvars.example`. Do not put AWS access keys, DB passwords, JWT secrets, or real `.env` content in these Terraform tfvars secrets.

`SOH_API_ENV_DEV` example:

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

`SOH_API_ENV_PROD` example:

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

Do not commit real `.env` files. GitHub Actions creates `.env`, uploads it to S3, and EC2 downloads it through the instance profile.
Terraform outputs `db_address`, `db_endpoint`, and `db_master_user_secret_arn`; use the Secrets Manager secret value to populate the datasource password in the GitHub environment secret.

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

Terraform plan/apply uses the same AWS credentials in the current workflows. That principal also needs permissions for managed infrastructure resources, including EC2/VPC, ELBv2, IAM, Auto Scaling, Route 53 when enabled, RDS, and Secrets Manager. Scope these permissions to `soh-api-*`, the configured VPC resources, and the Terraform state bucket where practical.

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

The RDS security group only accepts TCP 3306 from the EC2 security group. RDS is created in private DB subnets with `publicly_accessible = false`.

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
