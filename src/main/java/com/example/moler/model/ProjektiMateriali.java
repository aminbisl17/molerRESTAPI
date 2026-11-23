
package com.example.moler.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "projekti_materiali")
public class ProjektiMateriali {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projektit", nullable = false)
    private projekti projekti;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_materialit", nullable = false)
    private materiali materiali;

    @Column(name = "sasia", nullable = false, precision = 12, scale = 2)
    private BigDecimal sasia;

    @Column(name = "cmimi_ne_momentin_e_perdorimit",
            nullable = false, precision = 12, scale = 2)
    private BigDecimal cmimiNeMomentinEPerdorimit;

    public ProjektiMateriali() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public projekti getProjekti() {
        return projekti;
    }

    public void setProjekti(projekti projekti) {
        this.projekti = projekti;
    }

    public materiali getMateriali() {
        return materiali;
    }

    public void setMateriali(materiali materiali) {
        this.materiali = materiali;
    }

    public BigDecimal getSasia() {
        return sasia;
    }

    public void setSasia(BigDecimal sasia) {
        this.sasia = sasia;
    }

    public BigDecimal getCmimiNeMomentinEPerdorimit() {
        return cmimiNeMomentinEPerdorimit;
    }

    public void setCmimiNeMomentinEPerdorimit(BigDecimal cmimiNeMomentinEPerdorimit) {
        this.cmimiNeMomentinEPerdorimit = cmimiNeMomentinEPerdorimit;
    }
}
