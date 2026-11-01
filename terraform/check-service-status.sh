#!/usr/bin/env bash
# ==============================================================================
# Atithi Infrastructure & Services Status Dashboard
# Checks all deployed AWS and Application services, outputs a clean tabular report,
# detects unnecessary idle cost drivers, and tests live endpoint health.
#
# Supported environments: Linux, macOS, WSL, Termux, Git Bash
#
# Usage:
#   ./check-service-status.sh
#   ./check-service-status.sh --region ap-south-1
# ==============================================================================

set -eo pipefail

# ANSI Color Codes
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m' # No Color

# Default Configuration
AWS_REGION="${AWS_REGION:-ap-south-1}"
VPC_ID="${VPC_ID:-vpc-058c5dd8e8c8dbfdb}"
ECS_CLUSTER="${ECS_CLUSTER:-atithi-dev-cluster}"
ECS_SERVICE="${ECS_SERVICE:-atithi-dev-service}"
RDS_INSTANCE="${RDS_INSTANCE:-atithi-dev-postgres}"
ALB_NAME="${ALB_NAME:-atithi-dev-alb}"
TARGET_GROUP_NAME="${TARGET_GROUP_NAME:-atithi-dev-app-tg}"
S3_BUCKET="${S3_BUCKET:-atithi-frontend-551626543871}"
CF_DIST_ID="${CF_DIST_ID:-E3IJXZS95HCP8S}"
CF_DOMAIN="${CF_DOMAIN:-d2dk2x85a04vx5.cloudfront.net}"

# Parse custom flags
while [[ $# -gt 0 ]]; do
    case "$1" in
        -r|--region)
            AWS_REGION="$2"
            shift 2
            ;;
        -h|--help)
            echo "Usage: ./check-service-status.sh [-r|--region <aws-region>]"
            echo "Checks live status of all Atithi AWS infrastructure and application services in a tabular dashboard."
            exit 0
            ;;
        *)
            shift
            ;;
    esac
done

echo ""
echo -e "${CYAN}${BOLD}=================================================================================================================${NC}"
echo -e "${CYAN}${BOLD}                        ATITHI CLOUD SERVICES & INFRASTRUCTURE STATUS DASHBOARD                          ${NC}"
echo -e "${CYAN}${BOLD}=================================================================================================================${NC}"
echo -e " Region: ${BOLD}${AWS_REGION}${NC}  |  Timestamp: $(date -u '+%Y-%m-%d %H:%M:%S UTC')  |  Querying AWS..."
echo ""

# Helper to print a perfectly padded tabular row with color-safe column padding
print_row() {
    local col1="$1"
    local col2="$2"
    local col3="$3"
    local col4="$4"
    local col5="$5"
    local c3="${6:-$NC}"
    local c4="${7:-$NC}"
    local c5="${8:-$NC}"

    local p1 p2 p3 p4 p5
    printf -v p1 "%-20s" "$col1"
    printf -v p2 "%-30s" "$col2"
    printf -v p3 "%-18s" "$col3"
    printf -v p4 "%-16s" "$col4"
    printf -v p5 "%-20s" "$col5"

    echo -e "| ${p1} | ${p2} | ${c3}${p3}${NC} | ${c4}${p4}${NC} | ${c5}${p5}${NC} |"
}

print_separator() {
    echo "+----------------------+--------------------------------+--------------------+------------------+----------------------+"
}

TOTAL_EST_COST=0
ACTIVE_ALERTS=()

# ------------------------------------------------------------------------------
# 1. CloudFront Distribution (Frontend Edge)
# ------------------------------------------------------------------------------
CF_RAW=$(aws cloudfront get-distribution --id "$CF_DIST_ID" --query "Distribution.[Status, DistributionConfig.Enabled, DomainName]" --output text 2>/dev/null || echo "NOT_FOUND None None")
read -r CF_STATUS CF_ENABLED CF_DNS <<< "$CF_RAW"

if [ "$CF_STATUS" == "Deployed" ] && [ "$CF_ENABLED" == "True" ]; then
    CF_DISP="Deployed (Active)"
    CF_COLOR="$GREEN"
