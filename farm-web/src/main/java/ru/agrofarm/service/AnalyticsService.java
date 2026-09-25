package ru.agrofarm.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.agrofarm.dto.Mapper;
import ru.agrofarm.entity.*;
import ru.agrofarm.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class AnalyticsService {

    static final BigDecimal GROWTH_THRESHOLD = new BigDecimal("1.5");

    private final UserRepository users;
    private final FarmUnitRepository units;
    private final OperationRepository operations;
    private final BudgetLimitRepository limits;
    private final RecommendationRepository recommendations;

    public AnalyticsService(UserRepository users, FarmUnitRepository units, OperationRepository operations,
                            BudgetLimitRepository limits, RecommendationRepository recommendations) {
        this.users = users;
        this.units = units;
        this.operations = operations;
        this.limits = limits;
        this.recommendations = recommendations;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        YearMonth ym = YearMonth.now();
        Periods.Range r = Periods.month(ym);
        BigDecimal income = operations.sumByType(Category.INCOME, r.from(), r.to());
        BigDecimal expense = operations.sumByType(Category.EXPENSE, r.from(), r.to());

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalFarmers", users.count());
        m.put("totalUnits", units.count());
        m.put("totalOperations", operations.count());
        m.put("cultivatedAreaHa", units.sumCultivatedArea());
        m.put("totalHeads", units.sumHeadCount());
        m.put("monthIncome", income);
        m.put("monthExpense", expense);
        m.put("monthProfit", income.subtract(expense));
        m.put("monthly", monthlyTrend(6));

        List<Map<String, Object>> cats = new ArrayList<>();
        for (Object[] row : operations.allExpensesByCategory(r.from(), r.to())) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("name", row[0]);
            c.put("total", row[2]);
            cats.add(c);
        }
        m.put("expenseStructure", cats);

        List<Map<String, Object>> recent = new ArrayList<>();
        for (Operation o : operations.findRecent(PageRequest.of(0, 10))) {
            Map<String, Object> t = Mapper.operation(o);
            t.put("userName", o.getUnit().getUser().getFullName());
            recent.add(t);
        }
        m.put("recentOperations", recent);
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> monthlyTrend(int months) {
        List<Map<String, Object>> list = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            Periods.Range r = Periods.month(ym);
            BigDecimal inc = operations.sumByType(Category.INCOME, r.from(), r.to());
            BigDecimal exp = operations.sumByType(Category.EXPENSE, r.from(), r.to());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("monthName", Periods.monthName(ym));
            m.put("income", inc);
            m.put("expense", exp);
            m.put("profit", inc.subtract(exp));
            list.add(m);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> farmers() {
        Periods.Range r = Periods.month(YearMonth.now());
        List<Map<String, Object>> list = new ArrayList<>();
        for (AppUser u : users.findAll(org.springframework.data.domain.Sort.by("lastName", "firstName"))) {
            BigDecimal inc = operations.sumByUserAndType(u.getId(), Category.INCOME, r.from(), r.to());
            BigDecimal exp = operations.sumByUserAndType(u.getId(), Category.EXPENSE, r.from(), r.to());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("fullName", u.getFullName());
            m.put("email", u.getEmail());
            m.put("active", Boolean.TRUE.equals(u.getActive()));
            m.put("unitCount", units.countByUserId(u.getId()));
            m.put("areaHa", units.sumAreaByUser(u.getId()));
            m.put("heads", units.sumHeadCountByUser(u.getId()));
            m.put("opsThisMonth", operations.countByUserBetween(u.getId(), r.from(), r.to()));
            m.put("monthIncome", inc);
            m.put("monthExpense", exp);
            m.put("monthProfit", inc.subtract(exp));
            list.add(m);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> farmerDetail(Long userId) {
        AppUser u = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фермер не найден"));
        Map<String, Object> m = new LinkedHashMap<>(Mapper.user(u));
        m.put("areaHa", units.sumAreaByUser(userId));
        m.put("heads", units.sumHeadCountByUser(userId));
        m.put("units", units.findByUserIdOrderByCreatedAtAsc(userId).stream().map(Mapper::unit).toList());

        List<Map<String, Object>> monthly = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            Periods.Range r = Periods.month(ym);
            BigDecimal inc = operations.sumByUserAndType(userId, Category.INCOME, r.from(), r.to());
            BigDecimal exp = operations.sumByUserAndType(userId, Category.EXPENSE, r.from(), r.to());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("monthName", Periods.monthName(ym));
            row.put("income", inc);
            row.put("expense", exp);
            row.put("profit", inc.subtract(exp));
            monthly.add(row);
        }
        m.put("monthly", monthly);
        m.put("recentOperations", operations.findRecentByUser(userId, PageRequest.of(0, 15))
                .stream().map(Mapper::operation).toList());
        LocalDate today = LocalDate.now();
        m.put("limits", limits.findByUserId(userId).stream().map(l -> {
            Periods.Range r = Periods.forLimit(l.getPeriod(), today);
            return FarmService.limitMap(l, operations.sumExpenseByCategory(userId, l.getCategory().getId(), r.from(), r.to()));
        }).toList());
        m.put("recommendationCount", recommendations.countByUserId(userId));
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> units() {
        LocalDate to = LocalDate.now();
        LocalDate from = YearMonth.now().minusMonths(2).atDay(1);
        Map<Long, FarmUnit> byId = new HashMap<>();
        for (FarmUnit u : units.findAllWithOwner()) byId.put(u.getId(), u);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : operations.allUnitResults(from, to)) {
            FarmUnit u = byId.get((Long) row[0]);
            if (u == null) continue;
            BigDecimal inc = (BigDecimal) row[7];
            BigDecimal exp = (BigDecimal) row[8];
            Map<String, Object> m = Mapper.unit(u);
            m.put("userId", u.getUser().getId());
            m.put("userName", u.getUser().getFullName());
            m.put("income", inc);
            m.put("expense", exp);
            m.put("profit", inc.subtract(exp));
            m.put("costPerHa", u.getAreaHa().signum() > 0
                    ? exp.divide(u.getAreaHa(), 2, RoundingMode.HALF_UP) : null);
            m.put("costPerHead", u.getHeadCount() > 0
                    ? exp.divide(BigDecimal.valueOf(u.getHeadCount()), 2, RoundingMode.HALF_UP) : null);
            list.add(m);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> unitOperations(Long unitId) {
        if (!units.existsById(unitId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Участок не найден");
        }
        return operations.findByUnit(unitId).stream().map(Mapper::operation).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> anomalies() {
        LocalDate today = LocalDate.now();

        List<Map<String, Object>> limitRows = new ArrayList<>();
        for (BudgetLimit l : limits.findAll()) {
            Periods.Range r = Periods.forLimit(l.getPeriod(), today);
            BigDecimal spent = operations.sumExpenseByCategory(l.getUser().getId(), l.getCategory().getId(), r.from(), r.to());
            Map<String, Object> m = FarmService.limitMap(l, spent);
            if (!"ok".equals(m.get("status"))) limitRows.add(m);
        }
        limitRows.sort(Comparator.comparing((Map<String, Object> m) -> (BigDecimal) m.get("percent")).reversed());

        List<Map<String, Object>> growth = new ArrayList<>();
        LocalDate thisFrom = today.withDayOfMonth(1);
        LocalDate lastFrom = thisFrom.minusMonths(1);
        LocalDate lastTo = thisFrom.minusDays(1);
        for (AppUser u : users.findAll()) {
            BigDecimal cur = operations.sumExpenseByUser(u.getId(), thisFrom, today);
            BigDecimal prev = operations.sumExpenseByUser(u.getId(), lastFrom, lastTo);
            if (prev.signum() > 0 && cur.compareTo(prev.multiply(GROWTH_THRESHOLD)) > 0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("userId", u.getId());
                m.put("userName", u.getFullName());
                m.put("lastMonth", prev);
                m.put("thisMonth", cur);
                m.put("growthPercent", cur.subtract(prev).multiply(BigDecimal.valueOf(100))
                        .divide(prev, 1, RoundingMode.HALF_UP));
                growth.add(m);
            }
        }

        List<Map<String, Object>> losses = new ArrayList<>();
        for (Map<String, Object> u : units()) {
            BigDecimal inc = (BigDecimal) u.get("income");
            BigDecimal exp = (BigDecimal) u.get("expense");
            if (inc.signum() > 0 && exp.compareTo(inc) > 0) losses.add(u);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("limitAnomalies", limitRows);
        result.put("expenseGrowth", growth);
        result.put("unprofitableUnits", losses);
        return result;
    }
}
