package com.example.moler.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "puntori")
public class puntori {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emri", nullable = false, length = 100)
    private String emri;

    @Column(name = "mbiemri", nullable = false, length = 100)
    private String mbiemri;

    @Column(name = "numri_telefonit", nullable = false, length = 20)
    private String numriTelefonit;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "pozita", nullable = false, length = 2)
    private String pozita;

    @Column(name = "rroga_ditore", nullable = false, precision = 10, scale = 2)
    private BigDecimal rrogaDitore;

    @Column(name = "statusi", nullable = false, length = 2)
    private String statusi;

    @Column(name = "data_krijimit", insertable = false, updatable = false)
    private LocalDateTime dataKrijimit;

   @Column(name = "data_e_deaktivizimit", insertable = false, updatable = false)
private LocalDateTime dataEDeaktivizimit; 

public LocalDateTime getDataEDeaktivizimit() {
    return dataEDeaktivizimit;
}
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

    public String getPozita() {
        return pozita;
    }

    public void setPozita(String pozita) {
        this.pozita = pozita;
    }

    public BigDecimal getRrogaDitore() {
        return rrogaDitore;
    }

    public void setRrogaDitore(BigDecimal rrogaDitore) {
        this.rrogaDitore = rrogaDitore;
    }

    public String getStatusi() {
        return statusi;
    }

    public void setStatusi(String statusi) {
        this.statusi = statusi;
    }

    public LocalDateTime getDataKrijimit() {
        return dataKrijimit;
    }

    public void setDataKrijimit(LocalDateTime dataKrijimit) {
        this.dataKrijimit = dataKrijimit;
    }

    public puntori() {}

    // Generate getters and setters
}