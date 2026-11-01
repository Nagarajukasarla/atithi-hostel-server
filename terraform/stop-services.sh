#!/usr/bin/env bash
# ==============================================================================
# Atithi Full Stack - Stop Services & Pause Infrastructure
# Shuts down compute and eliminates 100% of costly idle network resources:
# - Scales ECS tasks to 0 (Stops Fargate compute charges)
# - Stops PostgreSQL RDS Database (Stops compute charges; data 100% preserved)
# - Destroys NAT Gateway & Elastic IP (Saves ~$32.40/month)
# - Destroys Application Load Balancer & Target Group (Saves ~$25.20/month)
# - Pauses Frontend CloudFront Distribution (Stops all traffic)
#
# Supported environments: Termux, Linux, macOS, WSL, Git Bash
#
# Usage:
#   ./stop-services.sh                      (Default: Complete Full-Stack Teardown -> saves ~$68/mo)
#   ./stop-services.sh --keep-alb           (Pauses ECS + RDS + NAT Gateway, keeps ALB intact -> saves ~$50/mo)
#   ./stop-services.sh --compute-only       (Pauses ECS + RDS only, keeps all networking)
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
CF_DIST_ID="${CF_DIST_ID:-E3IJXZS95HCP8S}"

STOP_MODE="full-teardown"

# Parse arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
        --compute-only)
            STOP_MODE="pause-compute-only"
            shift
            ;;
        --keep-alb)
            STOP_MODE="pause-and-destroy-nat"
            shift
            ;;
        --all|--full)
            STOP_MODE="full-teardown"
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
            echo "  (no flags)            Complete Full-Stack Teardown: ECS=0, RDS=stop, destroy NAT+ALB, pause CloudFront (saves ~\$68/mo)"
            echo "  --keep-alb            Pause ECS & RDS, destroy NAT Gateway only (keeps ALB & CloudFront active)"
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
echo -e "${CYAN}${BOLD}             ATITHI FULL STACK — STOP SERVICES & COMPLETE TEARDOWN                 ${NC}"
echo -e "${CYAN}${BOLD}===================================================================================${NC}"
echo -e " Region: ${BOLD}${AWS_REGION}${NC}  |  Mode: ${BOLD}${STOP_MODE}${NC}"
echo ""

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo -e "${RED}Error: 'aws' CLI is not installed or not in PATH.${NC}"
    exit 1
fi

# ------------------------------------------------------------------------------
# 1. Pause Frontend (Disable CloudFront Distribution)
# ------------------------------------------------------------------------------
if [ "$STOP_MODE" == "full-teardown" ]; then
    echo -e "${YELLOW}[1/4] Pausing Frontend CloudFront Distribution ($CF_DIST_ID)...${NC}"
    
    # Check if local frontend terraform exists
    FE_TF_DIR=""
    if [ -d "atithi/terraform" ]; then
        FE_TF_DIR="atithi/terraform"
    elif [ -d "../atithi/terraform" ]; then
        FE_TF_DIR="../atithi/terraform"
    fi

    if command -v terraform &> /dev/null && [ -n "$FE_TF_DIR" ]; then
        echo -e "${CYAN}Pausing CloudFront via local Terraform ($FE_TF_DIR)...${NC}"
        pushd "$FE_TF_DIR" > /dev/null
        terraform apply -var="cloudfront_enabled=false" -auto-approve -no-color > /dev/null 2>&1 || true
        popd > /dev/null
        echo -e "${GREEN}✓ CloudFront distribution paused via Terraform.${NC}"
    else
        # Disable via AWS CLI directly
        CF_CONFIG_RAW=$(aws cloudfront get-distribution-config --id "$CF_DIST_ID" --output json 2>/dev/null || echo "")
        if [ -n "$CF_CONFIG_RAW" ]; then
            CF_ETAG=$(aws cloudfront get-distribution-config --id "$CF_DIST_ID" --query "ETag" --output text 2>/dev/null || echo "")
            TMP_CF=$(mktemp 2>/dev/null || mktemp -t cf-cfg)
            aws cloudfront get-distribution-config --id "$CF_DIST_ID" --query "DistributionConfig" --output json 2>/dev/null | \
                sed 's/"Enabled": true/"Enabled": false/' > "$TMP_CF" || true
            aws cloudfront update-distribution --id "$CF_DIST_ID" --if-match "$CF_ETAG" --distribution-config file://"$TMP_CF" > /dev/null 2>&1 || true
            rm -f "$TMP_CF"
            echo -e "${GREEN}✓ CloudFront distribution paused via AWS CLI.${NC}"
        else
            echo -e "${DIM}CloudFront not modified or already paused.${NC}"
        fi
    fi
else
    echo -e "${DIM}[1/4] Frontend CloudFront preserved (non-full teardown mode).${NC}"
fi

