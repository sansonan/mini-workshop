package com.example.workshopbooking.auth.dto;

import com.example.workshopbooking.auth.security.AccountPrincipal;

public record AuthenticatedAccountResponse(
        Long accountId,
        String email,
        String displayName,
        String role) {

    public static AuthenticatedAccountResponse from(AccountPrincipal principal) {
        return new AuthenticatedAccountResponse(
                principal.getAccountId(), principal.getUsername(), principal.getDisplayName(), principal.getRole());
    }
}
