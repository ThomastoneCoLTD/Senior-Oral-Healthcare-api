terraform {
  backend "s3" {
    bucket = "<TERRAFORM_STATE_BUCKET>"
    key    = "soh-api/dev/terraform.tfstate"
    region = "ap-northeast-2"
  }
}
