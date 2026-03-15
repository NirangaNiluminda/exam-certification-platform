-- seed-data.sql
-- Sample data for demo purposes
-- Run against exam-service and grading-service databases

-- ── exam-service DB ────────────────────────────────────────────

-- 3 Sample Exams
INSERT INTO exams (id, title, description, duration_minutes, passing_score, total_marks, price) VALUES
('a1000000-0000-0000-0000-000000000001',
 'Cloud Computing Fundamentals',
 'Tests knowledge of cloud architecture, deployment models, scalability, and cloud-native patterns.',
 60, 70, 10, 49.99),

('a2000000-0000-0000-0000-000000000002',
 'Docker & Kubernetes Essentials',
 'Covers containerization, Docker images, Kubernetes deployments, Helm charts, and orchestration.',
 45, 70, 10, 39.99),

('a3000000-0000-0000-0000-000000000003',
 'Java Spring Boot Professional',
 'Tests Spring Boot, Spring Security, REST APIs, JPA, and microservices patterns.',
 90, 70, 10, 59.99);

-- ── Exam 1: Cloud Computing Fundamentals ──────────────────────
INSERT INTO questions (id, exam_id, question_text, option_a, option_b, option_c, option_d, marks, sort_order) VALUES
('b1010000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'Which cloud deployment model provides the highest level of control over infrastructure?',
 'Public Cloud', 'Private Cloud', 'Hybrid Cloud', 'Community Cloud', 1, 1),

('b1020000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'What does the CAP theorem state about distributed systems?',
 'You can achieve Consistency, Availability, and Partition Tolerance simultaneously',
 'You can only achieve two of: Consistency, Availability, Partition Tolerance',
 'Partition Tolerance is optional in cloud systems',
 'Consistency is always prioritized over Availability', 1, 2),

('b1030000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'Which pattern ensures atomic writes of domain events to avoid lost messages?',
 'SAGA Pattern', 'Transactional Outbox Pattern', 'Event Sourcing', 'CQRS', 1, 3),

('b1040000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'In horizontal scaling, what happens when load increases?',
 'CPU and RAM of the single server are upgraded',
 'More server instances are added and load is distributed',
 'The database is replicated to multiple regions',
 'The application is rewritten to be more efficient', 1, 4),

('b1050000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'Which service model gives customers the most control over OS, middleware, and runtime?',
 'SaaS', 'PaaS', 'IaaS', 'FaaS', 1, 5),

('b1060000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'What is the primary benefit of stateless microservices?',
 'They require less memory per instance',
 'Any instance can handle any request, enabling easy horizontal scaling',
 'They do not need a database',
 'They are faster because they skip authentication', 1, 6),

('b1070000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'What does BASE stand for in distributed databases?',
 'Basically Available, Soft state, Eventual consistency',
 'Binary Atomic State Engine',
 'Broadly Available Synchronized Environment',
 'Base-level ACID for Scalable Environments', 1, 7),

('b1080000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'Which pattern avoids tight coupling between microservices for long-running transactions?',
 '2-Phase Commit (2PC)', 'SAGA Pattern', 'Distributed Lock', 'XA Transactions', 1, 8),

('b1090000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'What is the role of an API Gateway in microservices?',
 'It stores data for all microservices',
 'It handles routing, authentication, rate limiting, and load balancing for client requests',
 'It runs all business logic',
 'It replaces the message broker', 1, 9),

('b1100000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001',
 'Which replication strategy in Kafka ensures high availability?',
 'Single broker with RAID storage',
 'Replication factor of 3 with min.insync.replicas=2',
 'A backup broker that only activates on failure',
 'Consumer group replication', 1, 10);

-- ── Exam 2: Docker & Kubernetes Essentials ────────────────────
INSERT INTO questions (id, exam_id, question_text, option_a, option_b, option_c, option_d, marks, sort_order) VALUES
('b2010000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'What is a Docker container?',
 'A virtual machine with its own OS kernel',
 'A lightweight isolated process sharing the host OS kernel',
 'A compiled Java application',
 'A cloud storage bucket', 1, 1),

('b2020000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'Which Kubernetes object ensures a specified number of Pod replicas are always running?',
 'Service', 'ConfigMap', 'ReplicaSet', 'Ingress', 1, 2),

('b2030000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'What does a HorizontalPodAutoscaler (HPA) do?',
 'Increases Pod CPU and memory limits',
 'Automatically scales the number of Pods based on CPU or custom metrics',
 'Balances traffic between services',
 'Upgrades Kubernetes node versions', 1, 3),

('b2040000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'Which Kubernetes service type exposes a service externally using a cloud load balancer?',
 'ClusterIP', 'NodePort', 'LoadBalancer', 'ExternalName', 1, 4),

