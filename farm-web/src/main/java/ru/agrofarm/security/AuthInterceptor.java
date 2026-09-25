package ru.agrofarm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String SESSION_ATTR = "farm.session";

    private static final Set<String> PUBLIC = Set.of(
            "/api/auth/register", "/api/auth/login", "/api/agronomist/login");

    private final TokenService tokens;
    private final ObjectMapper mapper;

    public AuthInterceptor(TokenService tokens, ObjectMapper mapper) {
        this.tokens = tokens;
        this.mapper = mapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) return true;
        String path = req.getRequestURI();
        if (PUBLIC.contains(path)) return true;

        Optional<TokenService.Session> session = tokens.find(extractToken(req));
        if (session.isEmpty()) {
            return reject(res, 401, "Требуется авторизация");
        }

        boolean agronomistArea = path.startsWith("/api/agronomist") || path.startsWith("/export");
        boolean anyRole = path.equals("/api/auth/logout");
        String required = agronomistArea ? TokenService.ROLE_AGRONOMIST : TokenService.ROLE_USER;
        if (!anyRole && !required.equals(session.get().role())) {
            return reject(res, 403, "Недостаточно прав для этого действия");
        }
        req.setAttribute(SESSION_ATTR, session.get());
        return true;
    }

    public static String extractToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return header.substring(7).trim();
        }
        return null;
    }

    private boolean reject(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        mapper.writeValue(res.getWriter(), Map.of("error", message));
        return false;
    }
}
