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

## Oral Exercise Access Policy

- The first core oral-exercise video is available immediately. Each later core video opens as soon as the immediately preceding core video is completed, without waiting for another signup week.
- Locked core-video responses keep the thumbnail metadata but omit `videoUrl`.
- `POST /oral-exercise/interactions` enforces the same previous-completion rule, so a client cannot bypass the sequence by calling the interaction API directly.
- The intro and always-open videos remain available independently of the five-week core sequence.

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
   - EC2 instance roles should read only their own `app.jar` and `.env`. Oral-exercise videos are served from `s3://tms-static-hosting/oral-exercise/video/`, and thumbnails from `s3://tms-static-hosting/oral-exercise/video-thumbnails/`.
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
   For GitHub Actions apply, store the filled tfvars content in `SOH_TERRAFORM_TFVARS_DEV` and `SOH_TERRAFORM_TFVARS_PROD_HCL`.
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
VPC: existing development VPC soh-api-dev-vpc
Public subnets: soh-api-dev-public-1, soh-api-dev-public-2
Private app subnets: soh-api-dev-private-app-1, soh-api-dev-private-app-2
Private DB subnets: soh-api-dev-private-db-1, soh-api-dev-private-db-2
ALB: soh-api-prod-alb
Target group: soh-api-prod-tg
Launch template: soh-api-prod-lt
ASG: soh-api-prod-asg
Origin domain: api.soh.thomabio.com
Release type: prod
EC2 instance type: t3.medium
RDS: soh-api-prod-mysql, MySQL 8.4.10, db.t3.small, single-AZ
RDS database: thomastone
```

Prod intentionally reuses the development VPC and its existing public, private app, and private DB subnets. Do not create a separate prod VPC unless the deployment policy is explicitly changed. Production Terraform apply is workflow-dispatch only and should use GitHub Environment approval through `production-infra`.
If an older prod VPC was already created by Terraform, review the prod plan before apply and migrate or remove state intentionally; do not approve an unexpected VPC/subnet/NAT destroy plan during production deployment.
Prod RDS has deletion protection enabled and requires a final snapshot on destroy unless intentionally changed.

## API Deployment Flow

Development API deployment:

1. Push `dev` branch.
2. GitHub Actions runs `deploy-api-dev.yml`.
3. Gradle/Maven auto-detect builds a Spring Boot `app.jar`. Gradle deploy builds skip `test` and `asciidoctor` because this project wires REST Docs generation into `bootJar`; run full tests separately before release approval.
4. `SOH_API_ENV_DEV` creates `.env`.
5. Uploads `s3://denti-backends/soh/dev/app.jar`.
6. Uploads `s3://denti-backends/soh/dev/.env`.
7. Waits for any existing `soh-api-dev-asg` Instance Refresh to finish, then starts a new refresh with `MinHealthyPercentage=0` so the single-instance dev API can recover when the existing target is already unhealthy.
8. New EC2 instances run User Data and download `app.jar` and `.env` from S3.
9. systemd starts `soh-api-dev`.
10. Check `https://soh-dev.thomabio.com/api/actuator/health`.

Production API deployment:

1. Push `prod` branch.
2. GitHub Actions runs `deploy-api-prod.yml`.
3. Gradle/Maven auto-detect builds a Spring Boot `app.jar`. Gradle deploy builds skip `test` and `asciidoctor` because this project wires REST Docs generation into `bootJar`; run full tests separately before release approval.
4. `SOH_API_ENV_PROD` creates `.env`; the workflow rejects datasource URL, username, password, or driver keys so RDS credentials cannot be copied to GitHub or S3.
5. Waits until Terraform has promoted the ASG launch template to the default version and verifies that its User Data contains the Secrets Manager JDBC configuration.
6. Uploads `s3://denti-backends/soh/prod/app.jar`.
7. Uploads the credential-free `s3://denti-backends/soh/prod/.env`.
8. Waits for any existing `soh-api-prod-asg` Instance Refresh to finish, then starts a new refresh.
9. New EC2 instances run User Data, download `app.jar` and `.env`, and inject the RDS managed secret ARN at boot.
10. systemd starts `soh-api-prod`.
11. Check `https://api.soh.thomabio.com/api/actuator/health`.

