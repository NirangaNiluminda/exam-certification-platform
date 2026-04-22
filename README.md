# Online Exam & Certification Platform
### EC7205 Cloud Computing — University of Ruhuna | Assignment 2 | Deadline: 25 April 2026

A cloud-native microservices platform where students enroll in certification exams, take timed exams, receive instant AI-style grading, and get auto-generated PDF certificates.

---

## Architecture

```
Client → API Gateway (port 8080 / 20080 in Docker)
           ├── auth-service         — JWT RS256 registration & login
           ├── enrollment-service   — SAGA Orchestrator (drives the full flow)
           ├── payment-service      — SAGA Participant (auto-approves < $1000)
           ├── exam-service         — SAGA Participant (creates exam slot)
           ├── grading-service      — SAGA Participant (grades submission)
           ├── certificate-service  — SAGA Participant (generates PDF cert)
           └── notification-service — Async email stub (Kafka consumer only)

Infrastructure (all Docker containers):
  PostgreSQL × 6     — one DB per service (Database-per-Service pattern)
  Kafka × 3 brokers  — event streaming backbone
  Debezium           — CDC: reads PostgreSQL WAL → publishes to Kafka
  MinIO              — S3-compatible object store for certificate PDFs
  Kafka UI           — visual event inspector (port 8088)
  SonarQube          — static code analysis (port 9090)
  Jenkins            — CD pipeline (port 8089)
```

**Key Patterns:** SAGA Orchestration · Transactional Outbox · CDC via Debezium · Database-per-Service · Event-Driven Architecture · JWT RS256 Auth

---

## Enrollment SAGA Flow

```
POST /enrollments
      │
      ▼
  PENDING ──outbox──▶ payment.inbox.events
                              │
                    PaymentService processes
                              │
                    ◀── payment.outbox.events ──
                              │
                     PAYMENT_COMPLETED
                              │
                    ──outbox──▶ exam.inbox.events
                              │
                    ExamService creates slot
                              │
                    ◀── enrollment.inbox.events ──
                              │
                     EXAM_SCHEDULED
                              │
                    ──outbox──▶ grading queue
                              │
                    Student submits answers
                              │
                     GRADED (score + pass/fail)
                              │
                    ──outbox──▶ certificate queue
                              │
                    CertificateService generates PDF → MinIO
                              │
                     CERTIFIED ✅
```

**Compensation path** (price > $1000): PENDING → PAYMENT_FAILED → CANCELLED → notification sent.

---

## Prerequisites

- Docker 24+ and Docker Compose v2
- Java 21 + Maven 3.9+ (for local builds only — CI handles this)
- OpenSSL (for JWT key generation)

---

## Quick Start

### Step 1 — Generate JWT keys
```bash
chmod +x scripts/generate-jwt-keys.sh
./scripts/generate-jwt-keys.sh
# Creates: keys/private_key.pem and keys/public_key.pem
```

### Step 2 — Start all services
```bash
docker compose up --build -d
```
> First startup takes ~3–5 minutes. Kafka brokers, Postgres instances, and Debezium all need to initialize.

### Step 3 — Register Debezium CDC connectors
```bash
chmod +x register-connectors.sh
./register-connectors.sh
```
This registers one Debezium connector per service so PostgreSQL WAL changes flow into Kafka automatically.

### Step 4 — Seed exam and answer-key data
```bash
# Exam questions (exam-service DB)
docker exec -i postgres-exam psql -U exam_user -d exam_db < scripts/seed-data.sql

# Answer keys (grading-service DB)
docker exec -i postgres-grading psql -U grading_user -d grading_db < scripts/seed-data.sql
```

### Step 5 — Verify health
```bash
curl http://localhost:8080/actuator/health   # API Gateway
curl http://localhost:8081/actuator/health   # Auth
curl http://localhost:8082/actuator/health   # Enrollment
curl http://localhost:8084/actuator/health   # Exam
```

---

## Service URLs

| Service | Host Port | Purpose |
|---|---|---|
| API Gateway | http://localhost:8080 | Single entry point for all API calls |
| Auth Service | http://localhost:8081 | Register / login |
| Enrollment Service | http://localhost:8082 | Enroll, check SAGA status |
| Payment Service | http://localhost:8083 | Auto-processes payments |
| Exam Service | http://localhost:8084 | Exam slots, submit answers |
| Grading Service | http://localhost:8085 | Grades submissions |
| Certificate Service | http://localhost:8086 | Generates PDF certs |
| Notification Service | http://localhost:8087 | Email stub |
| Kafka UI | http://localhost:8088 | View Kafka topics & events |
| Jenkins CD | http://localhost:8089 | CD pipeline dashboard |
| MinIO Console | http://localhost:9001 | View certificate PDFs (user: minioadmin / minioadmin) |
| SonarQube | http://localhost:9090 | Code quality dashboard |

