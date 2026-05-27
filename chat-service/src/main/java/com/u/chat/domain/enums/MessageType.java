package com.u.chat.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum MessageType {
    text,
    image;

    @JsonValue
    public String toValue() {
        return name();
    }
}
