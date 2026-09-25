package ru.agrofarm.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rec_id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "agronomist_id")
    private Long agronomistId;

    @Column(nullable = false, length = 12)
    private String topic = "general";

    @Column(name = "rec_message", nullable = false, length = 1000)
    private String message;

    @Column(name = "is_read", nullable = false)
    private Boolean read = false;

    @Column(name = "is_auto", nullable = false)
    private Boolean auto = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Recommendation() {}

    public Recommendation(AppUser user, Long agronomistId, String topic, String message, boolean auto) {
        this.user = user;
        this.agronomistId = agronomistId;
        this.topic = topic;
        this.message = message;
        this.auto = auto;
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
    public Long getAgronomistId() { return agronomistId; }
    public void setAgronomistId(Long agronomistId) { this.agronomistId = agronomistId; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Boolean getRead() { return read; }
    public void setRead(Boolean read) { this.read = read; }
    public Boolean getAuto() { return auto; }
    public void setAuto(Boolean auto) { this.auto = auto; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