> **Note:** Docker Compose maps internal ports to 200xx externally (e.g., api-gateway 8080 → host 20080). The 80xx ports above work when services are run outside Docker, or via gateway port-forwarding.

---

## Sample API Walkthrough

### 1. Register a student
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"student@test.com","password":"Test@1234","fullName":"John Smith"}'
```

### 2. Login and capture token
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"student@test.com","password":"Test@1234"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
echo $TOKEN
```

### 3. Browse available exams
```bash
curl http://localhost:8080/api/v1/exams \
  -H "Authorization: Bearer $TOKEN"
```

### 4. Enroll (triggers the SAGA)
```bash
ENROLLMENT=$(curl -s -X POST http://localhost:8080/api/v1/enrollments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"examId":"a1000000-0000-0000-0000-000000000001","examPrice":49.99}')
echo $ENROLLMENT
ENROLLMENT_ID=$(echo $ENROLLMENT | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
```
Returns immediately with `status: PENDING`. SAGA runs asynchronously.

### 5. Poll enrollment status
```bash
curl http://localhost:8080/api/v1/enrollments/$ENROLLMENT_ID \
  -H "Authorization: Bearer $TOKEN"
# Watch status progress: PENDING → PAYMENT_COMPLETED → EXAM_SCHEDULED
```

### 6. Get exam slot and submit answers
```bash
# Get slot ID from enrollment response (field: examSlotId)
SLOT_ID=<slotId from enrollment>

curl -X POST http://localhost:8080/api/v1/exams/slots/$SLOT_ID/submit \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "answers": [
      {"questionId":"b1010000-0000-0000-0000-000000000001","selectedOption":"B"},
      {"questionId":"b1020000-0000-0000-0000-000000000001","selectedOption":"B"}
    ]
  }'
```

### 7. Check final status and download certificate
```bash
# Poll until status = CERTIFIED
curl http://localhost:8080/api/v1/enrollments/$ENROLLMENT_ID \
  -H "Authorization: Bearer $TOKEN"

# Download certificate PDF
CERT_ID=<certificateId from enrollment>
curl http://localhost:8080/api/v1/certificates/$CERT_ID \
  -H "Authorization: Bearer $TOKEN" --output cert.pdf

# Public verify (no auth needed)
curl http://localhost:8080/api/v1/certificates/verify/<verifyCode>
```

### Failure path (payment rejection)
```bash
# Price > 1000 triggers PAYMENT_FAILED → CANCELLED
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"examId":"a1000000-0000-0000-0000-000000000001","examPrice":1500.00}'
# SAGA: PENDING → PAYMENT_FAILED → CANCELLED
```

---

## CI/CD Pipeline

### Overview

```
Push to main
    │
    ▼
GitHub Actions CI (.github/workflows/ci.yml)
    ├── Stage 2: Build         (ubuntu-latest)  — mvn clean package
    ├── Stage 3: SonarQube     (self-hosted)     — mvn sonar:sonar → localhost:9090
    ├── Stage 4: Test          (ubuntu-latest)   — unit + integration + OWASP
    └── Stage 5: Push to GHCR (ubuntu-latest)   — docker build + push :SHA + :latest
                                                   then triggers Jenkins via webhook
Jenkins CD (Jenkinsfile)
    ├── Stage 6:  Pull images from GHCR
    ├── Stage 7:  Deploy to Staging (verify container health)
    ├── Stage 8:  Smoke Tests (curl /actuator/health)
    ├── Stage 9:  Manual Approval Gate (niranga must approve)
    ├── Stage 10: Tag as Production (push :production to GHCR)
    └── Stage 11: Post-Deploy Health Check
```

### GitHub Actions Secrets required

| Secret | Value |
|---|---|
| `SONAR_TOKEN` | `sqa_6dbf2b9e38615f738900fdb686b951d429b6c83e` |
| `JENKINS_URL` | Jenkins URL (Cloudflare tunnel or local) |
| `JENKINS_USER` | `niranga` |
| `JENKINS_API_TOKEN` | Jenkins API token for niranga user |

> `SONAR_HOST_URL` is **no longer needed as a secret** — the SonarQube job runs on the self-hosted runner and connects directly to `http://localhost:9090`.

### Self-Hosted Runner (SonarQube job)

The SonarQube analysis stage runs on a **self-hosted GitHub Actions runner** installed on this machine. This avoids Cloudflare tunnel issues with binary plugin downloads.

**Runner location:** `/home/sdvn_mobility_flooding/actions-runner/`

**Start runner (if not running):**
```bash
cd /home/sdvn_mobility_flooding/actions-runner
nohup ./run.sh > /tmp/github-runner.log 2>&1 &
tail -f /tmp/github-runner.log   # should say "Listening for Jobs"
```

**Check runner status:** GitHub → repo Settings → Actions → Runners → should show `exam-platform-runner` as **Idle**.

### Jenkins Setup

