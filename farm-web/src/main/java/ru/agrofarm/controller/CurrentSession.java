package ru.agrofarm.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.agrofarm.security.AuthInterceptor;
import ru.agrofarm.security.TokenService;

/** Доступ к сессии, которую AuthInterceptor положил в запрос. */
final class CurrentSession {

    private CurrentSession() {}

    static Long subjectId(HttpServletRequest req) {
        Object s = req.getAttribute(AuthInterceptor.SESSION_ATTR);
        if (!(s instanceof TokenService.Session session)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Требуется авторизация");
        }
        return session.subjectId();
    }
}
