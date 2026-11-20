package com.example.moler.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "projekti")
public class projekti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emri", nullable = false, length = 150)
    private String emri;

    @Column(name = "lokacioni", nullable = false, length = 255)
    private String lokacioni;

    @Column(name = "kerkesa")
    private String kerkesa;

    @Column(name = "statusi", nullable = false, length = 2)
    private String statusi;

    @Column(name = "prioriteti", nullable = false, length = 1)
    private String prioriteti;

    @Column(name = "statusi_projektit", nullable = false, length = 2)
    private String statusiProjektit;

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

    public String getLokacioni() {
        return lokacioni;
    }

    public void setLokacioni(String lokacioni) {
        this.lokacioni = lokacioni;
    }

    public String getKerkesa() {
        return kerkesa;
    }

    public void setKerkesa(String kerkesa) {
        this.kerkesa = kerkesa;
    }

    public String getStatusi() {
        return statusi;
    }

    public void setStatusi(String statusi) {
        this.statusi = statusi;
    }

    public String getPrioriteti() {
        return prioriteti;
    }

    public void setPrioriteti(String prioriteti) {
        this.prioriteti = prioriteti;
    }

    public String getStatusiProjektit() {
        return statusiProjektit;
    }

    public void setStatusiProjektit(String statusiProjektit) {
        this.statusiProjektit = statusiProjektit;
    }

    public LocalDateTime getDataKrijimit() {
        return dataKrijimit;
    }

    public void setDataKrijimit(LocalDateTime dataKrijimit) {
        this.dataKrijimit = dataKrijimit;
    }

    public projekti() {}

    // Generate getters and setters
}