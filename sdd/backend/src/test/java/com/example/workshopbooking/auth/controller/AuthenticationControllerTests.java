package com.example.workshopbooking.auth.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.workshopbooking.auth.entity.Account;
import com.example.workshopbooking.auth.repository.AccountRepository;
import com.example.workshopbooking.auth.service.AccountUserDetailsService;
import com.example.workshopbooking.auth.service.AuthenticationService;
import com.example.workshopbooking.common.ApiExceptionHandler;
import com.example.workshopbooking.config.PasswordEncoderConfig;
import com.example.workshopbooking.config.SecurityConfig;
import com.example.workshopbooking.config.SpaCsrfTokenRequestHandler;
import jakarta.servlet.http.Cookie;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(AuthenticationController.class)
@Import({
    SecurityConfig.class,
    SpaCsrfTokenRequestHandler.class,
    AccountUserDetailsService.class,
    AuthenticationService.class,
    PasswordEncoderConfig.class,
    ApiExceptionHandler.class,
    AuthenticationControllerTests.TestRepositories.class
})
class AuthenticationControllerTests {

    private static final String EMAIL = "admin@example.com";
    private static final String PASSWORD = "correct-password";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${server.servlet.session.timeout}")
    private Duration sessionTimeout;

    @Value("${server.servlet.session.cookie.http-only}")
    private boolean sessionCookieHttpOnly;

    @Value("${server.servlet.session.cookie.same-site}")
    private String sessionCookieSameSite;

    @Value("${server.servlet.session.cookie.secure}")
    private boolean sessionCookieSecure;

    private String passwordHash;

