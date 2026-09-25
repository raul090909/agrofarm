package ru.agrofarm.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Участок фермы: поле, животноводческий блок, теплица или склад. */
@Entity
@Table(name = "farm_units")
public class FarmUnit {

    public static final String FIELD = "field";
    public static final String LIVESTOCK = "livestock";
    public static final String GREENHOUSE = "greenhouse";
    public static final String STORAGE = "storage";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "unit_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "unit_name", nullable = false, length = 60)
    private String name;

    @Column(name = "unit_type", nullable = false, length = 15)
    private String type;

    @Column(name = "area_ha", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaHa = BigDecimal.ZERO;

    @Column(name = "head_count", nullable = false)
    private Integer headCount = 0;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FarmUnit() {}

    public FarmUnit(AppUser user, String name, String type, BigDecimal areaHa, Integer headCount, String description) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.areaHa = areaHa != null ? areaHa : BigDecimal.ZERO;
        this.headCount = headCount != null ? headCount : 0;
        this.description = description;
    }

    public String getTypeLabel() {
        return switch (type) {
            case FIELD -> "Поле";
            case LIVESTOCK -> "Животноводство";
            case GREENHOUSE -> "Теплица";
            case STORAGE -> "Склад";
            default -> type;
        };
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public BigDecimal getAreaHa() { return areaHa; }
    public void setAreaHa(BigDecimal areaHa) { this.areaHa = areaHa; }
    public Integer getHeadCount() { return headCount; }
    public void setHeadCount(Integer headCount) { this.headCount = headCount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
