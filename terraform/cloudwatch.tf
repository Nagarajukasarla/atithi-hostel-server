resource "aws_cloudwatch_log_group" "ecs" {
  name              = "/ecs/atithi-dev"
  retention_in_days = 7

  tags = {
    Name = "${local.name_prefix}-ecs-logs"
  }
}