('b2050000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'What is Helm in the Kubernetes ecosystem?',
 'A container runtime',
 'A package manager for Kubernetes that uses charts',
 'A monitoring tool',
 'A Kubernetes network plugin', 1, 5),

('b2060000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'What Linux feature provides CPU and memory resource isolation for containers?',
 'Namespaces', 'cgroups (control groups)', 'iptables', 'systemd', 1, 6),

('b2070000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'Which deployment strategy allows routing a small percentage of traffic to a new version first?',
 'Recreate', 'Blue/Green', 'Canary', 'Rolling', 1, 7),

('b2080000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'What is stored in a Kubernetes ConfigMap?',
 'Encrypted secrets like passwords and API keys',
 'Non-sensitive configuration data like environment variables',
 'Container images',
 'Network routing rules', 1, 8),

('b2090000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'Which Kubernetes probe restarts a container if it fails?',
 'Readiness Probe', 'Liveness Probe', 'Startup Probe', 'Health Probe', 1, 9),

('b2100000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002',
 'What does a Dockerfile ENTRYPOINT instruction define?',
 'The base image to use',
 'The command that runs when the container starts',
 'The files to copy into the image',
 'The port to expose', 1, 10);

-- ── Exam 3: Java Spring Boot Professional ─────────────────────
INSERT INTO questions (id, exam_id, question_text, option_a, option_b, option_c, option_d, marks, sort_order) VALUES
('b3010000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'Which annotation marks a class as a Spring-managed component?',
 '@Bean', '@Component', '@Controller', '@Repository', 1, 1),

('b3020000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'What does @Transactional(propagation = MANDATORY) mean?',
 'A new transaction is always created',
 'The method must be called within an existing transaction or an exception is thrown',
 'The transaction is optional',
 'The transaction is always read-only', 1, 2),

('b3030000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'Which Spring Security feature makes JWT authentication stateless?',
 'HttpSession management',
 'No server-side session; the token carries all claims',
 'Cookie-based auth',
 'LDAP integration', 1, 3),

('b3040000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'What is the purpose of Spring Actuator?',
 'To generate API documentation',
 'To expose health, metrics, and management endpoints for monitoring',
 'To manage database migrations',
 'To handle message queuing', 1, 4),

('b3050000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'Which annotation is used for input validation on REST request bodies?',
 '@Validated', '@Valid', '@Constraint', '@RequestBody', 1, 5),

('b3060000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'What does Flyway provide in a Spring Boot application?',
 'Automated horizontal scaling',
 'Version-controlled database schema migrations',
 'JWT token generation',
 'Kafka consumer management', 1, 6),

('b3070000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'Which HTTP status code should a successful POST (resource created) return?',
 '200 OK', '201 Created', '204 No Content', '202 Accepted', 1, 7),

('b3080000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'What is Spring Cloud Gateway used for?',
 'Database connection pooling',
 'API routing, authentication filtering, and rate limiting for microservices',
 'Generating PDF reports',
 'Container orchestration', 1, 8),

('b3090000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'What does @KafkaListener annotation do?',
 'Publishes messages to a Kafka topic',
 'Marks a method as a Kafka consumer that processes messages from a topic',
 'Creates a new Kafka topic',
 'Configures Kafka broker connection settings', 1, 9),

('b3100000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003',
 'What is the Database-per-Service pattern?',
 'All microservices share a single database schema',
 'Each microservice owns its own database, with no direct DB access by other services',
 'Each service uses a different database vendor',
 'Services share tables but have separate connection pools', 1, 10);


-- ── grading-service DB — answer keys ──────────────────────────
-- (run this against grading_db)

INSERT INTO answer_keys (question_id, exam_id, correct_option) VALUES
-- Exam 1: Cloud Computing
('b1010000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'B'),
('b1020000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'B'),
('b1030000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'B'),
('b1040000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'B'),
('b1050000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'C'),
('b1060000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'B'),
('b1070000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'A'),
('b1080000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'B'),
('b1090000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'B'),
('b1100000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'B'),
-- Exam 2: Docker & Kubernetes
('b2010000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'B'),
('b2020000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'C'),
('b2030000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'B'),
('b2040000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'C'),
('b2050000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'B'),
('b2060000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'B'),
('b2070000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'C'),
('b2080000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'B'),
('b2090000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'B'),
('b2100000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'B'),
-- Exam 3: Spring Boot
('b3010000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B'),
('b3020000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B'),
('b3030000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B'),
('b3040000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B'),
('b3050000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B'),
('b3060000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B'),
('b3070000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B'),
('b3080000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B'),
('b3090000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B'),
('b3100000-0000-0000-0000-000000000003', 'a3000000-0000-0000-0000-000000000003', 'B');
