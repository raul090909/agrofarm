package ru.agrofarm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище токенов авторизации. Токен — случайная строка из 256 бит,
 * привязанная к роли и идентификатору субъекта, с ограниченным сроком жизни.
 */
@Service
public class TokenService {

    public static final String ROLE_USER = "user";
    public static final String ROLE_AGRONOMIST = "agronomist";

    /** Данные сессии, связанной с токеном. */
    public record Session(String token, String role, Long subjectId, Instant expiresAt) {}

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final Duration ttl;

    public TokenService(@Value("${agrofarm.auth.token-ttl-hours:12}") long ttlHours) {
        this.ttl = Duration.ofHours(ttlHours);
    }

    public Session issue(String role, Long subjectId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Session session = new Session(token, role, subjectId, Instant.now().plus(ttl));
        sessions.put(token, session);
        return session;
    }

    public Optional<Session> find(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        Session s = sessions.get(token);
        if (s == null) return Optional.empty();
        if (s.expiresAt().isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(s);
    }

    public void revoke(String token) {
        if (token != null) sessions.remove(token);
    }
}
