package ru.agrofarm.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import ru.agrofarm.dto.FarmRequests;
import ru.agrofarm.dto.Mapper;
import ru.agrofarm.entity.AppUser;
import ru.agrofarm.entity.Recommendation;
import ru.agrofarm.repository.RecommendationRepository;
import ru.agrofarm.repository.UserRepository;
import ru.agrofarm.service.AnalyticsService;

import java.util.List;
import java.util.Map;

/** REST API веб-панели агронома. */
@RestController
@RequestMapping("/api/agronomist")
public class AgronomistApiController {

    private final AnalyticsService analytics;
    private final UserRepository users;
    private final RecommendationRepository recommendations;

    public AgronomistApiController(AnalyticsService analytics, UserRepository users,
                                   RecommendationRepository recommendations) {
        this.analytics = analytics;
        this.users = users;
        this.recommendations = recommendations;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return analytics.dashboard();
    }

    @GetMapping("/farmers")
    public List<Map<String, Object>> farmers() {
        return analytics.farmers();
    }

    @GetMapping("/farmers/{id}")
    public Map<String, Object> farmer(@PathVariable Long id) {
        return analytics.farmerDetail(id);
    }

    @PutMapping("/farmers/{id}/active")
    @Transactional
    public Map<String, Object> setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        AppUser u = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фермер не найден"));
        Boolean active = body.get("active");
        if (active == null) throw new IllegalArgumentException("active");
        u.setActive(active);
        users.save(u);
        return Map.of("success", true, "active", active);
    }

    @GetMapping("/units")
    public List<Map<String, Object>> units() {
        return analytics.units();
    }

    @GetMapping("/units/{id}/operations")
    public List<Map<String, Object>> unitOperations(@PathVariable Long id) {
        return analytics.unitOperations(id);
    }

    @GetMapping("/anomalies")
    public Map<String, Object> anomalies() {
        return analytics.anomalies();
    }

    // ---------------- рекомендации

    @GetMapping("/recommendations")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> recommendations() {
        return recommendations.findAllByOrderByCreatedAtDesc().stream().map(Mapper::recommendation).toList();
    }

    @PostMapping("/recommendations")
    @Transactional
    public ResponseEntity<Map<String, Object>> send(HttpServletRequest r, @Valid @RequestBody FarmRequests.RecommendationReq req) {
        AppUser user = users.findById(req.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фермер не найден"));
        String topic = req.topic() == null || req.topic().isBlank() ? "general" : req.topic();
        Recommendation rec = recommendations.save(new Recommendation(user, CurrentSession.subjectId(r), topic,
                req.message().trim(), false));
        return ResponseEntity.status(HttpStatus.CREATED).body(Mapper.recommendation(rec));
    }

    @PutMapping("/recommendations/{id}")
    @Transactional
    public Map<String, Object> edit(@PathVariable Long id, @Valid @RequestBody FarmRequests.RecommendationEdit req) {
        Recommendation rec = recommendations.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Рекомендация не найдена"));
        rec.setMessage(req.message().trim());
        return Mapper.recommendation(recommendations.save(rec));
    }

    @DeleteMapping("/recommendations/{id}")
    @Transactional
    public Map<String, Object> delete(@PathVariable Long id) {
        if (!recommendations.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Рекомендация не найдена");
        }
        recommendations.deleteById(id);
        return Map.of("success", true);
    }
}
