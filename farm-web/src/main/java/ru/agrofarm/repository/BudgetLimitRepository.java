package ru.agrofarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agrofarm.entity.BudgetLimit;

import java.util.List;
import java.util.Optional;

public interface BudgetLimitRepository extends JpaRepository<BudgetLimit, Long> {
    List<BudgetLimit> findByUserId(Long userId);
    Optional<BudgetLimit> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndCategoryIdAndPeriod(Long userId, Long categoryId, String period);
    void deleteByCategoryId(Long categoryId);
}
