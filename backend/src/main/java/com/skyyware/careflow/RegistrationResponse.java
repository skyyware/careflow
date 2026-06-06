package com.skyyware.careflow;

import java.time.Instant;

public record RegistrationResponse(String status, boolean emailSent, Instant receivedAt) {
}
