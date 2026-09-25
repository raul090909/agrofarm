package ru.agrofarm.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.agrofarm.entity.Operation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OperationRepository extends JpaRepository<Operation, Long> {

    String FETCH = "SELECT o FROM Operation o JOIN FETCH o.category JOIN FETCH o.unit u JOIN FETCH u.user ";

    @Query(FETCH + "WHERE u.user.id = :userId AND o.operationDate BETWEEN :from AND :to " +
           "ORDER BY o.operationDate DESC, o.id DESC")
    List<Operation> findByUserBetween(@Param("userId") Long userId,
                                      @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(FETCH + "WHERE u.user.id = :userId ORDER BY o.operationDate DESC, o.id DESC")
    List<Operation> findRecentByUser(@Param("userId") Long userId, Pageable pageable);

    @Query(FETCH + "WHERE o.id = :id AND u.user.id = :userId")
    Optional<Operation> findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Operation o " +
           "WHERE o.unit.user.id = :userId AND o.type = :type AND o.operationDate BETWEEN :from AND :to")
    BigDecimal sumByUserAndType(@Param("userId") Long userId, @Param("type") String type,
                                @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT c.name, c.icon, SUM(o.amount) FROM Operation o JOIN o.category c " +
           "WHERE o.unit.user.id = :userId AND o.type = 'expense' AND o.operationDate BETWEEN :from AND :to " +
           "GROUP BY c.id, c.name, c.icon ORDER BY SUM(o.amount) DESC")
    List<Object[]> expensesByCategory(@Param("userId") Long userId,
                                      @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Operation o " +
           "WHERE o.unit.user.id = :userId AND o.category.id = :categoryId AND o.type = 'expense' " +
           "AND o.operationDate BETWEEN :from AND :to")
    BigDecimal sumExpenseByCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId,
                                    @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT u.id, u.name, u.type, " +
           "COALESCE(SUM(CASE WHEN o.type = 'income' THEN o.amount ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN o.type = 'expense' THEN o.amount ELSE 0 END), 0) " +
           "FROM FarmUnit u LEFT JOIN Operation o ON o.unit = u AND o.operationDate BETWEEN :from AND :to " +
           "WHERE u.user.id = :userId GROUP BY u.id, u.name, u.type ORDER BY u.name")
    List<Object[]> unitResults(@Param("userId") Long userId,
                               @Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByCategoryId(Long categoryId);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Operation o " +
           "WHERE o.type = :type AND o.operationDate BETWEEN :from AND :to")
    BigDecimal sumByType(@Param("type") String type, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COUNT(o) FROM Operation o WHERE o.unit.user.id = :userId AND o.operationDate BETWEEN :from AND :to")
    long countByUserBetween(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Operation o " +
           "WHERE o.unit.user.id = :userId AND o.type = 'expense' AND o.operationDate BETWEEN :from AND :to")
    BigDecimal sumExpenseByUser(@Param("userId") Long userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(FETCH + "ORDER BY o.createdAt DESC, o.id DESC")
    List<Operation> findRecent(Pageable pageable);

    @Query(FETCH + "WHERE o.operationDate BETWEEN :from AND :to ORDER BY o.operationDate DESC, o.id DESC")
    List<Operation> findBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(FETCH + "WHERE u.id = :unitId ORDER BY o.operationDate DESC, o.id DESC")
    List<Operation> findByUnit(@Param("unitId") Long unitId);

    @Query("SELECT u.id, u.name, u.type, w.id, w.firstName, w.secondName, w.lastName, " +
           "COALESCE(SUM(CASE WHEN o.type = 'income' THEN o.amount ELSE 0 END), 0), " +
           "COALESCE(SUM(CASE WHEN o.type = 'expense' THEN o.amount ELSE 0 END), 0) " +
           "FROM FarmUnit u JOIN u.user w LEFT JOIN Operation o ON o.unit = u AND o.operationDate BETWEEN :from AND :to " +
           "GROUP BY u.id, u.name, u.type, w.id, w.firstName, w.secondName, w.lastName ORDER BY u.name")
    List<Object[]> allUnitResults(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("SELECT c.name, c.icon, SUM(o.amount) FROM Operation o JOIN o.category c " +
           "WHERE o.type = 'expense' AND o.operationDate BETWEEN :from AND :to " +
           "GROUP BY c.id, c.name, c.icon ORDER BY SUM(o.amount) DESC")
    List<Object[]> allExpensesByCategory(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
