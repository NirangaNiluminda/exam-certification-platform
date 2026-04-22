# Online Exam & Certification Platform
### EC7205 Cloud Computing — University of Ruhuna | Assignment 2

A cloud-native microservices platform where students enroll in certification exams, take timed anti-cheat exams, receive instant grading, and get auto-generated PDF certificates...

---

## Architecture

```
Client → API Gateway (8080)
           ├── auth-service        (8081) — JWT RS256
           ├── enrollment-service  (8082) — SAGA Orchestrator
           ├── payment-service     (8083) — SAGA Participant
           ├── exam-service        (8084) — SAGA Participant
           ├── grading-service     (8085) — SAGA Participant
           ├── certificate-service (8086) — SAGA Participant
           └── notification-service(8087) — Async email stub

Infrastructure:
  Kafka (3 brokers) + Debezium CDC + PostgreSQL (6 instances) + MinIO
  Jenkins (8089) + SonarQube (9090) + Kafka UI (8088)
```

**Patterns:** SAGA Orchestration · Transactional Outbox · CDC (Debezium) · Database-per-Service · Event-Driven Architecture

---

## Prerequisites

- Docker 24+ and Docker Compose v2
- Java 21 (for building)
- Maven 3.9+
- OpenSSL (for JWT key generation)

---

## Quick Start (Docker Compose)

### Step 1 — Generate JWT keys
```bash
chmod +x scripts/generate-jwt-keys.sh
./scripts/generate-jwt-keys.sh
# Creates keys/private_key.pem and keys/public_key.pem
```

### Step 2 — Start all services
```bash
docker-compose up --build -d
```
> First startup takes ~3-5 minutes for all services to initialise.

### Step 3 — Register Debezium connectors
```bash
chmod +x register-connectors.sh
./register-connectors.sh
```

### Step 4 — Load seed data
```bash
# Exam data (exam-service DB)
docker exec -i postgres-exam psql -U exam_user -d exam_db < scripts/seed-data.sql

# Answer keys (grading-service DB)
docker exec -i postgres-grading psql -U grading_user -d grading_db < scripts/seed-data.sql
```

### Step 5 — Verify all services are healthy
```bash
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # Auth
curl http://localhost:8082/actuator/health  # Enrollment
curl http://localhost:8084/actuator/health  # Exam
```

---

## Service URLs

| Service | URL |
|---|---|
| API Gateway | http://localhost:8080 |
| Auth Service | http://localhost:8081 |
| Enrollment Service | http://localhost:8082 |
| Exam Service | http://localhost:8084 |
| Kafka UI (events viewer) | http://localhost:8088 |
| MinIO Console (certificates) | http://localhost:9001 |
| SonarQube (code quality) | http://localhost:9090 |
| Jenkins (CD pipeline) | http://localhost:8089 |

---

## Sample API Calls

### Register a student
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"student@test.com","password":"Test@1234","fullName":"John Smith"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"student@test.com","password":"Test@1234"}'
# Returns: { "accessToken": "eyJ..." }
```

### Browse available exams
```bash
curl http://localhost:8080/api/v1/exams \
  -H "Authorization: Bearer <token>"
```

### Enroll in an exam
```bash
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"examId":"a1000000-0000-0000-0000-000000000001","examPrice":49.99}'
# Returns immediately: { "id":"...", "status":"PENDING" }
# SAGA runs async: PENDING → PAYMENT_COMPLETED → EXAM_SCHEDULED
```

### Check enrollment status (poll after enrolling)
```bash
curl http://localhost:8080/api/v1/enrollments/<enrollmentId> \
  -H "Authorization: Bearer <token>"
```

### Submit exam answers
```bash
curl -X POST http://localhost:8080/api/v1/exams/slots/<slotId>/submit \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "answers": [
      {"questionId":"b1010000-0000-0000-0000-000000000001","selectedOption":"B"},
      {"questionId":"b1020000-0000-0000-0000-000000000001","selectedOption":"B"}
    ]
  }'
```

### Download certificate
```bash
curl http://localhost:8080/api/v1/certificates/<certificateId> \
  -H "Authorization: Bearer <token>"
```

### Verify certificate (public, no auth)
```bash
curl http://localhost:8080/api/v1/certificates/verify/<verifyCode>
```

---

## Kubernetes Deployment (Helm)

```bash
# Install minikube (if not installed)
minikube start --cpus=4 --memory=8192

# Add Helm repos
helm repo add bitnami https://charts.bitnami.com/bitnami
helm repo update

# Deploy
helm install exam-platform ./charts/exam-platform \
  --namespace exam-platform \
  --create-namespace \
  -f charts/exam-platform/values-dev.yaml

# Check status
kubectl get pods -n exam-platform
kubectl get hpa -n exam-platform   # HorizontalPodAutoscaler

# Access
minikube tunnel
```

---

## Demo: Failure Path (Payment Rejection)

To trigger the compensation path, enroll with `examPrice > 1000`:
```bash
curl -X POST http://localhost:8080/api/v1/enrollments \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"examId":"a1000000-0000-0000-0000-000000000001","examPrice":1500.00}'
# SAGA: PENDING → PAYMENT_FAILED → CANCELLED
# Compensation: notification sent to student
```

Watch it in Kafka UI at http://localhost:8088 — see events flow through topics.

---

## CI/CD Pipeline

| Tool | Role | URL |
|---|---|---|
| GitHub Actions | CI: build, SonarQube, test, push to GHCR | `.github/workflows/ci.yml` |
| Jenkins | CD: staging → approval → production | `Jenkinsfile` |
| SonarQube | Static code analysis | http://localhost:9090 |
| GHCR | Docker image registry | `ghcr.io/your-org/exam-platform/*` |

---

## Team
- EC7205 Cloud Computing — University of Ruhuna
- Assignment 2 | Deadline: 25th April 2026

sonaqube token - sqa_6dbf2b9e38615f738900fdb686b951d429b6c83e