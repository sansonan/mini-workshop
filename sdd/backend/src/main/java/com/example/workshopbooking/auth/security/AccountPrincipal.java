package com.example.workshopbooking.auth.security;

import com.example.workshopbooking.auth.entity.Account;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public final class AccountPrincipal implements UserDetails {

    private final Long accountId;
    private final String email;
    private final String passwordHash;
    private final String displayName;
    private final String role;
    private final boolean active;

    public AccountPrincipal(Account account) {
        this.accountId = account.getAccountId();
        this.email = account.getEmail();
        this.passwordHash = account.getPasswordHash();
        this.displayName = account.getDisplayName();
        this.role = account.getRole();
        this.active = account.isActive();
    }

    public Long getAccountId() { return accountId; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return passwordHash; }
    @Override public boolean isEnabled() { return active; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
