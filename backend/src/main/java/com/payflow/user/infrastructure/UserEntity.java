package com.payflow.user.infrastructure;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import com.payflow.user.domain.UserStatus;

@Entity
@Table(name = "users")
public class UserEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 64) private String publicId;
    @Column(nullable = false, length = 100) private String firstName;
    @Column(nullable = false, length = 100) private String lastName;
    @Column(nullable = false, length = 254) private String email;
    @Column(nullable = false, length = 255) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private UserStatus status;
    @Column(nullable = false) private boolean emailVerified;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private Instant updatedAt;

    protected UserEntity() {}

    public UserEntity(String firstName, String lastName, String email, String passwordHash) {
        id = UUID.randomUUID();
        publicId = "PF-USR-" + UUID.randomUUID();
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.passwordHash = passwordHash;
        status = UserStatus.ACTIVE;
        createdAt = updatedAt = Instant.now();
    }

    public UUID id() { return id; }
    public String publicId() { return publicId; }
    public String firstName() { return firstName; }
    public String lastName() { return lastName; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public UserStatus status() { return status; }
}
