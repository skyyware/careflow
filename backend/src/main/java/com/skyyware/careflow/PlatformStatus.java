package com.skyyware.careflow;

import java.time.Instant;

public record PlatformStatus(
    String status,
    int openCases,
    int highPriorityCases,
    int pendingLabResults,
    String eventStream,
    String deployment,
    Instant generatedAt
) {
}
