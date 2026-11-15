package com.example.workshopbooking.config;

import com.example.workshopbooking.common.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    AuthenticationProvider accountAuthenticationProvider(
            UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    AuthenticationManager accountAuthenticationManager(AuthenticationProvider accountAuthenticationProvider) {
        return new ProviderManager(accountAuthenticationProvider);
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new ChangeSessionIdAuthenticationStrategy();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfAuthenticationStrategy csrfAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
        return new CsrfAuthenticationStrategy(csrfTokenRepository);
    }

    @Bean
    CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieName("XSRF-TOKEN");
        repository.setHeaderName("X-XSRF-TOKEN");
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));
        // If Secure is not explicitly configured, the repository follows request.isSecure():
        // false for the local HTTP proxy and true for HTTPS requests in production.
        return repository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieCsrfTokenRepository csrfTokenRepository,
            SpaCsrfTokenRequestHandler csrfTokenRequestHandler,
            AuthenticationProvider accountAuthenticationProvider,
            AuthenticationManager accountAuthenticationManager,
            SecurityContextRepository securityContextRepository,
            ObjectMapper objectMapper)
            throws Exception {
        http.csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfTokenRequestHandler));

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/api/v1/workshops", "/api/v1/workshops/*")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/workshops/*/registrations")
                .permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login")
                .permitAll()
                .requestMatchers("/api/v1/auth/me", "/api/v1/auth/logout")
                .authenticated()
                .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")
                .requestMatchers("/api/v1/instructor/**")
                .hasRole("INSTRUCTOR")
                .anyRequest()
                .authenticated());

        // This is a REST API; do not enable Spring Security's default HTML login form or HTTP Basic.
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.authenticationProvider(accountAuthenticationProvider);
        http.authenticationManager(accountAuthenticationManager);
        http.securityContext(security -> security
                .securityContextRepository(securityContextRepository)
                .requireExplicitSave(true));
        http.sessionManagement(session -> session
                .sessionFixation(sessionFixation -> sessionFixation.changeSessionId()));
        http.logout(logout -> logout
                .logoutRequestMatcher(authenticatedLogoutRequestMatcher())
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler(csrfRefreshingLogoutSuccessHandler(csrfTokenRepository)));
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jsonAuthenticationEntryPoint(objectMapper))
                .accessDeniedHandler(jsonAccessDeniedHandler(objectMapper)));

        return http.build();
    }

    private static LogoutSuccessHandler csrfRefreshingLogoutSuccessHandler(CsrfTokenRepository csrfTokenRepository) {
        return (request, response, authentication) -> {
            CsrfToken freshToken = csrfTokenRepository.generateToken(request);
            csrfTokenRepository.saveToken(freshToken, request, response);
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
        };
    }

    private static RequestMatcher authenticatedLogoutRequestMatcher() {
        return request -> {
            String logoutPath = request.getContextPath() + "/api/v1/auth/logout";
            if (!HttpMethod.POST.matches(request.getMethod()) || !logoutPath.equals(request.getRequestURI())) {
                return false;
            }
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken);
        };
    }

    private static AuthenticationEntryPoint jsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        return (request, response, exception) -> writeError(
                objectMapper, response, HttpServletResponse.SC_UNAUTHORIZED,
                "UNAUTHENTICATED", "Authentication is required.", request.getRequestURI());
    }

    private static AccessDeniedHandler jsonAccessDeniedHandler(ObjectMapper objectMapper) {
        return (request, response, exception) -> {
            boolean csrfFailure = exception instanceof CsrfException;
            writeError(
                    objectMapper, response, HttpServletResponse.SC_FORBIDDEN,
                    csrfFailure ? "CSRF_REJECTED" : "FORBIDDEN",
                    csrfFailure ? "The request could not be verified." : "You do not have permission to access this resource.",
                    request.getRequestURI());
        };
    }

    private static void writeError(
            ObjectMapper objectMapper, HttpServletResponse response, int status,
            String code, String message, String path) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(status, code, message, path));
    }
}
