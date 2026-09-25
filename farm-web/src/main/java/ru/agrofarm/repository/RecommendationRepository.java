package ru.agrofarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agrofarm.entity.Recommendation;

import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Recommendation> findAllByOrderByCreatedAtDesc();
    Optional<Recommendation> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);
}
