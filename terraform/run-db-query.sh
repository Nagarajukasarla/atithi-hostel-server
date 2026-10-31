#!/usr/bin/env bash
# ==============================================================================
# Atithi Backend - Run Arbitrary SQL Query on Private PostgreSQL RDS
# Run from Termux / Linux / macOS / WSL
#
# Usage:
#   ./run-db-query.sh "\dt"
#   ./run-db-query.sh "SELECT * FROM users LIMIT 5;"
# ==============================================================================

set -eo pipefail

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

AWS_REGION="${AWS_REGION:-ap-south-1}"
ECS_CLUSTER="${ECS_CLUSTER:-atithi-dev-cluster}"
TASK_DEF="${TASK_DEF:-atithi-dev-db-client:1}"
SUBNET_ID="${SUBNET_ID:-subnet-079397bbbde4c4216}"
SECURITY_GROUP_ID="${SECURITY_GROUP_ID:-sg-00043f8ddcad6687e}"
LOG_GROUP="/ecs/atithi-dev"

QUERY="${1:-\dt}"

echo -e "${CYAN}Executing query against live PostgreSQL RDS...${NC}"
echo -e "Query: ${YELLOW}${QUERY}${NC}"

TMP_OVERRIDE=$(mktemp 2>/dev/null || mktemp -t ecs-override)
trap 'rm -f "$TMP_OVERRIDE"' EXIT

cat <<EOF > "$TMP_OVERRIDE"
{
  "containerOverrides": [
    {
      "name": "db-client",
      "command": [
        "psql",
        "-c",
        "$QUERY"
      ]
    }
  ]
}
EOF

RUN_OUTPUT=$(aws ecs run-task \
    --cluster "$ECS_CLUSTER" \
    --task-definition "$TASK_DEF" \
    --launch-type "FARGATE" \
    --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_ID],securityGroups=[$SECURITY_GROUP_ID],assignPublicIp=DISABLED}" \
    --overrides "file://$TMP_OVERRIDE" \
    --region "$AWS_REGION" \
    --output json)

if command -v jq &> /dev/null; then
    TASK_ARN=$(echo "$RUN_OUTPUT" | jq -r '.tasks[0].taskArn')
else
    TASK_ARN=$(echo "$RUN_OUTPUT" | grep -o '"taskArn": "[^"]*' | cut -d'"' -f4 | head -n1)
fi

TASK_ID="${TASK_ARN##*/}"

if [ -z "$TASK_ID" ] || [ "$TASK_ID" == "null" ]; then
    echo -e "${RED}Failed to launch ECS task. Raw response:${NC}"
    echo "$RUN_OUTPUT"
    exit 1
fi

echo -e "${GREEN}Task started: $TASK_ID. Waiting for execution...${NC}"
aws ecs wait tasks-stopped --cluster "$ECS_CLUSTER" --tasks "$TASK_ID" --region "$AWS_REGION"

LOG_STREAM="db-client/db-client/${TASK_ID}"
echo -e "\n${GREEN}=== Query Output ===${NC}"
aws logs get-log-events \
    --log-group-name "$LOG_GROUP" \
    --log-stream-name "$LOG_STREAM" \
    --region "$AWS_REGION" \
    --query "events[].message" \
    --output text
