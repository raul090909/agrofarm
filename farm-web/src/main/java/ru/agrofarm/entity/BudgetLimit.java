package ru.agrofarm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Лимит расходов по статье на месяц или квартал. */
@Entity
@Table(name = "budget_limits")
public class BudgetLimit {

    public static final String MONTH = "month";
    public static final String QUARTER = "quarter";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "limit_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "time_period", nullable = false, length = 8)
    private String period;

    public BudgetLimit() {}

    public BudgetLimit(AppUser user, Category category, BigDecimal amount, String period) {
        this.user = user;
        this.category = category;
        this.amount = amount;
        this.period = period;
    }

    public String getPeriodLabel() {
        return MONTH.equals(period) ? "в месяц" : "в квартал";
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
