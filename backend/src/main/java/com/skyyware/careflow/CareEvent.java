package com.skyyware.careflow;

import java.time.Instant;

public record CareEvent(
    String id,
    String caseId,
    String type,
    String summary,
    String source,
    Instant createdAt
) {
}
