package com.isaru66.model;

import com.fasterxml.jackson.annotation.JsonProperty;

// Validation response class
public class ValidationResponse {
    @JsonProperty("validationResponse")
    private String validationResponse;

    public ValidationResponse(String validationCode) {
        this.validationResponse = validationCode;
    }
}