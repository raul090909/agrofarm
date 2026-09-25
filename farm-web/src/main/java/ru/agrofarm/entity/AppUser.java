package ru.agrofarm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @Column(name = "second_name", length = 30)
    private String secondName;

    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Column(nullable = false, length = 10)
    private String role = "user";

    public AppUser() {}

    public AppUser(String fullName, String email, String passwordHash) {
        setFullName(fullName);
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder(firstName != null ? firstName : "");
        if (secondName != null && !secondName.isBlank()) sb.append(' ').append(secondName);
        if (lastName != null && !lastName.isBlank() && !"—".equals(lastName)) sb.append(' ').append(lastName);
        return sb.toString();
    }

    public void setFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            this.firstName = "—";
            this.secondName = null;
            this.lastName = "—";
            return;
        }
        String[] parts = fullName.trim().split("\\s+");
        this.firstName = cut(parts[0]);
        if (parts.length >= 3) {
            this.secondName = cut(parts[1]);
            this.lastName = cut(parts[parts.length - 1]);
        } else if (parts.length == 2) {
            this.secondName = null;
            this.lastName = cut(parts[1]);
        } else {
            this.secondName = null;
            this.lastName = "—";
        }
    }

    private static String cut(String s) {
        return s.length() > 30 ? s.substring(0, 30) : s;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getSecondName() { return secondName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
