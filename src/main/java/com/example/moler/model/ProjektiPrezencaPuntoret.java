
package com.example.moler.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "projekti_prezenca_puntoret",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UQ_PPP_Projekti_Puntori",
            columnNames = {"id_projektit", "id_puntorit"}
        )
    }
)
public class ProjektiPrezencaPuntoret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projektit", nullable = false)
    private projekti projekti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_puntorit", nullable = false)
    private puntori puntori;

    public ProjektiPrezencaPuntoret() {
    }

    public Long getId() {
        return id;
    }

    public projekti getProjekti() {
        return projekti;
    }

    public void setProjekti(projekti projekti) {
        this.projekti = projekti;
    }

    public puntori getPuntori() {
        return puntori;
    }

    public void setPuntori(puntori puntori) {
        this.puntori = puntori;
    }
}
