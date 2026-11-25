package com.example.moler.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Prezenca_Puntoreve")
public class prezencapuntoreve {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Projects worked on this day (prezenca_puntoreve_detajet)
    @OneToMany(mappedBy = "prezenca", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrezencaPuntoreveDetajet> detajet = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_puntorit", nullable = false)
    private puntori puntori;

    @Column(name = "dita", nullable = false)
    private LocalDate dita;

    @Column(name = "lloji_orarit", nullable = false, length = 1)
    private String llojiOrarit;

    @Column(name = "statusi", nullable = false, length = 2)
    private String statusi;

    @Column(name = "arsyeja_anulimit")
    private String arsyejaAnulimit;

    @Column(name = "data_anulimit")
    private LocalDateTime dataAnulimit;

    @Column(name = "data_krijimit", insertable = false, updatable = false)
    private LocalDateTime dataKrijimit;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<PrezencaPuntoreveDetajet> getDetajet() {
        return detajet;
    }

    public void shtoProjekt(projekti projekti) {
        detajet.add(new PrezencaPuntoreveDetajet(this, projekti));
    }

    public puntori getPuntori() {
        return puntori;
    }

    public void setPuntori(puntori puntori) {
        this.puntori = puntori;
    }

    public LocalDate getDita() {
        return dita;
    }

    public void setDita(LocalDate dita) {
        this.dita = dita;
    }

    public String getLlojiOrarit() {
        return llojiOrarit;
    }

    public void setLlojiOrarit(String llojiOrarit) {
        this.llojiOrarit = llojiOrarit;
    }

    public String getStatusi() {
        // Column is char(2): SQL Server pads "v"/"c" to "v "/"c ", so trim it
        return statusi == null ? null : statusi.trim();
    }

    public void setStatusi(String statusi) {
        this.statusi = statusi;
    }

    public String getArsyejaAnulimit() {
        return arsyejaAnulimit;
    }

    public void setArsyejaAnulimit(String arsyejaAnulimit) {
        this.arsyejaAnulimit = arsyejaAnulimit;
    }

    public LocalDateTime getDataAnulimit() {
        return dataAnulimit;
    }

    public void setDataAnulimit(LocalDateTime dataAnulimit) {
        this.dataAnulimit = dataAnulimit;
    }

    public LocalDateTime getDataKrijimit() {
        return dataKrijimit;
    }

    public void setDataKrijimit(LocalDateTime dataKrijimit) {
        this.dataKrijimit = dataKrijimit;
    }

    public prezencapuntoreve() {}

    // Generate getters and setters
}