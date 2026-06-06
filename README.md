# CareFlow

CareFlow is a compact healthcare workflow cockpit for discharge readiness, clinical coordination and event auditability. The app uses synthetic case data only; it does not contain real patient information.

It was built as a focused proof-of-capability for an Angular / JavaEE-style healthcare project:

- Angular 22 with standalone components and signals
- Jakarta REST API on Quarkus 3 / Java 21
- Bean validation, health endpoints and integration-style API tests
- Docker image and Kubernetes manifests
- Git-based stage deployment as an isolated service

## Product slice

CareFlow helps a team see what blocks the next clinical step:

- Prioritized clinical worklist with ward, priority and search filters
- Discharge readiness score and checklist
- Medication review and lab dependencies
- Structured clinical note creation through the API
- Audit timeline for Kafka-ready clinical events

The data model intentionally uses synthetic case identifiers such as `CF-1024` instead of real patient data.

## Local development

Required runtimes:

- Node.js 26
- Java 21
- Maven 3.9+

Install dependencies:

```bash
npm run install:all
```

Run the backend:

```bash
npm run backend:dev
```

Run the frontend:

```bash
npm run frontend:dev
```

The Angular development server proxies `/api` and `/q` to the Quarkus backend.

## Build and test

```bash
npm run check
npm run build
```

`npm run build` compiles Angular, copies the static frontend into the Quarkus resources folder and packages the Java application.

## API

```http
GET /api/status
GET /api/cases
GET /api/cases/{caseId}
POST /api/cases/{caseId}/notes
GET /api/events
GET /q/health
```

Example note payload:

```json
{
  "author": "Care coordination",
  "body": "Medication reconciliation requested before discharge."
}
```

## Docker

```bash
docker compose up --build
```

The default container runs the full Angular + Quarkus application on `http://localhost:8080`. The optional `event-stream` profile starts a local Redpanda broker for integration experiments:

```bash
docker compose --profile event-stream up --build
```

## Kubernetes

The minimal manifests live in `deploy/k8s`.

```bash
kubectl apply -f deploy/k8s/
```

## Stage deployment

Stage deployment is deliberately isolated:

- subdomain: `careflow.stage.dev`
- service: `careflow.service`
- port: `8097`
- release repository: `/srv/git/careflow-release.git`
- runtime directory: `/srv/www/careflow.stage.dev/current`

Deploy after the server-side bare repository and hook exist:

```bash
npm run deploy:stage
```

The release repository contains only the packaged Quarkus application, not the development tree.