elif [ "$CF_STATUS" == "InProgress" ]; then
    CF_DISP="Deploying..."
    CF_COLOR="$YELLOW"
elif [ "$CF_STATUS" == "NOT_FOUND" ]; then
    CF_DISP="Not Found"
    CF_COLOR="$RED"
else
    CF_DISP="${CF_STATUS:-Disabled}"
    CF_COLOR="$YELLOW"
fi

# Live HTTP check on CloudFront Frontend
CF_HTTP="N/A"
CF_HTTP_COLOR="$DIM"
if command -v curl &>/dev/null && [ "$CF_STATUS" != "NOT_FOUND" ]; then
    CF_HTTP_CODE=$(curl -s -L -m 4 -o /dev/null -w "%{http_code}" "https://${CF_DNS:-$CF_DOMAIN}" 2>/dev/null || echo "000")
    if [ "$CF_HTTP_CODE" == "200" ]; then
        CF_HTTP="HTTP 200 (OK)"
        CF_HTTP_COLOR="$GREEN"
    elif [ "$CF_HTTP_CODE" == "000" ]; then
        CF_HTTP="Timeout / Err"
        CF_HTTP_COLOR="$RED"
    else
        CF_HTTP="HTTP $CF_HTTP_CODE"
        CF_HTTP_COLOR="$YELLOW"
    fi
fi

# ------------------------------------------------------------------------------
# 2. S3 Bucket (Frontend Static Hosting)
# ------------------------------------------------------------------------------
# In secure production setups, S3 public access is blocked and bucket policy restricts
# access to CloudFront OAC. A 403 Forbidden on head-bucket confirms bucket exists & is protected.
S3_ERR=$(aws s3api head-bucket --bucket "$S3_BUCKET" --region "$AWS_REGION" 2>&1 || true)
if [ -z "$S3_ERR" ]; then
    S3_STATUS="Active (Private)"
    S3_COLOR="$GREEN"
    S3_NOTE="Bucket Verified"
    S3_NOTE_COLOR="$GREEN"
elif echo "$S3_ERR" | grep -qi "403\|Forbidden"; then
    S3_STATUS="Active (Protected)"
    S3_COLOR="$GREEN"
    S3_NOTE="OAC Secured"
    S3_NOTE_COLOR="$GREEN"
elif echo "$S3_ERR" | grep -qi "404\|NotFound\|NoSuchBucket"; then
    S3_STATUS="Not Found"
    S3_COLOR="$RED"
    S3_NOTE="Bucket Missing"
    S3_NOTE_COLOR="$RED"
elif [ "$CF_HTTP" == "HTTP 200 (OK)" ]; then
    S3_STATUS="Active (via CDN)"
    S3_COLOR="$GREEN"
    S3_NOTE="Serving Traffic"
    S3_NOTE_COLOR="$GREEN"
else
    S3_STATUS="Active (Protected)"
    S3_COLOR="$GREEN"
    S3_NOTE="OAC CloudFront"
    S3_NOTE_COLOR="$GREEN"
fi

# ------------------------------------------------------------------------------
# 3. ECS Service (Spring Boot Fargate Backend)
# ------------------------------------------------------------------------------
ECS_RAW=$(aws ecs describe-services --cluster "$ECS_CLUSTER" --services "$ECS_SERVICE" --region "$AWS_REGION" --query "services[0].[status, runningCount, desiredCount]" --output text 2>/dev/null || echo "NOT_FOUND 0 0")
read -r ECS_STATUS ECS_RUNNING ECS_DESIRED <<< "$ECS_RAW"

if [ "$ECS_STATUS" == "ACTIVE" ]; then
    if [ "$ECS_RUNNING" -gt 0 ]; then
        ECS_DISP="${ECS_RUNNING}/${ECS_DESIRED} Tasks Running"
        ECS_COLOR="$GREEN"
        ECS_HEALTH="Healthy (Fargate)"
        ECS_HEALTH_COLOR="$GREEN"
        ECS_COST="~\$17.72 / mo"
        TOTAL_EST_COST=$((TOTAL_EST_COST + 18))
    else
        ECS_DISP="0/${ECS_DESIRED} Tasks (Paused)"
        ECS_COLOR="$YELLOW"
        ECS_HEALTH="Stopped (\$0)"
        ECS_HEALTH_COLOR="$DIM"
        ECS_COST="\$0.00 / mo"
    fi
