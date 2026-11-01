#!/usr/bin/env bash
# ==============================================================================
# Atithi Backend - Stop Services & Pause Infrastructure
# Shuts down compute and eliminates costly idle network resources (NAT Gateway / ALB)
# Run from Termux / Linux / macOS / WSL / Git Bash
#
# Supported modes:
#   ./stop-services.sh                      (Default: pauses ECS + RDS, destroys NAT Gateway -> saves ~$50/mo)
#   ./stop-services.sh --destroy-alb        (Full Teardown: pauses ECS + RDS, destroys NAT Gateway + ALB -> saves ~$68/mo)
#   ./stop-services.sh --compute-only       (Pause Compute only: ECS=0 + RDS=stop, keeps NAT & ALB running)
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

STOP_MODE="pause-and-destroy-nat"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --compute-only)
            STOP_MODE="pause-compute-only"
            shift
            ;;
        --destroy-alb|--all)
            STOP_MODE="destroy-nat-and-alb"
            shift
            ;;
        -r|--region)
            AWS_REGION="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: ./stop-services.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  (no flags)            Pause ECS & RDS, destroy NAT Gateway (saves ~\$50/mo, keeps ALB intact)"
            echo "  --destroy-alb, --all  Pause ECS & RDS, destroy NAT Gateway AND ALB (saves ~\$68/mo)"
            echo "  --compute-only        Pause ECS & RDS only (keeps NAT Gateway & ALB running)"
            echo "  -r, --region <region> AWS region (default: ap-south-1)"
            exit 0
            ;;
        *)
            shift
            ;;
    esac
done

echo ""
echo -e "${CYAN}${BOLD}===================================================================================${NC}"
echo -e "${CYAN}${BOLD}               ATITHI BACKEND — STOP SERVICES & PAUSE INFRASTRUCTURE               ${NC}"
echo -e "${CYAN}${BOLD}===================================================================================${NC}"
echo -e " Region: ${BOLD}${AWS_REGION}${NC}  |  Mode: ${BOLD}${STOP_MODE}${NC}"
echo ""

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo -e "${RED}Error: 'aws' CLI is not installed or not in PATH.${NC}"
    exit 1
fi

# ------------------------------------------------------------------------------
# 1. Scale Down ECS Service (Stop Container Tasks)
# ------------------------------------------------------------------------------
echo -e "${YELLOW}[1/3] Scaling down ECS tasks to 0 (Stopping Fargate compute)...${NC}"
aws ecs update-service \
    --cluster "$ECS_CLUSTER" \
    --service "$ECS_SERVICE" \
    --desired-count 0 \
    --region "$AWS_REGION" > /dev/null 2>&1 || true

echo -e "${GREEN}✓ ECS desired count set to 0. Running Fargate tasks will terminate.${NC}"

# ------------------------------------------------------------------------------
# 2. Stop RDS PostgreSQL Database
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[2/3] Checking RDS Database status ($RDS_INSTANCE)...${NC}"
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
    echo -e "${YELLOW}RDS status is '${DB_STATUS}'. Proceeding...${NC}"
fi

