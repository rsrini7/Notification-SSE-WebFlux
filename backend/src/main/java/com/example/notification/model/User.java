package com.example.notification.model;

import lombok.Data;
import jakarta.persistence.*;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    private String username;
    private String email;
    private boolean enabled;
}