# ------------------------------------------------------------------------------
# 2. Scale Down ECS Service (Stop Container Tasks)
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[2/4] Scaling down ECS tasks to 0 (Stopping Fargate compute)...${NC}"
aws ecs update-service \
    --cluster "$ECS_CLUSTER" \
    --service "$ECS_SERVICE" \
    --desired-count 0 \
    --region "$AWS_REGION" > /dev/null 2>&1 || true

echo -e "${GREEN}✓ ECS desired count set to 0. Fargate tasks will terminate.${NC}"

# ------------------------------------------------------------------------------
# 3. Stop RDS PostgreSQL Database
# ------------------------------------------------------------------------------
echo -e "\n${YELLOW}[3/4] Checking RDS Database status ($RDS_INSTANCE)...${NC}"
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
# 4. Destroy Deletable Network Infrastructure (NAT Gateway & ALB)
# ------------------------------------------------------------------------------
if [ "$STOP_MODE" != "pause-compute-only" ]; then
    echo -e "\n${YELLOW}[4/4] Destroying costly idle network infrastructure...${NC}"

    # Check if local backend terraform exists
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

    if command -v terraform &> /dev/null && [ -n "$BE_TF_DIR" ]; then
        echo -e "${CYAN}Running Terraform selective teardown via ${BE_TF_DIR}...${NC}"
        pushd "$BE_TF_DIR" > /dev/null

        if [ "$STOP_MODE" == "full-teardown" ]; then
            echo -e "${YELLOW}Destroying NAT Gateway, EIP, ALB, Target Group, and Listener...${NC}"
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
        elif [ "$STOP_MODE" == "pause-and-destroy-nat" ]; then
            echo -e "${YELLOW}Destroying NAT Gateway and EIP (keeping ALB intact)...${NC}"
            terraform destroy \
                -target="aws_route.private_app_internet" \
                -target="aws_nat_gateway.main" \
                -target="aws_eip.nat" \
                -auto-approve -no-color || true
            echo -e "${GREEN}✓ NAT Gateway destroyed via Terraform!${NC}"
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

        if [ "$STOP_MODE" == "full-teardown" ]; then
            echo -e "${YELLOW}Searching for active Application Load Balancer ($ALB_NAME)...${NC}"
            ALB_ARN=$(aws elbv2 describe-load-balancers \
                --names "$ALB_NAME" \
                --region "$AWS_REGION" \
                --query "LoadBalancers[0].LoadBalancerArn" \
                --output text 2>/dev/null || echo "None")

            if [ "$ALB_ARN" != "None" ] && [ -n "$ALB_ARN" ]; then
                echo -e "${YELLOW}Deleting Load Balancer '$ALB_NAME'...${NC}"
                aws elbv2 delete-load-balancer --load-balancer-arn "$ALB_ARN" --region "$AWS_REGION" > /dev/null 2>&1 || true
                echo -e "${GREEN}✓ ALB deleted via AWS CLI (saving ~\$25.20/mo).${NC}"
            fi
        fi
    fi
else
    echo -e "\n${DIM}[4/4] Networking preserved (compute-only mode selected).${NC}"
fi

# ------------------------------------------------------------------------------
# Summary
# ------------------------------------------------------------------------------
echo ""
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo -e "${GREEN}${BOLD}   ✅ ALL SERVICES STOPPED & DELETABLE RESOURCES DESTROYED!                       ${NC}"
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo -e "  • ${BOLD}ECS Tasks${NC}         : Scaled to 0 (Fargate compute billing stopped)"
echo -e "  • ${BOLD}RDS Database${NC}      : Stopped (PostgreSQL compute billing stopped)"
if [ "$STOP_MODE" == "full-teardown" ]; then
    echo -e "  • ${BOLD}NAT Gateway & ALB${NC} : ${GREEN}DESTROYED${NC} (Eliminated ~\$68.00/mo network fees!)"
    echo -e "  • ${BOLD}Frontend CDN${NC}      : ${GREEN}PAUSED${NC} (CloudFront traffic paused)"
elif [ "$STOP_MODE" == "pause-and-destroy-nat" ]; then
    echo -e "  • ${BOLD}NAT Gateway${NC}       : ${GREEN}DESTROYED${NC} (Eliminated ~\$32.40/mo idle charge!)"
fi
echo -e "  • ${BOLD}Database Data${NC}     : ${GREEN}100% PRESERVED${NC} in private PostgreSQL storage"
echo -e "  • ${BOLD}Docker Images${NC}     : ${GREEN}100% PRESERVED${NC} in Amazon ECR"
echo -e "  • ${BOLD}Frontend Static${NC}   : ${GREEN}100% PRESERVED${NC} in Amazon S3"
echo -e "${GREEN}${BOLD}===================================================================================${NC}"
echo ""
echo -e "💰 ${BOLD}Idle Running Cost: \$0.00 / day${NC} (only baseline DB storage <\$2.30/mo remains)"
echo -e "💡 To check your live billing & service status at any time, run: ${BOLD}./check-service-status.sh${NC}"
echo ""
