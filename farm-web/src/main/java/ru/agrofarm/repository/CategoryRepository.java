package ru.agrofarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agrofarm.entity.Category;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Глобальные статьи плюс собственные статьи фермера. */
    @Query("SELECT c FROM Category c WHERE c.global = true OR c.user.id = :userId ORDER BY c.type ASC, c.name ASC")
    List<Category> findVisibleFor(@Param("userId") Long userId);

    /** Статья доступна фермеру, если она глобальная или его собственная. */
    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.global = true OR c.user.id = :userId)")
    Optional<Category> findVisibleById(@Param("id") Long id, @Param("userId") Long userId);

    Optional<Category> findByNameAndGlobalTrue(String name);

    long countByGlobalTrue();

    boolean existsByUserIdAndNameIgnoreCaseAndType(Long userId, String name, String type);
}
