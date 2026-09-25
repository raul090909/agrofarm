package ru.agrofarm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "operations")
public class Operation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operation_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private FarmUnit unit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "op_type", nullable = false, length = 10)
    private String type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(name = "quantity_unit", length = 10)
    private String quantityUnit;

    @Column(length = 255)
    private String description;

    @Column(name = "operation_date", nullable = false)
    private LocalDate operationDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Operation() {}

    public Operation(FarmUnit unit, Category category, String type, BigDecimal amount,
                     BigDecimal quantity, String quantityUnit, String description, LocalDate operationDate) {
        this.unit = unit;
        this.category = category;
        this.type = type;
        this.amount = amount;
        this.quantity = quantity;
        this.quantityUnit = quantityUnit;
        this.description = description;
        this.operationDate = operationDate;
    }

    public String getTypeLabel() {
        return Category.INCOME.equals(type) ? "Доход" : "Расход";
    }

    public Long getId() { return id; }
    public FarmUnit getUnit() { return unit; }
    public void setUnit(FarmUnit unit) { this.unit = unit; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getQuantityUnit() { return quantityUnit; }
    public void setQuantityUnit(String quantityUnit) { this.quantityUnit = quantityUnit; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDate getOperationDate() { return operationDate; }
    public void setOperationDate(LocalDate operationDate) { this.operationDate = operationDate; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
