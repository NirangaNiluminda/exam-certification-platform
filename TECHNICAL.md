# Technical Documentation

**EC7205 Cloud Computing — University of Ruhuna | Assignment 2 | April 2026**
**Project: Online Exam & Certification Platform**

---

## 1. Introduction

This project is an online exam and certification platform built using cloud-native principles. Students can register, enroll in certification exams, take timed multiple-choice exams, receive instant grading, and download PDF certificates.

The system is designed to be scalable, highly available, secure, and easy to deploy using modern DevOps tools.

**Mark allocation addressed:**

| Criteria | Where it is implemented |
|---|---|
| Functionality (20%) | Full exam flow: register → enroll → pay → exam → grade → certificate |
| Cloud-native architecture (20%) | Microservices, containers, event-driven, stateless services |
| Scalability & availability (15%) | Kafka 3-broker cluster, 6 independent databases, horizontal scaling ready |
| Security (10%) | JWT RS256, Spring Security, role-based access, HTTPS via ngrok |
| Deployment & DevOps (10%) | GitHub Actions CI + Jenkins CD + Docker + GHCR + SonarQube |
| Communication methods (10%) | REST (sync) + Kafka events (async) + SAGA pattern |
| Documentation & clarity (15%) | This document + README + code comments |

---

## 2. Architecture

### 2.1 Why Microservices?

Each part of the system (auth, enrollment, payment, exam, grading, certificate, notification) is a separate service with its own database. This means:

- Each service can be deployed, updated, and scaled independently
- A failure in one service does not crash the whole system
- New features (like a new payment method) can be added to one service without touching others
- Each service is a small Spring Boot application that is easy to understand and maintain

### 2.2 Services and Responsibilities

| Service | Technology | What it does |
|---|---|---|
| api-gateway | Spring Cloud Gateway | Single entry point, JWT validation, routes requests |
| auth-service | Spring Boot + PostgreSQL | Register, login, issue JWT tokens |
| enrollment-service | Spring Boot + PostgreSQL | SAGA orchestrator, drives the whole enrollment flow |
| payment-service | Spring Boot + PostgreSQL | Auto-approves payments under $1000, rejects over $1000 |
| exam-service | Spring Boot + PostgreSQL | Creates exam slots, stores questions, handles submissions |
| grading-service | Spring Boot + PostgreSQL | Compares student answers to answer keys, calculates score |
| certificate-service | Spring Boot + PostgreSQL + MinIO | Generates PDF certificate, stores in MinIO, creates download URL |
| notification-service | Spring Boot | Listens for events and sends email notifications (stub for demo) |
| frontend | React + TypeScript + Vite | Student and admin web interface |

### 2.3 Infrastructure

| Component | Purpose | Why this choice |
|---|---|---|
| PostgreSQL × 6 | One database per service | Database-per-Service pattern: each service owns its data |
| Apache Kafka × 3 | Event streaming | High throughput, durable, replay-able, 3 brokers for fault tolerance |
| Debezium | Change Data Capture (CDC) | Reads PostgreSQL transaction log (WAL) and sends changes to Kafka reliably |
| MinIO | Object storage for PDFs | S3-compatible, runs locally, no cloud cost |
| Nginx | Reverse proxy for frontend | Serves the React app and proxies API calls |
| Docker Compose | Container orchestration | Easy one-command startup for the full platform |

---

## 3. Cloud-Native Architecture Patterns

### 3.1 SAGA Orchestration Pattern

A SAGA is a sequence of steps that each update one service. If a step fails, previous steps are compensated (rolled back).

**Why SAGA instead of a single database transaction?**
Because each service has its own database. Traditional database transactions cannot span multiple databases. SAGA solves this by breaking the transaction into smaller steps that each service completes independently.

**The Enrollment SAGA has these steps:**

