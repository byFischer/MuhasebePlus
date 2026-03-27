package com.MuhasebePlus.demo.user.entity;

import java.security.Timestamp;
import java.sql.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
public class User {
    private long user_id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String role;
    private char phoneNumber;
    private Date birthDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
