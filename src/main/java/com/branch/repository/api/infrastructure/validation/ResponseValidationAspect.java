package com.branch.repository.api.infrastructure.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * AOP Aspect that automatically validates response objects from REST controllers.
 * This ensures all response DTOs meet their validation constraints before being sent to clients.
 */
@Aspect
@Component
public class ResponseValidationAspect {

    private static final Logger log = LoggerFactory.getLogger(ResponseValidationAspect.class);

    private final Validator validator;

    public ResponseValidationAspect(Validator validator) {
        this.validator = validator;
    }

    /**
     * Validates ResponseEntity bodies after controller methods return.
     * Applies to all methods in REST controller packages.
     */
    @AfterReturning(
        pointcut = "execution(* com.branch.repository.api..adapter.in.rest..*Controller.*(..))",
        returning = "result"
    )
    public void validateResponse(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            Object body = responseEntity.getBody();
            if (body != null) {
                Set<ConstraintViolation<Object>> violations = validator.validate(body);
                if (!violations.isEmpty()) {
                    log.error("Response validation failed with {} violations", violations.size());
                    violations.forEach(violation ->
                        log.error("Validation error: {} - {}", violation.getPropertyPath(), violation.getMessage())
                    );
                    throw new ConstraintViolationException("Response validation failed", violations);
                }
            }
        }
    }
}