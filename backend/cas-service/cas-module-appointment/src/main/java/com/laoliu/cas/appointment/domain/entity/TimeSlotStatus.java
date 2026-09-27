package com.laoliu.cas.appointment.domain.entity;

/** Persisted time-slot state. The booking flow continues to use its existing boolean contract. */
public enum TimeSlotStatus {
    SCHEDULED(1),
    OCCUPIED(0),
    CANCELLED(-1);

    private final int code;

    TimeSlotStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
