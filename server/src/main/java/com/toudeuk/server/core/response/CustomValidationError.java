package com.toudeuk.server.core.response;

import java.util.HashMap;
import java.util.Map;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

public class CustomValidationError {
    private Map<String, String> errors = new HashMap<>();

    public CustomValidationError(BindingResult bindingResult) {
        bindingResult.getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
    }
}
