# Online Exam & Certification Platform

**EC7205 Cloud Computing — University of Ruhuna | Assignment 2 | April 2026**

A cloud-native microservices platform where students enroll in certification exams, take timed online exams, get auto-graded results, and receive downloadable PDF certificates.

---

## What This Project Does

1. Admin creates exams with questions
2. Student registers, logs in, and enrolls in an exam
3. Payment is processed automatically
4. An exam slot is created for the student
5. Student takes the timed exam and submits answers
6. System grades the exam instantly
7. If the student passes, a PDF certificate is generated and stored
8. Student can download the certificate with a unique verification code

---

## System Architecture

```
Browser
   |
   ▼
Frontend (React) ──► Nginx (port 20000)
                          |
                          ▼
                   API Gateway (port 8080)
                          |
          ┌───────────────┼───────────────────┐
          ▼               ▼                   ▼
     auth-service   enrollment-service   exam-service
     (JWT login)    (SAGA controller)    (slots + grading)
                          |
                    Kafka (3 brokers)
                          |
          ┌───────────────┼───────────────┐
          ▼               ▼               ▼
    payment-service  grading-service  certificate-service
    (auto-approve)   (MCQ grader)     (PDF + MinIO)
                                          |
                                   notification-service
                                   (email stub)

Infrastructure:
  PostgreSQL × 6  — one database per service
  Kafka × 3       — event streaming (Kafka-1:9092, Kafka-2:9093, Kafka-3:9094)
  Debezium        — reads PostgreSQL WAL changes and sends to Kafka
  MinIO           — stores certificate PDF files
  Zookeeper       — Kafka cluster coordination
  Kafka UI        — view Kafka messages (port 8088)
  SonarQube       — code quality scan (port 9090)
  Jenkins         — deployment pipeline (port 8089)
```

---

## Full SAGA Flow (Enrollment to Certificate)

```
Student clicks Enroll
        │
        ▼
Enrollment created → status: PENDING
        │
        ▼ (Kafka event)
Payment Service processes payment
  ├── Price < $1000  → PAYMENT_COMPLETED
  └── Price > $1000  → PAYMENT_FAILED → CANCELLED (compensation)
        │
        ▼ (Kafka event)
Exam Service creates a slot for the student → EXAM_SCHEDULED
        │
        ▼
Student takes the exam and submits answers → EXAM_SUBMITTED
        │
        ▼ (Kafka event)
Grading Service checks answers against answer key → GRADED
  ├── Pass → trigger certificate generation
  └── Fail → status stays FAILED
        │
        ▼ (Kafka event)
Certificate Service generates PDF → uploads to MinIO → CERTIFIED
        │
        ▼
Student can view and download the PDF certificate
```

---

## Prerequisites

- Docker 24+ and Docker Compose v2
- Java 21 and Maven 3.9+ (only needed if building locally — CI handles this automatically)
- OpenSSL (for generating JWT keys)

---

## How to Run — Step by Step

### Step 1 — Generate JWT Keys

```bash
chmod +x scripts/generate-jwt-keys.sh
./scripts/generate-jwt-keys.sh
```

This creates `keys/private_key.pem` and `keys/public_key.pem` used for JWT token signing.

### Step 2 — Start All Services

```bash
docker compose up --build -d
```

First startup takes about 3 to 5 minutes. Kafka, PostgreSQL, and Debezium all need time to initialize.

Check that everything is running:
```bash
docker ps
```

### Step 3 — Register Debezium CDC Connectors

```bash
chmod +x register-connectors.sh
./register-connectors.sh
```

This tells Debezium to watch each PostgreSQL database and send changes to Kafka automatically.

### Step 4 — Load Exam Data

```bash
# Load exam questions into exam-service database
docker exec -i postgres-exam psql -U exam_user -d exam_db < scripts/seed-data.sql

# Load answer keys into grading-service database
docker exec -i postgres-grading psql -U grading_user -d grading_db < scripts/seed-data.sql
```

### Step 5 — Verify Everything is Healthy

```bash
curl http://localhost:8080/actuator/health   # API Gateway
curl http://localhost:8081/actuator/health   # Auth Service
curl http://localhost:8082/actuator/health   # Enrollment Service
curl http://localhost:8084/actuator/health   # Exam Service
```

All should return `{"status":"UP"}`.

