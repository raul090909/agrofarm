package ru.agrofarm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.agrofarm.dto.AuthRequests;
import ru.agrofarm.dto.FarmRequests;
import ru.agrofarm.dto.Mapper;
import ru.agrofarm.service.AuthService;
import ru.agrofarm.service.FarmService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** REST API мобильного приложения фермера. Все данные ограничены владельцем токена. */
@RestController
@RequestMapping("/api")
public class FarmerApiController {

    private final FarmService farm;
    private final AuthService auth;

    public FarmerApiController(FarmService farm, AuthService auth) {
        this.farm = farm;
        this.auth = auth;
    }

    // ---------------- профиль

    @GetMapping("/profile")
    public Map<String, Object> profile(HttpServletRequest r) {
        return Mapper.user(farm.requireUser(CurrentSession.subjectId(r)));
    }

    @PutMapping("/profile")
    public Map<String, Object> updateProfile(HttpServletRequest r, @Valid @RequestBody AuthRequests.Profile req) {
        return Mapper.user(auth.updateProfile(CurrentSession.subjectId(r), req));
    }

    @PutMapping("/profile/password")
    public Map<String, Object> changePassword(HttpServletRequest r, @Valid @RequestBody AuthRequests.ChangePassword req) {
        auth.changePassword(CurrentSession.subjectId(r), req);
        return Map.of("success", true);
    }

    // ---------------- участки

    @GetMapping("/units")
    public List<Map<String, Object>> units(HttpServletRequest r) {
        return farm.listUnits(CurrentSession.subjectId(r)).stream().map(Mapper::unit).toList();
    }

    @PostMapping("/units")
    public ResponseEntity<Map<String, Object>> createUnit(HttpServletRequest r, @Valid @RequestBody FarmRequests.Unit req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(Mapper.unit(farm.createUnit(CurrentSession.subjectId(r), req)));
    }

    @PutMapping("/units/{id}")
    public Map<String, Object> updateUnit(HttpServletRequest r, @PathVariable Long id, @Valid @RequestBody FarmRequests.Unit req) {
        return Mapper.unit(farm.updateUnit(CurrentSession.subjectId(r), id, req));
    }

    @DeleteMapping("/units/{id}")
    public Map<String, Object> deleteUnit(HttpServletRequest r, @PathVariable Long id) {
        farm.deleteUnit(CurrentSession.subjectId(r), id);
        return Map.of("success", true);
    }

    // ---------------- статьи

    @GetMapping("/categories")
    public List<Map<String, Object>> categories(HttpServletRequest r) {
        return farm.listCategories(CurrentSession.subjectId(r)).stream().map(Mapper::category).toList();
    }

    @PostMapping("/categories")
    public ResponseEntity<Map<String, Object>> createCategory(HttpServletRequest r, @Valid @RequestBody FarmRequests.CategoryReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Mapper.category(farm.createCategory(CurrentSession.subjectId(r), req)));
    }

    @DeleteMapping("/categories/{id}")
    public Map<String, Object> deleteCategory(HttpServletRequest r, @PathVariable Long id) {
        farm.deleteCategory(CurrentSession.subjectId(r), id);
        return Map.of("success", true);
    }

    // ---------------- операции

    @GetMapping("/operations")
    public List<Map<String, Object>> operations(HttpServletRequest r,
                                                @RequestParam(required = false) String type,
                                                @RequestParam(required = false) Long unitId,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (type != null && !type.equals("income") && !type.equals("expense")) {
            throw new IllegalArgumentException("type");
        }
        return farm.listOperations(CurrentSession.subjectId(r), type, unitId, from, to)
                .stream().map(Mapper::operation).toList();
    }

    @GetMapping("/operations/recent")
    public List<Map<String, Object>> recent(HttpServletRequest r, @RequestParam(defaultValue = "5") int limit) {
        return farm.recentOperations(CurrentSession.subjectId(r), limit).stream().map(Mapper::operation).toList();
    }

    @PostMapping("/operations")
    public ResponseEntity<Map<String, Object>> createOperation(HttpServletRequest r, @Valid @RequestBody FarmRequests.Operation req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Mapper.operation(farm.createOperation(CurrentSession.subjectId(r), req)));
    }

    @PutMapping("/operations/{id}")
    public Map<String, Object> updateOperation(HttpServletRequest r, @PathVariable Long id,
                                               @Valid @RequestBody FarmRequests.Operation req) {
        return Mapper.operation(farm.updateOperation(CurrentSession.subjectId(r), id, req));
    }

    @DeleteMapping("/operations/{id}")
    public Map<String, Object> deleteOperation(HttpServletRequest r, @PathVariable Long id) {
        farm.deleteOperation(CurrentSession.subjectId(r), id);
        return Map.of("success", true);
    }

    // ---------------- аналитика

    @GetMapping("/stats/summary")
    public Map<String, Object> summary(HttpServletRequest r, @RequestParam(required = false) String month) {
        return farm.summary(CurrentSession.subjectId(r), parseMonth(month));
    }

    @GetMapping("/stats/expenses-by-category")
    public List<Map<String, Object>> byCategory(HttpServletRequest r, @RequestParam(required = false) String month) {
        return farm.expensesByCategory(CurrentSession.subjectId(r), parseMonth(month));
    }

    @GetMapping("/stats/units")
    public List<Map<String, Object>> byUnit(HttpServletRequest r, @RequestParam(required = false) String month) {
        return farm.unitResults(CurrentSession.subjectId(r), parseMonth(month));
    }

    @GetMapping("/stats/trend")
    public List<Map<String, Object>> trend(HttpServletRequest r, @RequestParam(defaultValue = "6") int months) {
        return farm.monthlyTrend(CurrentSession.subjectId(r), Math.max(1, Math.min(months, 24)));
    }

    // ---------------- лимиты

    @GetMapping("/limits")
    public List<Map<String, Object>> limits(HttpServletRequest r) {
        return farm.listLimits(CurrentSession.subjectId(r));
    }

    @PostMapping("/limits")
    public ResponseEntity<Map<String, Object>> createLimit(HttpServletRequest r, @Valid @RequestBody FarmRequests.Limit req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(farm.createLimit(CurrentSession.subjectId(r), req));
    }

    @DeleteMapping("/limits/{id}")
    public Map<String, Object> deleteLimit(HttpServletRequest r, @PathVariable Long id) {
        farm.deleteLimit(CurrentSession.subjectId(r), id);
        return Map.of("success", true);
    }

    // ---------------- рекомендации

    @GetMapping("/recommendations")
    public List<Map<String, Object>> recommendations(HttpServletRequest r) {
        return farm.listRecommendations(CurrentSession.subjectId(r)).stream().map(Mapper::recommendation).toList();
    }

    @PutMapping("/recommendations/{id}/read")
    public Map<String, Object> markRead(HttpServletRequest r, @PathVariable Long id) {
        farm.markRecommendationRead(CurrentSession.subjectId(r), id);
        return Map.of("success", true);
    }

    private static YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) return YearMonth.now();
        try {
            return YearMonth.parse(month);
        } catch (Exception e) {
            throw new IllegalArgumentException("month");
        }
    }
}
