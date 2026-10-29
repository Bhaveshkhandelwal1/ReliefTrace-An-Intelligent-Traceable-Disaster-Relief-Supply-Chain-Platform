package com.disasterreliefnet.common;

import com.disasterreliefnet.common.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleResourceNotFound() {
        ProblemDetail problem = handler.handleResourceNotFound(new ResourceNotFoundException("Donor", "123"));
        assertEquals(HttpStatus.NOT_FOUND.value(), problem.getStatus());
        assertEquals("Resource Not Found", problem.getTitle());
        assertNotNull(problem.getProperties().get("timestamp"));
    }

    @Test
    void shouldHandleConflict() {
        ProblemDetail problem = handler.handleConflict(new ConflictException("Email already exists"));
        assertEquals(HttpStatus.CONFLICT.value(), problem.getStatus());
        assertEquals("Conflict", problem.getTitle());
    }

    @Test
    void shouldHandleBusinessRuleViolation() {
        ProblemDetail problem = handler.handleBusinessRuleViolation(new BusinessRuleException("Insufficient inventory"));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY.value(), problem.getStatus());
        assertEquals("Unprocessable Entity", problem.getTitle());
    }

    @Test
    void shouldHandleUnauthorized() {
        ProblemDetail problem = handler.handleUnauthorized(new UnauthorizedException("Token expired"));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), problem.getStatus());
        assertEquals("Unauthorized", problem.getTitle());
    }

    @Test
    void shouldHandleForbidden() {
        ProblemDetail problem = handler.handleForbidden(new ForbiddenException("Action forbidden"));
        assertEquals(HttpStatus.FORBIDDEN.value(), problem.getStatus());
        assertEquals("Forbidden", problem.getTitle());
    }

    @Test
    void shouldHandleAccessDenied() {
        ProblemDetail problem = handler.handleAccessDenied(new AccessDeniedException("Forbidden"));
        assertEquals(HttpStatus.FORBIDDEN.value(), problem.getStatus());
        assertEquals("Forbidden", problem.getTitle());
    }

    @Test
    void shouldHandleGenericException() {
        ProblemDetail problem = handler.handleGenericException(new RuntimeException("Crash"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problem.getStatus());
        assertEquals("Internal Server Error", problem.getTitle());
    }
}
