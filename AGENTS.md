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
ASG: soh-api-dev-asg
Secret: SOH_API_ENV_DEV
Health URL: https://soh-dev.thomabio.com/api/actuator/health
```

Production API:

```text
Workflow: .github/workflows/deploy-api-prod.yml
Branch: prod
Artifact path: s3://denti-backends/soh/prod/app.jar
Env path: s3://denti-backends/soh/prod/.env
ASG: soh-api-prod-asg
Secret: SOH_API_ENV_PROD
Health URL: https://soh.thomabio.com/api/actuator/health
```

Never swap the dev/prod artifact paths.

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
- Confirm `denti-backends` region with `aws s3api get-bucket-location --bucket denti-backends`.

## Secrets

Required GitHub Secrets:

```text
AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY
SOH_API_ENV_DEV
SOH_API_ENV_PROD
```

Never write AWS access keys, DB credentials, JWT secrets, or real `.env` contents into repository files.

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

Do this in the same commit.

## New Project Startup Checklist

For every new SOH-style project or environment, make sure the repository has a README section covering:

- Repository names and branch deployment mapping.
- Build tool, Java/Node version, build command, and output artifact.
- GitHub Secrets for AWS credentials and environment-specific app configuration.
- Terraform state bucket bootstrap, backend placeholder replacement, ACM ARN, and S3 bucket region check.
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
