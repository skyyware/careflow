package com.skyyware.careflow;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class CareFlowStore {
    private final Map<String, CareCase> cases = new ConcurrentHashMap<>();

    public CareFlowStore() {
        seed();
    }

    public PlatformStatus status() {
        List<CareCase> current = cases();
        int highPriority = (int) current.stream().filter(careCase -> priorityRank(careCase.priority()) <= 2).count();
        int pendingLabs = current.stream().mapToInt(careCase -> careCase.labSignal().pendingResults()).sum();
        return new PlatformStatus(
            "ready",
            current.size(),
            highPriority,
            pendingLabs,
            "kafka-ready clinical event feed",
            "kubernetes-ready",
            Instant.now()
        );
    }

    public List<CareCase> cases() {
        return cases.values().stream()
            .sorted(Comparator.comparingInt((CareCase careCase) -> priorityRank(careCase.priority()))
                .thenComparingInt(CareCase::slaMinutes))
            .toList();
    }

    public Optional<CareCase> findCase(String id) {
        return Optional.ofNullable(cases.get(id));
    }

    public List<CareEvent> events() {
        return cases().stream()
            .flatMap(careCase -> careCase.timeline().stream())
            .sorted(Comparator.comparing(CareEvent::createdAt).reversed())
            .limit(20)
            .toList();
    }

    public ClinicalNote addNote(String caseId, CreateNoteRequest request) {
        CareCase current = findCase(caseId).orElseThrow(() -> new CareCaseNotFoundException(caseId));
        Instant now = Instant.now();
        ClinicalNote note = new ClinicalNote(UUID.randomUUID().toString(), request.author(), request.body(), now);
        CareEvent event = new CareEvent(
            UUID.randomUUID().toString(),
            caseId,
            "NOTE_ADDED",
            "Clinical note added by " + request.author(),
            "careflow-ui",
            now
        );

        List<ClinicalNote> notes = new ArrayList<>(current.notes());
        notes.add(0, note);
        List<CareEvent> timeline = new ArrayList<>(current.timeline());
        timeline.add(0, event);

        cases.put(caseId, copy(current, notes, timeline, now));
        return note;
    }

    private CareCase copy(CareCase current, List<ClinicalNote> notes, List<CareEvent> timeline, Instant updatedAt) {
        return new CareCase(
            current.id(),
            current.patientCode(),
            current.ward(),
            current.pathway(),
            current.priority(),
            current.slaMinutes(),
            current.nextAction(),
            current.owner(),
            current.status(),
            current.readinessScore(),
            current.riskFlags(),
            current.checklist(),
            current.medicationReview(),
            current.labSignal(),
            List.copyOf(notes),
            List.copyOf(timeline),
            updatedAt
        );
    }

    private int priorityRank(String priority) {
        return switch (priority.toLowerCase()) {
            case "critical" -> 1;
            case "high" -> 2;
            case "medium" -> 3;
            default -> 4;
        };
    }

    private void seed() {
        Instant now = Instant.now();
        add(new CareCase(
            "case-discharge-1024",
            "CF-1024",
            "Cardiology B",
            "Discharge readiness",
            "High",
            42,
            "Medication reconciliation before discharge order",
            "Dr. Lena Hoffmann",
            "Awaiting clinical review",
            76,
            List.of("pending labs", "medication interaction", "home-care slot"),
            List.of(
                new ChecklistItem("task-medication", "Medication reconciliation completed", false, "Pharmacy"),
                new ChecklistItem("task-labs", "Final troponin result reviewed", false, "Lab"),
                new ChecklistItem("task-care", "Home-care appointment confirmed", true, "Care coordination"),
                new ChecklistItem("task-summary", "Discharge summary drafted", true, "Resident physician")
            ),
            new MedicationReview("Needs review", 2, "Pharmacy Team", "Resolve ACE inhibitor interaction"),
            new LabSignal("Pending", 1, false, "Review final troponin before 14:00"),
            List.of(new ClinicalNote(
                UUID.randomUUID().toString(),
                "Care coordination",
                "Family pickup confirmed. Waiting for medication reconciliation and final lab result.",
                now.minusSeconds(900)
            )),
            List.of(
                new CareEvent(UUID.randomUUID().toString(), "case-discharge-1024", "FHIR_UPDATE", "Lab result pending flag synced", "integration-adapter", now.minusSeconds(720)),
                new CareEvent(UUID.randomUUID().toString(), "case-discharge-1024", "RISK_FLAG", "Medication interaction detected", "medication-service", now.minusSeconds(540)),
                new CareEvent(UUID.randomUUID().toString(), "case-discharge-1024", "KAFKA_EVENT", "Discharge readiness event published", "clinical-events", now.minusSeconds(300))
            ),
            now.minusSeconds(180)
        ));

        add(new CareCase(
            "case-oncology-2048",
            "CF-2048",
            "Oncology Day Unit",
            "Therapy appointment",
            "Medium",
            95,
            "Confirm infusion chair and latest blood count",
            "Nurse Lead Weber",
            "Planning",
            68,
            List.of("capacity", "lab dependency"),
            List.of(
                new ChecklistItem("task-chair", "Infusion chair reserved", false, "Scheduling"),
                new ChecklistItem("task-blood", "Blood count reviewed", false, "Oncology"),
                new ChecklistItem("task-consent", "Consent document available", true, "Administration")
            ),
            new MedicationReview("Clear", 0, "Oncology Pharmacy", "No action required"),
            new LabSignal("Pending", 2, false, "CBC expected by 11:30"),
            List.of(),
            List.of(
                new CareEvent(UUID.randomUUID().toString(), "case-oncology-2048", "WORKLIST", "Case moved to planning queue", "careflow-ui", now.minusSeconds(1200)),
                new CareEvent(UUID.randomUUID().toString(), "case-oncology-2048", "KAFKA_EVENT", "Therapy appointment event published", "clinical-events", now.minusSeconds(600))
            ),
            now.minusSeconds(240)
        ));

        add(new CareCase(
            "case-er-3091",
            "CF-3091",
            "Emergency",
            "Observation handover",
            "Critical",
            18,
            "Escalate bed assignment and complete handover note",
            "Dr. Martin Schulz",
            "Escalated",
            51,
            List.of("bed capacity", "handover overdue"),
            List.of(
                new ChecklistItem("task-bed", "Observation bed assigned", false, "Bed management"),
                new ChecklistItem("task-handover", "Handover note completed", false, "Emergency physician"),
                new ChecklistItem("task-contact", "Ward contact informed", true, "Nursing")
            ),
            new MedicationReview("Not started", 1, "Emergency Pharmacy", "Review anticoagulant history"),
            new LabSignal("Critical review", 1, true, "Doctor review required now"),
            List.of(new ClinicalNote(UUID.randomUUID().toString(), "Triage", "Patient-code CF-3091 requires immediate observation handover. No direct identifiers stored in this workspace.", now.minusSeconds(360))),
            List.of(
                new CareEvent(UUID.randomUUID().toString(), "case-er-3091", "ALERT", "Critical lab review required", "lab-adapter", now.minusSeconds(330)),
                new CareEvent(UUID.randomUUID().toString(), "case-er-3091", "SLA", "Handover SLA under 20 minutes", "workflow-engine", now.minusSeconds(240))
            ),
            now.minusSeconds(90)
        ));
    }

    private void add(CareCase careCase) {
        cases.put(careCase.id(), careCase);
    }
}
