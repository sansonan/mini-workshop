package com.example.workshopbooking.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;

class SecurityConfigurationTests {

    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    private CookieCsrfTokenRepository repository;
    private CsrfFilter csrfFilter;

    @BeforeEach
    void setUp() {
        this.repository = new SecurityConfig().csrfTokenRepository();
        this.csrfFilter = new CsrfFilter(this.repository);
        this.csrfFilter.setRequestHandler(new SpaCsrfTokenRequestHandler());
    }

    @Test
    void safeWorkshopGetMaterializesReadableCsrfCookieForHttp() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/workshops");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.csrfFilter.doFilter(request, response, testEndpoint());

        Cookie csrfCookie = response.getCookie(CSRF_COOKIE);
        assertNotNull(csrfCookie, "CSRF cookie was not returned");
        assertEquals("/", csrfCookie.getPath());
        assertEquals("Lax", csrfCookie.getAttribute("SameSite"));
        assertFalse(csrfCookie.isHttpOnly());
        assertFalse(csrfCookie.getSecure());
    }

    @Test
    void safeWorkshopGetUsesSecureCookieForHttpsRequest() throws Exception {
        MockHttpServletRequest request = request("GET", "/api/v1/workshops");
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.csrfFilter.doFilter(request, response, testEndpoint());

        Cookie csrfCookie = response.getCookie(CSRF_COOKIE);
        assertNotNull(csrfCookie, "CSRF cookie was not returned");
        assertTrue(csrfCookie.getSecure());
    }

    @Test
    void stateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
        MockHttpServletRequest request = request("POST", "/api/v1/test/csrf");
        MockHttpServletResponse response = new MockHttpServletResponse();

        this.csrfFilter.doFilter(request, response, testEndpoint());

        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());
    }

    @Test
    void stateChangingRequestAcceptsRawCookieTokenInApprovedHeader() throws Exception {
        MockHttpServletRequest getRequest = request("GET", "/api/v1/workshops");
        MockHttpServletResponse getResponse = new MockHttpServletResponse();
        this.csrfFilter.doFilter(getRequest, getResponse, testEndpoint());
        Cookie csrfCookie = getResponse.getCookie(CSRF_COOKIE);
        assertNotNull(csrfCookie, "CSRF cookie was not returned");
        String token = csrfCookie.getValue();

        MockHttpServletRequest postRequest = request("POST", "/api/v1/test/csrf");
        postRequest.setCookies(new Cookie(CSRF_COOKIE, token));
        postRequest.addHeader(CSRF_HEADER, token);
        MockHttpServletResponse postResponse = new MockHttpServletResponse();
        MockFilterChain chain = testEndpoint();

        this.csrfFilter.doFilter(postRequest, postResponse, chain);

        assertNotNull(chain.getRequest());
        assertEquals(HttpServletResponse.SC_OK, postResponse.getStatus());
    }

    private static MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }

    private static MockFilterChain testEndpoint() {
        // The servlet is test-only; no production endpoint is created for CSRF verification.
        return new MockFilterChain(new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response)
                    throws ServletException, IOException {
                response.setStatus(HttpServletResponse.SC_OK);
            }
        });
    }
}
