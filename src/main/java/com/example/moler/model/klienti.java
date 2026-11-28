package com.example.moler.model;

import jakarta.persistence.*;

@Entity
@Table(name = "klienti")
public class klienti {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emri", nullable = false, length = 100)
    private String emri;

    @Column(name = "mbiemri", nullable = false, length = 100)
    private String mbiemri;

    @Column(name = "numri_i_telefonit", length = 20)
    private String numriITelefonit;

    @Column(name = "lokacioni", nullable = false, length = 255)
    private String lokacioni;

    public klienti() {}

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

    public String getNumriITelefonit() {
        return numriITelefonit;
    }

    public void setNumriITelefonit(String numriITelefonit) {
        this.numriITelefonit = numriITelefonit;
    }

    public String getLokacioni() {
        return lokacioni;
    }

    public void setLokacioni(String lokacioni) {
        this.lokacioni = lokacioni;
    }
}
