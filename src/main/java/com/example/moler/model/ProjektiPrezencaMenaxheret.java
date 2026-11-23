
package com.example.moler.model;

import jakarta.persistence.*;

@Entity
@Table(
    name = "projekti_prezenca_menaxheret",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UQ_PPM_Projekti_Menaxheri",
            columnNames = {"id_projektit", "id_menaxherit"}
        )
    }
)
public class ProjektiPrezencaMenaxheret {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projektit", nullable = false)
    private projekti projekti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_menaxherit", nullable = false)
    private menaxheri menaxheri;

    public ProjektiPrezencaMenaxheret() {
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

    public menaxheri getMenaxheri() {
        return menaxheri;
    }

    public void setMenaxheri(menaxheri menaxheri) {
        this.menaxheri = menaxheri;
    }
}
