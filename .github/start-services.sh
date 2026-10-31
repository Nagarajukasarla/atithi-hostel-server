#!/usr/bin/env bash
# ==============================================================================
# Atithi Backend - Start Services (Resume Infrastructure)
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
CLOUDFRONT_URL="https://d2dk2x85a04vx5.cloudfront.net"

echo -e "${CYAN}======================================================${NC}"
echo -e "${CYAN}   Atithi AWS Backend - STARTING / RESUMING SERVICES  ${NC}"
echo -e "${CYAN}   Region : ${AWS_REGION}${NC}"
echo -e "${CYAN}======================================================${NC}"

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo -e "${RED}Error: 'aws' CLI is not installed or not in PATH.${NC}"
    exit 1
fi

# ------------------------------------------------------------------------------
# 1. Check and Start RDS Database
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}Step 1: Checking RDS Database ($RDS_INSTANCE)...${NC}"
DB_STATUS=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE" \
    --region "$AWS_REGION" \
    --query "DBInstances[0].DBInstanceStatus" \
    --output text 2>/dev/null || echo "not-found")

echo -e "Current RDS Database Status: ${GREEN}${DB_STATUS}${NC}"

if [ "$DB_STATUS" == "stopped" ]; then
    echo -e "${YELLOW}Database is stopped. Initiating start...${NC}"
    aws rds start-db-instance \
        --db-instance-identifier "$RDS_INSTANCE" \
        --region "$AWS_REGION" > /dev/null
    
    echo -e "${CYAN}Waiting for RDS instance to transition to 'available' (typically ~2-3 mins)...${NC}"
    aws rds wait db-instance-available \
        --db-instance-identifier "$RDS_INSTANCE" \
        --region "$AWS_REGION"
    echo -e "${GREEN}✓ RDS Database is now AVAILABLE!${NC}"
elif [ "$DB_STATUS" == "starting" ]; then
    echo -e "${CYAN}Database is currently starting up. Waiting for 'available' state...${NC}"
    aws rds wait db-instance-available \
        --db-instance-identifier "$RDS_INSTANCE" \
        --region "$AWS_REGION"
    echo -e "${GREEN}✓ RDS Database is now AVAILABLE!${NC}"
elif [ "$DB_STATUS" == "available" ]; then
    echo -e "${GREEN}✓ RDS Database is already running and AVAILABLE.${NC}"
else
    echo -e "${YELLOW}RDS status is: $DB_STATUS. Proceeding...${NC}"
fi

# ------------------------------------------------------------------------------
# 2. Scale Up ECS Service (Start Fargate Task)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}Step 2: Checking ECS Service ($ECS_SERVICE)...${NC}"
DESIRED_COUNT=$(aws ecs describe-services \
    --cluster "$ECS_CLUSTER" \
    --services "$ECS_SERVICE" \
    --region "$AWS_REGION" \
    --query "services[0].desiredCount" \
    --output text 2>/dev/null || echo "0")

echo -e "Current ECS Service Desired Count: ${CYAN}${DESIRED_COUNT}${NC}"

if [ "$DESIRED_COUNT" -eq 0 ]; then
    echo -e "${YELLOW}Scaling up ECS service to desired-count = 1...${NC}"
    aws ecs update-service \
        --cluster "$ECS_CLUSTER" \
        --service "$ECS_SERVICE" \
        --desired-count 1 \
        --region "$AWS_REGION" > /dev/null
    echo -e "${GREEN}Scale-up command sent. Fargate task is starting.${NC}"
else
    echo -e "${GREEN}✓ ECS service already has desired-count >= 1.${NC}"
fi

echo -e "${CYAN}Waiting for ECS service tasks to stabilize...${NC}"
aws ecs wait services-stable \
    --cluster "$ECS_CLUSTER" \
    --service "$ECS_SERVICE" \
    --region "$AWS_REGION"
echo -e "${GREEN}✓ ECS service is STABLE and running!${NC}"

# ------------------------------------------------------------------------------
# 3. Verify Health Check
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}Step 3: Verifying live health check on CloudFront...${NC}"
echo -e "Testing URL: ${CYAN}${CLOUDFRONT_URL}/actuator/health${NC}"

for i in {1..10}; do
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "${CLOUDFRONT_URL}/actuator/health" || true)
    if [ "$HTTP_STATUS" == "200" ]; then
        echo -e "${GREEN}✓ Health Check PASSED (HTTP 200 OK)!${NC}"
        echo -e "\n${GREEN}======================================================${NC}"
        echo -e "${GREEN}   🎉 ALL SERVICES ARE LIVE AND FULLY OPERATIONAL!   ${NC}"
        echo -e "${GREEN}   Site URL: ${CLOUDFRONT_URL}${NC}"
        echo -e "${GREEN}======================================================${NC}"
        exit 0
    fi
    echo -e "Attempt $i/10: Received HTTP $HTTP_STATUS, retrying in 5s..."
    sleep 5
done

echo -e "${YELLOW}Notice: Health check polling timed out, but containers are booting.${NC}"
echo -e "${GREEN}Services are started.${NC}"