Production deploy workflow runs are not auto-cancelled by newer prod deploy runs; they queue behind the active run to avoid interrupting an artifact upload or ASG refresh.

## GitHub Actions Workflows

```text
terraform-plan.yml        -> pull_request touching infra/terraform/** and workflow_dispatch
terraform-apply-dev.yml   -> workflow_dispatch only
terraform-apply-prod.yml  -> workflow_dispatch only, environment production-infra
deploy-api-dev.yml        -> dev branch push and workflow_dispatch
deploy-api-prod.yml       -> prod branch push and workflow_dispatch
```

`terraform-plan.yml` temporarily disables the S3 backend file in the ephemeral GitHub Actions checkout and runs against local state with `terraform.tfvars.example`. This keeps PR validation working before the real backend bucket is configured. Treat that PR plan as a syntax/provider sanity check, not as the authoritative remote-state deployment plan. The apply workflows use the S3 backend and the filled `SOH_TERRAFORM_TFVARS_*` secrets.

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
SOH_TERRAFORM_TFVARS_PROD_HCL
```

Each `SOH_TERRAFORM_TFVARS_*` secret should contain the filled content of that environment's `terraform.tfvars.example`. Do not put AWS access keys, DB passwords, JWT secrets, or real `.env` content in these Terraform tfvars secrets.
The Terraform apply workflows mask each tfvars line before Terraform can report a parse error and reject application `.env` keys or sensitive variable names before `terraform init`. Keep `SOH_API_ENV_*` and `SOH_TERRAFORM_TFVARS_*` as separate GitHub Secrets; they are not interchangeable.
`SOH_TERRAFORM_TFVARS_PROD_HCL` should keep `db_engine_version = "8.4.10"` unless the production RDS instance is intentionally upgraded. The production apply workflow rejects other values because short version values such as `8.4` can resolve to an older patch version and make Terraform try an invalid downgrade. Keep `create_route53_record = true` so Terraform preserves the `api.soh.thomabio.com` Route 53 alias record.

The deploy workflows create `.env` from `SOH_API_ENV_DEV` or `SOH_API_ENV_PROD` and upload it to S3. Do not put RDS passwords in GitHub Secrets. Production additionally rejects `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, and `SPRING_DATASOURCE_DRIVER_CLASS_NAME` before upload. During EC2 boot, the launch template writes those datasource settings from Terraform's RDS endpoint and managed secret ARN through the AWS Secrets Manager JDBC driver.
The dev/prod deploy workflows also accept dedicated DaeguChain overrides: `DAEGU_CHAIN_APP_KEY_DEV`, `DAEGU_CHAIN_APP_KEY_PROD`, `DAEGU_CHAIN_TOKEN_DEV`, `DAEGU_CHAIN_TOKEN_PROD`, `TOKEN_SERVER_BASE_URL_DEV`, and `TOKEN_SERVER_BASE_URL_PROD`. At least one of `DAEGU_CHAIN_APP_KEY` or `DAEGU_CHAIN_TOKEN` must be present in the generated `.env` for token list/create/transfer APIs.

`SOH_API_ENV_DEV` example:

Store the secret as multiline text. Each `KEY=VALUE` pair must be on its own line; do not paste it as one concatenated line.

