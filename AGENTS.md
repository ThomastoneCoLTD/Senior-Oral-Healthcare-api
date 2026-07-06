# Repository Operating Notes

This file is for Codex and other agents working on the SOH API repository from any PC. Keep it aligned with `README.md`.

## Project

- Repository: `ThomastoneCoLTD/Senior-Oral-Healthcare-api`
- Service: SOH API
- App directory: `api_server`
- Framework: Spring Boot
- Build tool: Gradle Wrapper
- Java: 17
- Health path: `/api/actuator/health`
- AWS account: `160885266674`
- AWS region: `ap-northeast-2`
- Artifact bucket: `denti-backends`
- Artifact prefix: `soh`

## Branch Policy

```text
main push -> no deploy
dev push  -> development API deploy
prod push -> production API deploy
```

Do not create a workflow that deploys from `main`.

## API Deployment

Development API:

```text
Workflow: .github/workflows/deploy-api-dev.yml
Branch: dev
Artifact path: s3://denti-backends/soh/dev/app.jar
Env path: s3://denti-backends/soh/dev/.env
Shared video path: s3://denti-backends/soh/video/
ASG: soh-api-dev-asg
Secret: SOH_API_ENV_DEV
Health URL: https://soh-dev.thomabio.com/api/actuator/health
Instance refresh: uses MinHealthyPercentage 0 because dev runs a single API instance and must be able to replace an unhealthy target.
```

Production API:

```text
Workflow: .github/workflows/deploy-api-prod.yml
Branch: prod
Artifact path: s3://denti-backends/soh/prod/app.jar
Env path: s3://denti-backends/soh/prod/.env
Shared video path: s3://denti-backends/soh/video/
ASG: soh-api-prod-asg
Secret: SOH_API_ENV_PROD
Health URL: https://soh.thomabio.com/api/actuator/health
```

Never swap the dev/prod artifact paths.
EC2 roles must be able to read their environment `app.jar` and `.env`, read the shared `s3://denti-backends/soh/video/*` prefix used for presigned oral-exercise video playback, and write runtime uploads such as oral-check photos under their environment prefix (`s3://denti-backends/soh/dev/*` or `s3://denti-backends/soh/prod/*`). Runtime write permissions must not allow overwriting `app.jar` or `.env`.

## Terraform

Terraform lives under:

```text
infra/terraform
```

Modules:

```text
network
security
iam
alb
rds
launch_template
autoscaling
```

Environments:

```text
infra/terraform/environments/dev
infra/terraform/environments/prod
```

Do not run `terraform apply` from Codex. Apply must be run by GitHub Actions or a human operator after review.

Before Terraform apply:

- Create or choose the Terraform state S3 bucket.
- Replace `<TERRAFORM_STATE_BUCKET>` in backend files.
- Fill ACM `certificate_arn`.
- Review compute defaults: EC2 instance type is `t3.medium`; RDS instance class is `db.t3.small`.
- RDS uses MySQL in private DB subnets and stores the managed master password in AWS Secrets Manager.
- Confirm `denti-backends` region with `aws s3api get-bucket-location --bucket denti-backends`.

`terraform-plan.yml` disables the S3 backend file inside the temporary GitHub Actions checkout and plans with local state plus `terraform.tfvars.example`. Use it as PR validation only. Authoritative apply plans use the real S3 backend and `SOH_TERRAFORM_TFVARS_DEV` / `SOH_TERRAFORM_TFVARS_PROD`.

## Manual AWS Console Guide

Manual setup instructions live in:

```text
readme_수동.md
```

Use it when rebuilding the API AWS environment directly from the AWS web console instead of Terraform. Keep it aligned with Terraform, CI/CD, S3 artifact paths, ASG names, ALB/Route 53 settings, CloudFront `/api/*` behavior, and deployment verification steps.

## Secrets

