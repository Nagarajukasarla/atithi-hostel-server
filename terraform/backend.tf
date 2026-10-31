terraform {
  backend "s3" {
    bucket  = "atithi-terraform-state-551626543871"
    key     = "dev/terraform.tfstate"
    region  = "ap-south-1"
    encrypt = true
  }
}