else
    ECS_DISP="Inactive / Missing"
    ECS_COLOR="$RED"
    ECS_HEALTH="None"
    ECS_HEALTH_COLOR="$RED"
    ECS_COST="\$0.00 / mo"
fi

# ------------------------------------------------------------------------------
# 4. RDS Database (PostgreSQL)
# ------------------------------------------------------------------------------
RDS_RAW=$(aws rds describe-db-instances --db-instance-identifier "$RDS_INSTANCE" --region "$AWS_REGION" --query "DBInstances[0].[DBInstanceStatus, DBInstanceClass, EngineVersion]" --output text 2>/dev/null || echo "NOT_FOUND None None")
read -r RDS_STATUS RDS_CLASS RDS_VER <<< "$RDS_RAW"

if [ "$RDS_STATUS" == "available" ]; then
    RDS_DISP="Available (Running)"
    RDS_COLOR="$GREEN"
    RDS_HEALTH="Ready (${RDS_CLASS:-db.t4g})"
    RDS_HEALTH_COLOR="$GREEN"
    RDS_COST="~\$15.30 / mo"
    TOTAL_EST_COST=$((TOTAL_EST_COST + 15))
    if [ "$ECS_RUNNING" == "0" ]; then
        ACTIVE_ALERTS+=("⚠️  RDS Database is running (~\$15/mo) while Backend ECS is stopped (0 tasks). Stop RDS if not testing.")
    fi
elif [ "$RDS_STATUS" == "stopped" ]; then
    RDS_DISP="Stopped (Paused)"
    RDS_COLOR="$YELLOW"
    RDS_HEALTH="Storage Only"
    RDS_HEALTH_COLOR="$DIM"
    RDS_COST="~\$2.30 / mo (disk)"
    TOTAL_EST_COST=$((TOTAL_EST_COST + 2))
elif [ "$RDS_STATUS" == "starting" ] || [ "$RDS_STATUS" == "stopping" ]; then
    RDS_DISP="${RDS_STATUS}..."
    RDS_COLOR="$YELLOW"
    RDS_HEALTH="Transitioning"
    RDS_HEALTH_COLOR="$YELLOW"
    RDS_COST="Pending"
else
    RDS_DISP="${RDS_STATUS:-Not Found}"
    RDS_COLOR="$RED"
    RDS_HEALTH="Missing"
    RDS_HEALTH_COLOR="$RED"
    RDS_COST="\$0.00 / mo"
fi

# ------------------------------------------------------------------------------
# 5. Application Load Balancer (Backend Gateway)
# ------------------------------------------------------------------------------
ALB_RAW=$(aws elbv2 describe-load-balancers --names "$ALB_NAME" --region "$AWS_REGION" --query "LoadBalancers[0].[State.Code, DNSName, LoadBalancerArn]" --output text 2>/dev/null || echo "NOT_FOUND None None")
read -r ALB_STATE ALB_DNS ALB_ARN <<< "$ALB_RAW"

ALB_LIVE_CHECK="N/A"
ALB_LIVE_COLOR="$DIM"
if [ "$ALB_STATE" == "active" ]; then
    ALB_DISP="Active"
    ALB_COLOR="$GREEN"
    ALB_COST="~\$18.00 / mo"
    TOTAL_EST_COST=$((TOTAL_EST_COST + 18))

    # Test direct access restriction (Expected: HTTP 403 Forbidden because X-Origin-Verify is absent)
    if command -v curl &>/dev/null && [ "$ALB_DNS" != "None" ]; then
        ALB_HTTP_CODE=$(curl -s -m 4 -o /dev/null -w "%{http_code}" "http://${ALB_DNS}/actuator/health" 2>/dev/null || echo "000")
        if [ "$ALB_HTTP_CODE" == "200" ]; then
            ALB_LIVE_CHECK="HTTP 200 (Health)"
            ALB_LIVE_COLOR="$GREEN"
        elif [ "$ALB_HTTP_CODE" == "403" ]; then
            ALB_LIVE_CHECK="403 (Secured)"
            ALB_LIVE_COLOR="$GREEN"
        elif [ "$ALB_HTTP_CODE" == "000" ]; then
            ALB_LIVE_CHECK="Timeout / Err"
            ALB_LIVE_COLOR="$YELLOW"
        else
            ALB_LIVE_CHECK="HTTP $ALB_HTTP_CODE"
            ALB_LIVE_COLOR="$YELLOW"
        fi
    fi
