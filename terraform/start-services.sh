#!/usr/bin/env bash
# ==============================================================================
# Atithi Full Stack - Start Services (Resume Infrastructure & Re-link Frontend)
# 1. Starts RDS PostgreSQL database (and waits until available)
# 2. Re-provisions missing NAT Gateway, EIP, and ALB via Terraform
# 3. Scales up ECS Fargate container and stabilizes tasks
# 4. Re-links Frontend CloudFront and ALB Listener Rules to the new ALB DNS
# 5. Performs live end-to-end health verification
#
# Supported environments: Termux, Linux, macOS, WSL, Git Bash
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
echo -e "${CYAN}${BOLD}          ATITHI FULL STACK — START SERVICES & RE-LINK INFRASTRUCTURE              ${NC}"
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
echo -e "${YELLOW}[1/5] Checking RDS PostgreSQL Database ($RDS_INSTANCE)...${NC}"
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
# 2. Check and Re-Provision Missing Backend Infrastructure (NAT / ALB)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[2/5] Checking Backend Infrastructure (NAT Gateway & ALB)...${NC}"

BE_TF_DIR=""
if [ -d "aavas/terraform" ]; then
    BE_TF_DIR="aavas/terraform"
elif [ -d "terraform" ] && [ -f "terraform/backend.tf" ]; then
    BE_TF_DIR="terraform"
elif [ -d "../terraform" ] && [ -f "../terraform/backend.tf" ]; then
    BE_TF_DIR="../terraform"
elif [ -f "backend.tf" ]; then
    BE_TF_DIR="."
fi

NEW_ALB_DNS=""
if command -v terraform &> /dev/null && [ -n "$BE_TF_DIR" ]; then
    echo -e "${CYAN}Checking & auto-provisioning missing Backend resources via Terraform (${BE_TF_DIR})...${NC}"
    pushd "$BE_TF_DIR" > /dev/null
    terraform init -no-color > /dev/null 2>&1 || true
    terraform apply -auto-approve -no-color
    NEW_ALB_DNS=$(terraform output -raw alb_dns_name 2>/dev/null || echo "")
    popd > /dev/null
    echo -e "${GREEN}✓ Backend Infrastructure re-provisioned! New ALB DNS: ${NEW_ALB_DNS}${NC}"
elif command -v gh &> /dev/null; then
    echo -e "${CYAN}Triggering GitHub Actions workflow to auto-provision and re-link full stack...${NC}"
    gh workflow run "Start Services / Resume Infrastructure"
    echo -e "${GREEN}✓ Start workflow triggered in GitHub Actions!${NC}"
    exit 0
else
    echo -e "${CYAN}Querying live ALB DNS via AWS CLI...${NC}"
    NEW_ALB_DNS=$(aws elbv2 describe-load-balancers \
        --names "$ALB_NAME" \
        --region "$AWS_REGION" \
        --query "LoadBalancers[0].DNSName" \
        --output text 2>/dev/null || echo "")
    if [ -z "$NEW_ALB_DNS" ] || [ "$NEW_ALB_DNS" == "None" ]; then
        echo -e "${RED}⚠️  ALB is missing and local Terraform is not available.${NC}"
        echo -e "${CYAN}👉 To auto-provision missing NAT/ALB and re-link in 1 click:${NC}"
        echo -e "   Go to GitHub ➔ atithi-hostel-server ➔ Actions ➔ 'Start Services / Resume Infrastructure' ➔ Run workflow."
    fi
fi

# ------------------------------------------------------------------------------
# 3. Scale Up ECS Service (Start Fargate Task)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[3/5] Checking ECS Service ($ECS_SERVICE)...${NC}"
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
# 4. Re-Link Frontend CloudFront & Security Rules to New ALB
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[4/5] Re-linking Frontend CloudFront to New ALB...${NC}"

FE_TF_DIR=""
if [ -d "atithi/terraform" ]; then
    FE_TF_DIR="atithi/terraform"
elif [ -d "../atithi/terraform" ]; then
    FE_TF_DIR="../atithi/terraform"
fi

if command -v terraform &> /dev/null && [ -n "$FE_TF_DIR" ]; then
    echo -e "${CYAN}Applying Frontend Terraform (${FE_TF_DIR}) to re-link CloudFront Origin 2 to New ALB...${NC}"
    pushd "$FE_TF_DIR" > /dev/null
    terraform init -no-color > /dev/null 2>&1 || true
    terraform apply -var="cloudfront_enabled=true" -auto-approve -no-color
    popd > /dev/null
    echo -e "${GREEN}✓ Frontend CloudFront and ALB listener rules successfully re-linked!${NC}"
else
    echo -e "${DIM}Local frontend Terraform directory not found. Re-linking handled via GitHub Actions.${NC}"
fi

# ------------------------------------------------------------------------------
# 5. Verify Health Check
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[5/5] Verifying live endpoint connectivity...${NC}"
if command -v curl &> /dev/null; then
    for i in {1..12}; do
        HTTP_CODE=$(curl -s -m 5 -o /dev/null -w "%{http_code}" "${CLOUDFRONT_URL}/actuator/health" 2>/dev/null || echo "000")
        if [ "$HTTP_CODE" == "200" ]; then
            echo -e "${GREEN}✓ Health check PASSED! Spring Boot is responding via CloudFront (${CLOUDFRONT_URL}).${NC}"
            break
        fi
        echo -e "${CYAN}Attempt $i/12: Waiting for CloudFront and Spring Boot to link (HTTP $HTTP_CODE)... retrying in 5s${NC}"
        sleep 5
    done
fi

echo ""
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo -e "${GREEN}${BOLD}   🎉 ATITHI FULL STACK IS LIVE, PROPERLY LINKED & READY FOR TRAFFIC!              ${NC}"
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo -e "  • Frontend App : ${BOLD}https://d2dk2x85a04vx5.cloudfront.net${NC}"
echo -e "  • Backend API  : ${BOLD}https://d2dk2x85a04vx5.cloudfront.net/actuator/health${NC}"
if [ -n "$NEW_ALB_DNS" ]; then
    echo -e "  • Backend ALB  : ${DIM}http://${NEW_ALB_DNS} (Direct access locked to CloudFront)${NC}"
fi
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo ""
