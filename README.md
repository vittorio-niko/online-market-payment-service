# Payment Microservice 🚀💳📊

**Payment Service** is the reactive financial orchestration backbone of the online marketplace ecosystem. Built on **Java 21** and **Spring Boot 3.3.x**, it processes transaction operations asynchronously over **Apache Kafka** using high-performance transactional persistence. To guarantee transactional operations across distributed data boundaries, it employs a **MongoDB Replica Set** architecture configured with enterprise-grade **Transactional Inbox and Outbox design patterns** to enforce exactly-once message processing and completely avoid double-billing anomalies.

---

## 🛠 Tech Stack

* **Core Engine:** Java 21, Spring Boot 3.3.x, Spring Data MongoDB.
* **Distributed Database Layer:** MongoDB 6.0 (Configured as a local Replica Set `rs0`), Mongock database migration framework.
* **Messaging & Processing Architecture:** Spring Kafka, Spring Scheduling Engine (Asynchronous Inbox/Outbox processors).
* **Security & IAM Execution:** Spring Security OAuth2 Resource Server (JWT parsing with role validation engines via Keycloak mapping).
* **Observability & Metrics:** Micrometer Observation, Micrometer Tracing using OpenTelemetry (OTel) bridge adapters, Prometheus metrics telemetry, Spring Boot Actuator engine.
* **DevOps Engineering:** Multi-stage JVM runtime containers, native Kubernetes manifests.

---

## 🚀 Key Features

### 🔄 Distributed Transaction Guarantee (Inbox/Outbox)
* **Transactional Outbox Engine:** Decouples core database document states from public Kafka message distribution loops, stage-storing transaction records inside the local outbox data store atomically within the database scope before broker propagation.
* **Transactional Inbox Idempotency:** Implements strict message processing idempotency checks by parsing specific cross-service `paymentId` allocations coming directly from upstream microservices, filtering duplicate messages out to safe-guard processing loops.
* **Replica Set Architecture:** Enforces explicit transactional rollback guarantees across Document sets by configuring MongoDB as a standalone replication cluster (`rs0`) combined with native multi-document database transaction configurations.

### 📊 Secure Administrative Auditing & Reporting
* **Deep Financial Metrics:** Aggregates, tracks, and processes historical ledger data streams, providing specialized analytical tracking metrics dynamically via localized REST query boundaries.
* **Role-Restricted Execution:** Safeguards analytical aggregation paths behind standard OAuth2 Resource Server layers, enforcing mandatory `ADMIN` role scopes via Keycloak verification checks.
* **Asynchronous Log Scheduling:** Leverages integrated Spring schedulers to calculate system-wide processing amounts and generate atomic daily sum logs without disrupting live transaction processing routines.

---

## 📐 API Routing Matrix

All endpoints require active bearer token credentials and are explicitly restricted to security contexts evaluated with the **`ADMIN`** role abstraction layer:

| Endpoint Interface | HTTP Method | Auth Level | Description |
| :--- | :--- | :--- | :--- |
| `/admin/payments/{paymentId}` | `GET` | Bearer JWT (`ADMIN`) | Extracts exact metadata and log traces for a specific payment ID |
| `/admin/payments/orders/{orderId}` | `GET` | Bearer JWT (`ADMIN`) | Fetches an overview mapping of all processed logs tied to an Order ID|
| `/admin/payments/users/{userId}` | `GET` | Bearer JWT (`ADMIN`) | Queries comprehensive user payment histories with embedded pagination|
| `/admin/payments/users/{userId}/payment-sum` | `GET` | Bearer JWT (`ADMIN`) | Computes successful financial transaction sums for an isolated date value |
| `/admin/payments/users/{userId}/payment-sum/range` | `GET` | Bearer JWT (`ADMIN`) | Calculates a user's total successful payments within a given date range |
| `/admin/payments/daily-sum` | `GET` | Bearer JWT (`ADMIN`) | Extracts the pre-aggregated system-wide financial summary log for a specific date |
| `/admin/payments/daily-sum/range` | `GET` | Bearer JWT (`ADMIN`) | Evaluates paginated rows of system-wide daily summaries across a date range |
| `/admin/payments/daily-sum/range/total` | `GET` | Bearer JWT (`ADMIN`) | Computes total gross processed volume across all users over a time window |

---

## 📂 Required Directory Architecture

To run the unified ecosystem orchestration scripts, your local development workspace must be structured with all service repositories placed **side-by-side inside a single parent directory**:

```text
workspace/
├── payment-service/        <-- You are here
│   ├── docker-compose.yaml
│   └── Dockerfile
├── api-gateway/
├── order-service/
└── user-service/
```

---

## ⚙️ Environment Configuration

Populate your local system configurations using target variables before initiating builds. The primary parameters are outlined below:

```env
# Runtime Port Configuration
SERVER_PORT=8080
PAYMENT_SERVICE_PORT_INTERNAL=8080
PAYMENT_SERVICE_PORT_EXTERNAL=8085

# Database Infrastructure (MongoDB Replica Set)
MONGO_USER=admin
MONGO_PASS=admin_secure_password
MONGO_DB_NAME=payment_db
MONGO_DB_URI=mongodb://admin:admin_secure_password@payment-db:27017/payment_db?authSource=admin&replicaSet=rs0

# Broker Links
KAFKA_BOOTSTRAP_SERVERS=kafka:9092

# Keycloak Security URI Access Points
KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/online-market
```

---

## 🛠️ Deployment Instructions

### Option 1: Running via Docker Compose Natively
If you choose to run the payment component independently outside of the global workspace orchestration wrappers:

```bash
docker compose up -d --build
```
* **Database Verification:** Automatically spins up the database instance (`payment-mongodb`), configures a unique operational security key file (`mongodb.key`), and triggers a secondary provisioning task (`db-setup`) to safely execute `rs.initiate()` setups inside the database engine.
* **Ecosystem Communication:** Binds components onto the shared network boundary (`local_market_net`).
* **Runtime Diagnostic Tracking:** Monitor streaming microservice wellness entries via: `http://localhost:8085/actuator/health`.

---

### Option 2: Kubernetes Deployment (Minikube Local Testing)
Deployment specifications feature configuration setups mapped directly to cluster resource definitions. Follow the sequential terminal commands below:

1. Connect your active terminal context to route image assets straight into the Minikube internal Docker daemon:
```bash
   eval $(minikube docker-env)
   ```
2. Build the optimized JVM release layer locally within the cluster execution space:
```bash
   docker build -t payment-service:latest .
   ```
3. **Define Production Secrets:** Before deploying the service pods, you **MUST** create the target Kubernetes database secret object named **`payment-db-secret`** inside your active namespace. This configuration block must define the following required parameter variables:
* `MONGO_DB_URI` — Full connection string including authorization credentials, port coordinates, target authentication source collection parameter keys, and the `replicaSet=rs0` identifier flag.
* `MONGO_INITDB_DATABASE` — Core document collection database context namespace moniker (e.g., `payment_db`).
* `MONGO_INITDB_ROOT_USERNAME` — Root administrator level database profile identifier access username.
* `MONGO_INITDB_ROOT_PASSWORD` — Cryptographic secure database access security key password.

4. Apply the configured resource manifests recursively across the target runtime parameters:
```bash
   kubectl apply -f . --recursive
   ```

*The cluster configuration continuously monitors routing performance using custom reactive OTel logging pipelines, guarded with active `livenessProbe` and `readinessProbe` checking routines running on internal container ports.*