    private static final Map<String, Account> ACCOUNTS = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        this.passwordHash = this.passwordEncoder.encode(PASSWORD);
        ACCOUNTS.clear();
        ACCOUNTS.put(EMAIL, account(this.passwordHash, true));
    }

    @Test
    void loginCreatesSessionReturnsOnlyApprovedAccountFieldsAndRefreshesCsrf() throws Exception {
        Cookie initialCsrf = obtainCsrfCookie();
        MockHttpSession preAuthenticationSession = new MockHttpSession();
        String previousSessionId = preAuthenticationSession.getId();

        MvcResult login = this.mockMvc.perform(post("/api/v1/auth/login")
                        .session(preAuthenticationSession)
                        .cookie(initialCsrf)
                        .header(CSRF_HEADER, initialCsrf.getValue())
                        .contentType("application/json")
                        .content("""
                                {"email":"  ADMIN@EXAMPLE.COM ","password":"correct-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(17))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.displayName").value("Workshop Admin"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.sessionId").doesNotExist())
                .andReturn();

        MockHttpSession authenticatedSession = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(authenticatedSession);
        assertNotEquals(previousSessionId, authenticatedSession.getId(), "Login must rotate the session ID");
        Cookie refreshedCsrf = lastCookie(login, CSRF_COOKIE);
        assertNotNull(refreshedCsrf);
        assertNotEquals(initialCsrf.getValue(), refreshedCsrf.getValue());

        this.mockMvc.perform(get("/api/v1/auth/me").session(authenticatedSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(17))
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    @Test
    void invalidPasswordAndMissingAccountReturnSameGenericUnauthorizedError() throws Exception {
        Cookie csrf = obtainCsrfCookie();
        this.mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType("application/json")
                        .content("{\"email\":\"admin@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());

        ACCOUNTS.remove("unknown@example.com");
        this.mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType("application/json")
                        .content("{\"email\":\"unknown@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void inactiveAccountCannotLogin() throws Exception {
        ACCOUNTS.put(EMAIL, account(this.passwordHash, false));
        Cookie csrf = obtainCsrfCookie();

        this.mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType("application/json")
                        .content("{\"email\":\"admin@example.com\",\"password\":\"correct-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void currentUserRequiresAuthentication() throws Exception {
        this.mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void logoutInvalidatesSessionAndReturnsFreshCsrfToken() throws Exception {
        Cookie initialCsrf = obtainCsrfCookie();
        MvcResult login = login(initialCsrf, null);
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        Cookie loginCsrf = lastCookie(login, CSRF_COOKIE);
        assertNotNull(loginCsrf);

        this.mockMvc.perform(post("/api/v1/auth/logout").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_REJECTED"));
        assertFalse(session.isInvalid(), "A rejected logout must not invalidate the session");

        MvcResult logout = this.mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .cookie(loginCsrf)
                        .header(CSRF_HEADER, loginCsrf.getValue()))
                .andExpect(status().isNoContent())
                .andReturn();

        assertTrue(session.isInvalid());
        assertTrue(logout.getResponse().getHeaders("Set-Cookie").stream()
                .anyMatch(value -> value.startsWith("JSESSIONID=") && value.contains("Max-Age=0")));
        Cookie logoutCsrf = lastCookie(logout, CSRF_COOKIE);
        assertNotNull(logoutCsrf);
        assertNotEquals(loginCsrf.getValue(), logoutCsrf.getValue());
        this.mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginAndLogoutRejectMissingOrInvalidCsrfHeader() throws Exception {
        this.mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"admin@example.com\",\"password\":\"correct-password\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_REJECTED"));

        Cookie csrf = obtainCsrfCookie();
        this.mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf).header(CSRF_HEADER, "stale-token")
                        .contentType("application/json")
                        .content("{\"email\":\"admin@example.com\",\"password\":\"correct-password\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_REJECTED"));
    }

    @Test
    void validatesLoginRequestUsingStandardFieldErrorContract() throws Exception {
        Cookie csrf = obtainCsrfCookie();
        this.mockMvc.perform(post("/api/v1/auth/login")
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType("application/json")
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void sessionTimeoutAndCookiePolicyAreConfiguredForDevelopmentHttp() {
        assertEquals(Duration.ofMinutes(30), this.sessionTimeout);
        assertTrue(this.sessionCookieHttpOnly);
        assertEquals("lax", this.sessionCookieSameSite);
        assertFalse(this.sessionCookieSecure);
    }

    @Test
    void visitorCanReachOnlyPublicWorkshopAndRegistrationRoutes() throws Exception {
        this.mockMvc.perform(get("/api/v1/workshops"))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/v1/workshops/42"))
                .andExpect(status().isNotFound());

        this.mockMvc.perform(post("/api/v1/workshops/42/registrations")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_REJECTED"));

        Cookie csrf = obtainCsrfCookie();
        this.mockMvc.perform(post("/api/v1/workshops/42/registrations")
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminRouteFamilyRequiresAdminRole() throws Exception {
        var instructor = SecurityMockMvcRequestPostProcessors.user("instructor").roles("INSTRUCTOR");
        var admin = SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN");
        Cookie csrf = obtainCsrfCookie();

        this.mockMvc.perform(get("/api/v1/admin/workshops").with(instructor))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        this.mockMvc.perform(get("/api/v1/admin/workshops").with(admin))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(post("/api/v1/admin/workshops").with(instructor)
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        this.mockMvc.perform(post("/api/v1/admin/workshops").with(admin)
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(put("/api/v1/admin/workshops/42").with(instructor)
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        this.mockMvc.perform(put("/api/v1/admin/workshops/42").with(admin)
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(delete("/api/v1/admin/workshops/42").with(instructor)
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        this.mockMvc.perform(delete("/api/v1/admin/workshops/42").with(admin)
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue()))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/v1/admin/workshops/42/registrations").with(instructor))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        this.mockMvc.perform(get("/api/v1/admin/workshops/42/registrations").with(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void instructorRouteFamilyRequiresInstructorRoleWithoutOwnershipChecks() throws Exception {
        var admin = SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN");
        var instructor = SecurityMockMvcRequestPostProcessors.user("instructor").roles("INSTRUCTOR");

        this.mockMvc.perform(get("/api/v1/instructor/workshops").with(admin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        this.mockMvc.perform(get("/api/v1/instructor/workshops").with(instructor))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/v1/instructor/workshops/42/registrations").with(instructor))
                .andExpect(status().isNotFound());
        this.mockMvc.perform(get("/api/v1/instructor/workshops/42/registrations").with(admin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void protectedAndUnmatchedRoutesReturnUnauthenticatedJson() throws Exception {
        Cookie csrf = obtainCsrfCookie();
        this.mockMvc.perform(get("/api/v1/admin/workshops"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        this.mockMvc.perform(get("/api/v1/instructor/workshops"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        this.mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(csrf).header(CSRF_HEADER, csrf.getValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        this.mockMvc.perform(get("/api/v1/not-declared"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        this.mockMvc.perform(get("/api/v1/not-declared")
                        .with(SecurityMockMvcRequestPostProcessors.user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
    }

    private Cookie obtainCsrfCookie() throws Exception {
        MvcResult result = this.mockMvc.perform(get("/api/v1/workshops")).andReturn();
        Cookie csrf = result.getResponse().getCookie(CSRF_COOKIE);
        assertNotNull(csrf, "The safe public request must materialize the CSRF cookie");
        assertNotNull(csrf.getValue());
        return csrf;
    }

    private static Cookie lastCookie(MvcResult result, String name) {
        Cookie matching = null;
        for (Cookie cookie : result.getResponse().getCookies()) {
            if (cookie.getName().equals(name)) matching = cookie;
        }
        return matching;
    }

    private MvcResult login(Cookie csrf, MockHttpSession session) throws Exception {
        var request = post("/api/v1/auth/login")
                .cookie(csrf).header(CSRF_HEADER, csrf.getValue())
                .contentType("application/json")
                .content("{\"email\":\"admin@example.com\",\"password\":\"correct-password\"}");
        if (session != null) request.session(session);
        return this.mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    }

    private static Account account(String hash, boolean active) {
        Account account = new Account(EMAIL, hash, active ? "Workshop Admin" : "Inactive Admin", "ADMIN", active);
        ReflectionTestUtils.setField(account, "accountId", 17L);
        return account;
    }

    @TestConfiguration
    static class TestRepositories {
        @Bean
        @Primary
        AccountRepository accountRepository() {
            return (AccountRepository) Proxy.newProxyInstance(
                    AccountRepository.class.getClassLoader(), new Class<?>[]{AccountRepository.class},
                    (proxy, method, args) -> {
                        if (method.getName().equals("findByEmail")) {
                            return Optional.ofNullable(ACCOUNTS.get((String) args[0]));
                        }
                        if (method.getName().equals("toString")) return "in-memory account repository";
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == args[0];
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
