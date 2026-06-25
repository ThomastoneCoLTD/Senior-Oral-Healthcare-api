terraform {
  backend "s3" {
    bucket = "thomabio-soh-terraform-state"
    key    = "soh-api/prod/terraform.tfstate"
    region = "ap-northeast-2"
  }
}
