package com.skyyware.careflow;

public record MedicationReview(
    String status,
    int openInteractions,
    String lastReviewedBy,
    String nextAction
) {
}
