package com.tecnocampus.LS2.protube_back.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users", uniqueConstraints = @UniqueConstraint(name = "uk_app_users_email", columnNames = "email"))
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "text")
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 80)
    private String displayName;

    @Column(length = 2048)
    private String avatarUrl;

    @Column(length = 1000)
    private String description;

    protected User() {}

    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getDisplayName() { return displayName == null ? "" : displayName; }
    public String getAvatarUrl() { return avatarUrl == null ? "" : avatarUrl; }
    public String getDescription() { return description == null ? "" : description; }

    public void updateProfile(String displayName, String avatarUrl, String description) {
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.description = description;
    }
}
