package ru.agrofarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.agrofarm.entity.Agronomist;

import java.util.Optional;

public interface AgronomistRepository extends JpaRepository<Agronomist, Long> {
    Optional<Agronomist> findByLoginIgnoreCase(String login);
}
