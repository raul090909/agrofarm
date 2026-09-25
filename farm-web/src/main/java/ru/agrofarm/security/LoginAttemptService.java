package ru.agrofarm.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginAttemptService {

    private static final class Attempt {
        int failures;
        Instant firstFailure;
        Instant lockedUntil;
    }

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockTime;

    public LoginAttemptService(@Value("${agrofarm.auth.max-failed-attempts:5}") int maxFailures,
                               @Value("${agrofarm.auth.lock-minutes:5}") long lockMinutes) {
        this.maxFailures = maxFailures;
        this.lockTime = Duration.ofMinutes(lockMinutes);
    }

    public void assertAllowed(String key) {
        Attempt a = attempts.get(key.toLowerCase());
        if (a != null && a.lockedUntil != null) {
            if (a.lockedUntil.isAfter(Instant.now())) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "Слишком много неудачных попыток входа. Повторите позже.");
            }
            attempts.remove(key.toLowerCase());
        }
    }

    public void registerFailure(String key) {
        Instant now = Instant.now();
        attempts.compute(key.toLowerCase(), (k, a) -> {
            if (a == null || a.firstFailure.plus(lockTime).isBefore(now)) {
                a = new Attempt();
                a.firstFailure = now;
            }
            a.failures++;
            if (a.failures >= maxFailures) a.lockedUntil = now.plus(lockTime);
            return a;
        });
    }

    public void registerSuccess(String key) {
        attempts.remove(key.toLowerCase());
    }
}
