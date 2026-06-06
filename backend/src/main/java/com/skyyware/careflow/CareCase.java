package com.skyyware.careflow;

import java.time.Instant;
import java.util.List;

public record CareCase(
    String id,
    String patientCode,
    String ward,
    String pathway,
    String priority,
    int slaMinutes,
    String nextAction,
    String owner,
    String status,
    int readinessScore,
    List<String> riskFlags,
    List<ChecklistItem> checklist,
    MedicationReview medicationReview,
    LabSignal labSignal,
    List<ClinicalNote> notes,
    List<CareEvent> timeline,
    Instant updatedAt
) {
}
