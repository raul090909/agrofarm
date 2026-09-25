package ru.agrofarm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.agrofarm.dto.AuthRequests;
import ru.agrofarm.dto.Mapper;
import ru.agrofarm.security.AuthInterceptor;
import ru.agrofarm.security.TokenService;
import ru.agrofarm.service.AuthService;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService auth;
    private final TokenService tokens;

    public AuthController(AuthService auth, TokenService tokens) {
        this.auth = auth;
        this.tokens = tokens;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody AuthRequests.Register req) {
        AuthService.LoginResult r = auth.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenBody(r.session(), Mapper.user(r.user())));
    }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@Valid @RequestBody AuthRequests.Login req) {
        AuthService.LoginResult r = auth.login(req);
        return tokenBody(r.session(), Mapper.user(r.user()));
    }

    @PostMapping("/agronomist/login")
    public Map<String, Object> agronomistLogin(@Valid @RequestBody AuthRequests.AgronomistLogin req) {
        AuthService.AgronomistLoginResult r = auth.loginAgronomist(req);
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", r.agronomist().getId());
        profile.put("fullName", r.agronomist().getFullName());
        profile.put("login", r.agronomist().getLogin());
        return tokenBody(r.session(), profile);
    }

    @PostMapping("/auth/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        tokens.revoke(AuthInterceptor.extractToken(request));
        return Map.of("success", true);
    }

    private static Map<String, Object> tokenBody(TokenService.Session s, Map<String, Object> profile) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", s.token());
        m.put("expiresAt", s.expiresAt().toString());
        m.put("profile", profile);
        return m;
    }
}