```
Step 1: Create enrollment (PENDING)
Step 2: Request payment → if approved → PAYMENT_COMPLETED
Step 3: Create exam slot → EXAM_SCHEDULED
Step 4: Student submits exam → EXAM_SUBMITTED
Step 5: Grade exam → GRADED
Step 6: Generate certificate → CERTIFIED
```

**Compensation (rollback) path:**
If payment is rejected (price > $1000), the system automatically:
1. Marks enrollment as PAYMENT_FAILED
2. Sends a notification to the student
3. Marks enrollment as CANCELLED

### 3.2 Transactional Outbox Pattern

**Problem:** When a service updates its database and sends a Kafka event, what happens if the service crashes between those two operations? The database is updated but the event is never sent.

**Solution (Transactional Outbox):**
1. Service writes the event to an `outbox_events` table in the same database transaction as the main update
2. Debezium reads the `outbox_events` table via CDC and sends the event to Kafka
3. This means the event is only sent if the database transaction succeeded

This guarantees no events are lost.

### 3.3 Database-per-Service Pattern

Each service has its own PostgreSQL instance. Services never directly read each other's databases. All communication goes through Kafka events or REST APIs. This means:

- Each service can choose its own schema without affecting others
- Schema changes in one service do not break other services
- Each database can be scaled or optimized independently

### 3.4 Event-Driven Architecture

Most communication between services is done through Kafka events, not direct REST calls. This means:

- Services do not need to know about each other
- If one service is temporarily down, events wait in Kafka and are processed when the service comes back
- The system is loosely coupled and more resilient

---

## 4. Scalability and High Availability

### 4.1 Kafka 3-Broker Cluster

Kafka runs with 3 brokers (kafka-1, kafka-2, kafka-3). This means:

- If one broker goes down, the other two continue serving
- Topics are replicated across brokers so no data is lost
- Multiple consumers can read from different partitions in parallel

### 4.2 Stateless Services

All microservices are stateless. Each request carries a JWT token with all the information needed to authenticate. No session data is stored in the service. This means:

- Multiple instances of the same service can run at the same time
- Load balancers can route requests to any instance
- Scaling up is as simple as starting another container

### 4.3 Independent Databases

Because each service has its own database, one service being under heavy load does not affect the database performance of other services.

### 4.4 Docker Container Restart Policies

All containers use `restart: unless-stopped`. If a container crashes, Docker automatically restarts it. This provides basic self-healing without any extra tools.

### 4.5 Health Checks

Every Spring Boot service exposes `/actuator/health`. Jenkins CD pipeline checks these endpoints after deployment to confirm all services are healthy before approving the deployment.

---

## 5. Security

### 5.1 JWT RS256 Authentication

The system uses JSON Web Tokens (JWT) with RS256 (RSA asymmetric signing). This means:

- The `auth-service` signs tokens with a **private key** that only it knows
- Every other service verifies tokens using the **public key**
- Even if someone steals the public key, they cannot create fake tokens because they do not have the private key

The keys are generated at startup using OpenSSL and mounted into containers as read-only files.

### 5.2 API Gateway — Centralized Authentication

Every request must pass through the `api-gateway`. The gateway:
1. Reads the JWT from the `Authorization: Bearer <token>` header
2. Validates the signature using the public key
3. Extracts user information (ID, email, name, role) and adds them as request headers
4. Passes the request to the target service

This means individual services do not need to handle authentication themselves — they just trust the headers added by the gateway.

### 5.3 Role-Based Access Control (RBAC)

There are two roles: `STUDENT` and `ADMIN`. The API gateway and individual services use Spring Security `@PreAuthorize` annotations to enforce who can do what:

- Only `ADMIN` can create/edit exams and questions
- Only the `STUDENT` who enrolled can take their own exam
- Certificate download requires authentication

### 5.4 HTTPS via ngrok

The Jenkins webhook endpoint is exposed through ngrok, which provides HTTPS automatically. External webhooks from GitHub Actions reach Jenkins over a secure encrypted connection.

### 5.5 Pre-signed MinIO URLs

