terraform {
  backend "s3" {
    bucket = "thomabio-soh-terraform-state"
    key    = "soh-api/dev/terraform.tfstate"
    region = "ap-northeast-2"
  }
}
