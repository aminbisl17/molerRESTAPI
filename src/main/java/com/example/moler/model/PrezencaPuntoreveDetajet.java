package com.example.moler.model;

import jakarta.persistence.*;

/**
 * One project worked on within a worker's daily attendance (prezenca_puntoreve).
 */
@Entity
@Table(name = "prezenca_puntoreve_detajet")
public class PrezencaPuntoreveDetajet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_prezenca_puntoreve", nullable = false)
    private prezencapuntoreve prezenca;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projektit", nullable = false)
    private projekti projekti;

    public PrezencaPuntoreveDetajet() {}

    public PrezencaPuntoreveDetajet(prezencapuntoreve prezenca, projekti projekti) {
        this.prezenca = prezenca;
        this.projekti = projekti;
    }

    public Long getId() {
        return id;
    }

    public prezencapuntoreve getPrezenca() {
        return prezenca;
    }

    public void setPrezenca(prezencapuntoreve prezenca) {
        this.prezenca = prezenca;
    }

    public projekti getProjekti() {
        return projekti;
    }

    public void setProjekti(projekti projekti) {
        this.projekti = projekti;
    }
}