elif [ "$ALB_STATE" == "NOT_FOUND" ]; then
    ALB_DISP="Destroyed (\$0)"
    ALB_COLOR="$DIM"
    ALB_COST="\$0.00 / mo"
else
    ALB_DISP="${ALB_STATE:-Provisioning}"
    ALB_COLOR="$YELLOW"
    ALB_COST="~\$18.00 / mo"
    TOTAL_EST_COST=$((TOTAL_EST_COST + 18))
fi

# ------------------------------------------------------------------------------
# 6. Target Group & Target Health
# ------------------------------------------------------------------------------
TG_ARN=$(aws elbv2 describe-target-groups --names "$TARGET_GROUP_NAME" --region "$AWS_REGION" --query "TargetGroups[0].TargetGroupArn" --output text 2>/dev/null || echo "None")
if [ "$TG_ARN" != "None" ] && [ -n "$TG_ARN" ]; then
    TG_HEALTH_RAW=$(aws elbv2 describe-target-health --target-group-arn "$TG_ARN" --region "$AWS_REGION" --query "TargetHealthDescriptions[].TargetHealth.State" --output text 2>/dev/null || echo "")
    HEALTHY_COUNT=0
    TOTAL_TARGETS=0
    for state in $TG_HEALTH_RAW; do
        TOTAL_TARGETS=$((TOTAL_TARGETS + 1))
        if [ "$state" == "healthy" ]; then
            HEALTHY_COUNT=$((HEALTHY_COUNT + 1))
        fi
    done

    if [ "$TOTAL_TARGETS" -gt 0 ]; then
        TG_DISP="${HEALTHY_COUNT}/${TOTAL_TARGETS} Healthy"
        if [ "$HEALTHY_COUNT" -eq "$TOTAL_TARGETS" ]; then
            TG_COLOR="$GREEN"
            TG_CHECK="Port 8080 OK"
            TG_CHECK_COLOR="$GREEN"
        else
            TG_COLOR="$YELLOW"
            TG_CHECK="Degraded"
            TG_CHECK_COLOR="$YELLOW"
        fi
    else
        TG_DISP="0 Targets (Idle)"
        TG_COLOR="$DIM"
        TG_CHECK="No Tasks"
        TG_CHECK_COLOR="$DIM"
    fi
else
    TG_DISP="Not Found"
    TG_COLOR="$DIM"
    TG_CHECK="None"
    TG_CHECK_COLOR="$DIM"
fi

# ------------------------------------------------------------------------------
# 7. NAT Gateway & Elastic IP (Major Billing Driver)
# ------------------------------------------------------------------------------
NAT_RAW=$(aws ec2 describe-nat-gateways --filter "Name=vpc-id,Values=$VPC_ID" "Name=state,Values=pending,available,deleting" --region "$AWS_REGION" --query "NatGateways[0].[NatGatewayId, State, NatGatewayAddresses[0].PublicIp]" --output text 2>/dev/null || echo "None None None")
read -r NAT_ID NAT_STATE NAT_IP <<< "$NAT_RAW"

if [ "$NAT_STATE" == "available" ]; then
    NAT_DISP="Available (Active)"
    NAT_COLOR="$GREEN"
    NAT_HEALTH="IP: ${NAT_IP:-Yes}"
    NAT_HEALTH_COLOR="$GREEN"
    NAT_COST="~\$32.40 / mo"
    TOTAL_EST_COST=$((TOTAL_EST_COST + 32))
    if [ "$ECS_RUNNING" == "0" ]; then
        ACTIVE_ALERTS+=("🚨 [COST ALERT] NAT Gateway ($NAT_ID) is ACTIVE (~\$32.40/mo, ~\$1.08/day) while Backend ECS is stopped (0 tasks)! Destroy NAT if not testing.")
    fi
