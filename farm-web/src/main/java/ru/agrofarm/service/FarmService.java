package ru.agrofarm.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.agrofarm.dto.FarmRequests;
import ru.agrofarm.entity.*;
import ru.agrofarm.repository.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

@Service
public class FarmService {

    private final UserRepository users;
    private final FarmUnitRepository units;
    private final CategoryRepository categories;
    private final OperationRepository operations;
    private final BudgetLimitRepository limits;
    private final RecommendationRepository recommendations;

    public FarmService(UserRepository users, FarmUnitRepository units, CategoryRepository categories,
                       OperationRepository operations, BudgetLimitRepository limits,
                       RecommendationRepository recommendations) {
        this.users = users;
        this.units = units;
        this.categories = categories;
        this.operations = operations;
        this.limits = limits;
        this.recommendations = recommendations;
    }

    @Transactional(readOnly = true)
    public List<FarmUnit> listUnits(Long userId) {
        return units.findByUserIdOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public FarmUnit createUnit(Long userId, FarmRequests.Unit req) {
        String name = req.name().trim();
        if (units.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Участок с таким названием уже существует");
        }
        AppUser user = requireUser(userId);
        return units.save(new FarmUnit(user, name, req.type(), req.areaHa(), req.headCount(), trimOrNull(req.description())));
    }

    @Transactional
    public FarmUnit updateUnit(Long userId, Long unitId, FarmRequests.Unit req) {
        FarmUnit unit = requireUnit(userId, unitId);
        String name = req.name().trim();
        if (!unit.getName().equalsIgnoreCase(name) && units.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Участок с таким названием уже существует");
        }
        unit.setName(name);
        unit.setType(req.type());
        unit.setAreaHa(req.areaHa() != null ? req.areaHa() : BigDecimal.ZERO);
        unit.setHeadCount(req.headCount() != null ? req.headCount() : 0);
        unit.setDescription(trimOrNull(req.description()));
        return units.save(unit);
    }

    @Transactional
    public void deleteUnit(Long userId, Long unitId) {
        FarmUnit unit = requireUnit(userId, unitId);
        if (units.countByUserId(userId) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Нельзя удалить единственный участок");
        }
        units.delete(unit);
    }

    @Transactional(readOnly = true)
    public List<Category> listCategories(Long userId) {
        return categories.findVisibleFor(userId);
    }

    @Transactional
    public Category createCategory(Long userId, FarmRequests.CategoryReq req) {
        String name = req.name().trim();
        if (categories.existsByUserIdAndNameIgnoreCaseAndType(userId, name, req.type())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Такая статья уже есть");
        }
        String icon = req.icon() == null || req.icon().isBlank() ? "other" : req.icon().trim();
        return categories.save(new Category(requireUser(userId), name, req.type(), icon, false));
    }

    @Transactional
    public void deleteCategory(Long userId, Long categoryId) {
        Category c = categories.findVisibleById(categoryId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не найдена"));
        if (Boolean.TRUE.equals(c.getGlobal())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Общие статьи удалять нельзя");
        }
        if (operations.countByCategoryId(categoryId) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "По статье есть операции — сначала удалите их");
        }
        limits.deleteByCategoryId(categoryId);
        categories.delete(c);
    }

    @Transactional(readOnly = true)
    public List<Operation> listOperations(Long userId, String type, Long unitId, LocalDate from, LocalDate to) {
        LocalDate f = from != null ? from : LocalDate.of(2000, 1, 1);
        LocalDate t = to != null ? to : LocalDate.of(2100, 12, 31);
        if (f.isAfter(t)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата начала позже даты окончания");
        }
        return operations.findByUserBetween(userId, f, t).stream()
                .filter(o -> type == null || type.equals(o.getType()))
                .filter(o -> unitId == null || unitId.equals(o.getUnit().getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Operation> recentOperations(Long userId, int limit) {
        int safe = Math.max(1, Math.min(limit, 50));
        return operations.findRecentByUser(userId, PageRequest.of(0, safe));
    }

    @Transactional
    public Operation createOperation(Long userId, FarmRequests.Operation req) {
        Operation op = new Operation();
        fillOperation(userId, op, req);
        op = operations.save(op);
        if (Category.EXPENSE.equals(op.getType())) {
            notifyIfLimitExceeded(userId, op.getCategory(), op.getOperationDate());
        }
        return op;
    }

    @Transactional
    public Operation updateOperation(Long userId, Long id, FarmRequests.Operation req) {
        Operation op = operations.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Операция не найдена"));
        fillOperation(userId, op, req);
        return operations.save(op);
    }

    @Transactional
    public void deleteOperation(Long userId, Long id) {
        Operation op = operations.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Операция не найдена"));
        operations.delete(op);
    }

    private void fillOperation(Long userId, Operation op, FarmRequests.Operation req) {
        FarmUnit unit = requireUnit(userId, req.unitId());
        Category category = categories.findVisibleById(req.categoryId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не найдена"));
        if (!category.getType().equals(req.type())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Тип операции не совпадает с типом статьи");
        }
        if ((req.quantity() == null) != (req.quantityUnit() == null || req.quantityUnit().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Укажите и количество, и единицу измерения");
        }
        if (req.operationDate().isAfter(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Дата операции не может быть в будущем");
        }
        if (req.operationDate().isBefore(LocalDate.of(2000, 1, 1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Слишком ранняя дата операции");
        }
        op.setUnit(unit);
        op.setCategory(category);
        op.setType(req.type());
        op.setAmount(req.amount().setScale(2, RoundingMode.HALF_UP));
        op.setQuantity(req.quantity());
        op.setQuantityUnit(req.quantity() == null ? null : req.quantityUnit());
        op.setDescription(trimOrNull(req.description()));
        op.setOperationDate(req.operationDate());
    }

    private void notifyIfLimitExceeded(Long userId, Category category, LocalDate date) {
        for (BudgetLimit limit : limits.findByUserId(userId)) {
            if (!limit.getCategory().getId().equals(category.getId())) continue;
            Periods.Range r = Periods.forLimit(limit.getPeriod(), date);
            BigDecimal spent = operations.sumExpenseByCategory(userId, category.getId(), r.from(), r.to());
            if (spent.compareTo(limit.getAmount()) > 0) {
                recommendations.save(new Recommendation(limit.getUser(), null, "finance",
                        String.format(Locale.forLanguageTag("ru"),
                                "Превышен лимит по статье «%s» (%s): израсходовано %,.2f ₽ из %,.2f ₽.",
                                category.getName(), limit.getPeriodLabel(), spent, limit.getAmount()),
                        true));
            }
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summary(Long userId, YearMonth ym) {
        Periods.Range r = Periods.month(ym);
        BigDecimal income = operations.sumByUserAndType(userId, Category.INCOME, r.from(), r.to());
        BigDecimal expense = operations.sumByUserAndType(userId, Category.EXPENSE, r.from(), r.to());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("month", ym.toString());
        m.put("income", income);
        m.put("expense", expense);
        m.put("profit", income.subtract(expense));
        m.put("unitCount", units.countByUserId(userId));
        m.put("totalAreaHa", units.sumAreaByUser(userId));
        m.put("totalHeads", units.sumHeadCountByUser(userId));
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> expensesByCategory(Long userId, YearMonth ym) {
        Periods.Range r = Periods.month(ym);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : operations.expensesByCategory(userId, r.from(), r.to())) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", row[0]);
            m.put("icon", row[1]);
            m.put("total", row[2]);
            list.add(m);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> unitResults(Long userId, YearMonth ym) {
        Periods.Range r = Periods.month(ym);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Object[] row : operations.unitResults(userId, r.from(), r.to())) {
            BigDecimal income = (BigDecimal) row[3];
            BigDecimal expense = (BigDecimal) row[4];
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("unitId", row[0]);
            m.put("unitName", row[1]);
            m.put("unitType", row[2]);
            m.put("income", income);
            m.put("expense", expense);
            m.put("profit", income.subtract(expense));
            list.add(m);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> monthlyTrend(Long userId, int months) {
        List<Map<String, Object>> list = new ArrayList<>();
        YearMonth now = YearMonth.now();
        for (int i = months - 1; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            Periods.Range r = Periods.month(ym);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("month", ym.toString());
            m.put("monthName", Periods.monthName(ym));
            m.put("income", operations.sumByUserAndType(userId, Category.INCOME, r.from(), r.to()));
            m.put("expense", operations.sumByUserAndType(userId, Category.EXPENSE, r.from(), r.to()));
            list.add(m);
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listLimits(Long userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (BudgetLimit l : limits.findByUserId(userId)) {
            Periods.Range r = Periods.forLimit(l.getPeriod(), today);
            BigDecimal spent = operations.sumExpenseByCategory(userId, l.getCategory().getId(), r.from(), r.to());
            list.add(limitMap(l, spent));
        }
        return list;
    }

    @Transactional
    public Map<String, Object> createLimit(Long userId, FarmRequests.Limit req) {
        Category c = categories.findVisibleById(req.categoryId(), userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не найдена"));
        if (!Category.EXPENSE.equals(c.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Лимит можно установить только на статью расходов");
        }
        if (limits.existsByUserIdAndCategoryIdAndPeriod(userId, c.getId(), req.period())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Лимит на эту статью и период уже установлен");
        }
        BudgetLimit l = limits.save(new BudgetLimit(requireUser(userId), c, req.amount(), req.period()));
        Periods.Range r = Periods.forLimit(l.getPeriod(), LocalDate.now());
        return limitMap(l, operations.sumExpenseByCategory(userId, c.getId(), r.from(), r.to()));
    }

    @Transactional
    public void deleteLimit(Long userId, Long limitId) {
        BudgetLimit l = limits.findByIdAndUserId(limitId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Лимит не найден"));
        limits.delete(l);
    }

    public static Map<String, Object> limitMap(BudgetLimit l, BigDecimal spent) {
        BigDecimal percent = l.getAmount().signum() > 0
                ? spent.multiply(BigDecimal.valueOf(100)).divide(l.getAmount(), 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        String status = spent.compareTo(l.getAmount()) > 0 ? "exceeded"
                : percent.compareTo(BigDecimal.valueOf(80)) >= 0 ? "warning" : "ok";
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("userId", l.getUser().getId());
        m.put("userName", l.getUser().getFullName());
        m.put("categoryId", l.getCategory().getId());
        m.put("categoryName", l.getCategory().getName());
        m.put("amount", l.getAmount());
        m.put("period", l.getPeriod());
        m.put("periodLabel", l.getPeriodLabel());
        m.put("spent", spent);
        m.put("percent", percent);
        m.put("status", status);
        return m;
    }

    @Transactional(readOnly = true)
    public List<Recommendation> listRecommendations(Long userId) {
        return recommendations.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public void markRecommendationRead(Long userId, Long recId) {
        Recommendation r = recommendations.findByIdAndUserId(recId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Рекомендация не найдена"));
        r.setRead(true);
        recommendations.save(r);
    }

    @Transactional(readOnly = true)
    public AppUser requireUser(Long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }

    private FarmUnit requireUnit(Long userId, Long unitId) {
        return units.findByIdAndUserId(unitId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Участок не найден"));
    }

    private static String trimOrNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