Required GitHub Secrets:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
SOH_API_ENV_DEV
SOH_API_ENV_PROD
SOH_TERRAFORM_TFVARS_DEV
SOH_TERRAFORM_TFVARS_PROD
```

Never write AWS access keys, DB credentials, JWT secrets, or real `.env` contents into repository files.
`SOH_API_ENV_DEV` and `SOH_API_ENV_PROD` must include `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD`; populate the datasource password from the environment RDS Secrets Manager secret, not from repository files.
`SOH_API_ENV_DEV` and `SOH_API_ENV_PROD` should include DaeguChain and DID app configuration such as `DAEGU_CHAIN_APP_KEY`, `DAEGU_CHAIN_ID`, `DID_SERVER_BASE_URL`, `DID_CREATE_PATH`, `DID_DB_URL`, `DID_DB_USERNAME`, `DID_DB_PASSWORD`, `DID_DB_TABLE`, `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_TEMPLATE_ID`, `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_VALID_DAYS`, `DAEGU_CHAIN_TOKEN_OWNER_ADDRESS`, `DAEGU_CHAIN_TOKEN_OWNER_PRIVATE_KEY`, `DAEGU_CHAIN_TOKEN_SYMBOL`, `DAEGU_CHAIN_TOKEN_DECIMALS`, and `USER_REWARD_TOKEN_TRANSFER_ENABLED` when DaeguChain DID login, admin token creation, oral-exercise token rewards, or oral-exercise reward reclaim are enabled.
Development DID creation currently uses `DID_SERVER_BASE_URL=http://43.201.125.82`.
Reward reclaim reads `private_key` and `account_address` from the DID DB table configured by `DID_DB_TABLE` (default `DID`); use a read-only DB account and never log or persist the private key in SOH.
Development oral-exercise rewards keep `USER_REWARD_TOKEN_TRANSFER_ENABLED=false` by default so external token transfer failures do not block the exercise flow.
Optional `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_VALID_FROM` and `DAEGU_CHAIN_LOGIN_USER_CREDENTIAL_VALID_UNTIL` can pin the DID login credential validity window.
Outbound DaeguChain request body fields named `token` must be populated from the environment app key, not hardcoded in source.
`SOH_TERRAFORM_TFVARS_*` should contain filled Terraform variable values such as ACM ARN and instance sizes, but not DB passwords or application `.env` secrets.

## Validation

Prefer these checks after relevant changes:

```bash
python -c "import glob, yaml; [yaml.safe_load(open(f, encoding='utf-8-sig')) for f in glob.glob('.github/workflows/*.yml')]; print('YAML OK')"
terraform fmt -recursive infra/terraform
cd infra/terraform/environments/dev && terraform init -backend=false && terraform validate
cd ../prod && terraform init -backend=false && terraform validate
cd api_server && ./gradlew clean bootJar -x test -x asciidoctor
```

On Windows, set Java 17 before Gradle if the default Java is newer:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-17'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat clean bootJar -x test -x asciidoctor
```

If Terraform CLI is not installed, state that `terraform fmt` and `terraform validate` could not be run.

## Documentation Maintenance

Whenever CI/CD, Terraform, branch policy, AWS constants, GitHub Secrets, S3 paths, ASG names, CloudFront/API routing, or deployment commands change, update both:

```text
README.md
AGENTS.md
```

Do this in the same commit. If the change affects manual AWS console setup, update `readme_수동.md` in that commit as well.

## New Project Startup Checklist

For every new SOH-style project or environment, make sure the repository has a README section covering:

- Repository names and branch deployment mapping.
- Build tool, Java/Node version, build command, and output artifact.
- GitHub Secrets for AWS credentials and environment-specific app configuration.
- Terraform state bucket bootstrap, backend placeholder replacement, ACM ARN, and S3 bucket region check.
- RDS engine, instance class, subnet group, deletion protection, backup, and Secrets Manager password handling.
- CI/CD workflow triggers, with no deployment from `main` unless intentionally approved.
- Guard steps for branch, S3 path, release type, CloudFront distribution, and ASG names.
- AWS IAM policy for GitHub Actions and EC2 instance roles.
- CloudFront `/api/*` behavior and SPA fallback handling.
- Validation commands and expected results.
- Commit and push procedure.

Keep the same checklist in `README.md` and update it whenever the deployment model changes.

## Commit And Push Policy

After a successful repository update, do this when possible:

```bash
git status --short
git diff --check
git add <changed files>
git commit -m "<clear summary>"
git push origin <current-branch>
```

Only commit intended repository changes. Do not commit real `.env`, `terraform.tfvars`, Terraform state, build outputs, or local IDE files.

If commit or push cannot be completed because of missing credentials, network restrictions, conflicts, or user approval, leave the worktree cleanly described in the final response with the exact next commands.
