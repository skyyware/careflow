package com.skyyware.careflow;

public record LabSignal(
    String status,
    int pendingResults,
    boolean criticalFlag,
    String nextReview
) {
}
