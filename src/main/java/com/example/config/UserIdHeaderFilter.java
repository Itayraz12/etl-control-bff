package com.example.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class UserIdHeaderFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-user-id";
    public static final String MDC_KEY = "userId";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
            || !requestUri.startsWith("/api/")
            || "/api/auth/login".equals(requestUri);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String userId = request.getHeader(HEADER_NAME);
        if (!StringUtils.hasText(userId)) {
            if (request.getRequestURI().startsWith("/api/backend/admin/")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"message\":\"Unauthenticated user\"}");
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                    "Missing required header '" + HEADER_NAME + "'");
            }
            return;
        }

        MDC.put(MDC_KEY, userId);
        try {
            System.out.printf("Request received [userId=%s, method=%s, path=%s]%n",
                userId, request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}

