
package com.example.moler.repository;

import com.example.moler.model.projekti;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjektiRepository extends JpaRepository<projekti, Long> {

    List<projekti> findByStatusi(String statusi);

    long countByStatusiIn(List<String> statuset);

    List<projekti> findByPrioriteti(String prioriteti);

    List<projekti> findByStatusiProjektit(String statusiProjektit);

    List<projekti> findByStatusiAndStatusiProjektit(
            String statusi,
            String statusiProjektit
    );

    List<projekti> findByEmriContainingIgnoreCase(String emri);

    // Load the client together with the projects (avoids one query per project)
    @Override
    @EntityGraph(attributePaths = "klienti")
    List<projekti> findAll();

    @EntityGraph(attributePaths = "klienti")
    List<projekti> findByKlienti_Id(Long idKlientit);

    boolean existsByKlienti_Id(Long idKlientit);

    @Query(value = """
    SELECT
        p.id AS idProjekti,
        p.emri AS emri,
        p.lokacioni AS lokacioni,
        p.kerkesa AS kerkesa,
        p.statusi AS statusi,
        p.prioriteti AS prioriteti,
        p.statusi_projektit AS statusiProjektit,
        p.ambienti AS ambienti,
        p.data_krijimit AS dataKrijimit,
        p.data_fillimit_te_planifikuar AS dataFillimit,
        p.data_perfundimit_te_planifikuar AS dataPerfundimit,

        ppm.id_menaxherit AS idMenaxherit,
        ppp.id_puntorit AS idPuntorit,

        pm.id AS idProjektiMateriali,
        pm.id_materialit AS idMaterialit,
        m.emri AS emriMaterialit,
        m.cmimi AS cmimiMaterialit,
        m.kategoria AS kategoriaMaterialit,
        pm.sasia AS sasiaMaterialit,
        pm.cmimi_ne_momentin_e_perdorimit AS cmimiNeMomentinEPerdorimit,

        pd.m2 AS m2,
        pd.pagesa AS pagesa,
        pd.pershkrimi AS pershkrimi,
        pd.dita_fillimit AS ditaFillimit,
        pd.dita_anulimit AS ditaAnulimit,
        pd.dita_perfundimit AS ditaPerfundimit,
        pd.pershkrimi_anulimit AS pershkrimiAnulimit

    FROM dbo.projekti p
    LEFT JOIN dbo.projekti_prezenca_menaxheret ppm
        ON ppm.id_projektit = p.id
    LEFT JOIN dbo.projekti_prezenca_puntoret ppp
        ON ppp.id_projektit = p.id
    LEFT JOIN dbo.projekti_materiali pm
        ON pm.id_projektit = p.id
    LEFT JOIN dbo.materiali m
        ON m.id = pm.id_materialit
    LEFT JOIN dbo.projekti_detajet pd
        ON pd.id_projektit = p.id
    WHERE p.id = :id
    """, nativeQuery = true)
List<Object[]> findProjektiWithDetailsById(
        @Param("id") Long id
);
}