```text
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev
SERVER_SERVLET_CONTEXT_PATH=/api
FRONTEND_ORIGIN=https://soh-dev.thomabio.com
CORS_ALLOWED_ORIGINS=https://soh-dev.thomabio.com
JWT_SECRET=<DEV_JWT_SECRET>
DAEGU_CHAIN_APP_KEY=<DEV_DAEGU_CHAIN_APP_KEY>
DAEGU_CHAIN_ID=mitumt
DID_SERVER_BASE_URL=http://43.201.125.82
DID_CREATE_PATH=/did/create
TOKEN_SERVER_BASE_URL=http://43.201.125.82
DAEGU_CHAIN_TOKEN_OWNER_ADDRESS=<DEV_DAEGU_CHAIN_TOKEN_OWNER_ADDRESS>
DAEGU_CHAIN_TOKEN_SYMBOL=MYT
DAEGU_CHAIN_TOKEN_DECIMALS=18
USER_REWARD_TOKEN_TRANSFER_ENABLED=false
```

`SOH_API_ENV_PROD` example:

Store the secret as multiline text. Each `KEY=VALUE` pair must be on its own line; do not paste it as one concatenated line.

```text
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
SERVER_SERVLET_CONTEXT_PATH=/api
FRONTEND_ORIGIN=https://soh.thomabio.com
CORS_ALLOWED_ORIGINS=https://soh.thomabio.com
JWT_SECRET=<PROD_JWT_SECRET>
DAEGU_CHAIN_APP_KEY=<PROD_DAEGU_CHAIN_APP_KEY>
DAEGU_CHAIN_ID=mitumt
DID_SERVER_BASE_URL=<PROD_DID_SERVER_BASE_URL>
DID_CREATE_PATH=/did/create
TOKEN_SERVER_BASE_URL=<PROD_TOKEN_SERVER_BASE_URL>
DAEGU_CHAIN_TOKEN_OWNER_ADDRESS=<PROD_DAEGU_CHAIN_TOKEN_OWNER_ADDRESS>
DAEGU_CHAIN_TOKEN_SYMBOL=MYT
DAEGU_CHAIN_TOKEN_DECIMALS=18
USER_REWARD_TOKEN_TRANSFER_ENABLED=true
```

Do not commit real `.env` files. GitHub Actions creates `.env`, uploads it to S3, and EC2 downloads it through the instance profile.
Terraform passes `db_address`, `db_port`, `db_name`, and `db_master_user_secret_arn` to the launch template. EC2 rewrites `SPRING_DATASOURCE_URL` to `jdbc-secretsmanager:mysql://...`, sets `SPRING_DATASOURCE_USERNAME` to the secret ARN, clears `SPRING_DATASOURCE_PASSWORD`, and starts the app with `com.amazonaws.secretsmanager.sql.AWSSecretsManagerMySQLDriver`. The launch template resource promotes its managed latest version to the default version, and the production deploy workflow requires the ASG's explicit version to match that default before refreshing instances. The EC2 instance profile must keep `secretsmanager:DescribeSecret` and `secretsmanager:GetSecretValue` on that RDS managed secret. When Secrets Manager rotates the RDS password, the JDBC driver refreshes cached credentials for new DB connections, so GitHub Secrets do not need to be edited.
DaeguChain API requests use `DAEGU_CHAIN_APP_KEY` for every outbound request body field named `token`; keep app keys and any private keys only in environment secrets.
Mobile/tablet DaDaegu login uses `DADAEGU_LOGIN_ENABLED`, `DADAEGU_LOGIN_SITE_ID`, `DADAEGU_LOGIN_RSA_PRIVATE_KEY`, and optional `DADAEGU_LOGIN_REQUIRED_VC` (default `DaeguMasterVC`). The public `/login/dadaegu/config` response exposes only readiness, site ID, and required VC; the PKCS#8 RSA private key must remain only in the backend environment secret. `/login/dadaegu` decrypts the encrypted master-VC claims and first resolves the external DaDaegu DID through `dadaegu_user_identity`; for a first-time binding it may match an existing SOH user by normalized phone/name/birth date. Existing users receive normal SOH login tokens immediately. New users receive a 10-minute, one-use onboarding token and complete `POST /login/dadaegu/signUp` with only `realOrganization` and all required service-agreement IDs. The server then creates the SOH-only account, internal DaeguChain DID, reward wallet, agreement consents, and external identity mapping in one transaction before issuing login tokens. Raw onboarding tokens are never stored, and encrypted callback payloads plus all token/private-key/password fields are masked in application logs.

