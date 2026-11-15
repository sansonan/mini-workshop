package com.example.workshopbooking.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.workshopbooking.auth.entity.Account;
import com.example.workshopbooking.auth.repository.AccountRepository;
import com.example.workshopbooking.auth.security.AccountPrincipal;
import java.lang.reflect.Proxy;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccountUserDetailsServiceTests {

    @Test
    void normalizesEmailWithTrimAndLocaleRoot() {
        AccountRepository repository = repositoryReturning(email -> Optional.of(
                new Account(email, "encoded", "Admin", "ADMIN", true)));
        AccountUserDetailsService service = new AccountUserDetailsService(repository);

        AccountPrincipal result = (AccountPrincipal) service.loadUserByUsername("  I@EXAMPLE.COM  ");

        assertEquals("i@example.com", result.getUsername());
    }

    @Test
    void accountNotFoundUsesGenericUsernameNotFoundException() {
        AccountUserDetailsService service = new AccountUserDetailsService(repositoryReturning(email -> Optional.empty()));

        UsernameNotFoundException error = assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing@example.com"));

        assertEquals("Invalid credentials", error.getMessage());
    }

    @Test
    void inactiveAccountIsRejectedByDaoAuthenticationProvider() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash = encoder.encode("test-password");
        AccountRepository repository = repositoryReturning(email -> Optional.of(
                new Account(email, hash, "Inactive", "INSTRUCTOR", false)));
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(new AccountUserDetailsService(repository));
        provider.setPasswordEncoder(encoder);

        assertThrows(DisabledException.class, () -> provider.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated("inactive@example.com", "test-password")));
    }

    @Test
    void bcryptStrengthTwelveMatchesCorrectPasswordAndRejectsWrongPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash = encoder.encode("test-password");

        assertTrue(hash.startsWith("$2a$12$"));
        assertTrue(encoder.matches("test-password", hash));
        assertFalse(encoder.matches("wrong-password", hash));
        assertFalse(hash.equals("test-password"));
    }

    private static AccountRepository repositoryReturning(java.util.function.Function<String, Optional<Account>> find) {
        return (AccountRepository) Proxy.newProxyInstance(
                AccountRepository.class.getClassLoader(), new Class<?>[]{AccountRepository.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("findByEmail")) return find.apply((String) args[0]);
                    if (method.getName().equals("toString")) return "test account repository";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
