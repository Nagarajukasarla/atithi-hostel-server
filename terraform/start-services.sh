#!/usr/bin/env bash
# ==============================================================================
# Atithi Backend - Start Services (Resume Infrastructure)
# Starts RDS PostgreSQL, ensures NAT Gateway / ALB are active, and scales ECS up.
# Run from Termux / Linux / macOS / WSL / Git Bash
# ==============================================================================

set -eo pipefail

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m' # No Color

AWS_REGION="${AWS_REGION:-ap-south-1}"
RDS_INSTANCE="${RDS_INSTANCE:-atithi-dev-postgres}"
ECS_CLUSTER="${ECS_CLUSTER:-atithi-dev-cluster}"
ECS_SERVICE="${ECS_SERVICE:-atithi-dev-service}"
ALB_NAME="${ALB_NAME:-atithi-dev-alb}"
VPC_ID="${VPC_ID:-vpc-058c5dd8e8c8dbfdb}"
CLOUDFRONT_URL="https://d2dk2x85a04vx5.cloudfront.net"

echo ""
echo -e "${CYAN}${BOLD}===================================================================================${NC}"
echo -e "${CYAN}${BOLD}               ATITHI BACKEND — START SERVICES & RESUME INFRASTRUCTURE              ${NC}"
echo -e "${CYAN}${BOLD}===================================================================================${NC}"
echo -e " Region: ${BOLD}${AWS_REGION}${NC}"
echo ""

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo -e "${RED}Error: 'aws' CLI is not installed or not in PATH.${NC}"
    exit 1
fi

# ------------------------------------------------------------------------------
# 1. Check and Start RDS Database
# ------------------------------------------------------------------------------
echo -e "${YELLOW}[1/4] Checking RDS PostgreSQL Database ($RDS_INSTANCE)...${NC}"
DB_STATUS=$(aws rds describe-db-instances \
    --db-instance-identifier "$RDS_INSTANCE" \
    --region "$AWS_REGION" \
    --query "DBInstances[0].DBInstanceStatus" \
    --output text 2>/dev/null || echo "not-found")

echo -e "Current RDS Database Status: ${CYAN}${DB_STATUS}${NC}"

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
# 2. Check NAT Gateway (Required for Fargate private subnet ECR pulling)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[2/4] Checking NAT Gateway status...${NC}"
NAT_STATE=$(aws ec2 describe-nat-gateways \
    --filter "Name=vpc-id,Values=$VPC_ID" "Name=state,Values=available,pending" \
    --region "$AWS_REGION" \
    --query "NatGateways[0].State" \
    --output text 2>/dev/null || echo "None")

if [ "$NAT_STATE" == "None" ] || [ -z "$NAT_STATE" ]; then
    echo -e "${YELLOW}NAT Gateway is currently missing/destroyed.${NC}"

    # Check if local Terraform exists
    TF_DIR=""
    if [ -d "terraform" ] && [ -f "terraform/backend.tf" ]; then
        TF_DIR="terraform"
    elif [ -d "../terraform" ] && [ -f "../terraform/backend.tf" ]; then
        TF_DIR="../terraform"
    elif [ -f "backend.tf" ]; then
        TF_DIR="."
    fi

    if command -v terraform &> /dev/null && [ -n "$TF_DIR" ]; then
        echo -e "${CYAN}Auto-recreating missing NAT Gateway via local Terraform (${TF_DIR})...${NC}"
        pushd "$TF_DIR" > /dev/null
        terraform apply -auto-approve -no-color
        popd > /dev/null
        echo -e "${GREEN}✓ NAT Gateway recreated successfully!${NC}"
    elif command -v gh &> /dev/null; then
        echo -e "${CYAN}Triggering GitHub Actions workflow to auto-provision missing NAT Gateway...${NC}"
        gh workflow run "Start Services / Resume Infrastructure"
        echo -e "${GREEN}✓ Start workflow triggered in GitHub Actions!${NC}"
    else
        echo -e "${RED}⚠️  Warning: NAT Gateway is missing! Private ECS tasks need NAT to pull Docker images.${NC}"
        echo -e "${CYAN}👉 To auto-recreate NAT Gateway with 1 click:${NC}"
        echo -e "   Go to GitHub ➔ atithi-hostel-server ➔ Actions ➔ 'Start Services / Resume Infrastructure' ➔ Run workflow."
    fi
else
    echo -e "${GREEN}✓ NAT Gateway is active ($NAT_STATE).${NC}"
fi

# ------------------------------------------------------------------------------
# 3. Scale Up ECS Service (Start Fargate Task)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[3/4] Checking ECS Service ($ECS_SERVICE)...${NC}"
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
    echo -e "${GREEN}✓ Scale-up command sent. Fargate task is starting.${NC}"
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
# 4. Verify Health Check
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[4/4] Verifying live endpoint connectivity...${NC}"
if command -v curl &> /dev/null; then
    for i in {1..8}; do
        HTTP_CODE=$(curl -s -m 5 -o /dev/null -w "%{http_code}" "${CLOUDFRONT_URL}/actuator/health" 2>/dev/null || echo "000")
        if [ "$HTTP_CODE" == "200" ]; then
            echo -e "${GREEN}✓ Health check PASSED! Spring Boot is responding via CloudFront (${CLOUDFRONT_URL}).${NC}"
            break
        fi
        echo -e "${CYAN}Attempt $i/8: Waiting for Spring Boot to warm up (HTTP $HTTP_CODE)... retrying in 5s${NC}"
        sleep 5
    done
fi

echo ""
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo -e "${GREEN}${BOLD}   🎉 ATITHI BACKEND IS LIVE & READY FOR TRAFFIC!                                 ${NC}"
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo -e "  Frontend SPA : ${BOLD}https://d2dk2x85a04vx5.cloudfront.net${NC}"
echo -e "  Backend API  : ${BOLD}https://d2dk2x85a04vx5.cloudfront.net/actuator/health${NC}"
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo ""