Certificate PDFs are stored in MinIO (private). When a student wants to download their certificate, the system generates a **pre-signed URL** that is valid for 1 hour. This URL contains a cryptographic signature and does not require the MinIO password to use.

---

## 6. Communication Methods

### 6.1 Synchronous Communication (REST)

Used when an immediate response is needed:

| Call | From | To | Method |
|---|---|---|---|
| Register / Login | Browser | Auth Service | REST POST |
| Enroll in exam | Browser | Enrollment Service | REST POST |
| Submit exam answers | Browser | Exam Service | REST POST |
| View certificate | Browser | Certificate Service | REST GET |

All REST calls go through the API Gateway which handles JWT validation.

### 6.2 Asynchronous Communication (Kafka Events)

Used for background processing that does not need an immediate response:

| Event | Trigger | Effect |
|---|---|---|
| `PAYMENT_COMPLETED` | Payment Service | Enrollment Service creates exam slot |
| `EXAM_SLOT_SCHEDULED` | Exam Service | Enrollment status moves to EXAM_SCHEDULED |
| `EXAM_SUBMITTED` | Exam Service | Grading Service grades the exam |
| `EXAM_GRADED` | Grading Service | Certificate Service generates PDF |
| `CERTIFICATE_ISSUED` | Certificate Service | Enrollment moves to CERTIFIED |

### 6.3 Why Both?

- **REST** is used for user-facing operations where the user needs an immediate response (login, view data)
- **Kafka** is used for background processing where the user does not need to wait (payment processing, grading, certificate generation)

This combination gives both responsiveness and resilience.

---

## 7. Deployment & DevOps

### 7.1 Why Two Tools — GitHub Actions AND Jenkins?

This is a common question: why not just use GitHub Actions for everything?

**GitHub Actions** is used for **CI (Continuous Integration)** — every time a developer pushes code, GitHub automatically builds it, tests it, scans it, and packages it into a Docker image. GitHub Actions is the right tool here because it runs automatically on every push and integrates directly with the GitHub repository.

**Jenkins** is used for **CD (Continuous Deployment)** — taking the Docker images that GitHub Actions built and actually deploying them to the server. Jenkins is kept separate because:

1. **Manual approval gate** — Before any code goes to production, a human must log in to Jenkins and click "Approve". GitHub Actions has no easy way to pause for a human decision.
2. **Deployment runs on the local machine** — Jenkins runs at `localhost:8089` on the same machine as the running containers. It can directly call `docker compose` to update the running services. GitHub Actions runs on GitHub's servers and cannot do this.
3. **Industry standard** — In real companies, CI and CD are often separate tools. GitHub Actions handles build and test; a dedicated CD tool (Jenkins, ArgoCD, Spinnaker) handles deployment. This separation gives more control over what goes to production.

---

### 7.2 CI Pipeline — GitHub Actions (Step by Step)

The CI pipeline is defined in `.github/workflows/ci.yml`. Every push to the `main` branch triggers it automatically.

