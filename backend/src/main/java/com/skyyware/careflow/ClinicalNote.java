package com.skyyware.careflow;

import java.time.Instant;

public record ClinicalNote(
    String id,
    String author,
    String body,
    Instant createdAt
) {
}
