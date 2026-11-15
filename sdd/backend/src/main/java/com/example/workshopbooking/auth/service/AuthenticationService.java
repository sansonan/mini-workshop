package com.example.workshopbooking.auth.service;

import com.example.workshopbooking.auth.dto.LoginRequest;
import com.example.workshopbooking.auth.security.AccountPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfAuthenticationStrategy csrfAuthenticationStrategy;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            CsrfAuthenticationStrategy csrfAuthenticationStrategy,
            CsrfTokenRepository csrfTokenRepository) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.csrfAuthenticationStrategy = csrfAuthenticationStrategy;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    public AccountPrincipal login(LoginRequest loginRequest, HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = this.authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        loginRequest.email(), loginRequest.password()));

        // Rotate any pre-authentication session before persisting the authenticated context.
        this.sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        this.securityContextRepository.saveContext(context, request, response);

        // Authentication normally clears CSRF state through the filter success strategy.
        // This endpoint authenticates explicitly, so invoke the same lifecycle and then issue a new token.
        this.csrfAuthenticationStrategy.onAuthentication(authentication, request, response);
        CsrfToken freshToken = this.csrfTokenRepository.generateToken(request);
        this.csrfTokenRepository.saveToken(freshToken, request, response);

        if (!(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
            throw new AuthenticationServiceException("Unexpected authenticated principal type");
        }
        return principal;
    }
}
