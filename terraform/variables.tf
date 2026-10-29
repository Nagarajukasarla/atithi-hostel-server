variable "aws_region" {
  description = "AWS region where Atithi infrastructure will be deployed"
  type        = string
  default     = "ap-south-1"
}

variable "environment" {
  description = "Deployment environment"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "atithi"
}

variable "vpc_cidr" {
  description = "CIDR block for the Atithi VPC"
  type        = string
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidrs" {
  description = "CIDR blocks for public subnets"
  type        = list(string)

  default = [
    "10.0.1.0/24",
    "10.0.2.0/24"
  ]
}

variable "private_app_subnet_cidrs" {
  description = "CIDR blocks for private application subnets"
  type        = list(string)

  default = [
    "10.0.11.0/24",
    "10.0.12.0/24"
  ]
}

variable "private_db_subnet_cidrs" {
  description = "CIDR blocks for private database subnets"
  type        = list(string)

  default = [
    "10.0.21.0/24",
    "10.0.22.0/24"
  ]
}

variable "availability_zones" {
  description = "Availability zones used by Atithi"
  type        = list(string)

  default = [
    "ap-south-1a",
    "ap-south-1b"
  ]
}

variable "container_port" {
  description = "Port exposed by the application container"
  type        = number
  default     = 8080
}