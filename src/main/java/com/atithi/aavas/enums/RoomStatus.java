package com.atithi.aavas.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomStatus {
    VACANT("VACANT"),
    PARTIALLY_FILLED("PARTIALLY FILLED"),
    FILLED("FILLED");

    private final String label;

    RoomStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static RoomStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return VACANT;
        }
        for (RoomStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)
                    || status.label.equalsIgnoreCase(value)
                    || status.name().replace("_", " ").equalsIgnoreCase(value)
                    || status.name().replace(" ", "_").equalsIgnoreCase(value)) {
                return status;
            }
        }
        return VACANT;
    }
}
