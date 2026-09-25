package ru.agrofarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agrofarm.entity.FarmUnit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FarmUnitRepository extends JpaRepository<FarmUnit, Long> {

    List<FarmUnit> findByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<FarmUnit> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    @Query("SELECT u FROM FarmUnit u JOIN FETCH u.user ORDER BY u.user.lastName, u.name")
    List<FarmUnit> findAllWithOwner();

    @Query("SELECT COALESCE(SUM(u.areaHa), 0) FROM FarmUnit u WHERE u.type IN ('field', 'greenhouse')")
    BigDecimal sumCultivatedArea();

    @Query("SELECT COALESCE(SUM(u.areaHa), 0) FROM FarmUnit u WHERE u.user.id = :userId")
    BigDecimal sumAreaByUser(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(u.headCount), 0) FROM FarmUnit u")
    Long sumHeadCount();

    @Query("SELECT COALESCE(SUM(u.headCount), 0) FROM FarmUnit u WHERE u.user.id = :userId")
    Long sumHeadCountByUser(@Param("userId") Long userId);
}
