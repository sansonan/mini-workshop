package com.example.workshopbooking.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;

class ApiExceptionHandlerTests {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void resourceNotFoundUsesStandard404ErrorContract() {
        HttpServletRequest request = request("/api/v1/instructor/workshops/404/registrations");

        var response = this.handler.handleResourceNotFound(
                new ResourceNotFoundException("Workshop was not found."), request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("RESOURCE_NOT_FOUND", response.getBody().code());
        assertEquals(request.getRequestURI(), response.getBody().path());
    }

    @Test
    void ownershipDenialUsesStandard403ForbiddenErrorContract() {
        HttpServletRequest request = request("/api/v1/instructor/workshops/42/registrations");

        var response = this.handler.handleForbidden(new AccessDeniedException("not assigned"), request);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("FORBIDDEN", response.getBody().code());
        assertEquals(request.getRequestURI(), response.getBody().path());
    }

    private static HttpServletRequest request(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(uri);
        return request;
    }
}
