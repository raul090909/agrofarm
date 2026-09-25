package ru.agrofarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agrofarm.entity.AppUser;

import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
