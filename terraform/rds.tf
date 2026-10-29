resource "aws_db_subnet_group" "db" {
  name        = "${local.name_prefix}-db-subnet-group"
  description = "Database subnet group for Atithi RDS"
  subnet_ids  = aws_subnet.private_db[*].id

  tags = {
    Name = "${local.name_prefix}-db-subnet-group"
  }
}

resource "aws_db_instance" "postgres" {
  identifier             = "${local.name_prefix}-postgres"
  allocated_storage      = 20
  max_allocated_storage  = 50
  storage_type           = "gp3"
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = "db.t4g.micro"
  db_name                = "atithi_aavas"
  username               = jsondecode(aws_secretsmanager_secret_version.db_credentials.secret_string)["username"]
  password               = jsondecode(aws_secretsmanager_secret_version.db_credentials.secret_string)["password"]
  db_subnet_group_name   = aws_db_subnet_group.db.name
  vpc_security_group_ids = [aws_security_group.rds.id]

  skip_final_snapshot       = false
  final_snapshot_identifier = "${local.name_prefix}-postgres-final-snapshot"
  publicly_accessible       = false
  deletion_protection       = false

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    Name = "${local.name_prefix}-postgres"
  }
}