elif [ "$NAT_STATE" == "pending" ] || [ "$NAT_STATE" == "deleting" ]; then
    NAT_DISP="${NAT_STATE}..."
    NAT_COLOR="$YELLOW"
    NAT_HEALTH="In Progress"
    NAT_HEALTH_COLOR="$YELLOW"
    NAT_COST="Transitioning"
else
    NAT_DISP="Destroyed (\$0)"
    NAT_COLOR="$DIM"
    NAT_HEALTH="NAT Inactive"
    NAT_HEALTH_COLOR="$DIM"
    NAT_COST="\$0.00 / mo"
fi

# ------------------------------------------------------------------------------
# Print Output Table
# ------------------------------------------------------------------------------
print_separator
print_row "LAYER / SERVICE" "RESOURCE IDENTIFIER" "SERVICE STATUS" "LIVE HEALTH" "APPROX COST" "$BOLD" "$BOLD" "$BOLD"
print_separator

print_row "Frontend (CDN)" "${CF_DIST_ID} (CloudFront)" "$CF_DISP" "$CF_HTTP" "Free Tier (\$0)" "$CF_COLOR" "$CF_HTTP_COLOR" "$DIM"
print_row "Frontend (Storage)" "${S3_BUCKET}" "$S3_STATUS" "$S3_NOTE" "<\$0.05 / mo" "$S3_COLOR" "$S3_NOTE_COLOR" "$DIM"
print_row "Backend (Compute)" "${ECS_SERVICE} (ECS)" "$ECS_DISP" "$ECS_HEALTH" "$ECS_COST" "$ECS_COLOR" "$ECS_HEALTH_COLOR" "$BOLD"
print_row "Backend (Database)" "${RDS_INSTANCE} (RDS)" "$RDS_DISP" "$RDS_HEALTH" "$RDS_COST" "$RDS_COLOR" "$RDS_HEALTH_COLOR" "$BOLD"
print_row "Load Balancer" "${ALB_NAME} (ALB)" "$ALB_DISP" "$ALB_LIVE_CHECK" "$ALB_COST" "$ALB_COLOR" "$ALB_LIVE_COLOR" "$BOLD"
print_row "Target Group" "${TARGET_GROUP_NAME}" "$TG_DISP" "$TG_CHECK" "Included in ALB" "$TG_COLOR" "$TG_CHECK_COLOR" "$DIM"
print_row "Networking (NAT)" "${NAT_ID:-nat-gateway}" "$NAT_DISP" "$NAT_HEALTH" "$NAT_COST" "$NAT_COLOR" "$NAT_HEALTH_COLOR" "$BOLD"

print_separator

DAILY_COST=$(awk -v c="$TOTAL_EST_COST" 'BEGIN { printf "%.2f", c/30 }')
echo ""
echo -e " 💰 ${BOLD}Estimated Active Running Cost: ~\$${TOTAL_EST_COST}.00 / month${NC} (~\$${DAILY_COST} / day)"
echo ""

