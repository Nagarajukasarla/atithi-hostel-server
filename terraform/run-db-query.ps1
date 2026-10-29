param (
    [Parameter(Mandatory=$false, Position=0)]
    [string]$Query = "\dt"
)

Write-Host "Executing query against live PostgreSQL RDS..." -ForegroundColor Cyan
Write-Host "Query: $Query" -ForegroundColor Yellow

$overrideJson = @"
{
  "containerOverrides": [
    {
      "name": "db-client",
      "command": ["psql", "-c", "$Query"]
    }
  ]
}
"@

$tempFile = [System.IO.Path]::GetTempFileName() + ".json"
[System.IO.File]::WriteAllText($tempFile, $overrideJson, [System.Text.UTF8Encoding]::new($false))

try {
    $runOutput = aws ecs run-task `
        --cluster "atithi-dev-cluster" `
        --task-definition "atithi-dev-db-client:1" `
        --launch-type "FARGATE" `
        --network-configuration "awsvpcConfiguration={subnets=[subnet-079397bbbde4c4216],securityGroups=[sg-00043f8ddcad6687e],assignPublicIp=DISABLED}" `
        --overrides "file://$tempFile" `
        --region "ap-south-1" | ConvertFrom-Json

    $taskArn = $runOutput.tasks[0].taskArn
    $taskId = $taskArn.Split('/')[-1]
    Write-Host "Task started: $taskId. Waiting for execution..." -ForegroundColor Green

    aws ecs wait tasks-stopped --cluster "atithi-dev-cluster" --tasks $taskId --region "ap-south-1"

    $logStream = "db-client/db-client/$taskId"
    Write-Host "=== Query Output ===" -ForegroundColor Green
    aws logs get-log-events --log-group-name "/ecs/atithi-dev" --log-stream-name $logStream --region "ap-south-1" --query "events[].message" --output text
}
finally {
    if (Test-Path $tempFile) {
        Remove-Item $tempFile -Force
    }
}
