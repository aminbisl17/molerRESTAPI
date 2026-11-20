package com.example.moler.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "menaxheri")
public class menaxheri {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emri", nullable = false, length = 100)
    private String emri;

    @Column(name = "mbiemri", nullable = false, length = 100)
    private String mbiemri;

    @Column(name = "numri_telefonit", nullable = false, length = 20)
    private String numriTelefonit;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "data_krijimit", insertable = false, updatable = false)
    private LocalDateTime dataKrijimit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmri() {
        return emri;
    }

    public void setEmri(String emri) {
        this.emri = emri;
    }

    public String getMbiemri() {
        return mbiemri;
    }

    public void setMbiemri(String mbiemri) {
        this.mbiemri = mbiemri;
    }

    public String getNumriTelefonit() {
        return numriTelefonit;
    }

    public void setNumriTelefonit(String numriTelefonit) {
        this.numriTelefonit = numriTelefonit;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getDataKrijimit() {
        return dataKrijimit;
    }

    public void setDataKrijimit(LocalDateTime dataKrijimit) {
        this.dataKrijimit = dataKrijimit;
    }

    public menaxheri() {}

    // Generate getters and setters
}