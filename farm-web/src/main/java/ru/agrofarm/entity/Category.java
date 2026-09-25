package ru.agrofarm.entity;

import jakarta.persistence.*;

/** Статья дохода или расхода. Глобальные статьи общие для всех, пользовательские принадлежат фермеру. */
@Entity
@Table(name = "categories")
public class Category {

    public static final String INCOME = "income";
    public static final String EXPENSE = "expense";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "cat_name", nullable = false, length = 60)
    private String name;

    @Column(name = "cat_type", nullable = false, length = 10)
    private String type;

    @Column(name = "is_global", nullable = false)
    private Boolean global = false;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(nullable = false, length = 30)
    private String icon = "other";

    public Category() {}

    public Category(AppUser user, String name, String type, String icon, boolean global) {
        this.user = user;
        this.name = name;
        this.type = type;
        this.icon = icon;
        this.global = global;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Boolean getGlobal() { return global; }
    public void setGlobal(Boolean global) { this.global = global; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
}
