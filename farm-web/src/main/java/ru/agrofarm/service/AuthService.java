package ru.agrofarm.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.agrofarm.dto.AuthRequests;
import ru.agrofarm.entity.Agronomist;
import ru.agrofarm.entity.AppUser;
import ru.agrofarm.entity.FarmUnit;
import ru.agrofarm.entity.Recommendation;
import ru.agrofarm.repository.AgronomistRepository;
import ru.agrofarm.repository.FarmUnitRepository;
import ru.agrofarm.repository.RecommendationRepository;
import ru.agrofarm.repository.UserRepository;
import ru.agrofarm.security.LoginAttemptService;
import ru.agrofarm.security.TokenService;

import java.math.BigDecimal;

/** Регистрация и вход фермеров и агрономов. */
@Service
public class AuthService {

    private final UserRepository users;
    private final AgronomistRepository agronomists;
    private final FarmUnitRepository units;
    private final RecommendationRepository recommendations;
    private final PasswordEncoder encoder;
    private final TokenService tokens;
    private final LoginAttemptService attempts;

    public AuthService(UserRepository users, AgronomistRepository agronomists, FarmUnitRepository units,
                       RecommendationRepository recommendations, PasswordEncoder encoder,
                       TokenService tokens, LoginAttemptService attempts) {
        this.users = users;
        this.agronomists = agronomists;
        this.units = units;
        this.recommendations = recommendations;
        this.encoder = encoder;
        this.tokens = tokens;
        this.attempts = attempts;
    }

    public record LoginResult(TokenService.Session session, AppUser user) {}

    public record AgronomistLoginResult(TokenService.Session session, Agronomist agronomist) {}

    /** Регистрирует фермера, создаёт ему первый участок и приветственное сообщение. */
    @Transactional
    public LoginResult register(AuthRequests.Register req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Пользователь с таким email уже зарегистрирован");
        }
        AppUser user = users.save(new AppUser(req.fullName().trim(), email, encoder.encode(req.password())));
        units.save(new FarmUnit(user, "Основное поле", FarmUnit.FIELD, BigDecimal.ZERO, 0,
                "Создано автоматически при регистрации"));
        recommendations.save(new Recommendation(user, null, "general",
                "Добро пожаловать в «Сельхозферму»! Добавьте свои участки и начните вести учёт операций — "
                        + "агроном увидит данные и сможет дать рекомендации.", true));
        return new LoginResult(tokens.issue(TokenService.ROLE_USER, user.getId()), user);
    }

    @Transactional(readOnly = true)
    public LoginResult login(AuthRequests.Login req) {
        String email = req.email().trim().toLowerCase();
        attempts.assertAllowed("user:" + email);
        AppUser user = users.findByEmailIgnoreCase(email).orElse(null);
        // Одинаковое сообщение для «нет пользователя» и «неверный пароль» — не раскрываем, какие email существуют.
        if (user == null || !encoder.matches(req.password(), user.getPasswordHash())) {
            attempts.registerFailure("user:" + email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный email или пароль");
        }
        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Учётная запись заблокирована");
        }
        attempts.registerSuccess("user:" + email);
        return new LoginResult(tokens.issue(TokenService.ROLE_USER, user.getId()), user);
    }

    @Transactional(readOnly = true)
    public AgronomistLoginResult loginAgronomist(AuthRequests.AgronomistLogin req) {
        String login = req.login().trim();
        attempts.assertAllowed("agro:" + login);
        Agronomist a = agronomists.findByLoginIgnoreCase(login).orElse(null);
        if (a == null || !encoder.matches(req.password(), a.getPasswordHash())) {
            attempts.registerFailure("agro:" + login);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        }
        if (!Boolean.TRUE.equals(a.getActive())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Учётная запись заблокирована");
        }
        attempts.registerSuccess("agro:" + login);
        return new AgronomistLoginResult(tokens.issue(TokenService.ROLE_AGRONOMIST, a.getId()), a);
    }

    @Transactional
    public AppUser updateProfile(Long userId, AuthRequests.Profile req) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        user.setFullName(req.fullName().trim());
        return users.save(user);
    }

    @Transactional
    public void changePassword(Long userId, AuthRequests.ChangePassword req) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
        if (!encoder.matches(req.oldPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Текущий пароль указан неверно");
        }
        user.setPasswordHash(encoder.encode(req.newPassword()));
        users.save(user);
    }
}
