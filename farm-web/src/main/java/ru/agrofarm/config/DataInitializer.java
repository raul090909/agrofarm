package ru.agrofarm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.agrofarm.entity.*;
import ru.agrofarm.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository users;
    private final AgronomistRepository agronomists;
    private final FarmUnitRepository units;
    private final CategoryRepository categories;
    private final OperationRepository operations;
    private final BudgetLimitRepository limits;
    private final RecommendationRepository recommendations;
    private final PasswordEncoder encoder;
    private final boolean enabled;

    public DataInitializer(UserRepository users, AgronomistRepository agronomists, FarmUnitRepository units,
                           CategoryRepository categories, OperationRepository operations,
                           BudgetLimitRepository limits, RecommendationRepository recommendations,
                           PasswordEncoder encoder, @Value("${agrofarm.seed.enabled:true}") boolean enabled) {
        this.users = users;
        this.agronomists = agronomists;
        this.units = units;
        this.categories = categories;
        this.operations = operations;
        this.limits = limits;
        this.recommendations = recommendations;
        this.encoder = encoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categories.countByGlobalTrue() == 0) seedCategories();
        if (agronomists.count() == 0) {
            agronomists.save(new Agronomist("Ирина", "Петровна", "Соколова", "agronom@agrofarm.local",
                    "79001234567", "agronom", encoder.encode("agro123")));
        }
        if (!enabled || users.count() > 0) return;

        log.info("База пуста — добавляю демонстрационные данные");
        Random rnd = new Random(2026);
        LocalDate today = LocalDate.now();
        Long agroId = agronomists.findByLoginIgnoreCase("agronom").map(Agronomist::getId).orElse(null);

        AppUser ivan = user("Иван Петрович Кузнецов", "ivan@test.ru", "pass123", 200);
        FarmUnit wheat = unit(ivan, "Поле №1 (пшеница)", FarmUnit.FIELD, "120.00", 0, "Озимая пшеница, чернозём");
        FarmUnit barley = unit(ivan, "Поле №2 (ячмень)", FarmUnit.FIELD, "80.00", 0, "Яровой ячмень");
        FarmUnit dairy = unit(ivan, "Молочная ферма", FarmUnit.LIVESTOCK, "2.50", 45, "КРС, голштинская порода");
        FarmUnit barn = unit(ivan, "Зернохранилище", FarmUnit.STORAGE, "0.60", 0, "Вместимость 900 т");

        for (int m = 5; m >= 0; m--) {
            LocalDate base = today.minusMonths(m).withDayOfMonth(1);
            op(dairy, "Корма", "expense", 95_000 + rnd.nextInt(20_000), "3200", "кг", "Комбикорм и сено", day(base, 3, today));
            op(dairy, "Ветеринария", "expense", 12_000 + rnd.nextInt(6_000), null, null, "Плановый осмотр стада", day(base, 10, today));
            op(dairy, "Продажа молока", "income", 310_000 + rnd.nextInt(40_000), "9800", "л", "Поставка на молокозавод", day(base, 25, today));
            op(dairy, "Электроэнергия и вода", "expense", 18_000 + rnd.nextInt(4_000), null, null, "Доильный блок", day(base, 15, today));
            op(wheat, "Зарплата работников", "expense", 140_000, null, null, "Механизаторы, 2 чел.", day(base, 5, today));
            op(wheat, "Топливо и ГСМ", "expense", 40_000 + rnd.nextInt(30_000), "700", "л", "Дизельное топливо", day(base, 12, today));
        }
        op(wheat, "Семена и посадочный материал", "expense", 380_000, "36", "т", "Семена озимой пшеницы", today.minusMonths(4).withDayOfMonth(8));
        op(wheat, "Удобрения", "expense", 290_000, "24", "т", "Аммиачная селитра", today.minusMonths(3).withDayOfMonth(14));
        op(wheat, "Средства защиты растений", "expense", 120_000, "300", "л", "Гербицид", today.minusMonths(2).withDayOfMonth(6));
        op(wheat, "Продажа зерна", "income", 1_450_000, "420", "т", "Пшеница 3 класса", today.minusMonths(1).withDayOfMonth(18));
        op(barley, "Семена и посадочный материал", "expense", 160_000, "18", "т", "Семена ячменя", today.minusMonths(4).withDayOfMonth(20));
        op(barley, "Удобрения", "expense", 110_000, "9", "т", "Нитроаммофоска", today.minusMonths(3).withDayOfMonth(2));
        op(barley, "Продажа зерна", "income", 520_000, "190", "т", "Фуражный ячмень", today.minusMonths(1).withDayOfMonth(22));
        op(barn, "Техника и ремонт", "expense", 65_000, null, null, "Ремонт зерносушилки", today.minusMonths(2).withDayOfMonth(19));
        op(dairy, "Корма", "expense", 60_000, "2000", "кг", "Закупка сенажа впрок", day(today.withDayOfMonth(1), 2, today));

        limit(ivan, "Корма", "140000", BudgetLimit.MONTH);
        limit(ivan, "Топливо и ГСМ", "90000", BudgetLimit.MONTH);
        limit(ivan, "Ветеринария", "60000", BudgetLimit.QUARTER);

        AppUser anna = user("Анна Сергеевна Морозова", "anna@test.ru", "pass123", 150);
        FarmUnit gh1 = unit(anna, "Теплица №1 (томаты)", FarmUnit.GREENHOUSE, "0.80", 0, "Зимняя теплица");
        FarmUnit gh2 = unit(anna, "Теплица №2 (огурцы)", FarmUnit.GREENHOUSE, "0.50", 0, null);
        FarmUnit open = unit(anna, "Огород (картофель)", FarmUnit.FIELD, "6.00", 0, null);
        for (int m = 5; m >= 1; m--) {
            LocalDate base = today.minusMonths(m).withDayOfMonth(1);
            op(gh1, "Электроэнергия и вода", "expense", 45_000 + rnd.nextInt(10_000), null, null, "Досветка и полив", day(base, 7, today));
            op(gh1, "Продажа овощей", "income", 150_000 + rnd.nextInt(50_000), "1100", "кг", "Томаты, розничная сеть", day(base, 21, today));
            op(gh2, "Удобрения", "expense", 9_000 + rnd.nextInt(4_000), "150", "кг", "Водорастворимые удобрения", day(base, 9, today));
            op(gh2, "Продажа овощей", "income", 70_000 + rnd.nextInt(20_000), "900", "кг", "Огурцы, рынок", day(base, 24, today));
        }
        LocalDate cur = today.withDayOfMonth(1);
        op(gh1, "Техника и ремонт", "expense", 180_000, null, null, "Замена поликарбоната после града", day(cur, 1, today));
        op(gh1, "Электроэнергия и вода", "expense", 52_000, null, null, "Досветка и полив", day(cur, 2, today));
        op(open, "Семена и посадочный материал", "expense", 48_000, "3", "т", "Семенной картофель", today.minusMonths(4).withDayOfMonth(12));
        op(open, "Продажа овощей", "income", 30_000, "1500", "кг", "Картофель, частично", today.minusMonths(1).withDayOfMonth(5));
        op(open, "Зарплата работников", "expense", 60_000, null, null, "Уборка урожая", today.minusMonths(1).withDayOfMonth(3));
        limit(anna, "Электроэнергия и вода", "60000", BudgetLimit.MONTH);

        AppUser oleg = user("Олег Викторович Смирнов", "oleg@test.ru", "pass123", 60);
        FarmUnit sheep = unit(oleg, "Овчарня", FarmUnit.LIVESTOCK, "1.20", 120, "Романовская порода");
        FarmUnit pasture = unit(oleg, "Пастбище", FarmUnit.FIELD, "35.00", 0, null);
        for (int m = 1; m >= 0; m--) {
            LocalDate base = today.minusMonths(m).withDayOfMonth(1);
            op(sheep, "Корма", "expense", 25_000 + rnd.nextInt(5_000), "1500", "кг", "Сено, зерносмесь", day(base, 4, today));
            op(sheep, "Ветеринария", "expense", 8_000, null, null, "Вакцинация", day(base, 6, today));
        }
        op(sheep, "Продажа мяса", "income", 96_000, "12", "гол", "Реализация молодняка", day(today.minusMonths(1).withDayOfMonth(1), 20, today));
        op(sheep, "Субсидии и гранты", "income", 150_000, null, null, "Субсидия на поголовье", day(today.minusMonths(1).withDayOfMonth(1), 26, today));
        op(pasture, "Аренда земли", "expense", 35_000, null, null, "Аренда за квартал", day(today.minusMonths(1).withDayOfMonth(1), 2, today));

        rec(ivan, agroId, "crops", "Иван Петрович, по результатам почвенного анализа на поле №1 рекомендую "
                + "перейти на дробное внесение азотных удобрений: 2–3 подкормки вместо одной. Это снизит потери азота.", true, 20);
        rec(ivan, agroId, "livestock", "Расходы на корма в этом месяце выше плана. Проверьте рацион: "
                + "частичная замена комбикорма сенажем собственного производства снизит затраты на 10–15 %.", false, 1);
        rec(anna, agroId, "finance", "Анна Сергеевна, затраты на электроэнергию в теплицах растут. Рассмотрите "
                + "переход на светодиодную досветку и ночной тариф — окупаемость около 2 сезонов.", false, 3);
        rec(oleg, agroId, "general", "Олег Викторович, добро пожаловать! Чтобы аналитика была точной, заносите "
                + "все операции по овчарне и пастбищу, указывая количество (кг, гол.).", true, 40);
        rec(oleg, null, "general", "Добро пожаловать в «Сельхозферму»! Добавьте участки и начните вести учёт операций.", true, 60);

        log.info("Демонстрационные данные добавлены: фермеров {}, участков {}, операций {}",
                users.count(), units.count(), operations.count());
    }

    private void seedCategories() {
        String[][] rows = {
                {"Семена и посадочный материал", "expense", "seeds"}, {"Удобрения", "expense", "fertilizer"},
                {"Средства защиты растений", "expense", "protection"}, {"Корма", "expense", "feed"},
                {"Ветеринария", "expense", "vet"}, {"Топливо и ГСМ", "expense", "fuel"},
                {"Техника и ремонт", "expense", "machinery"}, {"Зарплата работников", "expense", "salary"},
                {"Аренда земли", "expense", "rent"}, {"Электроэнергия и вода", "expense", "energy"},
                {"Прочие расходы", "expense", "other"}, {"Продажа зерна", "income", "grain"},
                {"Продажа овощей", "income", "vegetables"}, {"Продажа молока", "income", "milk"},
                {"Продажа мяса", "income", "meat"}, {"Субсидии и гранты", "income", "subsidy"},
                {"Прочие доходы", "income", "other"}};
        for (String[] r : rows) categories.save(new Category(null, r[0], r[1], r[2], true));
    }

    private static LocalDate day(LocalDate base, int dayOfMonth, LocalDate today) {
        LocalDate d = base.withDayOfMonth(Math.min(dayOfMonth, base.lengthOfMonth()));
        return d.isAfter(today) ? today : d;
    }

    private AppUser user(String name, String email, String password, int daysAgo) {
        AppUser u = new AppUser(name, email, encoder.encode(password));
        u.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        return users.save(u);
    }

    private FarmUnit unit(AppUser owner, String name, String type, String area, int heads, String descr) {
        return units.save(new FarmUnit(owner, name, type, new BigDecimal(area), heads, descr));
    }

    private void op(FarmUnit unit, String category, String type, int amount, String qty, String qtyUnit,
                    String descr, LocalDate date) {
        Category c = categories.findByNameAndGlobalTrue(category).orElseThrow();
        Operation o = new Operation(unit, c, type, BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP),
                qty != null ? new BigDecimal(qty) : null, qtyUnit, descr, date);
        o.setCreatedAt(date.atTime(9 + (amount % 9), amount % 60));
        operations.save(o);
    }

    private void limit(AppUser user, String category, String amount, String period) {
        Category c = categories.findByNameAndGlobalTrue(category).orElseThrow();
        limits.save(new BudgetLimit(user, c, new BigDecimal(amount), period));
    }

    private void rec(AppUser user, Long agroId, String topic, String text, boolean read, int daysAgo) {
        Recommendation r = new Recommendation(user, agroId, topic, text, agroId == null);
        r.setRead(read);
        r.setCreatedAt(LocalDateTime.now().minusDays(daysAgo));
        recommendations.save(r);
    }
}