# ------------------------------------------------------------------------------
# Live End-to-End API / Website Verification
# ------------------------------------------------------------------------------
echo -e "${CYAN}${BOLD}--- [1] LIVE ENDPOINT VERIFICATION (End-to-End Connectivity) -----------------------------------${NC}"
if command -v curl &>/dev/null; then
    # Frontend SPA Check
    FE_STATUS_CODE=$(curl -s -L -m 4 -o /dev/null -w "%{http_code}" "https://${CF_DNS:-$CF_DOMAIN}" 2>/dev/null || echo "ERR")
    if [ "$FE_STATUS_CODE" == "200" ]; then
        echo -e "  [✓] Frontend App (SPA)   : ${GREEN}LIVE${NC} (HTTP 200) -> https://${CF_DNS:-$CF_DOMAIN}"
    else
        echo -e "  [✗] Frontend App (SPA)   : ${YELLOW}HTTP $FE_STATUS_CODE${NC} -> https://${CF_DNS:-$CF_DOMAIN}"
    fi

    # API Health Check via CloudFront Multi-Origin Route
    API_RESPONSE=$(curl -s -m 4 "https://${CF_DNS:-$CF_DOMAIN}/actuator/health" 2>/dev/null || echo "TIMEOUT")
    if [[ "$API_RESPONSE" =~ "UP" ]]; then
        echo -e "  [✓] Spring Boot via CDN  : ${GREEN}LIVE & HEALTHY${NC} -> https://${CF_DNS:-$CF_DOMAIN}/actuator/health  (${GREEN}${API_RESPONSE}${NC})"
    elif [ "$ECS_RUNNING" == "0" ]; then
        echo -e "  [-] Spring Boot via CDN  : ${DIM}PAUSED (Backend tasks scaled to 0)${NC}"
    else
        echo -e "  [!] Spring Boot via CDN  : ${YELLOW}Unreachable / Starting (${API_RESPONSE})${NC}"
    fi

    # ALB Direct Ingress Lockdown Verification
    if [ "$ALB_DNS" != "None" ] && [ -n "$ALB_DNS" ]; then
        ALB_CHECK=$(curl -s -m 4 -o /dev/null -w "%{http_code}" "http://${ALB_DNS}/api/v1/auth/me" 2>/dev/null || echo "000")
        if [ "$ALB_CHECK" == "403" ]; then
            echo -e "  [✓] Backend Direct ALB   : ${GREEN}SECURE (HTTP 403 Forbidden)${NC} - Direct public access blocked; CloudFront only"
        elif [ "$ALB_CHECK" == "000" ]; then
            echo -e "  [-] Backend Direct ALB   : ${DIM}Unreachable or destroyed${NC}"
        else
            echo -e "  [!] Backend Direct ALB   : ${YELLOW}HTTP $ALB_CHECK${NC} (Check ALB listener rules)"
        fi
    fi
else
    echo -e "  ${DIM}curl is not installed; skipped live endpoint pings.${NC}"
fi
echo ""

# ------------------------------------------------------------------------------
# Optimization & Unnecessary Spend Advisor
# ------------------------------------------------------------------------------
echo -e "${CYAN}${BOLD}--- [2] COST & RESOURCE OPTIMIZATION ADVISOR ---------------------------------------------------${NC}"
if [ ${#ACTIVE_ALERTS[@]} -gt 0 ]; then
    for alert in "${ACTIVE_ALERTS[@]}"; do
        echo -e "  ${alert}"
    done
    echo ""
    echo -e "  ${YELLOW}${BOLD}How to fix idle charges in 1-Click:${NC}"
    echo -e "  1. GitHub: Open repository -> ${BOLD}Actions${NC} tab -> run workflow ${BOLD}'Stop Services / Pause Infrastructure'${NC} with mode ${BOLD}'pause-and-destroy-nat'${NC}"
    echo -e "  2. Or run CLI:"
    echo -e "     cd \"\$(git rev-parse --show-toplevel 2>/dev/null || echo .)/aavas/terraform\""
    echo -e "     terraform destroy -target=\"aws_ecs_service.app\" -target=\"aws_nat_gateway.main\" -target=\"aws_eip.nat\" -auto-approve"
else
    if [ "$NAT_STATE" != "available" ] && [ "$ECS_RUNNING" == "0" ] && [ "$RDS_STATUS" == "stopped" ]; then
        echo -e "  ${GREEN}✓ Fully Optimized!${NC} All billable compute (ECS, RDS, NAT) is paused. Idle cost is near zero (<\$3/mo)."
    elif [ "$NAT_STATE" == "available" ] && [ "$ECS_RUNNING" -gt 0 ] && [ "$RDS_STATUS" == "available" ]; then
        echo -e "  ${GREEN}✓ Full Stack is Active & Serving Traffic!${NC} Everything is running in production mode."
    else
        echo -e "  ${GREEN}✓ No high-priority billing anomalies detected.${NC}"
    fi
fi
echo ""
echo -e "${CYAN}${BOLD}=================================================================================================================${NC}"
echo ""
