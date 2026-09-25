package ru.agrofarm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.agrofarm.entity.AppUser;
import ru.agrofarm.service.Periods;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Модульные тесты вспомогательной логики без запуска Spring. */
class UnitLogicTest {

    @Test @DisplayName("U01 Разбор ФИО из трёх, двух и одного слова")
    void fullName() {
        AppUser u = new AppUser();
        u.setFullName("  Иван   Петрович Кузнецов ");
        assertThat(u.getFirstName()).isEqualTo("Иван");
        assertThat(u.getSecondName()).isEqualTo("Петрович");
        assertThat(u.getLastName()).isEqualTo("Кузнецов");
        u.setFullName("Анна Морозова");
        assertThat(u.getFullName()).isEqualTo("Анна Морозова");
        u.setFullName("Олег");
        assertThat(u.getFullName()).isEqualTo("Олег");
    }

    @Test @DisplayName("U02 Границы квартала для лимита")
    void quarter() {
        Periods.Range r = Periods.forLimit("quarter", LocalDate.of(2026, 8, 15));
        assertThat(r.from()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(r.to()).isEqualTo(LocalDate.of(2026, 9, 30));
        Periods.Range q4 = Periods.forLimit("quarter", LocalDate.of(2026, 12, 31));
        assertThat(q4.from()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(q4.to()).isEqualTo(LocalDate.of(2026, 12, 31));
    }

    @Test @DisplayName("U03 Границы месяца для лимита, включая февраль високосного года")
    void month() {
        Periods.Range r = Periods.forLimit("month", LocalDate.of(2028, 2, 10));
        assertThat(r.from()).isEqualTo(LocalDate.of(2028, 2, 1));
        assertThat(r.to()).isEqualTo(LocalDate.of(2028, 2, 29));
    }
}
