# Operations & Diagnostics Quick Reference

This document provides verified, copy-paste instructions for accessing the private live database, activating users/hostels, and monitoring real-time logs via both AWS CLI and the AWS Management Console (GUI).

---

## 0. Instant Service Status & Cost Audit (Tabular Dashboard)

To view a real-time status table of all frontend and backend AWS services, test live endpoints, and detect any idle cost drivers (like an unneeded NAT Gateway):

```bash
cd "d:\resource\Projects\Hostel Management\aavas\terraform"
chmod +x check-service-status.sh
./check-service-status.sh
```

---

## 1. How to Check & Query the Live Database (PostgreSQL RDS)

Because the RDS instance is placed in private subnets with no public internet ingress, direct connections from outside the VPC are blocked for security. Use one of the verified methods below:

### Method A: Run Live SQL Queries in 1 Command (Recommended)

A helper script [`run-db-query.ps1`](file:///d:/resource/Projects/Hostel%20Management/aavas/terraform/run-db-query.ps1) is available in this folder. It runs a secure, one-off Fargate task inside your VPC using the registered task definition `atithi-dev-db-client:1`, executes your query, prints query results directly to your terminal, and terminates immediately.

> [!NOTE]
> This script does **NOT** leave any long-running resources behind. The container stops within seconds after the query finishes, and Fargate compute is released immediately.

```powershell
# Navigate to terraform directory
cd "d:\resource\Projects\Hostel Management\aavas\terraform"

# Windows (PowerShell):
powershell -ExecutionPolicy Bypass -File .\run-db-query.ps1 "\dt"
powershell -ExecutionPolicy Bypass -File .\run-db-query.ps1 "SELECT count(*) FROM users;"
powershell -ExecutionPolicy Bypass -File .\run-db-query.ps1 "SELECT id, email, role, status FROM users LIMIT 10;"

# Linux / Termux (Bash):
chmod +x run-db-query.sh
./run-db-query.sh "\dt"
./run-db-query.sh "SELECT count(*) FROM users;"
./run-db-query.sh "SELECT id, email, role, status FROM users LIMIT 10;"
```

---

### Quick Database Operations (User & Hostel Activation)

When new users register, you can activate their accounts and hostel entries with a single command:

#### Linux / Termux (1-Command Script):
```bash
chmod +x activate-user.sh

# Pass the email directly as an argument:
./activate-user.sh amrutha@atithi.com

# Or run interactively (it will prompt for the email):
./activate-user.sh
```

#### Windows (PowerShell):
```powershell
# 1. Activate User:
powershell -ExecutionPolicy Bypass -File .\run-db-query.ps1 "UPDATE users SET status = 'ACTIVE' WHERE email = 'amrutha@atithi.com';"

# 2. Activate Hostel:
powershell -ExecutionPolicy Bypass -File .\run-db-query.ps1 "UPDATE hostels SET status = 'ACTIVE' WHERE id = (SELECT hostel_id FROM users WHERE email = 'amrutha@atithi.com');"

# 3. Verify User Status:
powershell -ExecutionPolicy Bypass -File .\run-db-query.ps1 "SELECT email, role, status FROM users WHERE email = 'amrutha@atithi.com';"
```

---

### Method B: Native AWS CLI Command (Without Script)

You can run any SQL statement directly using standard AWS CLI:

```bash
# 1. Start a one-off query task in the private VPC subnet:
aws ecs run-task \
  --cluster "atithi-dev-cluster" \
  --task-definition "atithi-dev-db-client:1" \
  --launch-type "FARGATE" \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-079397bbbde4c4216],securityGroups=[sg-00043f8ddcad6687e],assignPublicIp=DISABLED}" \
  --overrides '{"containerOverrides":[{"name":"db-client","command":["psql","-c","SELECT table_name FROM information_schema.tables WHERE table_schema='\''public'\'';"]}]}' \
  --region "ap-south-1"

# 2. View the query output from CloudWatch (replace <TASK_ID> with the task ID from the output above):
aws logs get-log-events \
  --log-group-name "/ecs/atithi-dev" \
  --log-stream-name "db-client/db-client/<TASK_ID>" \
  --region "ap-south-1" \
  --query "events[].message" \
  --output text
```

---

### Method C: Inspect Live DB Credentials (Secrets Manager)

To check the active database credentials stored in AWS Secrets Manager:

```bash
aws secretsmanager get-secret-value \
  --secret-id "atithi-dev-db-credentials" \
  --region "ap-south-1" \
  --query "SecretString" \
  --output text
```

---

### Method D: AWS GUI (Console Navigation)

1. Open the [AWS RDS Console](https://ap-south-1.console.aws.amazon.com/rds/home?region=ap-south-1#databases:).
2. Click on **Databases** → select `atithi-dev-postgres`.
3. View **Connectivity & security** (Endpoint: `atithi-dev-postgres.cba6qwgmytsm.ap-south-1.rds.amazonaws.com`, Port: `5432`, VPC: `vpc-058c5dd8e8c8dbfdb`).
4. View **Configuration** (Engine: PostgreSQL 16.13, DB name: `atithi_aavas`).
5. Open the [AWS Secrets Manager Console](https://ap-south-1.console.aws.amazon.com/secretsmanager/listsecrets?region=ap-south-1).
6. Click on `atithi-dev-db-credentials` → scroll to **Secret value** → click **Retrieve secret value** to see username and password.

---

## 2. How to Monitor Live Logs (ECS / Spring Boot)

All application logs, HikariPool connections, SQL queries, and HTTP requests are streamed into AWS CloudWatch Log Group `/ecs/atithi-dev`.

### Method A: AWS CLI (Real-Time Live Tail)

Follow logs live as incoming requests arrive (like `tail -f`):

```bash
# Live stream all incoming logs in real time:
aws logs tail /ecs/atithi-dev --follow --region ap-south-1

# View logs from the last 15 minutes:
aws logs tail /ecs/atithi-dev --since 15m --region ap-south-1

# Filter specifically for errors or warnings:
aws logs tail /ecs/atithi-dev --filter-pattern "ERROR" --region ap-south-1

# Filter for HTTP requests handled by Spring MVC:
aws logs tail /ecs/atithi-dev --filter-pattern "Completed 200" --region ap-south-1
```

---

### Method B: AWS GUI (Console Navigation)

1. Open the [AWS CloudWatch Console](https://ap-south-1.console.aws.amazon.com/cloudwatch/home?region=ap-south-1).
2. In the left navigation menu, expand **Logs** and click **Log groups**.
3. In the list, click **`/ecs/atithi-dev`**.
4. **For Live Real-Time Streaming**:
   - In the top right, click the **Live tail** button (or navigate to **Live tail** in the left menu).
   - Click **Start tail**. Every log entry will appear in real time as requests hit the backend.
5. **For Historic Searching & Filtering**:
   - On the `/ecs/atithi-dev` page, click **Search all log streams**.
   - In the search bar at the top, enter a search term (e.g., `ERROR`, `HikariPool`, `DispatcherServlet`, or `RentChargeServiceImpl`).
   - Select the time range (e.g., *1 hour*, *Today*) to inspect matching log lines across all containers.
6. **From the ECS Console Directly**:
   - Open [AWS ECS Clusters Console](https://ap-south-1.console.aws.amazon.com/ecs/v2/clusters?region=ap-south-1).
   - Click `atithi-dev-cluster` → **Services** → `atithi-dev-service`.
   - Click on the **Logs** tab to view the container log stream directly within the ECS dashboard.