```
Developer pushes code to GitHub (main branch)
        │
        ▼
┌─────────────────────────────────────────────────┐
│  STAGE 1: BUILD  (ubuntu-latest)                │
│  - Checkout code                                │
│  - Set up Java 21                               │
│  - Restore Maven dependency cache (~684MB)      │
│  - mvn clean package -DskipTests                │
│    (compiles all 8 Java services into JARs)     │
│  - Save Maven cache for next run                │
│  - Upload JAR files as build artifacts          │
└──────────────────────┬──────────────────────────┘
                       │
          ┌────────────┴────────────┐
          │ Runs in PARALLEL        │
          ▼                         ▼
┌─────────────────────┐   ┌─────────────────────────┐
│  STAGE 2: SONARQUBE │   │  STAGE 3: TEST          │
│  (self-hosted       │   │  (ubuntu-latest)        │
│   runner)           │   │                         │
│  - Wait for SonarQube   │  - Set up Java 21       │
│    to be ready      │   │  - Restore Maven cache  │
│    (up to 4 min)    │   │  - mvn test             │
│  - Run SonarQube    │   │    (unit tests)         │
│    Maven scan       │   │  - mvn verify           │
│  - Static code      │   │    (integration tests)  │
│    analysis report  │   │  - OWASP dependency     │
│  continue-on-error  │   │    vulnerability scan   │
│  (does not block    │   │  - Upload test results  │
│   the pipeline)     │   │    as artifacts         │
└─────────────────────┘   └─────────────────────────┘
          │                         │
          └────────────┬────────────┘
                       │ Both must complete
                       ▼
┌─────────────────────────────────────────────────┐
│  STAGE 4: BUILD & PUSH DOCKER IMAGES            │
│  (ubuntu-latest, only runs on main branch)      │
│  - Compile all 8 services again (fresh JARs)    │
│  - Log in to GitHub Container Registry (GHCR)  │
│  - For each of 8 services:                      │
│      docker build -f ./service/Dockerfile       │
│      docker push ghcr.io/.../service:latest     │
│      docker push ghcr.io/.../service:<git-sha>  │
│  - Trigger Jenkins CD via webhook (curl POST)   │
└─────────────────────────────────────────────────┘
```

**Key design decisions in CI:**

- **SonarQube and Test run in parallel** — they both depend on the Build stage but do not depend on each other, so they run at the same time. This saves time compared to running them one after another.

- **SonarQube uses `continue-on-error: true`** — if SonarQube is down or reports issues, the pipeline still continues. The important stages (build, test, Docker push) are not blocked by code quality issues. This prevents a SonarQube restart from blocking the whole team.

- **SonarQube runs on a self-hosted runner** — GitHub-hosted runners run on GitHub's servers in a remote data centre. They cannot reach `localhost:9090`. The self-hosted runner is installed on the same machine as SonarQube, so it can connect directly.

- **Maven cache with `continue-on-error: true`** — the Maven dependency cache is ~684MB. GitHub's cache service sometimes times out when restoring large caches. Setting `continue-on-error: true` means a timeout falls back to downloading fresh dependencies instead of failing the entire build.

- **Docker images are tagged twice** — `:latest` for convenience and `:<8-char-git-sha>` for traceability. Each image can be traced back to the exact commit that built it.

---

### 7.3 CD Pipeline — Jenkins (Step by Step)

After GitHub Actions successfully pushes Docker images, it sends a webhook to Jenkins:

```
GitHub Actions sends POST to:
https://<your-ngrok-static-domain>/job/exam-platform-cd/buildWithParameters
  ?IMAGE_TAG=<git-sha>
  &COMMIT_SHA=<full-sha>
  &TRIGGERED_BY=github-actions
        │
        ▼
Jenkins receives the webhook and starts the pipeline
        │
        ▼
┌─────────────────────────────────────────────────┐
│  STEP 1: PULL IMAGES FROM GHCR                  │
│  - docker pull ghcr.io/.../auth-service:<tag>   │
│  - docker pull ghcr.io/.../exam-service:<tag>   │
│  - ... (all 8 services)                         │
│  - This downloads the exact images built by CI  │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│  STEP 2: DEPLOY TO STAGING                      │
│  - docker compose up -d with new image tags     │
│  - Containers are replaced one by one           │
│  - Old containers stop, new containers start    │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│  STEP 3: SMOKE TESTS (Health Checks)            │
│  - curl http://localhost:8080/actuator/health   │
│  - curl http://localhost:8081/actuator/health   │
│  - ... (all services)                           │
│  - If any service returns DOWN → pipeline fails │
│  - Confirms the new deployment is actually      │
│    running and responding                       │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│  STEP 4: MANUAL APPROVAL GATE  ◄── HUMAN HERE  │
│  - Jenkins pauses and waits                     │
│  - A person logs in to Jenkins dashboard        │
│  - Reviews the smoke test results               │
│  - Clicks "Approve" to continue OR              │
│    "Reject" to abort and roll back              │
│  - Nothing goes to production without           │
│    human sign-off                               │
└──────────────────────┬──────────────────────────┘
                       │ Only if Approved
                       ▼
┌─────────────────────────────────────────────────┐
│  STEP 5: TAG AS PRODUCTION                      │
│  - docker tag service:<git-sha> service:production│
│  - docker push ghcr.io/.../service:production   │
│  - The :production tag in GHCR always points    │
│    to the last approved and deployed image      │
└──────────────────────┬──────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────┐
│  STEP 6: FINAL HEALTH CHECK                     │
│  - Run health checks again on production        │
│  - Confirms the production deployment is live   │
│  - Pipeline marked as SUCCESS                   │
└─────────────────────────────────────────────────┘
```

