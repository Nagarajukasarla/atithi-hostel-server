#!/usr/bin/env bash
# ==============================================================================
# Atithi Backend - Activate User & Hostel in PostgreSQL RDS
# Run from Termux / Linux / macOS / WSL
#
# Usage:
#   ./activate-user.sh user@email.com
#   ./activate-user.sh -e user@email.com
#   ./activate-user.sh (prompts interactively)
# ==============================================================================

set -eo pipefail

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m' # No Color

AWS_REGION="${AWS_REGION:-ap-south-1}"
ECS_CLUSTER="${ECS_CLUSTER:-atithi-dev-cluster}"
TASK_DEF="${TASK_DEF:-atithi-dev-db-client:1}"
SUBNET_ID="${SUBNET_ID:-subnet-079397bbbde4c4216}"
SECURITY_GROUP_ID="${SECURITY_GROUP_ID:-sg-00043f8ddcad6687e}"
LOG_GROUP="/ecs/atithi-dev"

# Parse Email Argument
EMAIL=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        -e|--email)
            EMAIL="$2"
            shift 2
            ;;
        *)
            if [ -z "$EMAIL" ]; then
                EMAIL="$1"
            fi
            shift
            ;;
    esac
done

# If not provided via arguments, prompt interactively
if [ -z "$EMAIL" ]; then
    echo -e "${CYAN}======================================================${NC}"
    echo -e "${CYAN}       Atithi AWS - User & Hostel Activation          ${NC}"
    echo -e "${CYAN}======================================================${NC}"
    read -r -p "Enter user email to activate: " EMAIL
fi

# Validate input
if [ -z "$EMAIL" ]; then
    echo -e "${RED}Error: Email cannot be empty.${NC}"
    echo -e "Usage: $0 <email_address>"
    exit 1
fi

echo -e "\n${CYAN}======================================================${NC}"
echo -e "${CYAN}Target Email : ${BOLD}${EMAIL}${NC}"
echo -e "${CYAN}Cluster      : ${ECS_CLUSTER}${NC}"
echo -e "${CYAN}Region       : ${AWS_REGION}${NC}"
echo -e "${CYAN}======================================================${NC}"

# Check dependencies
if ! command -v aws &> /dev/null; then
    echo -e "${RED}Error: 'aws' CLI is not installed or not in PATH.${NC}"
    exit 1
fi

if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}Notice: 'jq' is recommended for JSON parsing. Continuing with python/grep fallback if needed.${NC}"
fi

# Build Combined SQL Query
SQL_QUERY="UPDATE users SET status = 'ACTIVE' WHERE email = '${EMAIL}'; UPDATE hostels SET status = 'ACTIVE' WHERE id = (SELECT hostel_id FROM users WHERE email = '${EMAIL}'); SELECT id, email, role, status, hostel_id FROM users WHERE email = '${EMAIL}'; SELECT id, name, status FROM hostels WHERE id = (SELECT hostel_id FROM users WHERE email = '${EMAIL}');"

# Create a secure temporary JSON override file
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
        "$SQL_QUERY"
      ]
    }
  ]
}
EOF

echo -e "\n${YELLOW}1. Starting one-shot Fargate task to execute SQL inside private VPC...${NC}"

# Run the task
RUN_OUTPUT=$(aws ecs run-task \
    --cluster "$ECS_CLUSTER" \
    --task-definition "$TASK_DEF" \
    --launch-type "FARGATE" \
    --network-configuration "awsvpcConfiguration={subnets=[$SUBNET_ID],securityGroups=[$SECURITY_GROUP_ID],assignPublicIp=DISABLED}" \
    --overrides "file://$TMP_OVERRIDE" \
    --region "$AWS_REGION" \
    --output json)

# Extract Task ID
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

echo -e "${GREEN}✓ Task launched: ${BOLD}${TASK_ID}${NC}"
echo -e "${CYAN}2. Waiting for query to finish and container to terminate...${NC}"

# Wait for task to stop
aws ecs wait tasks-stopped \
    --cluster "$ECS_CLUSTER" \
    --tasks "$TASK_ID" \
    --region "$AWS_REGION"

echo -e "${GREEN}✓ Query execution complete. Container stopped and de-allocated.${NC}"

# Fetch execution logs from CloudWatch
LOG_STREAM="db-client/db-client/${TASK_ID}"
echo -e "\n${GREEN}=================== QUERY RESULT =====================${NC}"
aws logs get-log-events \
    --log-group-name "$LOG_GROUP" \
    --log-stream-name "$LOG_STREAM" \
    --region "$AWS_REGION" \
    --query "events[].message" \
    --output text
echo -e "${GREEN}======================================================${NC}"
echo -e "${GREEN}🎉 User [${EMAIL}] and associated Hostel are now ACTIVE!${NC}\n"
