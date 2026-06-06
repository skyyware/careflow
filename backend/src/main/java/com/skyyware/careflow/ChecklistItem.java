package com.skyyware.careflow;

public record ChecklistItem(
    String id,
    String label,
    boolean done,
    String owner
) {
}
