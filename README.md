<div align="center">

# 🎓 Online Exam & Certification Platform

**Event-driven microservices platform — enrol, pay, sit a timed exam, get auto-graded
and download a verifiable PDF certificate. Orchestrated end-to-end with the SAGA pattern over Kafka.**

![Java](https://img.shields.io/badge/Java%2021-Spring%20Boot-6DB33F?logo=springboot&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-3%20brokers-231F20?logo=apachekafka&logoColor=white)
![Debezium](https://img.shields.io/badge/Debezium-CDC%20%2B%20Outbox-E52E2E)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-DB%20per%20service-4169E1?logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React-TypeScript-61DAFB?logo=react&logoColor=black)
![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions%20%2B%20Jenkins-2088FF?logo=githubactions&logoColor=white)

</div>

<!-- TODO: add 2–3 screenshots (exam runner, certificate PDF, Kafka UI showing the SAGA events):
![Exam runner](docs/img/exam.png) -->

## Why it's interesting

A "simple" enrol → pay → exam → certificate flow touches six services and six databases. The hard
part isn't the CRUD — it's keeping that flow **consistent without distributed transactions**
when any step can fail. This project solves that with production patterns:

- **SAGA orchestration** — the enrollment service drives the workflow and issues
  **compensating actions** (e.g. payment failure → enrollment cancelled).
- **Transactional Outbox + Debezium CDC** — events are written in the same DB transaction as the
  state change, then streamed from the PostgreSQL WAL to Kafka. No dual-write, no lost events.
- **Database-per-service** — 6 isolated PostgreSQL instances; services share events, never tables.
- **RS256 JWT at the gateway** — asymmetric keys; only the auth service can sign, every service
  can verify. Role-based access for students and admins.
- **Pre-signed MinIO URLs** for certificate downloads, each with a unique verification code.

## Architecture

```mermaid
flowchart TB
    UI[React frontend] --> NG[Nginx] --> GW[API Gateway<br/>JWT RS256 · RBAC]
    GW --> AUTH[auth-service]
    GW --> ENR[enrollment-service<br/>SAGA orchestrator]
    GW --> EXAM[exam-service<br/>slots + timer]
    ENR <-->|events| K[(Kafka · 3 brokers)]
    EXAM <--> K
    K <--> PAY[payment-service]
    K <--> GRADE[grading-service]
    K <--> CERT[certificate-service] --> M[(MinIO<br/>PDFs)]
    K --> NOTIF[notification-service]
    DB[(PostgreSQL ×6<br/>outbox tables)] -. WAL .-> DBZ[Debezium] --> K
```

### The SAGA

```mermaid
stateDiagram-v2
    [*] --> PENDING: student enrols
    PENDING --> PAYMENT_COMPLETED: payment ok
    PENDING --> CANCELLED: payment failed (compensate)
    PAYMENT_COMPLETED --> EXAM_SCHEDULED: slot created
    EXAM_SCHEDULED --> EXAM_SUBMITTED: answers submitted
    EXAM_SUBMITTED --> GRADED
    GRADED --> CERTIFIED: pass → PDF to MinIO
    GRADED --> FAILED: fail
```

## DevOps pipeline

| Stage | Tool | What happens |
|---|---|---|
| Build & test | GitHub Actions | Maven build (Java 21) with dependency cache |
| Quality gate | SonarQube (self-hosted runner) | Static analysis on every push |
| Package | GitHub Actions → GHCR | Docker images tagged with the git SHA |
| Deploy | Jenkins | Pull from GHCR → staging → smoke tests → **manual approval** → production tag → health check |

Why two CI tools, why a self-hosted runner, and every other trade-off is explained in
[`TECHNICAL.md`](TECHNICAL.md).

## Tech stack

**Backend** Java 21 · Spring Boot · Spring Cloud Gateway · **Messaging** Apache Kafka (3 brokers) ·
Debezium · **Data** PostgreSQL ×6 · MinIO · **Frontend** React · TypeScript · Vite ·
**Infra** Docker Compose · Nginx · **CI/CD** GitHub Actions · Jenkins · SonarQube · GHCR

## Quick start

```bash
./scripts/generate-jwt-keys.sh      # creates keys/ (git-ignored)
docker compose up -d --build        # 26 containers
./register-connectors.sh            # Debezium CDC connectors
# Frontend: http://localhost:20000 · Kafka UI: http://localhost:8088
```

Full step-by-step guide, Postman collection and troubleshooting: [`SETUP.md`](SETUP.md).

---

Built for EC7205 Cloud Computing, University of Ruhuna (2026).
**G.W. Niranga Niluminda** · [LinkedIn](https://linkedin.com/in/niranganiluminda)