DaDaegu onboarding uses the auto-created `dadaegu_signup_session` and `dadaegu_user_identity` tables. The signup session stores a SHA-256 token hash with expiry/consumption timestamps; expired rows are removed when a new session is issued. External DaDaegu DID values must remain separate from the user's internal reward DID and wallet provisioning state.

Production deploys may override those values without replacing the shared `SOH_API_ENV_PROD` payload by using dedicated Secrets: `DADAEGU_LOGIN_ENABLED_PROD`, `DADAEGU_LOGIN_SITE_ID_PROD`, `DADAEGU_LOGIN_RSA_PRIVATE_KEY_PROD`, and optional `DADAEGU_LOGIN_REQUIRED_VC_PROD`. When the enabled override is `true`, the workflow rejects the deployment unless both the site ID and RSA private key are present.
`DID_SERVER_BASE_URL` must point to the reachable DID service used by `/did/create`; development currently uses `http://43.201.125.82`. `TOKEN_SERVER_BASE_URL` must point to the same reachable token server for `/token/create`, `/token/transfer`, and `/token/token_list`; do not leave it at `http://localhost:5000` on deployed API servers. User signup DID provisioning sends `label` with the user's login identifier, stores the returned `did:key`, and login checks the SOH user DID value and DID issued status without VC-JWT credential verification.
Normal password-based signup and DID signup both complete only after a Daegu DID and reward wallet address are stored. If an older user reaches a reward request with a failed or missing DID/wallet, the reward service retries DID and wallet provisioning before token transfer; provisioning failures remain explicit instead of leaving a newly registered user in a partially configured state.
Oral-exercise reward reclaim sends collected token contracts back to `DAEGU_CHAIN_TOKEN_OWNER_ADDRESS` through the configured token server; SOH must not read, log, or persist user DID private keys for this reclaim flow.
When `USER_REWARD_TOKEN_TRANSFER_ENABLED=true`, oral-exercise video rewards are transferred through DaeguChain token contracts by reward token name. Development keeps this disabled by default so token transfer outages do not block exercise completion.

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
    },
    {
      "Sid": "WriteSohDevRuntimeUploads",
      "Effect": "Allow",
      "Action": ["s3:PutObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/dev/*"
      ]
    },
    {
      "Sid": "DenySohDevDeployArtifactOverwrite",
      "Effect": "Deny",
      "Action": ["s3:PutObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/dev/app.jar",
        "arn:aws:s3:::denti-backends/soh/dev/.env"
      ]
    },
    {
      "Sid": "SynthesizeTtsSpeech",
      "Effect": "Allow",
      "Action": ["polly:SynthesizeSpeech"],
      "Resource": "*"
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
    },
    {
      "Sid": "WriteSohProdRuntimeUploads",
      "Effect": "Allow",
      "Action": ["s3:PutObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/prod/*"
      ]
    },
    {
      "Sid": "DenySohProdDeployArtifactOverwrite",
      "Effect": "Deny",
      "Action": ["s3:PutObject"],
      "Resource": [
        "arn:aws:s3:::denti-backends/soh/prod/app.jar",
        "arn:aws:s3:::denti-backends/soh/prod/.env"
      ]
    },
    {
      "Sid": "SynthesizeTtsSpeech",
      "Effect": "Allow",
      "Action": ["polly:SynthesizeSpeech"],
      "Resource": "*"
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

Production API:

1. Use the dedicated backend domain: `api.soh.thomabio.com`.
2. Point the Route 53 record to `soh-api-prod-alb`.
3. Configure the frontend production `VITE_API_BASE_URL` as `https://api.soh.thomabio.com/api`.
4. Ensure backend CORS allows `https://soh.thomabio.com`.
5. A frontend CloudFront `/api/*` behavior is not required for production when the dedicated API domain is used.

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
curl -i https://api.soh.thomabio.com/api/actuator/health
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

## Member Real Organization

User registration APIs (`POST /login/signUp`, `POST /login/signUp/did`) require `realOrganization` with one of `대구1`, `대구2`, or `대구3`. The selected value is stored in nullable `user.real_organization` so pre-existing users and administrator bulk-upload records remain compatible.

The user login flows are separate: `POST /login` with `userType=user` verifies the login ID and BCrypt password without requiring an issued DID, while `POST /login/did` verifies the issued DID state using only the login ID. Administrator login behavior is unchanged.

Both user registration APIs require `userGender` (`M` or `W`) and `userBirthDate`. Standard user registration additionally requires `userPassword`, `findPwdQuestionId`, and `findPwdAnswer`; the question list is available from `GET /password/questions`. DID registration does not accept password-recovery question/answer input and stores inaccessible random credentials in the legacy non-null password/recovery columns, so the DID account remains DID-login-only. `POST /login/find-id` returns the login identifier only when name, normalized phone number, and birth date all match.

The `dev` and `prod` profiles upsert the nine legacy recovery questions with stable IDs `1` through `9` during application startup. This keeps existing `user.find_pwd_question_id` references usable and safely restores missing question rows in a newly provisioned database.

- The existing `user.organizationId` relationship is unchanged: newly registered users still belong to the organization managed by `tokenadmin`.
- `GET /user/info` returns `realOrganization` for the user profile page.
- `GET /admin/user` returns `realOrganization` in each user-list item for administrator and super-administrator views.
- Production and development profiles use Hibernate `ddl-auto: update`, so application startup adds the nullable column. Verify the generated schema change and the member-registration flow after deployment.

## Oral Analysis and Personalized Content

All authenticated SOH users can use plaque analysis, gingivitis analysis, questionnaires, and personalized content regardless of subscription plan. The frontend uses the same user routes for every plan, and the backend does not apply the former `GROWTH`/`MIDSIZE` personalized-content gate.

The user content menu always opens the personalized view and exposes a visible `Personalized Contents / All Contents` switch. The oral-status timeline distinguishes plaque and gingivitis records with the same green tooth and red heartbeat visual language as the analysis chooser, and labels each record as a plaque or gingivitis detection result.

Gingivitis analysis also exposes the Denti-K-compatible contract while preserving the legacy `/oralCheck/gingivitis` endpoint:

```text
POST /gingivitis-analyses
GET  /gingivitis-analyses/{analysisId}
GET  /gingivitis-condition
```

The new endpoints require a valid logged-in user token and validate that result lookups belong to the requesting user. Plaque AI `contents_type` and `plaque_contents` values are retained in the SOH response and used to resolve recommendations. If the AI returns direct content IDs, those take precedence; otherwise SOH resolves content through the existing oral-status mapping.

SOH now includes the Denti-K-compatible `content_curation_rule` entity and table contract. `analysis_type` accepts `QUESTIONNAIRE`, `GINGIVITIS`, or `PLAQUE`; `result_key` uses questionnaire A-K, gingivitis S/G/A/D, or plaque result grades. Active rules are ordered by `curation_rank` and `contents_id`, and duplicate `(analysis_type, result_key, contents_id)` rows are prohibited. Rule rows must reference SOH's own `contents.contents_id`; Denti-K IDs must not be copied without an explicit content-ID mapping. An analysis can legitimately show the completed/no-match state when the table exists but has no matching active rows. This change adds the DB schema contract but does not change Secrets, AWS resources, or deployment workflows.
