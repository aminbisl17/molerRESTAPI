package com.example.moler.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A request created by a worker (puntori) and sent to one manager (menaxheri),
 * who approves or declines it. The worker cannot be changed after creation.
 * data_krijimit, data_aprovimit and data_refuzimit are managed by SQL Server
 * (default value + trigger), so they are never written from Java.
 */
@Entity
@Table(name = "kerkesat")
public class Kerkesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_puntorit", updatable = false)
    private puntori puntori;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_menaxheri")
    private menaxheri menaxheri;

    @Column(name = "titulli", nullable = false, length = 255)
    private String titulli;

    // Optional
    @Column(name = "pershkrimi")
    private String pershkrimi;

    // w = waiting, a = accepted, d = declined
    @Column(name = "statusi", nullable = false, length = 1)
    private String statusi;

    @Column(name = "data_krijimit", insertable = false, updatable = false)
    private LocalDateTime dataKrijimit;

    @Column(name = "data_aprovimit", insertable = false, updatable = false)
    private LocalDateTime dataAprovimit;

    @Column(name = "data_refuzimit", insertable = false, updatable = false)
    private LocalDateTime dataRefuzimit;

    @Column(name = "pershkrimi_anulimit")
    private String pershkrimiAnulimit;

    @Column(name = "pershkrimi_aprovimit")
    private String pershkrimiAprovimit;

    public Kerkesa() {}

    public Long getId() {
        return id;
    }

    public puntori getPuntori() {
        return puntori;
    }

    public void setPuntori(puntori puntori) {
        this.puntori = puntori;
    }

    public menaxheri getMenaxheri() {
        return menaxheri;
    }

    public void setMenaxheri(menaxheri menaxheri) {
        this.menaxheri = menaxheri;
    }

    public String getTitulli() {
        return titulli;
    }

    public void setTitulli(String titulli) {
        this.titulli = titulli;
    }

    public String getPershkrimi() {
        return pershkrimi;
    }

    public void setPershkrimi(String pershkrimi) {
        this.pershkrimi = pershkrimi;
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

    public LocalDateTime getDataAprovimit() {
        return dataAprovimit;
    }

    public LocalDateTime getDataRefuzimit() {
        return dataRefuzimit;
    }

    public String getPershkrimiAnulimit() {
        return pershkrimiAnulimit;
    }

    public void setPershkrimiAnulimit(String pershkrimiAnulimit) {
        this.pershkrimiAnulimit = pershkrimiAnulimit;
    }

    public String getPershkrimiAprovimit() {
        return pershkrimiAprovimit;
    }

    public void setPershkrimiAprovimit(String pershkrimiAprovimit) {
        this.pershkrimiAprovimit = pershkrimiAprovimit;
    }
}
