package com.sms.SubscriptionService.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    ACTIVE("Active"),
    INACTIVE("InActive");

    private final String dbStatus;

    Status(String dbStatus) {
        this.dbStatus = dbStatus;
    }

    @JsonValue
    public String getDbStatus() {
        return dbStatus;
    }

    @JsonCreator
    public static Status fromString(String dbStatus) {
        for (Status status : Status.values()) {
            if (status.name().equalsIgnoreCase(dbStatus)) {
                return status;
            }
        }
        throw new IllegalArgumentException("No enum constant found for dbStatus: " + dbStatus);
    }
}
