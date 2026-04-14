package com.youssef.ecomeraauthservice.token;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TokenType {
    ACCESS("ACCESS"),
    REFRESH("REFRESH");

    @JsonValue
    private final String value;
}