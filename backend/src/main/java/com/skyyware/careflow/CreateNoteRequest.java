package com.skyyware.careflow;

import jakarta.validation.constraints.NotBlank;

public record CreateNoteRequest(
    @NotBlank String author,
    @NotBlank String body
) {
}
