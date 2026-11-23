
package com.example.moler.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "materiali")
public class materiali {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "emri", nullable = false, length = 150)
    private String emri;

    @Column(name = "cmimi", nullable = false, precision = 12, scale = 2)
    private BigDecimal cmimi;

    @Column(name = "kategoria", nullable = false, length = 20)
    private String kategoria;

    public materiali() {}

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

    public BigDecimal getCmimi() {
        return cmimi;
    }

    public void setCmimi(BigDecimal cmimi) {
        this.cmimi = cmimi;
    }

    public String getKategoria() {
        return kategoria;
    }

    public void setKategoria(String kategoria) {
        this.kategoria = kategoria;
    }
}
