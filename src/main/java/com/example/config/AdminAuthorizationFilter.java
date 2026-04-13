package com.example.config;

import com.example.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class AdminAuthorizationFilter extends OncePerRequestFilter {

    private final AuthService authService;

    public AdminAuthorizationFilter(AuthService authService) {
        this.authService = authService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod())
            || !request.getRequestURI().startsWith("/api/backend/admin/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(UserIdHeaderFilter.HEADER_NAME);
        if (!StringUtils.hasText(userId) || !authService.isAuthenticatedUser(userId)) {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthenticated user");
            return;
        }
        if (!authService.isAdminUser(userId)) {
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Admin access is required");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
}
