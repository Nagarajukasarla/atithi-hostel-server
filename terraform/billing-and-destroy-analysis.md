# AWS Infrastructure Inventory, Billing & Destroy Analysis

This document provides a complete audit of all deployed AWS cloud resources, their monthly costs, teardown risks, and copy-paste commands to pause, resume, or destroy services to optimize spending.

---

## 1. Complete Resource & Billing Audit Table

Every resource discovered in AWS account `551626543871` (region `ap-south-1`) categorized by origin, status, and cost:

| Service / Resource | Identifier / Name | Origin | Status | Billed? | Approx. Monthly Cost |
|---|---|---|---|---|---|
| **NAT Gateway** | `nat-038eadd8a474e5194` | Terraform (`networking.tf`) | Active | **YES** | **~$32.40 / mo** ($0.045/hr) |
| **Application Load Balancer** | `atithi-dev-alb` | Terraform (`alb.tf`) | Active | **YES** | **~$16.20 - $18.00 / mo** ($0.0225/hr + LCU) |
| **RDS PostgreSQL Instance** | `atithi-dev-postgres` (`db.t4g.micro`, 20 GB) | Terraform (`rds.tf`) | Active | **YES** (Free Tier eligible) | **~$15.30 / mo** ($0 for 1st yr if in Free Tier, else ~$0.018/hr + $2.30 storage) |
| **ECS Fargate Running Task** | `atithi-server` (0.5 vCPU, 1 GB) | Terraform (`ecs.tf`) | Active | **YES** | **~$17.72 / mo** ($0.0246/hr) |
| **Public IPv4 Addresses** | NAT EIP (`16.4.137.176`) + ALB IPs | Terraform (`networking.tf`, `alb.tf`) | Active | **YES** | **~$3.60 / mo** per IPv4 ($0.005/hr) |
| **Amazon S3 (Frontend Bucket)** | `atithi-frontend-551626543871` | Terraform (`atithi/terraform`) | Active | Minimal | **<$0.10 / mo** ($0.023/GB-mo) |
| **Amazon CloudFront CDN** | `d2dk2x85a04vx5.cloudfront.net` | Terraform (`atithi/terraform`) | Active | Free Tier | **$0.00 / mo** (1 TB free transfer/mo) |
| **Active ECR Repository** | `atithi-server` (~220 MB image) | Terraform (`ecr.tf`) | Active | Minimal | **~$0.03 / mo** ($0.10/GB-mo) |
| **Secrets Manager Secret** | `atithi-dev-db-credentials` | Terraform (`secrets.tf`) | Active | **YES** | **~$0.40 / mo** ($0.40 per secret) |
| **CloudWatch Log Group** | `/ecs/atithi-dev` | Terraform (`cloudwatch.tf`) | Active | Minimal | **<$0.05 / mo** (first 5 GB ingestion free) |
| **VPC & Subnets** | `vpc-058c5dd8e8c8dbfdb` (2 Pub, 2 App, 2 DB) | Terraform (`networking.tf`) | Active | Free | $0.00 |
| **Internet Gateway** | `igw-0a98c5b526a173bb2` | Terraform (`networking.tf`) | Active | Free | $0.00 |
| **Security Groups (3)** | `alb`, `ecs`, `rds` | Terraform (`security.tf`) | Active | Free | $0.00 |
| **IAM Roles & Policies** | Task Role & Execution Role | Terraform (`iam.tf`) | Active | Free | $0.00 |
| **ECS Cluster & Task Def** | `atithi-dev-cluster`, `atithi-dev-app` | Terraform (`ecs.tf`) | Active | Free | $0.00 |

> **Active Cost Summary**:
> - Running 24/7 total cost: **~$70 - $85 / month**.
> - **Top 3 billing drivers**:
>   1. **NAT Gateway**: ~$32.40/month
>   2. **ALB**: ~$18.00/month
>   3. **RDS Compute**: ~$15.30/month (if outside Free Tier)

---

## 2. What Happens If `terraform destroy` Is Run?

Running a simulated `terraform plan -destroy` reveals **all active backend resources scheduled for deletion**:

### What will be destroyed:
1. **ECS Service & Tasks**: `aws_ecs_service.app` will be stopped and deleted (container terminates).
2. **ECS Cluster**: `aws_ecs_cluster.main` deleted.
3. **Task Definition**: `aws_ecs_task_definition.app` deregistered.
4. **Load Balancer**: `aws_lb.main`, `aws_lb_listener.http`, `aws_lb_target_group.app` deleted.
5. **NAT Gateway & Elastic IP**: `aws_nat_gateway.main` and `aws_eip.nat` released.
6. **Networking**: All 6 subnets, 3 route tables, route table associations, and `aws_internet_gateway.main` deleted.
7. **Security Groups**: `alb`, `ecs`, `rds` and all rules deleted.
8. **VPC**: `aws_vpc.main` deleted.
9. **Secrets Manager**: `aws_secretsmanager_secret.db_credentials` scheduled for deletion.
10. **CloudWatch Log Group**: `/ecs/atithi-dev` deleted.
11. **IAM Roles**: `atithi-dev-ecs-task-role` and `atithi-dev-ecs-task-execution-role` deleted.

