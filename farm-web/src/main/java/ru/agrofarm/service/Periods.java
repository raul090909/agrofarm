package ru.agrofarm.service;

import java.time.LocalDate;
import java.time.YearMonth;

public final class Periods {

    private Periods() {}

    public record Range(LocalDate from, LocalDate to) {}

    public static Range month(YearMonth ym) {
        return new Range(ym.atDay(1), ym.atEndOfMonth());
    }

    public static Range forLimit(String period, LocalDate date) {
        if ("quarter".equals(period)) {
            int firstMonth = ((date.getMonthValue() - 1) / 3) * 3 + 1;
            LocalDate from = LocalDate.of(date.getYear(), firstMonth, 1);
            return new Range(from, from.plusMonths(3).minusDays(1));
        }
        YearMonth ym = YearMonth.from(date);
        return month(ym);
    }

    public static String monthName(YearMonth ym) {
        String[] names = {"", "Янв", "Фев", "Мар", "Апр", "Май", "Июн",
                "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"};
        return names[ym.getMonthValue()] + " " + ym.getYear();
    }
}
