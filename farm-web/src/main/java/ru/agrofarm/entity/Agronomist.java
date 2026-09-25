package ru.agrofarm.entity;

import jakarta.persistence.*;

/** Агроном-аналитик — пользователь веб-панели. */
@Entity
@Table(name = "agronomists")
public class Agronomist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agronomist_id")
    private Long id;

    @Column(name = "first_name", nullable = false, length = 30)
    private String firstName;

    @Column(name = "second_name", length = 30)
    private String secondName;

    @Column(name = "last_name", nullable = false, length = 30)
    private String lastName;

    @Column(name = "work_email", nullable = false, unique = true, length = 100)
    private String workEmail;

    @Column(name = "phone_number", length = 11)
    private String phoneNumber;

    @Column(nullable = false, unique = true, length = 30)
    private String login;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    public Agronomist() {}

    public Agronomist(String firstName, String secondName, String lastName, String workEmail,
                      String phoneNumber, String login, String passwordHash) {
        this.firstName = firstName;
        this.secondName = secondName;
        this.lastName = lastName;
        this.workEmail = workEmail;
        this.phoneNumber = phoneNumber;
        this.login = login;
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        StringBuilder sb = new StringBuilder(firstName);
        if (secondName != null && !secondName.isBlank()) sb.append(' ').append(secondName);
        sb.append(' ').append(lastName);
        return sb.toString();
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getSecondName() { return secondName; }
    public String getLastName() { return lastName; }
    public String getWorkEmail() { return workEmail; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getLogin() { return login; }
    public String getPasswordHash() { return passwordHash; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
