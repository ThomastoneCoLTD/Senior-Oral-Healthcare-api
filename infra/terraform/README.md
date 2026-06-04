# SOH API Terraform

This Terraform tree prepares the SOH API infrastructure on AWS. Codex must not run `terraform apply`; apply is executed by GitHub Actions or by a human operator.

## Bootstrap Required Once

Terraform remote state uses an S3 backend. Before running `terraform init`, create or choose a state bucket, then replace `<TERRAFORM_STATE_BUCKET>` in:

```text
infra/terraform/environments/dev/backend.tf
infra/terraform/environments/prod/backend.tf
```

Example state bucket names:

```text
thomabio-terraform-state
<organization-standard-terraform-state-bucket>
```

DynamoDB state locking is optional and can be added later.

## Environments

```text
infra/terraform/environments/dev
infra/terraform/environments/prod
```

Copy `terraform.tfvars.example` to `terraform.tfvars`, replace placeholders such as `certificate_arn`, then run plan/apply.

## Local Validation

If the backend bucket placeholder is not replaced yet, use backend disabled validation:

```bash
cd infra/terraform/environments/dev
terraform init -backend=false
terraform validate

cd ../prod
terraform init -backend=false
terraform validate
```

Format all Terraform files with:

```bash
terraform fmt -recursive infra/terraform
```

## S3 Gateway Endpoint Warning

The S3 Gateway Endpoint uses `com.amazonaws.${aws_region}.s3`. Confirm that the artifact bucket region matches the VPC region:

```bash
aws s3api get-bucket-location --bucket denti-backends
```

If `denti-backends` is in another region, revisit the artifact bucket or network path before deploying.
