package com.example.moler.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One notification for one recipient (worker or manager), shown in the app's notifications tab.
 */
@Entity
@Table(name = "njoftimet")
public class Njoftim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_puntorit")
    private puntori puntori;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_menaxherit")
    private menaxheri menaxheri;

    @Column(name = "tipi", length = 50)
    private String tipi; // null for manual messages

    @Column(name = "titulli", nullable = false, length = 100)
    private String titulli;

    @Column(name = "mesazhi", nullable = false, length = 500)
    private String mesazhi;

    @Column(name = "data_json", length = 1000)
    private String dataJson;

    @Column(name = "eshte_lexuar", nullable = false)
    private boolean eshteLexuar = false;

    @Column(name = "data_leximit")
    private LocalDateTime dataLeximit;

    @Column(name = "data_krijimit", insertable = false, updatable = false)
    private LocalDateTime dataKrijimit;

    public Njoftim() {}

    public Long getId() {
        return id;
    }

    public puntori getPuntori() {
        return puntori;
    }

    public menaxheri getMenaxheri() {
        return menaxheri;
    }

    /** Assigns the notification to a worker and clears any manager recipient. */
    public void setMarresPuntori(puntori puntori) {
        this.puntori = puntori;
        this.menaxheri = null;
    }

    /** Assigns the notification to a manager and clears any worker recipient. */
    public void setMarresMenaxheri(menaxheri menaxheri) {
        this.menaxheri = menaxheri;
        this.puntori = null;
    }

    public String getTipi() {
        return tipi;
    }

    public void setTipi(String tipi) {
        this.tipi = tipi;
    }

    public String getTitulli() {
        return titulli;
    }

    public void setTitulli(String titulli) {
        this.titulli = titulli;
    }

    public String getMesazhi() {
        return mesazhi;
    }

    public void setMesazhi(String mesazhi) {
        this.mesazhi = mesazhi;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    public boolean isEshteLexuar() {
        return eshteLexuar;
    }

    public void setEshteLexuar(boolean eshteLexuar) {
        this.eshteLexuar = eshteLexuar;
    }

    public LocalDateTime getDataLeximit() {
        return dataLeximit;
    }

    public void setDataLeximit(LocalDateTime dataLeximit) {
        this.dataLeximit = dataLeximit;
    }

    public LocalDateTime getDataKrijimit() {
        return dataKrijimit;
    }
}
