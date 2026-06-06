# Architecture

CareFlow is intentionally small, but structured like a maintainable enterprise application.

## Frontend

The Angular app is a single operational screen with local UI state managed through signals. It keeps the workflow fast and readable:

- `cases`, `status` and selected case are loaded from the REST API.
- Ward, priority and search filters are derived state.
- Clinical notes are posted to the backend and the selected case is refreshed.
- Fallback data keeps the screen usable during local frontend development without hiding API status.

## Backend

The backend exposes a Jakarta REST API with Quarkus:

- `CareFlowResource` owns the HTTP surface.
- `CareFlowStore` owns the de-identified reference data and update behavior.
- Java records model immutable case, event, lab, medication and checklist data.
- Bean validation rejects empty clinical notes.
- Quarkus health endpoints support container and Kubernetes probes.

The current persistence layer is intentionally in-memory because this implementation focuses on workflow, API contract and deployment. A database adapter can be introduced behind the same store boundary without changing the UI contract.

## Event stream

Timeline entries model Kafka-style clinical events. A real broker integration can be added behind an event publisher interface while keeping the API payload stable. The Docker Compose file includes an optional Redpanda profile for local event-stream experiments.

## Data safety

The application contains no real patient data. Every patient-like identifier is de-identified and intentionally limited to internal case codes such as `CF-1024`.

## Stage isolation

The stage deployment uses a dedicated port, service, vhost and release repository. It does not replace existing stage applications, install global server packages or reuse another application's runtime directory.
