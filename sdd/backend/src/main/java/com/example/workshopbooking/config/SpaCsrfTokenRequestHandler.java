package com.example.workshopbooking.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;

/**
 * Supports a JavaScript SPA that sends the raw token read from the CSRF cookie in a header,
 * while retaining Spring Security's XOR/BREACH handling for request attributes and parameters.
 */
@Component
public class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            Supplier<CsrfToken> csrfToken) {
        // Preserve BREACH protection for any token exposed as a request attribute.
        this.xor.handle(request, response, csrfToken);

        // Force Spring Security's deferred token to load so CookieCsrfTokenRepository can
        // set XSRF-TOKEN on an existing safe request when no token cookie is present.
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        CsrfTokenRequestHandler handler = StringUtils.hasText(headerValue) ? this.plain : this.xor;
        return handler.resolveCsrfTokenValue(request, csrfToken);
    }
}
