package com.example.moler.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "projekti_detajet")
public class projektidetajet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_projektit", nullable = false, unique = true)
    private projekti projekti;

    @Column(name = "m2", nullable = false, precision = 10, scale = 2)
    private BigDecimal m2;

    @Column(name = "pagesa", nullable = false, precision = 12, scale = 2)
    private BigDecimal pagesa;

    @Column(name = "pershkrimi")
    private String pershkrimi;

    @Column(name = "dita_fillimit")
    private LocalDateTime ditaFillimit;

    @Column(name = "dita_anulimit")
    private LocalDateTime ditaAnulimit;

    @Column(name = "dita_perfundimit")
    private LocalDateTime ditaPerfundimit;

    @Column(name = "pershkrimi_anulimit")
    private String pershkrimiAnulimit;

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

    public BigDecimal getM2() {
        return m2;
    }

    public void setM2(BigDecimal m2) {
        this.m2 = m2;
    }

    public BigDecimal getPagesa() {
        return pagesa;
    }

    public void setPagesa(BigDecimal pagesa) {
        this.pagesa = pagesa;
    }

    public String getPershkrimi() {
        return pershkrimi;
    }

    public void setPershkrimi(String pershkrimi) {
        this.pershkrimi = pershkrimi;
    }

    public LocalDateTime getDitaFillimit() {
        return ditaFillimit;
    }

    public void setDitaFillimit(LocalDateTime ditaFillimit) {
        this.ditaFillimit = ditaFillimit;
    }

    public LocalDateTime getDitaAnulimit() {
        return ditaAnulimit;
    }

    public void setDitaAnulimit(LocalDateTime ditaAnulimit) {
        this.ditaAnulimit = ditaAnulimit;
    }

    public LocalDateTime getDitaPerfundimit() {
        return ditaPerfundimit;
    }

    public void setDitaPerfundimit(LocalDateTime ditaPerfundimit) {
        this.ditaPerfundimit = ditaPerfundimit;
    }

    public String getPershkrimiAnulimit() {
        return pershkrimiAnulimit;
    }

    public void setPershkrimiAnulimit(String pershkrimiAnulimit) {
        this.pershkrimiAnulimit = pershkrimiAnulimit;
    }

    public projektidetajet() {}

    // Generate getters and setters
}