### Step 6 — Open the Frontend

Go to: **http://localhost:20000**

---

## Default Login Credentials

### Student Account (pre-seeded)

| Field | Value |
|---|---|
| Email | `niranganiluminda123@gmail.com` |
| Password | `Student@1234` |

### Admin Account (pre-seeded)

| Field | Value |
|---|---|
| Email | `admin@examplatform.com` |
| Password | `Admin@1234` |

### Register a New Student

Go to http://localhost:20000 → click Register → fill in your name, email, and password.

---

## Service URLs

| Service | URL | Purpose |
|---|---|---|
| Frontend | http://localhost:20000 | Student and admin web interface |
| API Gateway | http://localhost:20080 | All API calls go through here |
| Auth Service | http://localhost:20081 | Register and login |
| Enrollment Service | http://localhost:20082 | Enroll in exams |
| Payment Service | http://localhost:20083 | Handles payment processing |
| Exam Service | http://localhost:20084 | Exam slots and submissions |
| Grading Service | http://localhost:20085 | Grades exam answers |
| Certificate Service | http://localhost:20086 | Generates PDF certificates |
| Notification Service | http://localhost:20087 | Email notifications (stub) |
| Kafka UI | http://localhost:8088 | View all Kafka events |
| Jenkins | http://localhost:8089 | CD pipeline dashboard |
| MinIO Console | http://localhost:9001 | View stored certificate PDFs |
| SonarQube | http://localhost:9090 | Code quality reports |

---

## Infrastructure Credentials

### MinIO (Certificate Storage)

| Field | Value |
|---|---|
| Console URL | http://localhost:9001 |
| Username | `minioadmin` |
| Password | `minioadmin123` |
| Bucket | `certificates` |

### SonarQube (Code Quality)

| Field | Value |
|---|---|
| URL | http://localhost:9090 |
| Username | `admin` |
| Password | `admin` (change on first login) |
| Project Key | `exam-certification-platform` |

### Jenkins (CD Pipeline)

| Field | Value |
|---|---|
| Local URL | http://localhost:8089 |
| Public URL | `https://<your-ngrok-static-domain>` |
| Username | `<your-jenkins-username>` |

### PostgreSQL Databases

| Container | Port | Database | Username | Password |
|---|---|---|---|---|
| postgres-auth | 5441 | auth_db | auth_user | auth_pass |
| postgres-enrollment | 5442 | enrollment_db | enrollment_user | enrollment_pass |
| postgres-payment | 5443 | payment_db | payment_user | payment_pass |
| postgres-exam | 5444 | exam_db | exam_user | exam_pass |
| postgres-grading | 5445 | grading_db | grading_user | grading_pass |
| postgres-certificate | 5446 | certificate_db | certificate_user | certificate_pass |

---

## CI/CD Pipeline

### How It Works

```
Developer pushes code to GitHub (main branch)
        │
        ▼
GitHub Actions CI starts automatically
        │
  ┌─────┴─────┐
  ▼           ▼
Build        Test
(ubuntu)     (ubuntu)
Maven        Maven
compile      unit tests
  │           │
  └─────┬─────┘
        │
        ▼
SonarQube analysis (self-hosted runner → localhost:9090)
        │
        ▼
Build Docker images → push to GHCR (GitHub Container Registry)
        │
        ▼
Trigger Jenkins via webhook (ngrok tunnel)
        │
        ▼
Jenkins CD Pipeline:
  1. Pull new images from GHCR
  2. Deploy to staging environment
  3. Run smoke tests (health checks)
  4. Manual approval gate (must approve to go to production)
  5. Tag images as :production
  6. Final health check
```

### Technical Decisions in CI/CD

**Why self-hosted runner for SonarQube?**
SonarQube runs on `localhost:9090` on this machine. GitHub-hosted runners (ubuntu-latest) run in a different data center and cannot reach localhost. Using a self-hosted runner means the runner is on the same machine and can connect directly.

**Why ngrok for Jenkins webhook?**
Jenkins runs on `localhost:8089`. GitHub Actions needs to send a webhook to Jenkins to trigger the CD pipeline. Since Jenkins is behind a local network, we use ngrok to create a secure public tunnel.

We switched from `trycloudflare.com` (which gives a random URL every restart) to ngrok's free static domain (permanent, never changes even after restarts).

