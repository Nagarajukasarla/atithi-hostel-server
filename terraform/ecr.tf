resource "aws_ecr_repository" "atithi_server" {
  name                 = "atithi-server"
  image_tag_mutability = "MUTABLE"

  image_scanning_configuration {
    scan_on_push = true
  }

  encryption_configuration {
    encryption_type = "AES256"
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Name = "atithi-server"
  }
}

output "ecr_repository_url" {
  value = aws_ecr_repository.atithi_server.repository_url
}

output "ecr_repository_name" {
  value = aws_ecr_repository.atithi_server.name
}