**URL:** http://localhost:8089 (user: `niranga`)

**Credential required in Jenkins:**
- ID: `ghcr-token` (Secret Text) — GitHub PAT with `write:packages` scope

**Trigger CD manually:**
```bash
CRUMB=$(curl -s "http://localhost:8089/crumbIssuer/api/json" \
  -u "niranga:***REMOVED***" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['crumbRequestField']+':'+d['crumb'])")

curl -X POST "http://localhost:8089/job/exam-platform-cd/buildWithParameters" \
  -u "niranga:***REMOVED***" -H "$CRUMB" \
  --data "IMAGE_TAG=<git-sha>" --data "TRIGGERED_BY=manual"
```

---

## Infrastructure Details

### Kafka Topics (Debezium CDC outbox pattern)

| Topic | Producer | Consumer |
|---|---|---|
| `payment.inbox.events` | enrollment-service (SAGA) | payment-service |
| `payment.outbox.events` | Debezium CDC | enrollment-service |
| `exam.inbox.events` | enrollment-service (SAGA) | exam-service |
| `enrollment.inbox.events` | exam-service | enrollment-service (SAGA) |
| `grading.inbox.events` | enrollment-service (SAGA) | grading-service |
| `grading.outbox.events` | Debezium CDC | enrollment-service |
| `certificate.inbox.events` | enrollment-service (SAGA) | certificate-service |
| `certificate.outbox.events` | Debezium CDC | enrollment-service |
| `notification.inbox.events` | enrollment-service (SAGA) | notification-service |

### Debezium Connectors

Check connector status:
```bash
curl http://localhost:8083/connectors | python3 -m json.tool
curl http://localhost:8083/connectors/<name>/status
```

Restart a failed connector:
```bash
curl -X POST http://localhost:8083/connectors/<name>/restart
```

Re-register all connectors (after full restart):
```bash
./register-connectors.sh
```

### PostgreSQL Databases

| Container | Port | DB | User |
|---|---|---|---|
| postgres-auth | 5441 | auth_db | auth_user |
| postgres-enrollment | 5442 | enrollment_db | enrollment_user |
| postgres-payment | 5443 | payment_db | payment_user |
| postgres-exam | 5444 | exam_db | exam_user |
| postgres-grading | 5445 | grading_db | grading_user |
| postgres-certificate | 5446 | certificate_db | certificate_user |

### MinIO (Certificate Storage)

- Console: http://localhost:9001
- Credentials: `minioadmin` / `minioadmin`
- Bucket: `certificates` — PDFs stored as `<certificateId>.pdf`

---

## Troubleshooting

### Services not healthy after startup
Debezium needs all Kafka brokers running. If connectors show `FAILED`:
```bash
docker compose up -d kafka-2 kafka-3
# Wait ~60s for brokers to rejoin
./register-connectors.sh
```

### SAGA stuck at PAYMENT_COMPLETED (not advancing to EXAM_SCHEDULED)
The `exam.inbox.events` consumer may have missed the event or Debezium CDC is not running.
```bash
# Check Debezium connectors
curl http://localhost:8083/connectors/enrollment-outbox-connector/status

# Restart outbox connector
curl -X POST http://localhost:8083/connectors/enrollment-outbox-connector/restart
```

### Duplicate event / second enrollment skipped
The `OutboxEventDispatcher` auto-injects a unique `eventId` UUID to prevent idempotency collisions. If you see events being skipped, check the `event_log` table in the relevant service DB.

### Kafka Connect rebalance delay
After restarting Kafka brokers, Kafka Connect waits up to 5 minutes (`rebalance.timeout.ms=300000`) before the consumer group rebalances. This is normal — wait it out or restart the connect container.

### Container name conflict on restart
```bash
docker ps -a | grep <service-name>
docker rm -f <container-id>
docker compose up -d <service-name>
```

### SonarQube not starting
SonarQube is set to `restart: unless-stopped`. If it's down:
```bash
docker start sonarqube
# Wait ~60s then check:
curl http://localhost:9090/api/system/status
```

### GitHub Actions runner not listening
```bash
cd /home/sdvn_mobility_flooding/actions-runner
nohup ./run.sh > /tmp/github-runner.log 2>&1 &
tail -5 /tmp/github-runner.log
```

---

## Docker Image Registry (GHCR)

Images are published to `ghcr.io/niranganiluminda/exam-platform/<service>:<tag>`

Tags:
- `:latest` — latest CI build
- `:<8-char-git-sha>` — specific build (e.g., `da51b6bd`)
- `:production` — last Jenkins-approved production deploy

Pull a specific image:
```bash
echo $GHCR_TOKEN | docker login ghcr.io -u NirangaNiluminda --password-stdin
docker pull ghcr.io/niranganiluminda/exam-platform/api-gateway:production
```

---

## Team
EC7205 Cloud Computing — University of Ruhuna | Assignment 2 | April 2026
