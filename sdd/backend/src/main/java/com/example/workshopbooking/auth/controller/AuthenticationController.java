package com.example.workshopbooking.auth.controller;

import com.example.workshopbooking.auth.dto.AuthenticatedAccountResponse;
import com.example.workshopbooking.auth.dto.LoginRequest;
import com.example.workshopbooking.auth.security.AccountPrincipal;
import com.example.workshopbooking.auth.service.AuthenticationService;
import com.example.workshopbooking.common.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            AccountPrincipal principal = this.authenticationService.login(loginRequest, request, response);
            return ResponseEntity.ok(AuthenticatedAccountResponse.from(principal));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiErrorResponse.of(
                    HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS",
                    "Email or password is incorrect.", request.getRequestURI()));
        }
    }

    @GetMapping("/me")
    public AuthenticatedAccountResponse currentUser(@AuthenticationPrincipal AccountPrincipal principal) {
        return AuthenticatedAccountResponse.from(principal);
    }
}
