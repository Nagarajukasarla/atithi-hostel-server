#!/usr/bin/env bash
# ==============================================================================
# Atithi Backend - Stop Services (Pause Infrastructure)
# Run from Termux / Linux / macOS / WSL
# ==============================================================================

set -eo pipefail

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

AWS_REGION="${AWS_REGION:-ap-south-1}"
RDS_INSTANCE="${RDS_INSTANCE:-atithi-dev-postgres}"
ECS_CLUSTER="${ECS_CLUSTER:-atithi-dev-cluster}"
ECS_SERVICE="${ECS_SERVICE:-atithi-dev-service}"

echo -e "${CYAN}======================================================${NC}"
echo -e "${CYAN}   Atithi AWS Backend - STOPPING / PAUSING SERVICES   ${NC}"
echo -e "${CYAN}   Region : ${AWS_REGION}${NC}"
echo -e "${CYAN}======================================================${NC}"

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo -e "${RED}Error: 'aws' CLI is not installed or not in PATH.${NC}"
    exit 1
fi

# ------------------------------------------------------------------------------
# 1. Scale Down ECS Service (Stop Container Tasks)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}Step 1: Scaling down ECS tasks to 0...${NC}"
aws ecs update-service \
    --cluster "$ECS_CLUSTER" \
    --service "$ECS_SERVICE" \
    --desired-count 0 \
    --region "$AWS_REGION" > /dev/null

echo -e "${GREEN}✓ ECS desired count set to 0. Fargate tasks will terminate.${NC}"

# ------------------------------------------------------------------------------
# 2. Stop RDS PostgreSQL Database
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}Step 2: Checking RDS Database status...${NC}"
DB_STATUS=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE" \
    --region "$AWS_REGION" \
    --query "DBInstances[0].DBInstanceStatus" \
    --output text 2>/dev/null || echo "not-found")

echo -e "Current RDS Database Status: ${CYAN}${DB_STATUS}${NC}"

if [ "$DB_STATUS" == "available" ]; then
    echo -e "${YELLOW}Stopping RDS instance '${RDS_INSTANCE}'...${NC}"
    aws rds stop-db-instance \
        --db-instance-identifier "$RDS_INSTANCE" \
        --region "$AWS_REGION" > /dev/null
    echo -e "${GREEN}✓ RDS stop command sent successfully. Database is now stopping.${NC}"
elif [ "$DB_STATUS" == "stopped" ] || [ "$DB_STATUS" == "stopping" ]; then
    echo -e "${GREEN}✓ RDS is already in '${DB_STATUS}' state. No action needed.${NC}"
else
    echo -e "${YELLOW}RDS status is '${DB_STATUS}'. Cannot stop in this state.${NC}"
fi

echo -e "\n${GREEN}======================================================${NC}"
echo -e "${GREEN}   ✅ COMPUTE SERVICES PAUSED / STOPPED!             ${NC}"
echo -e "${GREEN}   ECS Tasks: Scaled to 0 (Fargate billing stopped)  ${NC}"
echo -e "${GREEN}   RDS Database: Stopped (Compute billing stopped)   ${NC}"
echo -e "${GREEN}   Data & ECR Images: 100% Preserved                 ${NC}"
echo -e "${GREEN}======================================================${NC}"