### Critical Blockers & Safeguards:
- ❌ **ECR Repository is Protected**: `aws_ecr_repository.atithi_server` has `lifecycle { prevent_destroy = true }`. Running a full `terraform destroy` will halt with an error to prevent accidental deletion of your Docker images.
- ⚠️ **RDS Database Data Risk**: In `rds.tf`, `skip_final_snapshot = true` and `deletion_protection = false`. If a full destroy forces deletion of the database, **all database tables and records would be wiped permanently without a backup!**

---

## 3. The 2-Command "Stop" and "Start" Solutions

Use one of the two strategies below depending on how much cost you want to cut:

---

### Solution 1: Zero-Risk Instant Pause & Resume (Recommended)

This approach drops your bill by **~95%** (stops ECS and RDS compute charges), **keeps 100% of your database data and ECR images safe**, and restarts in seconds with **zero configuration changes**:

#### To STOP (When not in use):
Copy and paste into your terminal:
```bash
# 1. Stop ECS task (terminates Fargate container):
aws ecs update-service --cluster atithi-dev-cluster --service atithi-dev-service --desired-count 0 --region ap-south-1

# 2. Stop RDS database (stops PostgreSQL compute charges):
aws rds stop-db-instance --db-instance-identifier atithi-dev-postgres --region ap-south-1
```

#### To START (When resuming work):
Copy and paste into your terminal:
```bash
# 1. Start RDS database:
aws rds start-db-instance --db-instance-identifier atithi-dev-postgres --region ap-south-1

# 2. Start ECS task (boots Spring Boot with existing DB and ECR image):
aws ecs update-service --cluster atithi-dev-cluster --service atithi-dev-service --desired-count 1 --region ap-south-1
```

---

### Solution 2: Destroy Costly Networking (ALB + NAT) with Terraform

If you will be away for an extended period and want to eliminate the NAT Gateway ($32/mo) and ALB ($18/mo) completely while keeping the Database, VPC, and ECR images 100% intact:

#### To STOP:
```bash
# Navigate to terraform directory:
cd "d:\resource\Projects\Hostel Management\aavas\terraform"

# Destroy NAT Gateway, ALB, and ECS service (saves ~$60/mo):
terraform destroy -target="aws_ecs_service.app" -target="aws_lb.main" -target="aws_nat_gateway.main" -target="aws_eip.nat" -auto-approve
```

#### To START:
```bash
# Recreate NAT, ALB, and restart ECS service:
terraform apply -auto-approve
```

> [!TIP]
> **Auto-Healing in CI/CD**: Even if you destroyed NAT/ALB using Solution 2, whenever you push new backend code to GitHub, your GitHub Actions CD pipeline (`backend-cd.yml`) automatically detects the missing NAT/ALB and **recreates them automatically** via `terraform apply -auto-approve` before deploying the Docker image!

---

## 4. 1-Click Automated GitHub Actions (No Terminal Needed!)

Instead of opening a local terminal and running commands manually, you can trigger these workflows directly in your browser from the **GitHub Actions** tab in your repository:

### 🛑 To Stop / Pause Services:
1. Go to your repository on GitHub: [`atithi-hostel-server`](https://github.com/Nagarajukasarla/atithi-hostel-server) → click **Actions** tab.
2. In the left workflow list, click **`Stop Services / Pause Infrastructure`**.
3. Click the **Run workflow** dropdown button.
4. Select your **Stop Mode**:
   - **`pause-and-destroy-nat`** *(Recommended)*: Scales ECS tasks to 0, stops RDS database, and destroys the NAT Gateway to eliminate ~$50/month idle cost while keeping ALB DNS intact!
   - **`pause-compute-only`**: Scales ECS tasks to 0 and stops RDS database (takes only 30s, zero Terraform changes).
   - **`destroy-nat-and-alb`**: Scales ECS tasks to 0, stops RDS database, and destroys NAT Gateway, ALB, and ECS service.
5. Click **Run workflow**.

---

### ▶️ To Start / Resume Services:
1. Go to [`atithi-hostel-server`](https://github.com/Nagarajukasarla/atithi-hostel-server) → click **Actions** tab.
2. In the left workflow list, click **`Start Services / Resume Infrastructure`**.
3. Click **Run workflow** → click the green **Run workflow** button.
4. The workflow will:
   - Check if RDS is stopped and automatically start it.
   - Run Terraform to recreate any missing NAT Gateway, routes, or ALB.
   - Scale the ECS service back to 1 task and wait for it to stabilize.
   - Verify health check (`HTTP 200`) and report live status!