**Why Maven cache with continue-on-error?**
The Maven dependency cache is ~684MB. GitHub Actions cache service sometimes times out downloading it. Setting `continue-on-error: true` means a cache timeout falls back to downloading fresh dependencies instead of failing the whole build.

### GitHub Actions Secrets Required

| Secret | Description |
|---|---|
| `SONAR_TOKEN` | SonarQube authentication token |
| `JENKINS_URL` | Your ngrok static domain URL (e.g. `https://<your-ngrok-domain>.ngrok-free.dev`) |
| `JENKINS_USER` | Your Jenkins username |
| `JENKINS_API_TOKEN` | Jenkins API token — generate from Jenkins → User → Configure → API Token |

### Docker Image Registry (GHCR)

Images are published to `ghcr.io/<your-github-username>/exam-platform/<service-name>`

Tags:
- `:latest` — most recent CI build
- `:<8-char-git-sha>` — specific build (example: `d1abaec4`)
- `:production` — approved and deployed to production

---

## Self-Hosted Runner Setup

The GitHub Actions self-hosted runner must be running on this machine for the SonarQube stage to work.

**Check if running:**
```bash
ps aux | grep run.sh
```

**Start the runner:**
```bash
cd <runner-install-directory>       # e.g. ~/actions-runner
nohup ./run.sh > /tmp/github-runner.log 2>&1 &
tail -f /tmp/github-runner.log
# Should show: "Listening for Jobs"
```

**Check on GitHub:** Go to repo → Settings → Actions → Runners → `exam-platform-runner` should show as **Idle**.

---

## ngrok Tunnel (Jenkins)

The ngrok tunnel makes Jenkins accessible from the internet so GitHub Actions can trigger CD deployments.

**Check if running:**
```bash
docker ps | grep ngrok-jenkins
```

**Start if stopped:**
```bash
docker run -d --name ngrok-jenkins \
  --restart unless-stopped \
  --network host \
  ngrok/ngrok:latest \
  http 8089 \
  --authtoken=<your-ngrok-auth-token> \
  --url=<your-ngrok-static-domain>
```

The static domain is a **permanent URL** — it never changes even after restarts. Get your auth token and static domain from https://dashboard.ngrok.com.

---

## Kafka Topics

| Topic | Who Sends | Who Receives |
|---|---|---|
| `payment.inbox.events` | Enrollment Service | Payment Service |
| `payment.outbox.events` | Debezium (Payment DB) | Enrollment Service |
| `exam.inbox.events` | Enrollment Service | Exam Service |
| `enrollment.inbox.events` | Exam Service | Enrollment Service |
| `grading.inbox.events` | Enrollment Service | Grading Service |
| `grading.outbox.events` | Debezium (Grading DB) | Enrollment Service |
| `certificate.inbox.events` | Enrollment Service | Certificate Service |
| `certificate.outbox.events` | Debezium (Certificate DB) | Enrollment Service |
| `notification.inbox.events` | Enrollment Service | Notification Service |

View all messages live at: **http://localhost:8088** (Kafka UI)

---

## Troubleshooting

### SonarQube shows "offline" on first load
SonarQube takes 2 to 3 minutes to start because Elasticsearch needs to initialize. Wait and refresh the page. The CI pipeline automatically waits up to 4 minutes for it to be ready.

### SonarQube keeps crashing (exit code 137)
This means the Docker memory limit is too low. Open Docker Desktop → Settings → Resources → increase Memory to at least 16GB (32GB recommended when running all services together).

### Enrollment SAGA is stuck
Check Debezium connectors:
```bash
curl http://localhost:8083/connectors
# Restart a failed connector:
curl -X POST http://localhost:8083/connectors/enrollment-outbox-connector/restart
```

### Certificate download URL not working
The certificate uses a pre-signed MinIO URL. Make sure the MinIO container is running:
```bash
docker ps | grep minio
```

### Jenkins CD pipeline not triggered by GitHub Actions
Check the ngrok tunnel is running and the `JENKINS_URL` GitHub secret is set to your ngrok static domain URL.

### Kafka Connect rebalance takes too long
After restarting Kafka brokers, wait up to 5 minutes for the consumer groups to rebalance. This is normal behavior.

### Container name conflict on restart
```bash
docker ps -a | grep <service-name>
docker rm -f <container-id>
docker compose up -d <service-name>
```

---