**Why the manual approval gate?**

In production systems, you never want code to go live automatically without a human review. The smoke tests check that services start and respond, but a human should verify that the application actually behaves correctly (no broken features, no data issues) before approving. This is standard practice in enterprise CI/CD.

---

### 7.4 SonarQube — Self-Hosted Runner Decision

**Problem:** SonarQube runs at `localhost:9090` on the development machine. GitHub Actions jobs run on GitHub's servers in a data centre. They cannot reach `localhost:9090`.

**Solution:** Install a GitHub Actions self-hosted runner on the same machine as SonarQube. The self-hosted runner runs the SonarQube job locally where `localhost:9090` is reachable.

Runner location: the directory where you ran the GitHub Actions runner install script (e.g. `~/actions-runner/`).

### 7.5 Jenkins — ngrok Permanent Tunnel Decision

**Problem:** Jenkins runs at `localhost:8089`. GitHub Actions needs to trigger Jenkins via a webhook after a successful build. GitHub cannot reach `localhost:8089`.

**First approach (Cloudflare trycloudflare.com):** Free but the URL changes every time the tunnel restarts, requiring a manual update to the GitHub secret `JENKINS_URL` every time.

**Final solution (ngrok static domain):** ngrok provides one free permanent static domain per account. The static domain never changes even after restarts. The GitHub secret `JENKINS_URL` only needs to be set once and never needs updating.

### 7.6 Docker Multi-Stage Builds

Each service uses a two-stage Dockerfile:
- **Stage 1 (builder):** Uses a full Maven image to compile the Java code and package the JAR
- **Stage 2 (runtime):** Uses a minimal JRE image and copies only the JAR

This keeps the final Docker image small (no Maven, no source code, no build tools).

### 7.7 GitHub Container Registry (GHCR)

Docker images are stored at `ghcr.io/<your-github-username>/exam-platform/<service-name>`

Three image tags are used:
- `:latest` — latest CI build
- `:<8-char-git-sha>` — specific build for traceability (e.g. `d1abaec4`)
- `:production` — approved and deployed to production via Jenkins

---

## 8. Database Design Decisions

### 8.1 Why PostgreSQL?

PostgreSQL supports:
- ACID transactions (important for the outbox pattern)
- Logical replication (required by Debezium CDC)
- JSONB columns (used for event payloads in the outbox table)
- UUID primary keys (used throughout for distributed ID generation)

### 8.2 Outbox Table Pattern

Every service that publishes Kafka events has an `outbox_events` table:

```sql
CREATE TABLE outbox_events (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id  UUID NOT NULL,
    aggregate_type VARCHAR(100),
    event_type    VARCHAR(100),
    payload       JSONB,
    topic         VARCHAR(200),
    created_at    TIMESTAMP DEFAULT now()
);
```

Debezium watches this table and sends new rows to Kafka automatically.

### 8.3 Idempotency

Every Kafka consumer has an `event_log` table that stores the `eventId` of every processed event. Before processing a new event, the consumer checks if the `eventId` is already in the log. If it is, the event is skipped. This prevents duplicate processing when Kafka delivers an event more than once.

---