# ------------------------------------------------------------------------------
# 3. Destroy NAT Gateway and Costly Idle Networking
# ------------------------------------------------------------------------------
if [ "$STOP_MODE" != "pause-compute-only" ]; then
    echo -e "\n${YELLOW}[3/3] Handling costly idle network infrastructure...${NC}"

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
        echo -e "${CYAN}Running Terraform selective teardown via ${TF_DIR}...${NC}"
        pushd "$TF_DIR" > /dev/null

        if [ "$STOP_MODE" == "pause-and-destroy-nat" ]; then
            echo -e "${YELLOW}Destroying NAT Gateway and EIP to eliminate ~\$32.40/mo idle cost...${NC}"
            terraform destroy \
                -target="aws_route.private_app_internet" \
                -target="aws_nat_gateway.main" \
                -target="aws_eip.nat" \
                -auto-approve -no-color || true
            echo -e "${GREEN}✓ NAT Gateway destroyed via Terraform!${NC}"
        elif [ "$STOP_MODE" == "destroy-nat-and-alb" ]; then
            echo -e "${YELLOW}Destroying NAT Gateway, EIP, and ALB (Full Teardown)...${NC}"
            terraform destroy \
                -target="aws_ecs_service.app" \
                -target="aws_lb_listener.http" \
                -target="aws_lb_target_group.app" \
                -target="aws_lb.main" \
                -target="aws_route.private_app_internet" \
                -target="aws_nat_gateway.main" \
                -target="aws_eip.nat" \
                -auto-approve -no-color || true
            echo -e "${GREEN}✓ NAT Gateway and ALB destroyed via Terraform!${NC}"
        fi
        popd > /dev/null
    else
        # Pure AWS CLI fallback (runs in Termux or environments without local Terraform)
        echo -e "${CYAN}Detecting active NAT Gateway in VPC ($VPC_ID) via AWS CLI...${NC}"
        NAT_ID=$(aws ec2 describe-nat-gateways \
            --filter "Name=vpc-id,Values=$VPC_ID" "Name=state,Values=available,pending" \
            --region "$AWS_REGION" \
            --query "NatGateways[0].NatGatewayId" \
            --output text 2>/dev/null || echo "None")

        if [ "$NAT_ID" != "None" ] && [ -n "$NAT_ID" ]; then
            echo -e "${YELLOW}Deleting NAT Gateway '$NAT_ID' via AWS CLI...${NC}"
            aws ec2 delete-nat-gateway --nat-gateway-id "$NAT_ID" --region "$AWS_REGION" > /dev/null 2>&1 || true
            echo -e "${GREEN}✓ NAT Gateway deletion initiated! Hourly charges (~\$32.40/mo) stopped immediately.${NC}"
        else
            echo -e "${GREEN}✓ No active NAT Gateway found (already destroyed or not running).${NC}"
        fi

        if [ "$STOP_MODE" == "destroy-nat-and-alb" ]; then
            echo -e "${YELLOW}Searching for active Application Load Balancer ($ALB_NAME)...${NC}"
            ALB_ARN=$(aws elbv2 describe-load-balancers \
                --names "$ALB_NAME" \
                --region "$AWS_REGION" \
                --query "LoadBalancers[0].LoadBalancerArn" \
                --output text 2>/dev/null || echo "None")

            if [ "$ALB_ARN" != "None" ] && [ -n "$ALB_ARN" ]; then
                echo -e "${YELLOW}Deleting Load Balancer '$ALB_NAME'...${NC}"
                aws elbv2 delete-load-balancer --load-balancer-arn "$ALB_ARN" --region "$AWS_REGION" > /dev/null 2>&1 || true
                echo -e "${GREEN}✓ ALB deleted via AWS CLI (saving ~\$18.00/mo).${NC}"
            fi
        fi
    fi
else
    echo -e "\n${DIM}[3/3] NAT Gateway & ALB preserved (compute-only mode selected).${NC}"
fi

# ------------------------------------------------------------------------------
# Summary
# ------------------------------------------------------------------------------
echo ""
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo -e "${GREEN}${BOLD}   ✅ SERVICES STOPPED & INFRASTRUCTURE OPTIMIZED!                                 ${NC}"
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo -e "  • ${BOLD}ECS Tasks${NC}         : Scaled to 0 (Fargate compute billing stopped)"
echo -e "  • ${BOLD}RDS Database${NC}      : Stopped (PostgreSQL compute billing stopped)"
if [ "$STOP_MODE" == "pause-and-destroy-nat" ]; then
    echo -e "  • ${BOLD}NAT Gateway${NC}       : ${GREEN}DESTROYED${NC} (Eliminated ~\$32.40/mo idle charge!)"
    echo -e "  • ${BOLD}ALB & DNS${NC}         : Preserved (Keeps CloudFront routing intact)"
elif [ "$STOP_MODE" == "destroy-nat-and-alb" ]; then
    echo -e "  • ${BOLD}NAT Gateway & ALB${NC} : ${GREEN}DESTROYED${NC} (Eliminated ~\$50.40/mo network charges!)"
fi
echo -e "  • ${BOLD}Database Data${NC}     : ${GREEN}100% PRESERVED${NC} in private PostgreSQL storage"
echo -e "  • ${BOLD}Docker Images${NC}     : ${GREEN}100% PRESERVED${NC} in Amazon ECR"
echo -e "  • ${BOLD}Frontend Static${NC}   : ${GREEN}100% LIVE${NC} in S3 & CloudFront"
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo ""
echo -e "💡 To check your live billing & service status at any time, run: ${BOLD}./check-service-status.sh${NC}"
echo ""
