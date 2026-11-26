package com.example.moler.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Expo push token of one device; belongs to exactly one worker or one manager.
 */
@Entity
@Table(name = "push_tokens")
public class PushToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_puntorit")
    private puntori puntori;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_menaxherit")
    private menaxheri menaxheri;

    @Column(name = "expo_token", nullable = false, unique = true, length = 255)
    private String expoToken;

    @Column(name = "platform", length = 1)
    private String platform; // "a" android, "i" ios

    @Column(name = "data_krijimit", insertable = false, updatable = false)
    private LocalDateTime dataKrijimit;

    @Column(name = "data_perditesimit")
    private LocalDateTime dataPerditesimit;

    public PushToken() {}

    public Long getId() {
        return id;
    }

    public puntori getPuntori() {
        return puntori;
    }

    public menaxheri getMenaxheri() {
        return menaxheri;
    }

    /** Assigns the token to a worker and clears any manager owner. */
    public void setPronarPuntori(puntori puntori) {
        this.puntori = puntori;
        this.menaxheri = null;
    }

    /** Assigns the token to a manager and clears any worker owner. */
    public void setPronarMenaxheri(menaxheri menaxheri) {
        this.menaxheri = menaxheri;
        this.puntori = null;
    }

    public String getExpoToken() {
        return expoToken;
    }

    public void setExpoToken(String expoToken) {
        this.expoToken = expoToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public LocalDateTime getDataKrijimit() {
        return dataKrijimit;
    }

    public LocalDateTime getDataPerditesimit() {
        return dataPerditesimit;
    }

    public void setDataPerditesimit(LocalDateTime dataPerditesimit) {
        this.dataPerditesimit = dataPerditesimit;
    }
}
