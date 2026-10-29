provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "Atithi"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}